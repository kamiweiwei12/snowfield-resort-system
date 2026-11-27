package edu.sysu.snowfield.order.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * order 模块对外端口：创建订单主体、明细与价格快照。
 *
 * <p>booking 编排层在一个事务内调用本端口与 lodging 端口，任一失败整体回滚（ADR-009）。</p>
 */
public interface OrderWritePort {

    /**
     * 在当前事务内锁定一个“用户 + 创建幂等键”命名空间。
     * 同键请求必须先串行化，再读取既有订单，避免并发先查后插竞态。
     */
    void lockCreateIdempotencyKey(long userId, String idempotencyKey);

    /** 创建订单主体，返回生成的 orderId。 */
    long createOrder(NewOrder order);

    /** 创建订单明细，返回 orderItemId。 */
    long createItem(NewItem item);

    /** 批量写价格快照行。 */
    void createSnapshotLines(List<SnapshotLine> lines);

    /** 按用户与幂等键查已有订单（用于幂等重放）。 */
    java.util.Optional<ExistingOrder> findByUserAndIdempotencyKey(long userId, String idempotencyKey);

    /** 取订单当前记录的总额（幂等重放时返回历史金额，不重算）。 */
    java.util.Optional<BigDecimal> findTotalByOrderId(long orderId);

    record NewOrder(
            String orderNo,
            long userId,
            long campgroundId,
            String status,
            BigDecimal totalAmount,
            String idempotencyKey,
            String requestFingerprint,
            OffsetDateTime expiresAt) {
    }

    record NewItem(
            long orderId,
            String itemType,
            String itemNameSnapshot,
            String status,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            String priceExplanationJson) {
    }

    record SnapshotLine(
            long orderItemId,
            java.time.LocalDate serviceDate,
            Long ruleId,
            String ruleNameSnapshot,
            String operationSnapshot,
            BigDecimal ruleValueSnapshot,
            BigDecimal amount) {
    }

    record ExistingOrder(long orderId, String orderNo, String requestFingerprint, String status) {
    }
}
