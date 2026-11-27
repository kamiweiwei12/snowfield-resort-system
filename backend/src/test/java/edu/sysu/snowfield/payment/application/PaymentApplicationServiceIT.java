package edu.sysu.snowfield.payment.application;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.sysu.snowfield.booking.application.BookingApplicationService;
import edu.sysu.snowfield.booking.application.CancelOrderCommand;
import edu.sysu.snowfield.booking.application.CancelledOrder;
import edu.sysu.snowfield.booking.application.CreateLodgingOrderCommand;
import edu.sysu.snowfield.booking.application.CreatedLodgingOrder;
import edu.sysu.snowfield.booking.application.OrderCancellationApplicationService;
import edu.sysu.snowfield.booking.application.OrderExpiryApplicationService;
import edu.sysu.snowfield.common.web.BusinessException;
import edu.sysu.snowfield.common.web.ErrorCode;
import edu.sysu.snowfield.support.AbstractPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentApplicationServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private BookingApplicationService bookingService;

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private OrderCancellationApplicationService cancellationService;

    @Autowired
    private OrderExpiryApplicationService expiryService;

    private long userId;
    private long resourceUnitId;

    @BeforeEach
    void setUp() throws SQLException {
        execute("""
                TRUNCATE TABLE user_role, app_user, campground, season, resource_type, holiday, price_rule,
                               package_offer, lodging_reservation
                RESTART IDENTITY CASCADE
                """);
        execute("INSERT INTO app_user (email, password_hash, display_name) VALUES ('payer@example.com', 'x', '付款人')");
        userId = scalar("SELECT user_id FROM app_user WHERE email = 'payer@example.com'");
        execute("INSERT INTO campground (code, name, timezone, currency) VALUES ('main', '雪野', 'Asia/Shanghai', 'CNY')");
        long campgroundId = scalar("SELECT campground_id FROM campground WHERE code = 'main'");
        execute("""
                INSERT INTO resource_type (campground_id, code, name, kind, base_capacity, base_price)
                VALUES (%d, 'CABIN', '木屋', 'CABIN', 4, 1280.00)
                """.formatted(campgroundId));
        long typeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("INSERT INTO resource_type_stay_mode (resource_type_id, stay_mode) VALUES (%d, 'OVERNIGHT')"
                .formatted(typeId));
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'PAY-A', '支付测试屋', 4)"
                .formatted(typeId));
        resourceUnitId = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'PAY-A'");
    }

    @Test
    void successfulPaymentConfirmsOrderItemsAndLodgingWithHistories() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-pay-success");

        PaymentResult result = paymentService.simulate(command(order.orderId(), "pay-success", "SUCCESS"));

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.method()).isEqualTo("SIMULATED");
        assertThat(result.paidAt()).isNotNull();
        assertThat(result.replayed()).isFalse();
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId()
                + " AND status = 'SUCCEEDED' AND amount = 1280.00")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE order_id = " + order.orderId()
                + " AND status = 'CONFIRMED' AND version = 1")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_item WHERE order_id = " + order.orderId()
                + " AND status = 'CONFIRMED'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation WHERE occupancy_status = 'CONFIRMED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE order_id = " + order.orderId()
                + " AND from_status = 'PENDING_PAYMENT' AND to_status = 'CONFIRMED'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_item_status_history WHERE to_status = 'CONFIRMED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history WHERE to_status = 'CONFIRMED'"))
                .isEqualTo(1);
    }

    @Test
    void failedPaymentLeavesOrderAndHoldsPending() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-pay-failure");

        PaymentResult result = paymentService.simulate(command(order.orderId(), "pay-failure", "FAILURE"));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.paidAt()).isNull();
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE order_id = " + order.orderId()
                + " AND status = 'PENDING_PAYMENT'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_item WHERE order_id = " + order.orderId()
                + " AND status = 'HELD'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation WHERE occupancy_status = 'HELD'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_status_history")).isZero();
    }

    @Test
    void samePaymentKeyAndOutcomeReplaysWithoutDuplicate() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-pay-replay");
        SimulatePaymentCommand command = command(order.orderId(), "pay-replay", "FAILURE");

        PaymentResult first = paymentService.simulate(command);
        PaymentResult replay = paymentService.simulate(command);

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.paymentId()).isEqualTo(first.paymentId());
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId())).isEqualTo(1);
    }

    @Test
    void samePaymentKeyWithDifferentOutcomeConflicts() {
        CreatedLodgingOrder order = createOrder("create-pay-conflict");
        paymentService.simulate(command(order.orderId(), "pay-conflict", "FAILURE"));

        assertThatThrownBy(() -> paymentService.simulate(command(order.orderId(), "pay-conflict", "SUCCESS")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Test
    void failedAttemptCanRetryWithNewKeyAndSucceed() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-pay-retry");
        paymentService.simulate(command(order.orderId(), "pay-failed-key", "FAILURE"));

        PaymentResult success = paymentService.simulate(command(order.orderId(), "pay-new-key", "SUCCESS"));

        assertThat(success.status()).isEqualTo("SUCCEEDED");
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId())).isEqualTo(2);
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId()
                + " AND status = 'SUCCEEDED'")).isEqualTo(1);
    }

    @Test
    void newKeyAfterSuccessReturnsExistingSuccessWithoutNewPayment() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-pay-existing-success");
        PaymentResult first = paymentService.simulate(command(order.orderId(), "pay-success-1", "SUCCESS"));

        PaymentResult second = paymentService.simulate(command(order.orderId(), "pay-success-2", "FAILURE"));

        assertThat(second.replayed()).isTrue();
        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(second.status()).isEqualTo("SUCCEEDED");
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId())).isEqualTo(1);
    }

    @Test
    void expiredOrderRejectsPaymentWithoutResidue() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-pay-expired");
        execute("UPDATE customer_order SET expires_at = now() - interval '1 minute' WHERE order_id = "
                + order.orderId());

        assertThatThrownBy(() -> paymentService.simulate(command(order.orderId(), "pay-expired", "SUCCESS")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_EXPIRED));
        assertThat(scalar("SELECT count(*) FROM payment")).isZero();
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE status = 'PENDING_PAYMENT'"))
                .isEqualTo(1);
    }

    @Test
    void nonOwnerCannotPayOrder() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-pay-owner");
        execute("INSERT INTO app_user (email, password_hash, display_name) VALUES ('other@example.com', 'x', '其他人')");
        long otherUser = scalar("SELECT user_id FROM app_user WHERE email = 'other@example.com'");

        assertThatThrownBy(() -> paymentService.simulate(
                new SimulatePaymentCommand(order.orderId(), otherUser, "pay-other", "SUCCESS")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        assertThat(scalar("SELECT count(*) FROM payment")).isZero();
    }

    @Test
    void concurrentSuccessWithDifferentKeysCreatesOnlyOneSuccessfulPayment() throws Exception {
        CreatedLodgingOrder order = createOrder("create-pay-concurrent");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<PaymentResult> first = executor.submit(() -> attempt(
                    command(order.orderId(), "pay-concurrent-a", "SUCCESS"), ready, start));
            Future<PaymentResult> second = executor.submit(() -> attempt(
                    command(order.orderId(), "pay-concurrent-b", "SUCCESS"), ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            PaymentResult a = first.get(20, TimeUnit.SECONDS);
            PaymentResult b = second.get(20, TimeUnit.SECONDS);
            assertThat(a.paymentId()).isEqualTo(b.paymentId());
            assertThat(List.of(a.replayed(), b.replayed())).containsExactlyInAnyOrder(false, true);
        }

        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId()
                + " AND status = 'SUCCEEDED'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE order_id = " + order.orderId()))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history"))
                .isEqualTo(1);
    }

    @Test
    void pendingOrderCancellationReleasesHoldAndWritesHistories() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-cancel-pending");

        CancelledOrder cancelled = cancellationService.cancel(
                new CancelOrderCommand(order.orderId(), userId, "cancel-pending"));

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.refunded()).isFalse();
        assertThat(cancelled.replayed()).isFalse();
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE order_id = " + order.orderId()
                + " AND status = 'CANCELLED' AND version = 1")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_item WHERE order_id = " + order.orderId()
                + " AND status = 'CANCELLED'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation WHERE occupancy_status = 'RELEASED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE to_status = 'CANCELLED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_item_status_history WHERE to_status = 'CANCELLED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history WHERE to_status = 'RELEASED'"))
                .isEqualTo(1);
    }

    @Test
    void paidOrderCancellationRefundsSuccessfulPayment() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-cancel-paid");
        paymentService.simulate(command(order.orderId(), "pay-before-cancel", "SUCCESS"));

        CancelledOrder cancelled = cancellationService.cancel(
                new CancelOrderCommand(order.orderId(), userId, "cancel-paid"));

        assertThat(cancelled.refunded()).isTrue();
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId()
                + " AND status = 'REFUNDED'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE status = 'CANCELLED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation WHERE occupancy_status = 'RELEASED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE order_id = " + order.orderId()))
                .isEqualTo(2);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history"))
                .isEqualTo(2);
    }

    @Test
    void repeatedCancellationReturnsSameTerminalResultWithoutDuplicateHistory() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-cancel-replay");
        CancelOrderCommand command = new CancelOrderCommand(order.orderId(), userId, "cancel-replay");
        cancellationService.cancel(command);

        CancelledOrder replay = cancellationService.cancel(command);

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.status()).isEqualTo("CANCELLED");
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE order_id = " + order.orderId()))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history"))
                .isEqualTo(1);
    }

    @Test
    void startedItemPreventsCancellationAndRollsBack() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-cancel-started");
        paymentService.simulate(command(order.orderId(), "pay-before-start", "SUCCESS"));
        execute("UPDATE order_item SET status = 'IN_USE' WHERE order_id = " + order.orderId());

        assertThatThrownBy(() -> cancellationService.cancel(
                new CancelOrderCommand(order.orderId(), userId, "cancel-started")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(scalar("SELECT count(*) FROM payment WHERE status = 'SUCCEEDED'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE status = 'CONFIRMED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation WHERE occupancy_status = 'CONFIRMED'"))
                .isEqualTo(1);
    }

    @Test
    void nonOwnerCannotCancelOrder() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-cancel-owner");
        execute("INSERT INTO app_user (email, password_hash, display_name) VALUES ('cancel-other@example.com', 'x', '其他人')");
        long otherUser = scalar("SELECT user_id FROM app_user WHERE email = 'cancel-other@example.com'");

        assertThatThrownBy(() -> cancellationService.cancel(
                new CancelOrderCommand(order.orderId(), otherUser, "cancel-other")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE status = 'PENDING_PAYMENT'"))
                .isEqualTo(1);
    }

    @Test
    void concurrentCancellationWritesOneTransitionOnly() throws Exception {
        CreatedLodgingOrder order = createOrder("create-cancel-concurrent");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<CancelledOrder> first = executor.submit(() -> attemptCancel(
                    new CancelOrderCommand(order.orderId(), userId, "cancel-a"), ready, start));
            Future<CancelledOrder> second = executor.submit(() -> attemptCancel(
                    new CancelOrderCommand(order.orderId(), userId, "cancel-b"), ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            CancelledOrder a = first.get(20, TimeUnit.SECONDS);
            CancelledOrder b = second.get(20, TimeUnit.SECONDS);
            assertThat(List.of(a.replayed(), b.replayed())).containsExactlyInAnyOrder(false, true);
        }
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE order_id = " + order.orderId()))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history"))
                .isEqualTo(1);
    }

    @Test
    void duePendingOrderExpiresAndReleasesLodgingWithHistories() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-expiry-due");
        execute("UPDATE customer_order SET expires_at = now() - interval '1 second' WHERE order_id = "
                + order.orderId());

        int expired = expiryService.expireDueOrders();

        assertThat(expired).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE order_id = " + order.orderId()
                + " AND status = 'EXPIRED' AND version = 1")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_item WHERE order_id = " + order.orderId()
                + " AND status = 'EXPIRED'")).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation WHERE occupancy_status = 'RELEASED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE to_status = 'EXPIRED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_item_status_history WHERE to_status = 'EXPIRED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history WHERE to_status = 'RELEASED'"))
                .isEqualTo(1);
    }

    @Test
    void expiryScanIsIdempotentAndIgnoresNotDueOrConfirmedOrders() throws SQLException {
        CreatedLodgingOrder notDue = createOrder("create-expiry-not-due");
        paymentService.simulate(command(notDue.orderId(), "pay-before-expiry", "SUCCESS"));
        execute("UPDATE customer_order SET expires_at = now() - interval '1 second' WHERE order_id = "
                + notDue.orderId());

        assertThat(expiryService.expireDueOrders()).isZero();
        assertThat(expiryService.expireDueOrders()).isZero();
        assertThat(scalar("SELECT count(*) FROM customer_order WHERE status = 'CONFIRMED'"))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE to_status = 'EXPIRED'"))
                .isZero();
    }

    @Test
    void secondExpiryScanDoesNotDuplicateTransitions() throws SQLException {
        CreatedLodgingOrder order = createOrder("create-expiry-replay");
        execute("UPDATE customer_order SET expires_at = now() - interval '1 second' WHERE order_id = "
                + order.orderId());
        assertThat(expiryService.expireDueOrders()).isEqualTo(1);

        assertThat(expiryService.expireDueOrders()).isZero();
        assertThat(scalar("SELECT count(*) FROM order_status_history WHERE order_id = " + order.orderId()))
                .isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM lodging_reservation_status_history"))
                .isEqualTo(1);
    }

    private PaymentResult attempt(SimulatePaymentCommand command,
                                  CountDownLatch ready,
                                  CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(10, TimeUnit.SECONDS);
        return paymentService.simulate(command);
    }

    private CancelledOrder attemptCancel(CancelOrderCommand command,
                                         CountDownLatch ready,
                                         CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(10, TimeUnit.SECONDS);
        return cancellationService.cancel(command);
    }

    private CreatedLodgingOrder createOrder(String key) {
        var line = new CreateLodgingOrderCommand.LodgingLine(
                resourceUnitId, "OVERNIGHT",
                OffsetDateTime.parse("2027-12-20T15:00+08"),
                OffsetDateTime.parse("2027-12-21T12:00+08"), 2);
        return bookingService.createLodgingOrder(
                new CreateLodgingOrderCommand(userId, key, List.of(line), null));
    }

    private SimulatePaymentCommand command(long orderId, String key, String outcome) {
        return new SimulatePaymentCommand(orderId, userId, key, outcome);
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
}
