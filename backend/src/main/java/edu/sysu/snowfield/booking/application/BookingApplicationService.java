package edu.sysu.snowfield.booking.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.sysu.snowfield.catalog.application.CatalogQueryPort;
import edu.sysu.snowfield.catalog.application.ResourceSnapshot;
import edu.sysu.snowfield.common.web.BusinessException;
import edu.sysu.snowfield.common.web.ErrorCode;
import edu.sysu.snowfield.lodging.application.LodgingConflictException;
import edu.sysu.snowfield.lodging.application.LodgingReservationPort;
import edu.sysu.snowfield.order.application.OrderWritePort;
import edu.sysu.snowfield.pricing.application.PricingQueryPort;
import edu.sysu.snowfield.pricing.domain.PriceCalculator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 预订编排中心（ADR-009）：唯一开启跨模块事务的地方。
 *
 * <p>一次创建订单在**单个事务**内完成：幂等检查 → 重算价格 → 建订单主体 → 逐行建明细 →
 * 写价格快照 → 建住宿占用。任一住宿占用冲突或校验失败都会使整单回滚，不产生孤儿数据
 * （契约 §2：混合购物车在单个事务内原子创建）。</p>
 *
 * <p>并发正确性不依赖「先查后写」：住宿重叠由数据库排斥约束裁决，
 * 本服务只负责把冲突翻译为 {@code BOOKING_CONFLICT}。</p>
 */
