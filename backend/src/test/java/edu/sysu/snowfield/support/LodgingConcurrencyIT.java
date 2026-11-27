package edu.sysu.snowfield.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BK-03：住宿资源并发抢订测试。
 *
 * <p>验收标准（`docs/testing/TEST_PLAN.md` §并发方案）：100 线程抢最后一个住宿资源，
 * 成功 1、冲突 99、无有效重叠。</p>
 *
 * <p><b>为什么必须用独立连接：</b>用一个测试事务模拟并发无法触发真正的数据库级竞争，
 * 排斥约束不会被真正考验（AGENTS.md §8、docs/ARCHITECTURE.md §7）。
 * 本测试让 100 个线程各自从连接池取独立连接并发 INSERT，由 PostgreSQL 的
 * {@code EXCLUDE USING gist} 约束裁决胜负。</p>
 */
class LodgingConcurrencyIT extends AbstractPostgresIntegrationTest {

    private static final int CONCURRENT_ATTEMPTS = 100;

    private long userId;
    private long campgroundId;
    private long resourceUnitId;
    private long resourceTypeId;

    @BeforeEach
    void setUp() throws SQLException {
        execute("""
                TRUNCATE TABLE user_role, app_user, campground, season, resource_type, holiday, price_rule,
                               package_offer, lodging_reservation
                RESTART IDENTITY CASCADE
                """);
        execute("INSERT INTO app_user (email, password_hash, display_name) VALUES ('p@example.com', 'x', '抢订者')");
        userId = scalar("SELECT user_id FROM app_user WHERE email = 'p@example.com'");
        execute("INSERT INTO campground (code, name, timezone, currency) VALUES ('main', '雪野', 'Asia/Shanghai', 'CNY')");
        campgroundId = scalar("SELECT campground_id FROM campground WHERE code = 'main'");
        execute("""
                INSERT INTO resource_type (campground_id, code, name, kind, base_capacity, base_price)
                VALUES (%d, 'CABIN', '木屋', 'CABIN', 4, 1280.00)
                """.formatted(campgroundId));
        resourceTypeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'LAST-01', '最后一间', 4)"
                .formatted(resourceTypeId));
        resourceUnitId = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'LAST-01'");
    }

    @Test
    void onlyOneOfOneHundredConcurrentBookingsSucceeds() throws Exception {
        // 预建 100 个订单项，让并发线程只争夺「占用」这一步，隔离出约束本身的行为。
        List<Long> itemIds = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
            itemIds.add(newOrderItem("SO-C-" + i));
        }

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_ATTEMPTS);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        AtomicInteger otherFailures = new AtomicInteger();
        List<String> unexpected = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
                final long itemId = itemIds.get(i);
                futures.add(pool.submit((Callable<Void>) () -> {
                    startGate.await();
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement ps = connection.prepareStatement("""
                                 INSERT INTO lodging_reservation
                                     (order_item_id, resource_unit_id, stay_mode, stay_period, guest_count, occupancy_status)
                                 VALUES (?, ?, 'OVERNIGHT',
                                         tstzrange('2027-02-01T15:00+08'::timestamptz,
                                                   '2027-02-03T12:00+08'::timestamptz, '[)'),
                                         2, 'HELD')
                                 """)) {
                        ps.setLong(1, itemId);
                        ps.setLong(2, resourceUnitId);
                        ps.executeUpdate();
                        succeeded.incrementAndGet();
                    } catch (SQLException ex) {
                        // 排斥约束冲突的 SQLState 为 23P01；串行化失败 40001 也应视为「未抢到」。
                        if ("23P01".equals(ex.getSQLState())) {
                            conflicted.incrementAndGet();
                        } else if ("40001".equals(ex.getSQLState()) || "40P01".equals(ex.getSQLState())) {
                            conflicted.incrementAndGet();
                        } else {
                            otherFailures.incrementAndGet();
                            synchronized (unexpected) {
                                unexpected.add(ex.getSQLState() + ": " + ex.getMessage());
                            }
                        }
                    } finally {
                        doneGate.countDown();
                    }
                    return null;
                }));
            }

            startGate.countDown();
            boolean finished = doneGate.await(120, TimeUnit.SECONDS);
            assertThat(finished).as("100 个并发请求必须在 120 秒内全部结束").isTrue();
            for (Future<?> f : futures) {
                f.get(120, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(otherFailures.get())
                .as("不得出现排斥约束以外的失败：" + unexpected)
                .isZero();
        assertThat(succeeded.get())
                .as("100 并发抢同一资源同一时段，只能有 1 个成功")
                .isEqualTo(1);
        assertThat(conflicted.get())
                .as("其余 99 个必须因约束冲突失败")
                .isEqualTo(CONCURRENT_ATTEMPTS - 1);

        // 无残留：数据库里只有一条有效占用，其余订单项没有占用记录。
        assertThat(scalar("SELECT count(*) FROM lodging_reservation"))
                .as("数据库中只能留下 1 条占用记录").isEqualTo(1);
        assertThat(scalar("""
                SELECT count(*) FROM lodging_reservation
                WHERE occupancy_status IN ('HELD','CONFIRMED','IN_USE')
                """)).as("有效占用只能有 1 条").isEqualTo(1);
    }

    @Test
    void concurrentBookingsOnDisjointPeriodsAllSucceed() throws Exception {
        // 反向验证：不是所有并发都会冲突，排斥约束只在时间重叠时才拒绝。
        int attempts = 20;
        List<Long> itemIds = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            itemIds.add(newOrderItem("SO-D-" + i));
        }

        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                final long itemId = itemIds.get(i);
                // 每个请求占用互不重叠的 3 天窗口；用 LocalDate 计算避免手写日期溢出月份。
                java.time.LocalDate checkIn = java.time.LocalDate.of(2027, 3, 1).plusDays(i * 3L);
                java.time.LocalDate checkOut = checkIn.plusDays(2);
                final String start = checkIn + "T15:00+08";
                final String end = checkOut + "T12:00+08";
                futures.add(pool.submit((Callable<Void>) () -> {
                    startGate.await();
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement ps = connection.prepareStatement("""
                                 INSERT INTO lodging_reservation
                                     (order_item_id, resource_unit_id, stay_mode, stay_period, guest_count, occupancy_status)
                                 VALUES (?, ?, 'OVERNIGHT', tstzrange(?::timestamptz, ?::timestamptz, '[)'), 2, 'HELD')
                                 """)) {
                        ps.setLong(1, itemId);
                        ps.setLong(2, resourceUnitId);
                        ps.setString(3, start);
                        ps.setString(4, end);
                        ps.executeUpdate();
                        succeeded.incrementAndGet();
                    }
                    return null;
                }));
            }
            startGate.countDown();
            for (Future<?> f : futures) {
                f.get(120, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(succeeded.get())
                .as("20 个互不重叠的并发预订应全部成功（期望 " + attempts + "，实际 " + succeeded.get() + "）")
                .isEqualTo(attempts);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation")).isEqualTo(attempts);
    }

    // ---- 辅助 ----

    private long newOrderItem(String orderNo) throws SQLException {
        execute("""
                INSERT INTO customer_order (order_no, user_id, campground_id, create_idempotency_key,
                                            create_request_fingerprint, expires_at)
                VALUES ('%s', %d, %d, '%s', 'fp', now() + interval '15 min')
                """.formatted(orderNo, userId, campgroundId, "key-" + orderNo));
        long orderId = scalar("SELECT order_id FROM customer_order WHERE order_no = '%s'".formatted(orderNo));
        execute("""
                INSERT INTO order_item (order_id, item_type, item_name_snapshot, quantity, unit_price, subtotal)
                VALUES (%d, 'LODGING', '最后一间', 1, 1280.00, 1280.00)
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
