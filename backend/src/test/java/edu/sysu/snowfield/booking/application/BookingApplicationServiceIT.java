package edu.sysu.snowfield.booking.application;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.sysu.snowfield.common.web.BusinessException;
import edu.sysu.snowfield.common.web.ErrorCode;
import edu.sysu.snowfield.support.AbstractPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BK-02：创建住宿订单的完整事务路径测试。
 *
 * <p>验证契约要求的「混合购物车在单个事务内原子创建」：任一住宿冲突或校验失败，
 * 整单回滚，不留下订单、明细、快照或占用中的任何孤儿数据。</p>
 */
class BookingApplicationServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private BookingApplicationService bookingService;

    private long userId;
    private long campgroundId;
    private long resourceTypeId;
    private long cabinA;
    private long cabinB;

    @BeforeEach
    void setUp() throws SQLException {
        execute("""
                TRUNCATE TABLE user_role, app_user, campground, season, resource_type, holiday, price_rule,
                               package_offer, lodging_reservation
                RESTART IDENTITY CASCADE
                """);
        execute("INSERT INTO app_user (email, password_hash, display_name) VALUES ('booker@example.com', 'x', '预订人')");
        userId = scalar("SELECT user_id FROM app_user WHERE email = 'booker@example.com'");
        execute("INSERT INTO campground (code, name, timezone, currency) VALUES ('main', '雪野', 'Asia/Shanghai', 'CNY')");
        campgroundId = scalar("SELECT campground_id FROM campground WHERE code = 'main'");
        execute("""
                INSERT INTO resource_type (campground_id, code, name, kind, base_capacity, base_price)
                VALUES (%d, 'CABIN', '木屋', 'CABIN', 4, 1280.00)
                """.formatted(campgroundId));
        resourceTypeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("INSERT INTO resource_type_stay_mode (resource_type_id, stay_mode) VALUES (%d, 'OVERNIGHT')"
                .formatted(resourceTypeId));
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'A', '甲', 4)"
                .formatted(resourceTypeId));
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'B', '乙', 4)"
                .formatted(resourceTypeId));
        cabinA = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'A'");
        cabinB = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'B'");
    }

    @Test
    void createsOrderWithItemsSnapshotsAndReservationAtomically() throws SQLException {
        var command = command("idem-1", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null);

        CreatedLodgingOrder created = bookingService.createLodgingOrder(command);

        assertThat(created.replayed()).isFalse();
        assertThat(created.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(created.orderNo()).startsWith("SO");

        // 订单主体
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE order_id = " + created.orderId())).isEqualTo(1);
        // 明细：1 晚住宿 = 1 个订单项
        assertThat(scalar("SELECT count(*) FROM order_item WHERE order_id = " + created.orderId())).isEqualTo(1);
        // 价格快照：2 个服务日（10、11 日）
        assertThat(scalar("""
                SELECT count(*) FROM price_snapshot_line s
                JOIN order_item i ON i.order_item_id = s.order_item_id
                WHERE i.order_id = %d
                """.formatted(created.orderId()))).isEqualTo(2);
        // 占用：1 条 HELD
        assertThat(scalar("SELECT count(*) FROM lodging_reservation WHERE occupancy_status = 'HELD'")).isEqualTo(1);
        // 金额 = 基础价 × 2 晚
        assertThat(new BigDecimal(created.totalAmount())).isEqualByComparingTo("2560.00");
    }

    @Test
    void multipleRoomsProduceSeparateItemsAndReservations() throws SQLException {
        var command = twoLineCommand("idem-2",
                line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"),
                line(cabinB, "2027-01-10T15:00+08", "2027-01-12T12:00+08"));

        CreatedLodgingOrder created = bookingService.createLodgingOrder(command);

        assertThat(scalar("SELECT count(*) FROM order_item WHERE order_id = " + created.orderId()))
                .as("两间房必须生成两个独立订单项（契约：每间住宿对应数量为 1 的订单项）")
                .isEqualTo(2);
        assertThat(scalar("""
                SELECT count(*) FROM lodging_reservation r
                JOIN order_item i ON i.order_item_id = r.order_item_id
                WHERE i.order_id = %d
                """.formatted(created.orderId()))).isEqualTo(2);
        assertThat(new BigDecimal(created.totalAmount())).isEqualByComparingTo("5120.00");
    }

    @Test
    void sameIdempotencyKeyWithSameRequestReplaysExistingOrder() throws SQLException {
        var command = command("idem-3", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null);

        CreatedLodgingOrder first = bookingService.createLodgingOrder(command);
        CreatedLodgingOrder second = bookingService.createLodgingOrder(command);

        assertThat(second.replayed()).isTrue();
        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(second.orderNo()).isEqualTo(first.orderNo());
        assertThat(scalar("SELECT count(*) FROM customer_order"))
                .as("幂等重放不得新建订单").isEqualTo(1);
    }

    @Test
    void sameIdempotencyKeyWithDifferentRequestConflicts() {
        bookingService.createLodgingOrder(
                command("idem-4", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null));

        var different = command("idem-4", line(cabinB, "2027-02-10T15:00+08", "2027-02-12T12:00+08"), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(different))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Test
    void overlappingReservationRollsBackWholeOrder() throws SQLException {
        // 先占住 A 房 1/10–1/12
        bookingService.createLodgingOrder(
                command("idem-5", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null));
        long ordersBefore = scalar("SELECT count(*) FROM customer_order");

        // 再下第二个订单：A 房冲突 + B 房可用 —— 整单必须回滚
        var conflicting = twoLineCommand("idem-6",
                line(cabinB, "2027-01-10T15:00+08", "2027-01-12T12:00+08"),
                line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"));

        assertThatThrownBy(() -> bookingService.createLodgingOrder(conflicting))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.BOOKING_CONFLICT));

        assertThat(scalar("SELECT count(*) FROM customer_order"))
                .as("冲突导致整单回滚，不得新建订单").isEqualTo(ordersBefore);
        assertThat(scalar("SELECT count(*) FROM order_item"))
                .as("不得留下孤儿订单项（含已成功的 B 房行）").isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation"))
                .as("不得留下 B 房的孤儿占用").isEqualTo(1);
    }

    @Test
    void priceChangedWhenExpectedTotalMismatches() throws SQLException {
        var command = command("idem-7", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), "9999.00");

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PRICE_CHANGED);
                    assertThat(be.getDetails()).containsKey("newTotal");
                });

        assertThat(scalar("SELECT count(*) FROM customer_order"))
                .as("PRICE_CHANGED 不得创建订单").isZero();
    }

    @Test
    void priceRuleConflictIsReported() throws SQLException {
        // 同优先级两条规则 → 契约要求返回 PRICE_RULE_CONFLICT，不得静默选胜者
        insertRule("SEASON", "MULTIPLIER", "1.20", 100);
        insertRule("HOLIDAY", "MULTIPLIER", "1.50", 100);

        var command = command("idem-8", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRICE_RULE_CONFLICT));
        assertThat(scalar("SELECT count(*) FROM customer_order")).isZero();
    }

    @Test
    void higherPriorityRuleWins() throws SQLException {
        insertRule("SEASON", "MULTIPLIER", "1.20", 10);
        insertRule("HOLIDAY", "MULTIPLIER", "1.50", 100);

        CreatedLodgingOrder created = bookingService.createLodgingOrder(
                command("idem-9", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null));

        // 1280 × 1.50 × 2 晚
        assertThat(new BigDecimal(created.totalAmount())).isEqualByComparingTo("3840.00");
    }

    @Test
    void invalidTimeRangeIsRejected() {
        var command = command("idem-10", line(cabinA, "2027-01-12T15:00+08", "2027-01-10T12:00+08"), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TIME_RANGE));
    }

    @Test
    void unknownResourceIsRejected() {
        var command = command("idem-11", line(999999, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void guestCountAboveCapacityIsRejected() {
        var command = new CreateLodgingOrderCommand(userId, "idem-12", List.of(
                new CreateLodgingOrderCommand.LodgingLine(
                        cabinA, "OVERNIGHT",
                        OffsetDateTime.parse("2027-01-10T15:00+08:00"),
                        OffsetDateTime.parse("2027-01-12T12:00+08:00"),
                        99)), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void unsupportedStayModeIsRejectedBeforeOrderCreation() throws SQLException {
        var command = command("idem-mode-unsupported",
                line(cabinA, "DAY_PASS", "2027-01-10T09:00+08", "2027-01-10T18:00+08"), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_OPEN));
        assertThat(scalar("SELECT count(*) FROM customer_order")).isZero();
    }

    @Test
    void dayPassOnOneBusinessDayProducesOnePriceSnapshot() throws SQLException {
        enableDayPass();
        var command = command("idem-day-pass",
                line(cabinA, "DAY_PASS", "2027-01-10T09:00+08", "2027-01-10T18:00+08"), null);

        CreatedLodgingOrder created = bookingService.createLodgingOrder(command);

        assertThat(new BigDecimal(created.totalAmount())).isEqualByComparingTo("1280.00");
        assertThat(scalar("SELECT count(*) FROM price_snapshot_line")).isEqualTo(1);
        assertThat(scalarString("SELECT service_date::text FROM price_snapshot_line"))
                .isEqualTo("2027-01-10");
    }

    @Test
    void dayPassServiceDateUsesShanghaiBusinessZoneInsteadOfInputOffset() throws SQLException {
        enableDayPass();
        // 两个输入偏移下分别显示 1/10 与 1/11，但换算到上海均为 1/11 08:30–19:00。
        var command = command("idem-day-pass-zone",
                line(cabinA, "DAY_PASS", "2027-01-10T16:30-08", "2027-01-11T03:00-08"), null);

        bookingService.createLodgingOrder(command);

        assertThat(scalarString("SELECT service_date::text FROM price_snapshot_line"))
                .isEqualTo("2027-01-11");
    }

    @Test
    void concurrentSameIdempotencyKeyAndRequestReplaysSingleOrder() throws Exception {
        int workers = 8;
        var command = command("idem-concurrent-same",
                line(cabinA, "2027-04-10T15:00+08", "2027-04-12T12:00+08"), null);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CreatedLodgingOrder>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    return bookingService.createLodgingOrder(command);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<CreatedLodgingOrder> results = new ArrayList<>();
            for (Future<CreatedLodgingOrder> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }

            assertThat(results).extracting(CreatedLodgingOrder::orderId).containsOnly(results.getFirst().orderId());
            assertThat(results).filteredOn(result -> !result.replayed()).hasSize(1);
            assertThat(results).filteredOn(CreatedLodgingOrder::replayed).hasSize(workers - 1);
            assertThat(scalar("SELECT count(*) FROM customer_order")).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentSameIdempotencyKeyWithDifferentRequestsReturnsStableConflict() throws Exception {
        var first = command("idem-concurrent-different",
                line(cabinA, "2027-05-10T15:00+08", "2027-05-12T12:00+08"), null);
        var second = command("idem-concurrent-different",
                line(cabinB, "2027-06-10T15:00+08", "2027-06-12T12:00+08"), null);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> firstFuture = executor.submit(() -> attemptCreate(first, ready, start));
            Future<Object> secondFuture = executor.submit(() -> attemptCreate(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> outcomes = List.of(
                    firstFuture.get(20, TimeUnit.SECONDS),
                    secondFuture.get(20, TimeUnit.SECONDS));

            assertThat(outcomes).filteredOn(CreatedLodgingOrder.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(outcome -> outcome instanceof BusinessException exception
                    && exception.getErrorCode() == ErrorCode.IDEMPOTENCY_CONFLICT).hasSize(1);
            assertThat(scalar("SELECT count(*) FROM customer_order")).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void duplicateResourceInOneRequestIsRejectedBeforeWritingOrder() throws SQLException {
        var duplicate = twoLineCommand("idem-duplicate-resource",
                line(cabinA, "2027-07-10T15:00+08", "2027-07-12T12:00+08"),
                line(cabinA, "2027-08-10T15:00+08", "2027-08-12T12:00+08"));

        assertThatThrownBy(() -> bookingService.createLodgingOrder(duplicate))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(scalar("SELECT count(*) FROM customer_order")).isZero();
    }

    @Test
    void blankIdempotencyKeyIsRejected() {
        var command = command(" ", line(cabinA, "2027-01-10T15:00+08", "2027-01-12T12:00+08"), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void nonPositiveGuestCountIsRejectedBeforeDatabaseConstraint() {
        var command = new CreateLodgingOrderCommand(userId, "idem-zero-guests", List.of(
                new CreateLodgingOrderCommand.LodgingLine(
                        cabinA, "OVERNIGHT",
                        OffsetDateTime.parse("2027-01-10T15:00+08:00"),
                        OffsetDateTime.parse("2027-01-12T12:00+08:00"),
                        0)), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void activeMaintenanceOverlapRejectsOrderWithoutResidue() throws SQLException {
        execute("""
                INSERT INTO resource_maintenance (resource_unit_id, period, status, reason)
                VALUES (%d, tstzrange('2027-09-11T00:00+08', '2027-09-13T00:00+08', '[)'),
                        'IN_PROGRESS', '屋顶维修')
                """.formatted(cabinA));
        var command = command("idem-maintenance-blocked",
                line(cabinA, "2027-09-10T15:00+08", "2027-09-12T12:00+08"), null);

        assertThatThrownBy(() -> bookingService.createLodgingOrder(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_OPEN));
        assertThat(scalar("SELECT count(*) FROM customer_order")).isZero();
        assertThat(scalar("SELECT count(*) FROM lodging_reservation")).isZero();
    }

    @Test
    void completedAndCancelledMaintenanceDoNotBlockOrder() throws SQLException {
        execute("""
                INSERT INTO resource_maintenance (resource_unit_id, period, status, reason)
                VALUES
                  (%d, tstzrange('2027-10-10T00:00+08', '2027-10-13T00:00+08', '[)'), 'COMPLETED', '已完成'),
                  (%d, tstzrange('2027-10-10T00:00+08', '2027-10-13T00:00+08', '[)'), 'CANCELLED', '已取消')
                """.formatted(cabinA, cabinA));
        var command = command("idem-maintenance-finished",
                line(cabinA, "2027-10-10T15:00+08", "2027-10-12T12:00+08"), null);

        CreatedLodgingOrder created = bookingService.createLodgingOrder(command);

        assertThat(created.replayed()).isFalse();
        assertThat(scalar("SELECT count(*) FROM lodging_reservation")).isEqualTo(1);
    }

    @Test
    void jacksonSerializesControlCharactersAndJsonbStoresRuleExplanation() throws SQLException {
        String ruleName = "跨行规则\n含\"引号\"、\\反斜杠与中文";
        insertNamedRule(ruleName, "FIXED", "999.00", 100);
        var command = command("idem-json-control",
                line(cabinA, "2027-11-10T15:00+08", "2027-11-11T12:00+08"), null);

        CreatedLodgingOrder created = bookingService.createLodgingOrder(command);

        String decoded = scalarString("""
                SELECT i.price_explanation #>> '{explanation,0}'
                FROM order_item i
                WHERE i.order_id = %d
                """.formatted(created.orderId()));
        assertThat(decoded).contains(ruleName);
        assertThat(scalar("""
                SELECT count(*) FROM order_item
                WHERE order_id = %d AND jsonb_typeof(price_explanation) = 'object'
                """.formatted(created.orderId()))).isEqualTo(1);
    }

    @Test
    void itemNameSnapshotUsesActualResourceUnitName() throws SQLException {
        var command = command("idem-real-resource-name",
                line(cabinA, "2027-12-10T15:00+08", "2027-12-11T12:00+08"), null);

        CreatedLodgingOrder created = bookingService.createLodgingOrder(command);

        assertThat(scalarString("SELECT item_name_snapshot FROM order_item WHERE order_id = "
                + created.orderId())).isEqualTo("甲");
    }

    // ---- 辅助 ----

    private void insertRule(String ruleType, String operation, String value, int priority) throws SQLException {
        execute("""
                INSERT INTO price_rule (campground_id, rule_type, operation, value, priority, valid_from, valid_to)
                VALUES (%d, '%s', '%s', %s, %d, '2026-01-01', '2027-12-31')
                """.formatted(campgroundId, ruleType, operation, value, priority));
    }

    private void insertNamedRule(String description, String operation, String value, int priority) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO price_rule
                         (campground_id, description, rule_type, operation, value, priority, valid_from, valid_to)
                     VALUES (?, ?, 'MANUAL_OVERRIDE', ?, ?, ?, '2026-01-01', '2027-12-31')
                     """)) {
            statement.setLong(1, campgroundId);
            statement.setString(2, description);
            statement.setString(3, operation);
            statement.setBigDecimal(4, new BigDecimal(value));
            statement.setInt(5, priority);
            statement.executeUpdate();
        }
    }

    private void enableDayPass() throws SQLException {
        execute("INSERT INTO resource_type_stay_mode (resource_type_id, stay_mode) VALUES (%d, 'DAY_PASS')"
                .formatted(resourceTypeId));
    }

    private Object attemptCreate(CreateLodgingOrderCommand command,
                                 CountDownLatch ready,
                                 CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await(10, TimeUnit.SECONDS);
        try {
            return bookingService.createLodgingOrder(command);
        } catch (BusinessException exception) {
            return exception;
        }
    }

    private CreateLodgingOrderCommand command(String idemKey,
                                              CreateLodgingOrderCommand.LodgingLine line,
                                              String expectedTotal) {
        return new CreateLodgingOrderCommand(userId, idemKey, List.of(line), expectedTotal);
    }

    private CreateLodgingOrderCommand twoLineCommand(String idemKey,
                                                     CreateLodgingOrderCommand.LodgingLine a,
                                                     CreateLodgingOrderCommand.LodgingLine b) {
        return new CreateLodgingOrderCommand(userId, idemKey, List.of(a, b), null);
    }

    private CreateLodgingOrderCommand.LodgingLine line(long unitId, String start, String end) {
        return line(unitId, "OVERNIGHT", start, end);
    }

    private CreateLodgingOrderCommand.LodgingLine line(long unitId, String stayMode, String start, String end) {
        return new CreateLodgingOrderCommand.LodgingLine(
                unitId, stayMode, OffsetDateTime.parse(start), OffsetDateTime.parse(end), 2);
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long scalar(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private String scalarString(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
