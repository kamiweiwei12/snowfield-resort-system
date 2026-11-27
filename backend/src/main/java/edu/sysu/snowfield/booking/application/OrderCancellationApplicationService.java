package edu.sysu.snowfield.booking.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.sysu.snowfield.common.web.BusinessException;
import edu.sysu.snowfield.common.web.ErrorCode;
import edu.sysu.snowfield.lodging.application.LodgingReservationPort;
import edu.sysu.snowfield.order.application.OrderCancellationPort;
import edu.sysu.snowfield.payment.application.PaymentWritePort;

/** 整单取消编排：订单、明细、支付与住宿占用在同一事务内同步。 */
@Service
public class OrderCancellationApplicationService {

    private final OrderCancellationPort orderCancellation;
    private final PaymentWritePort paymentWrite;
    private final LodgingReservationPort lodgingReservation;

    public OrderCancellationApplicationService(OrderCancellationPort orderCancellation,
                                               PaymentWritePort paymentWrite,
                                               LodgingReservationPort lodgingReservation) {
        this.orderCancellation = orderCancellation;
        this.paymentWrite = paymentWrite;
        this.lodgingReservation = lodgingReservation;
    }

    @Transactional
    public CancelledOrder cancel(CancelOrderCommand command) {
        if (command == null || command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key 不能为空");
        }

        OrderCancellationPort.CancellableOrder order = orderCancellation
                .lockOwnedOrder(command.orderId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if ("CANCELLED".equals(order.status())) {
            return new CancelledOrder(order.orderId(), "CANCELLED",
                    paymentWrite.hasRefundedPayment(order.orderId()), true);
        }
        if (!"PENDING_PAYMENT".equals(order.status()) && !"CONFIRMED".equals(order.status())) {
            throw invalidState(order.status());
        }
        if (orderCancellation.hasStartedItems(order.orderId())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "订单已有开始履约的项目，不能取消");
        }

        boolean refunded = paymentWrite.refundSucceededForOrder(order.orderId()) > 0;
        orderCancellation.cancelOrderAndItems(order.orderId(), command.userId());
        lodgingReservation.releaseForOrder(order.orderId(), "ORDER_CANCELLED");
        return new CancelledOrder(order.orderId(), "CANCELLED", refunded, false);
    }

    private static BusinessException invalidState(String status) {
        return new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                "当前订单状态不允许取消：" + status);
    }
}
