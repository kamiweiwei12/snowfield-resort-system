package edu.sysu.snowfield;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 真实 PostgreSQL 上的最小 Spring 上下文门禁。
 *
 * <p>类名以 {@code IT} 结尾，因此只在 Maven {@code -Pdb} profile 中运行；
 * 默认 {@code mvn test} 不连接数据库。它不能替代业务、迁移或并发测试。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SnowfieldApplicationIT {

    @Test
    void contextLoads() {
        // 上下文、数据源或 Flyway 初始化失败时本测试失败。
    }
}
