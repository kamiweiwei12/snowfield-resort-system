package edu.sysu.snowfield.payment.application;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.sysu.snowfield.common.web.BusinessException;
import edu.sysu.snowfield.common.web.ErrorCode;
import edu.sysu.snowfield.lodging.application.LodgingReservationPort;
import edu.sysu.snowfield.order.application.OrderPaymentPort;

/** 模拟支付用例：锁订单并在单事务内同步支付、订单、明细与占用状态。 */
@Service
public class PaymentApplicationService {

    private final OrderPaymentPort orderPayment;
    private final PaymentWritePort paymentWrite;
    private final LodgingReservationPort lodgingReservation;
    private final PaymentNumberGenerator paymentNumberGenerator;

    public PaymentApplicationService(OrderPaymentPort orderPayment,
                                     PaymentWritePort paymentWrite,
                                     LodgingReservationPort lodgingReservation,
                                     PaymentNumberGenerator paymentNumberGenerator) {
        this.orderPayment = orderPayment;
        this.paymentWrite = paymentWrite;
        this.lodgingReservation = lodgingReservation;
        this.paymentNumberGenerator = paymentNumberGenerator;
    }

    @Transactional
    public PaymentResult simulate(SimulatePaymentCommand command) {
        validate(command);

        OrderPaymentPort.PayableOrder order = orderPayment
                .lockOwnedOrder(command.orderId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        String fingerprint = command.fingerprint();
        var sameKey = paymentWrite.findByOrderAndIdempotencyKey(order.orderId(), command.idempotencyKey());
        if (sameKey.isPresent()) {
            PaymentWritePort.ExistingPayment payment = sameKey.get();
            if (!payment.requestFingerprint().equals(fingerprint)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT,
                        "相同支付幂等键对应了不同请求");
            }
            return result(payment, true);
        }

        var succeeded = paymentWrite.findSucceededByOrder(order.orderId());
        if (succeeded.isPresent()) {
            return result(succeeded.get(), true);
        }

        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "当前订单状态不允许支付：" + order.status());
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (!order.expiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.ORDER_EXPIRED);
        }

        boolean success = "SUCCESS".equals(command.normalizedOutcome());
        String status = success ? "SUCCEEDED" : "FAILED";
        OffsetDateTime paidAt = success ? now : null;
        String paymentNo = paymentNumberGenerator.next();
        long paymentId = paymentWrite.create(new PaymentWritePort.NewPayment(
                order.orderId(), paymentNo, status, order.totalAmount(),
                command.idempotencyKey(), fingerprint, paidAt));

        if (success) {
            orderPayment.confirmOrderAndItems(order.orderId(), command.userId());
            lodgingReservation.confirmForOrder(order.orderId());
        }

        return new PaymentResult(paymentId, paymentNo, order.orderId(), status,
                order.totalAmount().toPlainString(), "SIMULATED", paidAt, false);
    }

    private static void validate(SimulatePaymentCommand command) {
        if (command == null || command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key 不能为空");
        }
        String outcome = command.normalizedOutcome();
        if (!"SUCCESS".equals(outcome) && !"FAILURE".equals(outcome)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "desiredOutcome 只能是 SUCCESS 或 FAILURE");
        }
    }

    private static PaymentResult result(PaymentWritePort.ExistingPayment payment, boolean replayed) {
        return new PaymentResult(payment.paymentId(), payment.paymentNo(), payment.orderId(), payment.status(),
                payment.amount().toPlainString(), "SIMULATED", payment.paidAt(), replayed);
    }
}
