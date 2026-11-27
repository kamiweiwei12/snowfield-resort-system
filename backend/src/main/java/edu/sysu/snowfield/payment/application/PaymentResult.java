package edu.sysu.snowfield.payment.application;

import java.time.OffsetDateTime;

/** 模拟支付结果，对应 API_CONTRACT PaymentResponse。 */
public record PaymentResult(
        long paymentId,
        String paymentNo,
        long orderId,
        String status,
        String amount,
        String method,
        OffsetDateTime paidAt,
        boolean replayed) {
}
