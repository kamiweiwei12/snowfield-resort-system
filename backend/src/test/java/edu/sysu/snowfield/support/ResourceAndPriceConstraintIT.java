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
 * 营地、资源与价格迁移（V2/V3）的约束反例测试，对应任务板 DB-02/DB-03 的验收标准。
 *
 * <p>这些约束是契约要求「由数据库负责不可绕过的完整性」的具体证据（AGENTS.md §2）：
 * 每一条都用反例证明非法数据真的会被数据库拒绝，而不是只依赖服务层校验。</p>
 */
class ResourceAndPriceConstraintIT extends AbstractPostgresIntegrationTest {

    private long campgroundId;
    private long resourceTypeId;
    private long resourceUnitId;

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            // CASCADE 会连同 season/resource_type/... 一并清空；保留 flyway 历史与种子角色。
            statement.execute("TRUNCATE TABLE campground, holiday, price_rule RESTART IDENTITY CASCADE");
        }
        execute("INSERT INTO campground (code, name, timezone, currency) VALUES ('main', '雪野营地', 'Asia/Shanghai', 'CNY')");
        campgroundId = scalar("SELECT campground_id FROM campground WHERE code = 'main'");
        execute("""
                INSERT INTO resource_type (campground_id, code, name, kind, base_capacity, base_price)
                VALUES (%d, 'CABIN', '木屋', 'CABIN', 4, 1280.00)
                """.formatted(campgroundId));
        resourceTypeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("""
                INSERT INTO resource_unit (resource_type_id, code, name, capacity)
                VALUES (%d, 'C-01', '木屋01', 4)
                """.formatted(resourceTypeId));
        resourceUnitId = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'C-01'");
    }

    // ---- 营地 ----

    @Test
    void campgroundCodeIsUnique() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO campground (code, name, timezone, currency) VALUES ('main', '重复', 'Asia/Shanghai', 'CNY')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_campground_code");
    }

    @Test
    void campgroundRejectsNonCnyCurrency() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO campground (code, name, timezone, currency) VALUES ('us', 'US', 'Asia/Shanghai', 'USD')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_campground_currency");
    }

    @Test
    void campgroundRejectsNonBusinessTimezone() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO campground (code, name, timezone, currency) VALUES ('tz', 'TZ', 'UTC', 'CNY')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_campground_timezone");
    }

    // ---- 季节 ----

    @Test
    void seasonRejectsEndBeforeStart() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO season (campground_id, code, name, start_date, end_date)
                VALUES (%d, 'S1', '旺季', '2026-12-31', '2026-12-01')
                """.formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_season_date_order");
    }

    @Test
    void seasonAcceptsSameDayRange() {
        // 边界：单日季节合法（end >= start）
        assertThatCode(() -> execute("""
                INSERT INTO season (campground_id, code, name, start_date, end_date)
                VALUES (%d, 'ONEDAY', '单日', '2026-12-01', '2026-12-01')
                """.formatted(campgroundId))).doesNotThrowAnyException();
    }

    // ---- 资源 ----

    @Test
    void resourceTypeRejectsUnknownKind() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO resource_type (campground_id, code, name, kind, base_capacity, base_price)
                VALUES (%d, 'TENT', '帐篷', 'TENT', 2, 100.00)
                """.formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_resource_type_kind");
    }

    @Test
    void resourceTypeRejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO resource_type (campground_id, code, name, kind, base_capacity, base_price)
                VALUES (%d, 'ZERO', '零容量', 'CABIN', 0, 100.00)
                """.formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_resource_type_base_capacity");
    }

    @Test
    void resourceUnitCodeIsUniqueWithinType() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO resource_unit (resource_type_id, code, name, capacity)
                VALUES (%d, 'C-01', '重复木屋', 4)
                """.formatted(resourceTypeId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_resource_unit_type_code");
    }

    @Test
    void stayModeRejectsUnknownValue() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO resource_type_stay_mode (resource_type_id, stay_mode)
                VALUES (%d, 'WEEK')
                """.formatted(resourceTypeId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_resource_type_stay_mode");
    }

    // ---- 开放规则 ----

    @Test
    void openRuleRejectsWeekdayOutOfRange() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO resource_open_rule (resource_unit_id, valid_from, valid_to, weekday, open_time, close_time)
                VALUES (%d, '2026-01-01', '2026-12-31', 8, '08:00', '20:00')
                """.formatted(resourceUnitId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_open_rule_weekday");
    }

    @Test
    void openRuleRejectsReversedTimeWindow() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO resource_open_rule (resource_unit_id, valid_from, valid_to, weekday, open_time, close_time)
                VALUES (%d, '2026-01-01', '2026-12-31', 1, '20:00', '08:00')
                """.formatted(resourceUnitId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_open_rule_time_order");
    }

    // ---- 维修区间 ----

    @Test
    void maintenanceRejectsEmptyPeriod() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO resource_maintenance (resource_unit_id, period)
                VALUES (%d, 'empty'::tstzrange)
                """.formatted(resourceUnitId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_maintenance_period_nonempty");
    }

    @Test
    void maintenanceAcceptsValidRange() {
        assertThatCode(() -> execute("""
                INSERT INTO resource_maintenance (resource_unit_id, period, reason)
                VALUES (%d, tstzrange('2027-01-01T00:00+08', '2027-01-03T00:00+08', '[)'), '屋顶维修')
                """.formatted(resourceUnitId))).doesNotThrowAnyException();
    }

    // ---- 价格规则 ----

    @Test
    void priceRuleRejectsUnknownOperation() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO price_rule (campground_id, rule_type, operation, value, priority, valid_from, valid_to)
                VALUES (%d, 'SEASON', 'DIVIDE', 2.00, 10, '2026-12-01', '2026-12-31')
                """.formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_price_rule_operation");
    }

    @Test
    void priceRuleRejectsNegativeValue() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO price_rule (campground_id, rule_type, operation, value, priority, valid_from, valid_to)
                VALUES (%d, 'SEASON', 'MULTIPLIER', -1.00, 10, '2026-12-01', '2026-12-31')
                """.formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_price_rule_value");
    }

    @Test
    void priceRuleRejectsInvalidStayMode() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO price_rule (campground_id, stay_mode, rule_type, operation, value, priority, valid_from, valid_to)
                VALUES (%d, 'WEEKLY', 'SEASON', 'FIXED', 100.00, 10, '2026-12-01', '2026-12-31')
                """.formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_price_rule_stay_mode");
    }

    @Test
    void priceRuleAllowsNullResourceTypePerD001() {
        // D-001 方案B：resource_type_id 为 NULL、stay_mode 非空 = 全营地住宿模式规则。
        assertThatCode(() -> execute("""
                INSERT INTO price_rule (campground_id, resource_type_id, stay_mode, rule_type, operation, value, priority, valid_from, valid_to)
                VALUES (%d, NULL, 'OVERNIGHT', 'SEASON', 'MULTIPLIER', 1.20, 100, '2026-12-01', '2026-12-31')
                """.formatted(campgroundId))).doesNotThrowAnyException();
    }

    @Test
    void priceRuleBothNullIsCurrentlyPermitted() {
        // ISSUES R5-04 在 v1.0-rc 仍未裁定；本迁移刻意不加 CHECK，此处固定当前实际行为，
        // 便于将来契约裁定后以新迁移显式变更，而不是悄悄改变语义。
        assertThatCode(() -> execute("""
                INSERT INTO price_rule (campground_id, resource_type_id, stay_mode, rule_type, operation, value, priority, valid_from, valid_to)
                VALUES (%d, NULL, NULL, 'SEASON', 'MULTIPLIER', 1.30, 100, '2026-12-01', '2026-12-31')
                """.formatted(campgroundId))).doesNotThrowAnyException();
    }

    @Test
    void priceRuleWeekdayIsCascadeDeletedWithRule() throws SQLException {
        execute("""
                INSERT INTO price_rule (price_rule_id, campground_id, rule_type, operation, value, priority, valid_from, valid_to)
                OVERRIDING SYSTEM VALUE
                VALUES (9001, %d, 'WEEKEND', 'MULTIPLIER', 1.15, 50, '2026-12-01', '2026-12-31')
                """.formatted(campgroundId));
        execute("INSERT INTO price_rule_weekday (price_rule_id, weekday) VALUES (9001, 6), (9001, 7)");
        assertThat(scalar("SELECT count(*) FROM price_rule_weekday WHERE price_rule_id = 9001")).isEqualTo(2);

        execute("DELETE FROM price_rule WHERE price_rule_id = 9001");
        assertThat(scalar("SELECT count(*) FROM price_rule_weekday WHERE price_rule_id = 9001"))
                .as("价格规则删除后其星期关联必须级联删除")
                .isZero();
    }

    @Test
    void holidayIsUniquePerCampgroundAndDate() throws SQLException {
        execute("INSERT INTO holiday (campground_id, holiday_date, name) VALUES (%d, '2026-10-01', '国庆')".formatted(campgroundId));
        assertThatThrownBy(() -> execute(
                "INSERT INTO holiday (campground_id, holiday_date, name) VALUES (%d, '2026-10-01', '重复')".formatted(campgroundId)))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_holiday_campground_date");
    }

    // ---- 辅助 ----

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
