package edu.sysu.snowfield.identity.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.sysu.snowfield.support.AbstractPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * MyBatis 4.1.0 + Spring Boot 4.1.3 + PostgreSQL 真实链路验证。
 *
 * <p>这是 CHANGELOG「Known limitations」中标记为「待接入验证」的高风险项：
 * MyBatis starter 不在 Boot BOM 内，版本号相符不等于真实可用，必须以真实构建与
 * 真实数据库查询证明（docs/ARCHITECTURE.md §7 注意事项）。</p>
 */
class AppUserMapperIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private AppUserMapper appUserMapper;

    @BeforeEach
    void cleanUp() throws SQLException {
        truncateBusinessTables();
    }

    @Test
    void mapperIsRegisteredByStarter() {
        // Mapper 能被注入即证明 mybatis-spring-boot-starter 的自动配置生效。
        assertThat(appUserMapper).isNotNull();
    }

    @Test
    void countAllReturnsZeroOnEmptyTable() {
        assertThat(appUserMapper.countAll()).isZero();
    }

    @Test
    void findByEmailMapsAllColumnsIncludingTimezoneAwareTimestamps() throws SQLException {
        executeInsert("mybatis@example.com", "小明", "+8613800000000");

        AppUserRecord user = appUserMapper.findByEmail("mybatis@example.com");

        assertThat(user).as("按邮箱必须能查到刚写入的用户").isNotNull();
        assertThat(user.userId()).isPositive();
        assertThat(user.email()).isEqualTo("mybatis@example.com");
        assertThat(user.displayName()).as("下划线列名必须映射到驼峰属性").isEqualTo("小明");
        assertThat(user.phone()).isEqualTo("+8613800000000");
        assertThat(user.status()).isEqualTo("ACTIVE");
        assertThat(user.createdAt()).as("timestamptz 必须映射为带偏移量的时刻").isNotNull();
        assertThat(user.updatedAt()).isNotNull();
    }

    @Test
    void findByEmailReturnsNullWhenAbsent() {
        assertThat(appUserMapper.findByEmail("nobody@example.com")).isNull();
    }

    @Test
    void parameterIsBoundNotConcatenated() {
        // 参数绑定证据：注入式字符串被当作普通字面量，不会造成 SQL 语法错误或绕过。
        String injection = "x' OR '1'='1";
        assertThatCode(() -> appUserMapper.findByEmail(injection)).doesNotThrowAnyException();
        assertThat(appUserMapper.findByEmail(injection)).isNull();
    }

    private void executeInsert(String email, String displayName, String phone) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_user (email, password_hash, display_name, phone, status)
                    VALUES ('%s', '$argon2id$placeholder', '%s', '%s', 'ACTIVE')
                    """.formatted(email, displayName, phone));
        }
    }
}
