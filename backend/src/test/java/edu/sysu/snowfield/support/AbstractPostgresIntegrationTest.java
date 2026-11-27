package edu.sysu.snowfield.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库集成测试的公共基类：真实 PostgreSQL + 真实 Flyway 迁移。
 *
 * <p>不使用 H2 或内存替代（AGENTS.md §8）。每个测试方法运行前清空业务表，
 * 只清测试库 {@code snowfield_test}，与开发库 {@code snowfield} 完全隔离。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @Autowired
    protected DataSource dataSource;

    @Test
    void dataSourceIsRealPostgres16() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT version()");
            try (var rs = statement.getResultSet()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).containsIgnoringCase("PostgreSQL").contains("16.");
            }
        }
    }

    /**
     * 清空业务表，保留 {@code flyway_schema_history} 与种子角色。
     *
     * <p>使用 {@code TRUNCATE ... RESTART IDENTITY} 让每个用例的 ID 可预测；
     * {@code CASCADE} 只作用于外键，此处表结构受控，不会波及非业务表。</p>
     */
    protected void truncateBusinessTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    TRUNCATE TABLE user_role, app_user, season, resource_type, campground, holiday, price_rule
                    RESTART IDENTITY CASCADE
                    """);
        }
    }
}
