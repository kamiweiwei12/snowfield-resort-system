package edu.sysu.snowfield.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单、支付与价格快照迁移（V4）的约束测试，对应任务板 DB-04 的验收标准。
 *
 * <p>重点验证契约中「数据库负责不可绕过的完整性」的两条硬约束：
 * 幂等键作用域，以及一张订单至多一条 {@code SUCCEEDED} 支付。</p>
 */
class OrderAndPaymentConstraintIT extends AbstractPostgresIntegrationTest {

    private long userId;
    private long campgroundId;

    @BeforeEach
    void setUp() throws SQLException {
        execute("TRUNCATE TABLE user_role, app_user, campground, package_offer, price_rule RESTART IDENTITY CASCADE");
        execute("INSERT INTO app_user (email, password_hash, display_name) VALUES ('buyer@example.com', 'x', '买家')");
        userId = scalar("SELECT user_id FROM app_user WHERE email = 'buyer@example.com'");
        execute("INSERT INTO campground (code, name, timezone, currency) VALUES ('main', '雪野', 'Asia/Shanghai', 'CNY')");
        campgroundId = scalar("SELECT campground_id FROM campground WHERE code = 'main'");
    }

    // ---- 订单主体 ----

    @Test
    void orderNoIsUnique() throws SQLException {
        insertOrder("SO-0001", "key-1", "NEW");
        assertThatThrownBy(() -> insertOrder("SO-0001", "key-2", "NEW"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_order_no");
    }

    @Test
    void createIdempotencyKeyIsScopedToUser() throws SQLException {
        // 契约：创建键作用域为 (user_id, key)。同一用户复键必须冲突。
        insertOrder("SO-0002", "same-key", "NEW");
        assertThatThrownBy(() -> insertOrder("SO-0002-b", "same-key", "NEW"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_order_user_idempotency");
    }

    @Test
    void createIdempotencyKeyCanRepeatAcrossUsers() throws SQLException {
        // 契约：不同用户可使用相同键，命名空间彼此独立。
        execute("INSERT INTO app_user (email, password_hash, display_name) VALUES ('other@example.com', 'x', '另一个人')");
        long otherUserId = scalar("SELECT user_id FROM app_user WHERE email = 'other@example.com'");

        insertOrder("SO-0003", "shared-key", "NEW");
        assertThatCode(() -> insertOrderForUser(otherUserId, "SO-0004", "shared-key", "NEW"))
                .as("不同用户使用同一幂等键不得冲突")
                .doesNotThrowAnyException();
    }

    @Test
    void orderRejectsUnknownStatus() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO customer_order (order_no, user_id, campground_id, status,
                                            create_idempotency_key, create_request_fingerprint, expires_at)
                VALUES ('SO-0005', %d, %d, 'PAID', 'k5', 'fp', now() + interval '15 min')
                """.formatted(userId, campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_order_status");
    }

    @Test
    void orderRejectsNegativeTotal() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO customer_order (order_no, user_id, campground_id, total_amount,
                                            create_idempotency_key, create_request_fingerprint, expires_at)
                VALUES ('SO-0006', %d, %d, -1.00, 'k6', 'fp', now() + interval '15 min')
                """.formatted(userId, campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_order_total_amount");
    }

    @Test
    void orderRejectsNonCnyCurrency() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO customer_order (order_no, user_id, campground_id, currency,
                                            create_idempotency_key, create_request_fingerprint, expires_at)
                VALUES ('SO-0007', %d, %d, 'USD', 'k7', 'fp', now() + interval '15 min')
                """.formatted(userId, campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_order_currency");
    }

    // ---- 订单明细 ----

    @Test
    void orderItemRejectsUnknownType() throws SQLException {
        long orderId = insertOrder("SO-0010", "k10", "NEW");
        assertThatThrownBy(() -> insertItem(orderId, "HOTEL", 1))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_order_item_type");
    }

    @Test
    void orderItemRejectsNonPositiveQuantity() throws SQLException {
        long orderId = insertOrder("SO-0011", "k11", "NEW");
        assertThatThrownBy(() -> insertItem(orderId, "LODGING", 0))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_order_item_quantity");
    }

    @Test
    void orderItemAcceptsAllContractTypes() throws SQLException {
        long orderId = insertOrder("SO-0012", "k12", "NEW");
        for (String type : new String[]{"LODGING", "SKI_TICKET", "SKI_LESSON", "LIFT_SLOT", "EQUIPMENT_RENTAL"}) {
            String t = type;
            assertThatCode(() -> insertItem(orderId, t, 1)).doesNotThrowAnyException();
        }
    }

    @Test
    void deletingOrderCascadesToItems() throws SQLException {
        long orderId = insertOrder("SO-0013", "k13", "NEW");
        insertItem(orderId, "LODGING", 1);
        execute("DELETE FROM customer_order WHERE order_id = " + orderId);
        assertThat(scalar("SELECT count(*) FROM order_item WHERE order_id = " + orderId)).isZero();
    }

    // ---- 支付 ----

    @Test
    void paymentIdempotencyKeyIsScopedToOrder() throws SQLException {
        long order1 = insertOrder("SO-0020", "k20", "NEW");
        long order2 = insertOrder("SO-0021", "k21", "NEW");

        insertPayment(order1, "P-1", "SUCCEEDED", "pay-key", true);
        assertThatCode(() -> insertPayment(order2, "P-2", "SUCCEEDED", "pay-key", true))
                .as("不同订单可使用同一支付幂等键")
                .doesNotThrowAnyException();
    }

    @Test
    void paymentIdempotencyKeyCannotRepeatWithinOrder() throws SQLException {
        long orderId = insertOrder("SO-0022", "k22", "NEW");
        insertPayment(orderId, "P-3", "FAILED", "pay-key", false);
        assertThatThrownBy(() -> insertPayment(orderId, "P-4", "FAILED", "pay-key", false))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_payment_order_idempotency");
    }

    @Test
    void onlyOneSucceededPaymentPerOrder() throws SQLException {
        // 契约核心：部分唯一索引 uq_payment_succeeded_per_order 在数据库层兜底并发双成功。
        long orderId = insertOrder("SO-0023", "k23", "NEW");
        insertPayment(orderId, "P-5", "SUCCEEDED", "key-a", true);

        assertThatThrownBy(() -> insertPayment(orderId, "P-6", "SUCCEEDED", "key-b", true))
                .as("一张订单至多一条 SUCCEEDED 支付")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_payment_succeeded_per_order");
    }

    @Test
    void multipleFailedPaymentsAreAllowed() throws SQLException {
        // 契约：失败后可使用新键重试，失败记录可多条。
        long orderId = insertOrder("SO-0024", "k24", "NEW");
        insertPayment(orderId, "P-7", "FAILED", "fail-1", false);
        insertPayment(orderId, "P-8", "FAILED", "fail-2", false);
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + orderId + " AND status = 'FAILED'"))
                .isEqualTo(2);
    }

    @Test
    void paymentRejectsRealMethod() throws SQLException {
        // AGENTS.md §10：禁止真实收款，method 恒为 SIMULATED。
        long orderId = insertOrder("SO-0025", "k25", "NEW");
        assertThatThrownBy(() -> execute("""
                INSERT INTO payment (order_id, payment_no, status, amount, method, idempotency_key,
                                     request_fingerprint, paid_at)
                VALUES (%d, 'P-9', 'SUCCEEDED', 100.00, 'ALIPAY', 'm1', 'fp', now())
                """.formatted(orderId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_payment_method");
    }

    @Test
    void succeededPaymentRequiresPaidAt() throws SQLException {
        long orderId = insertOrder("SO-0026", "k26", "NEW");
        assertThatThrownBy(() -> insertPayment(orderId, "P-10", "SUCCEEDED", "nopa", false))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_payment_succeeded_has_paid_at");
    }

    // ---- 套餐 ----

    @Test
    void packageDiscountCannotExceedSubtotal() throws SQLException {
        long orderId = insertOrder("SO-0030", "k30", "NEW");
        execute("INSERT INTO package_offer (campground_id, code, name, pricing_mode, pricing_value, valid_from, valid_to) "
                + "VALUES (%d, 'PKG-1', '滑雪套餐', 'DISCOUNT_AMOUNT', 100.00, '2026-01-01', '2026-12-31')".formatted(campgroundId));
        long offerId = scalar("SELECT package_offer_id FROM package_offer WHERE code = 'PKG-1'");

        assertThatThrownBy(() -> execute("""
                INSERT INTO order_package (order_id, package_offer_id, package_code_snapshot, package_name_snapshot,
                                           pricing_mode_snapshot, pricing_value_snapshot, discount_amount, subtotal_before_discount)
                VALUES (%d, %d, 'PKG-1', '滑雪套餐', 'DISCOUNT_AMOUNT', 100.00, 500.00, 300.00)
                """.formatted(orderId, offerId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_order_package_discount_within_subtotal");
    }

    @Test
    void discountRateAboveOneIsRejected() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO package_offer (campground_id, code, name, pricing_mode, pricing_value, valid_from, valid_to)
                VALUES (%d, 'PKG-BAD', '错误折扣', 'DISCOUNT_RATE', 1.50, '2026-01-01', '2026-12-31')
                """.formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_package_offer_rate_range");
    }

    // ---- 价格快照 ----

    @Test
    void priceSnapshotPreservesHistoryWhenRuleDeleted() throws SQLException {
        long orderId = insertOrder("SO-0040", "k40", "NEW");
        long itemId = insertItem(orderId, "LODGING", 1);
        execute("""
                INSERT INTO price_rule (campground_id, rule_type, operation, value, priority, valid_from, valid_to)
                VALUES (%d, 'SEASON', 'MULTIPLIER', 1.20, 10, '2026-12-01', '2026-12-31')
                """.formatted(campgroundId));
        long ruleId = scalar("SELECT price_rule_id FROM price_rule");

        execute("""
                INSERT INTO price_snapshot_line (order_item_id, service_date, rule_id, rule_name_snapshot,
                                                 operation_snapshot, rule_value_snapshot, amount)
                VALUES (%d, '2026-12-24', %d, '圣诞旺季', 'MULTIPLIER', 1.20, 1536.00)
                """.formatted(itemId, ruleId));

        // 有快照引用时不得删除规则，否则价格可追溯性被破坏。
        assertThatThrownBy(() -> execute("DELETE FROM price_rule WHERE price_rule_id = " + ruleId))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("fk_snapshot_line_rule");
    }

    // ---- 辅助 ----

    private long insertOrder(String orderNo, String idemKey, String fingerprint) throws SQLException {
        return insertOrderForUser(userId, orderNo, idemKey, fingerprint);
    }

    private long insertOrderForUser(long ownerId, String orderNo, String idemKey, String fingerprint) throws SQLException {
        execute("""
                INSERT INTO customer_order (order_no, user_id, campground_id, create_idempotency_key,
                                            create_request_fingerprint, expires_at)
                VALUES (%s, %d, %d, '%s', '%s', now() + interval '15 min')
                """.formatted(quote(orderNo), ownerId, campgroundId, idemKey, fingerprint));
        return scalar("SELECT order_id FROM customer_order WHERE order_no = " + quote(orderNo));
    }

    private long insertItem(long orderId, String itemType, int quantity) throws SQLException {
        execute("""
                INSERT INTO order_item (order_id, item_type, item_name_snapshot, quantity, unit_price, subtotal)
                VALUES (%d, '%s', '测试项', %d, 100.00, %d)
                """.formatted(orderId, itemType, quantity, quantity * 100));
        return scalar("SELECT max(order_item_id) FROM order_item");
    }

    private void insertPayment(long orderId, String paymentNo, String status, String key, boolean succeeded) throws SQLException {
        String paidAt = succeeded ? "now()" : "NULL";
        execute("""
                INSERT INTO payment (order_id, payment_no, status, amount, idempotency_key, request_fingerprint, paid_at)
                VALUES (%d, '%s', '%s', 100.00, '%s', 'fp', %s)
                """.formatted(orderId, paymentNo, status, key, paidAt));
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
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
             var rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
