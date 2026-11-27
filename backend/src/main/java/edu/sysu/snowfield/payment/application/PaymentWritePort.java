package edu.sysu.snowfield.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/** payment 模块持久化端口。 */
public interface PaymentWritePort {

    Optional<ExistingPayment> findByOrderAndIdempotencyKey(long orderId, String idempotencyKey);

    Optional<ExistingPayment> findSucceededByOrder(long orderId);

    long create(NewPayment payment);

    int refundSucceededForOrder(long orderId);

    boolean hasRefundedPayment(long orderId);

    record NewPayment(long orderId, String paymentNo, String status, BigDecimal amount,
                      String idempotencyKey, String requestFingerprint, OffsetDateTime paidAt) {
    }

    record ExistingPayment(long paymentId, String paymentNo, long orderId, String status,
                           BigDecimal amount, String requestFingerprint, OffsetDateTime paidAt) {
    }
}
