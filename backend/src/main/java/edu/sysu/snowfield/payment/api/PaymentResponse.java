package edu.sysu.snowfield.payment.api;

import java.time.OffsetDateTime;

public record PaymentResponse(
        String id,
        String paymentNo,
        String orderId,
        String status,
        String amount,
        String method,
        OffsetDateTime paidAt) {
}
