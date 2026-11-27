package edu.sysu.snowfield.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 住宿时间排斥约束（V5）语义测试。
 *
 * <p>这是契约中「数据库负责不可绕过的完整性」最核心的一条：同一资源的重叠时段
 * 至多一条有效占用，且 {@code RELEASED} 必须被排除，否则已取消订单会永久阻塞资源。</p>
 */
class LodgingExclusionConstraintIT extends AbstractPostgresIntegrationTest {

    private long userId;
    private long campgroundId;
    private long resourceUnitId;

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
        long typeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'C-01', '木屋01', 4)".formatted(typeId));
        resourceUnitId = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'C-01'");
    }

    @Test
    void overlappingHeldReservationIsRejected() throws SQLException {
        insertReservation("HELD", "2026-12-24T15:00+08", "2026-12-26T12:00+08");

        assertThatThrownBy(() -> insertReservation("HELD", "2026-12-25T15:00+08", "2026-12-27T12:00+08"))
                .as("重叠时段的第二笔占用必须被排斥约束拒绝")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ex_lodging_reservation_no_overlap");
    }

    @Test
    void fullyContainedReservationIsRejected() throws SQLException {
        insertReservation("CONFIRMED", "2026-12-24T15:00+08", "2026-12-27T12:00+08");

        assertThatThrownBy(() -> insertReservation("IN_USE", "2026-12-25T15:00+08", "2026-12-26T12:00+08"))
                .as("被完全包含的时段同样重叠")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ex_lodging_reservation_no_overlap");
    }

    @Test
    void adjacentReservationsAreAllowed() throws SQLException {
        // 半开区间 [start, end)：入住 24 日 15:00 至 26 日 12:00，下一笔 26 日 12:00 开始不重叠。
        insertReservation("CONFIRMED", "2026-12-24T15:00+08", "2026-12-26T12:00+08");

        assertThatCode(() -> insertReservation("CONFIRMED", "2026-12-26T12:00+08", "2026-12-28T12:00+08"))
                .as("首尾相接的半开区间不得被判为重叠（AGENTS.md §3）")
                .doesNotThrowAnyException();
    }

    @Test
    void releasedReservationDoesNotBlockResource() throws SQLException {
        // 契约核心：RELEASED 必须从排斥谓词排除，否则已取消订单永久阻塞同一资源。
        insertReservation("RELEASED", "2026-12-24T15:00+08", "2026-12-26T12:00+08");

        assertThatCode(() -> insertReservation("HELD", "2026-12-24T15:00+08", "2026-12-26T12:00+08"))
                .as("RELEASED 占用不得阻塞新预订")
                .doesNotThrowAnyException();
    }

    @Test
    void releasedIsTerminalAndNoLongerExcluded() throws SQLException {
        long reservationId = insertReservation("HELD", "2026-12-24T15:00+08", "2026-12-26T12:00+08");

        // 释放后，同区间可再次占用
        execute("UPDATE lodging_reservation SET occupancy_status = 'RELEASED' WHERE reservation_id = " + reservationId);
        assertThatCode(() -> insertReservation("HELD", "2026-12-24T15:00+08", "2026-12-26T12:00+08"))
                .as("释放后同区间可再次占用")
                .doesNotThrowAnyException();
    }

    @Test
    void differentResourceUnitsDoNotConflict() throws SQLException {
        long typeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'C-02', '木屋02', 4)".formatted(typeId));
        long otherUnit = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'C-02'");

        insertReservation("HELD", "2026-12-24T15:00+08", "2026-12-26T12:00+08");
        assertThatCode(() -> insertReservationForUnit(otherUnit, "HELD", "2026-12-24T15:00+08", "2026-12-26T12:00+08"))
                .as("不同资源单元的相同时段互不排斥")
                .doesNotThrowAnyException();
    }

    @Test
    void emptyPeriodIsRejected() {
        // 'empty' 区间的 lower()/upper() 均为 NULL，故由 ck_..._period_bounded 先触发；
        // 两个 CHECK 都表达了「区间必须是有界非空」，此处只断言被数据库拒绝，不绑定具体约束名。
        assertThatThrownBy(() -> execute("""
                INSERT INTO lodging_reservation (order_item_id, resource_unit_id, stay_mode, stay_period, guest_count)
                VALUES (%d, %d, 'OVERNIGHT', 'empty'::tstzrange, 2)
                """.formatted(newOrderItem(), resourceUnitId)))
                .as("空区间必须被拒绝")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_lodging_reservation_period_");
    }

    @Test
    void unboundedPeriodIsRejected() {
        // 只有上界或下界的区间无法参与有意义的重叠判断，必须拒绝。
        assertThatThrownBy(() -> insertReservation("HELD", "2026-12-24T15:00+08", null))
                .as("上界为无限的区间必须被拒绝")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_lodging_reservation_period_bounded");
    }

    @Test
    void unknownOccupancyStatusIsRejected() {
        assertThatThrownBy(() -> insertReservation("PENDING_PAYMENT", "2026-12-24T15:00+08", "2026-12-26T12:00+08"))
                .as("OccupancyStatus 独立于 OrderItemStatus，PENDING_PAYMENT 不是合法占用状态")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_lodging_reservation_occupancy");
    }

    @Test
    void orderItemCanHaveOnlyOneReservation() throws SQLException {
        long itemId = newOrderItem();
        execute("""
                INSERT INTO lodging_reservation (order_item_id, resource_unit_id, stay_mode, stay_period, guest_count)
                VALUES (%d, %d, 'OVERNIGHT', tstzrange('2026-12-24T15:00+08','2026-12-26T12:00+08','[)'), 2)
                """.formatted(itemId, resourceUnitId));

        long typeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'C-09', '木屋09', 4)".formatted(typeId));
        long otherUnit = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'C-09'");

        assertThatThrownBy(() -> execute("""
                INSERT INTO lodging_reservation (order_item_id, resource_unit_id, stay_mode, stay_period, guest_count)
                VALUES (%d, %d, 'OVERNIGHT', tstzrange('2027-01-01T15:00+08','2027-01-03T12:00+08','[)'), 2)
                """.formatted(itemId, otherUnit)))
                .as("契约 order_item_id UQ：一个订单项至多一条住宿占用")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_lodging_reservation_order_item");
    }

    @Test
    void deletingOrderRemovesReservations() throws SQLException {
        long itemId = newOrderItem();
        insertReservationForItem(itemId, resourceUnitId, "HELD", "2026-12-24T15:00+08", "2026-12-26T12:00+08");

        execute("DELETE FROM customer_order");
        assertThat(scalar("SELECT count(*) FROM lodging_reservation"))
                .as("订单删除后住宿占用必须级联清除，不得留下孤儿占用")
                .isZero();
    }

    // ---- 辅助 ----

    private long insertReservation(String status, String start, String end) throws SQLException {
        return insertReservationForUnit(resourceUnitId, status, start, end);
    }

    private long insertReservationForUnit(long unitId, String status, String start, String end) throws SQLException {
        return insertReservationForItem(newOrderItem(), unitId, status, start, end);
    }

    private long insertReservationForItem(long itemId, long unitId, String status, String start, String end)
            throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     INSERT INTO lodging_reservation
                         (order_item_id, resource_unit_id, stay_mode, stay_period, guest_count, occupancy_status)
                     VALUES (?, ?, 'OVERNIGHT', tstzrange(?::timestamptz, ?::timestamptz, '[)'), 2, ?)
                     RETURNING reservation_id
                     """)) {
            ps.setLong(1, itemId);
            ps.setLong(2, unitId);
            ps.setString(3, start);
            if (end == null) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, end);
            }
            ps.setString(5, status);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** 建一笔最小合法订单及其订单项，返回 order_item_id。 */
    private long newOrderItem() throws SQLException {
        execute("""
                INSERT INTO customer_order (order_no, user_id, campground_id, create_idempotency_key,
                                            create_request_fingerprint, expires_at)
                VALUES ('SO-' || nextval('customer_order_order_id_seq'), %d, %d,
                        'key-' || nextval('customer_order_order_id_seq'), 'fp', now() + interval '15 min')
                """.formatted(userId, campgroundId));
        long orderId = scalar("SELECT max(order_id) FROM customer_order");
        execute("""
                INSERT INTO order_item (order_id, item_type, item_name_snapshot, quantity, unit_price, subtotal)
                VALUES (%d, 'LODGING', '木屋01', 1, 1280.00, 1280.00)
                """.formatted(orderId));
        return scalar("SELECT max(order_item_id) FROM order_item");
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