@Service
public class BookingApplicationService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int HOLD_MINUTES = 15;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final CatalogQueryPort catalogQuery;
    private final PricingQueryPort pricingQuery;
    private final OrderWritePort orderWrite;
    private final LodgingReservationPort lodgingReservation;
    private final OrderNumberGenerator orderNumberGenerator;

    public BookingApplicationService(CatalogQueryPort catalogQuery,
                                     PricingQueryPort pricingQuery,
                                     OrderWritePort orderWrite,
                                     LodgingReservationPort lodgingReservation,
                                     OrderNumberGenerator orderNumberGenerator) {
        this.catalogQuery = catalogQuery;
        this.pricingQuery = pricingQuery;
        this.orderWrite = orderWrite;
        this.lodgingReservation = lodgingReservation;
        this.orderNumberGenerator = orderNumberGenerator;
    }

    /**
     * 创建住宿订单。
     *
     * @throws BusinessException {@code IDEMPOTENCY_CONFLICT} 同键不同请求；
     *                           {@code RESOURCE_NOT_FOUND} 资源不可售；
     *                           {@code INVALID_TIME_RANGE} 起止非法；
     *                           {@code PRICE_CHANGED} 用户看到的报价已过期；
     *                           {@code PRICE_RULE_CONFLICT} 价格规则冲突；
     *                           {@code BOOKING_CONFLICT} 时段已被占用
     */
    @Transactional
    public CreatedLodgingOrder createLodgingOrder(CreateLodgingOrderCommand command) {
        if (command.lodgings() == null || command.lodgings().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "至少需要一个住宿项");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key 不能为空");
        }
        if (command.lodgings().stream().anyMatch(java.util.Objects::isNull)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "住宿项不能为空");
        }

        String fingerprint = command.fingerprint();

        // 1. 幂等：先按 (userId,key) 取得事务级锁，消除并发“先查后插”竞态；
        // 同键同请求返回原结果，同键不同请求冲突。
        orderWrite.lockCreateIdempotencyKey(command.userId(), command.idempotencyKey());
        var existing = orderWrite.findByUserAndIdempotencyKey(command.userId(), command.idempotencyKey());
        if (existing.isPresent()) {
            var order = existing.get();
            if (!order.requestFingerprint().equals(fingerprint)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT,
                        "相同幂等键对应了不同的请求内容");
            }
            return new CreatedLodgingOrder(order.orderId(), order.orderNo(), order.status(),
                    currentTotal(order.orderId()), true);
        }

        // 2. 校验并重算价格（服务端权威，客户端 expectedTotal 只用于发现价格变化）
        List<PricedLine> pricedLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Long campgroundId = null;
        Set<Long> selectedResourceIds = new HashSet<>();

        for (var line : command.lodgings()) {
            if (!selectedResourceIds.add(line.resourceUnitId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "同一创建请求不能重复选择资源：" + line.resourceUnitId());
            }
            ResourceSnapshot resource = catalogQuery.findResourceUnit(line.resourceUnitId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "资源不存在或不可售：" + line.resourceUnitId()));

            if (campgroundId == null) {
                campgroundId = resource.campgroundId();
            } else if (!campgroundId.equals(resource.campgroundId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "一个订单内的全部项目必须属于同一营地");
            }

            validatePeriod(line);
            if (!catalogQuery.supportsStayMode(resource.resourceTypeId(), line.stayMode())) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_OPEN,
                        "该资源类型不支持住宿模式：" + line.stayMode());
            }
            if (catalogQuery.hasActiveMaintenanceOverlap(
                    line.resourceUnitId(), line.startAt(), line.endAt())) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_OPEN,
                        "所选资源在请求时段内处于维修状态：" + line.resourceUnitId());
            }
            if (line.guestCount() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "入住人数必须大于 0");
            }
            if (line.guestCount() > resource.capacity()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "入住人数超过该资源容量：" + resource.capacity());
            }

            PricedLine priced = priceLine(line, resource);
            pricedLines.add(priced);
            total = total.add(priced.subtotal());
        }

        total = total.setScale(2, RoundingMode.HALF_UP);

        if (command.expectedTotal() != null && !command.expectedTotal().isBlank()) {
            BigDecimal expected;
            try {
                expected = new BigDecimal(command.expectedTotal()).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "expectedTotal 必须是十进制金额");
            }
            if (expected.compareTo(total) != 0) {
                throw new BusinessException(ErrorCode.PRICE_CHANGED,
                        "价格已变化，请确认最新报价",
                        java.util.Map.of("newTotal", total.toPlainString()));
            }
        }

        // 3. 建订单主体
        OffsetDateTime expiresAt = OffsetDateTime.now(BUSINESS_ZONE).plusMinutes(HOLD_MINUTES);
        String orderNo = orderNumberGenerator.next();
        long orderId = orderWrite.createOrder(new OrderWritePort.NewOrder(
                orderNo, command.userId(), campgroundId, "PENDING_PAYMENT", total,
                command.idempotencyKey(), fingerprint, expiresAt));

        // 4. 逐行建明细、快照与占用（任一冲突整单回滚）
        for (PricedLine priced : pricedLines) {
            long itemId = orderWrite.createItem(new OrderWritePort.NewItem(
                    orderId, "LODGING", priced.itemName(), "HELD", 1,
                    priced.unitPrice(), priced.subtotal(), priced.explanationJson()));

            orderWrite.createSnapshotLines(priced.snapshotLines(itemId));

            try {
                lodgingReservation.hold(itemId, priced.line().resourceUnitId(), priced.line().stayMode(),
                        priced.line().startAt(), priced.line().endAt(), priced.line().guestCount());
            } catch (LodgingConflictException conflict) {
                throw new BusinessException(ErrorCode.BOOKING_CONFLICT, conflict.getMessage());
            }
        }

        return new CreatedLodgingOrder(orderId, orderNo, "PENDING_PAYMENT", total.toPlainString(), false);
    }

    private void validatePeriod(CreateLodgingOrderCommand.LodgingLine line) {
        if (line.startAt() == null || line.endAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE, "起止时间不能为空");
        }
        // 半开区间 [start, end)：end 必须严格晚于 start
        if (!line.endAt().isAfter(line.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE, "结束时间必须晚于开始时间");
        }
    }

    private PricedLine priceLine(CreateLodgingOrderCommand.LodgingLine line, ResourceSnapshot resource) {
        List<LocalDate> dates = CreateLodgingOrderCommand.serviceDates(
                line.stayMode(), line.startAt(), line.endAt(), BUSINESS_ZONE);
        if (dates.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE, "住宿区间必须至少覆盖一个服务日");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderWritePort.SnapshotLine> snapshots = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        for (LocalDate date : dates) {
            List<PriceCalculator.Rule> rules = pricingQuery.findRulesFor(
                    resource.campgroundId(), resource.resourceTypeId(), line.stayMode(), date);

            PriceCalculator.Result result = PriceCalculator.priceFor(resource.basePrice(), rules);
            if (result.hasConflict()) {
                throw new BusinessException(ErrorCode.PRICE_RULE_CONFLICT,
                        "价格规则冲突：日期 " + date + " 在优先级 " + result.conflict().priority()
                                + " 上存在多条规则 " + result.conflict().ruleNames(),
                        java.util.Map.of("serviceDate", date.toString()));
            }

            subtotal = subtotal.add(result.unitPrice());
            explanations.add(date + "：" + result.explanation(date));

            Long ruleId = result.winner() == null ? null : result.winner().ruleId();
            snapshots.add(new OrderWritePort.SnapshotLine(
                    -1, // 占位，建明细后回填
                    date,
                    ruleId,
                    result.winner() == null ? null : result.winner().name(),
                    result.winner() == null ? null : result.winner().operation(),
                    result.winner() == null ? null : result.winner().value(),
                    result.unitPrice()));
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        String explanationJson = toJson(explanations);

        return new PricedLine(line, resource, resource.name(),
                subtotal, subtotal, explanationJson, snapshots);
    }

    private String currentTotal(long orderId) {
        // 幂等重放时直接返回既有订单记录中的总额（不再重算，避免规则变更影响历史单）
        return orderWrite.findTotalByOrderId(orderId).map(BigDecimal::toPlainString).orElse("0.00");
    }

    private static String toJson(List<String> parts) {
        try {
            return JSON.writeValueAsString(java.util.Map.of("explanation", parts));
        } catch (JacksonException exception) {
            throw new IllegalStateException("价格说明 JSON 序列化失败", exception);
        }
    }

    /** 计价中间结果。 */
    private record PricedLine(
            CreateLodgingOrderCommand.LodgingLine line,
            ResourceSnapshot resource,
            String itemName,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            String explanationJson,
            List<OrderWritePort.SnapshotLine> pendingSnapshots) {

        List<OrderWritePort.SnapshotLine> snapshotLines(long orderItemId) {
            return pendingSnapshots.stream()
                    .map(s -> new OrderWritePort.SnapshotLine(
                            orderItemId, s.serviceDate(), s.ruleId(), s.ruleNameSnapshot(),
                            s.operationSnapshot(), s.ruleValueSnapshot(), s.amount()))
                    .toList();
        }
    }
}
