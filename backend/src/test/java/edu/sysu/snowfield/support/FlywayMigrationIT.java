package edu.sysu.snowfield.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flyway 空库迁移与数据库约束反例测试（对应任务板 DB-01 的验收要点）。
 *
 * <p>验证目标：迁移在测试库真实执行成功、契约规定的唯一/检查/外键约束确实能拒绝非法数据。
 * 这些约束是「数据库负责不可绕过的完整性」（AGENTS.md §2）的实际证据。</p>
 */
class FlywayMigrationIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private javax.sql.DataSource ds;

    @BeforeEach
    void cleanUp() throws SQLException {
        truncateBusinessTables();
    }

    @Test
    void migrationHistoryContainsBaseline() throws SQLException {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            try (var rs = statement.executeQuery("""
                    SELECT version, success FROM flyway_schema_history WHERE version = '1'
                    """)) {
                assertThat(rs.next()).as("V1 迁移记录必须存在").isTrue();
                assertThat(rs.getString("version")).isEqualTo("1");
                assertThat(rs.getBoolean("success")).as("V1 迁移必须成功").isTrue();
            }
        }
    }

    @Test
    void seededRolesExist() throws SQLException {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            try (var rs = statement.executeQuery("SELECT code FROM role ORDER BY code")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("ADMIN");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("CUSTOMER");
            }
        }
    }

    @Test
    void duplicateEmailIsRejected() throws SQLException {
        insertUser("dup@example.com");
        assertThatThrownBy(() -> insertUser("dup@example.com"))
                .as("uq_app_user_email 必须拒绝重复邮箱")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uq_app_user_email");
    }

    @Test
    void uppercaseEmailIsRejected() {
        assertThatThrownBy(() -> insertUser("Mixed@Example.com"))
                .as("ck_app_user_email_lower 必须拒绝大写邮箱")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_app_user_email_lower");
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> insertUserWithStatus("bad-status@example.com", "PENDING"))
                .as("ck_app_user_status 必须拒绝契约外状态")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("ck_app_user_status");
    }

    @Test
    void userRoleForeignKeyIsEnforced() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO user_role (user_id, role_id) VALUES (999999, 1)
                """))
                .as("fk_user_role_user 必须拒绝不存在的用户")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("fk_user_role_user");
    }

    @Test
    void duplicateRoleGrantIsRejected() throws SQLException {
        insertUser("grant@example.com");
        execute("INSERT INTO user_role (user_id, role_id) SELECT user_id, role_id FROM app_user, role WHERE email = 'grant@example.com' AND code = 'CUSTOMER'");
        assertThatThrownBy(() -> execute("""
                INSERT INTO user_role (user_id, role_id)
                SELECT user_id, role_id FROM app_user, role WHERE email = 'grant@example.com' AND code = 'CUSTOMER'
                """))
                .as("pk_user_role 必须拒绝重复授予同一角色")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("pk_user_role");
    }

    @Test
    void failedStatementLeavesNoPartialRow() throws SQLException {
        // 事务回滚证据：整条 INSERT 因约束失败，表内不得留下任何行。
        assertThatThrownBy(() -> insertUserWithStatus("rollback@example.com", "INVALID"))
                .isInstanceOf(SQLException.class);
        assertThat(userCount()).as("失败语句不得留下残留行").isZero();
    }

    private void insertUser(String email) throws SQLException {
        insertUserWithStatus(email, "ACTIVE");
    }

    private void insertUserWithStatus(String email, String status) throws SQLException {
        execute("""
                INSERT INTO app_user (email, password_hash, display_name, status)
                VALUES ('%s', '$argon2id$placeholder', 'Tester', '%s')
                """.formatted(email, status));
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long userCount() throws SQLException {
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement();
             var rs = statement.executeQuery("SELECT count(*) FROM app_user")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
