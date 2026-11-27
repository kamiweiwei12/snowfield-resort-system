package edu.sysu.snowfield.order.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/** 支付用例对 order 模块的公开端口。 */
public interface OrderPaymentPort {

    Optional<PayableOrder> lockOwnedOrder(long orderId, long userId);

    void confirmOrderAndItems(long orderId, long changedBy);

    record PayableOrder(long orderId, long userId, String status,
                        BigDecimal totalAmount, OffsetDateTime expiresAt) {
    }
}
