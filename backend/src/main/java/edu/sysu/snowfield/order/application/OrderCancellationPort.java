package edu.sysu.snowfield.order.application;

import java.util.Optional;

/** 取消用例对 order 模块的公开端口。 */
public interface OrderCancellationPort {

    Optional<CancellableOrder> lockOwnedOrder(long orderId, long userId);

    boolean hasStartedItems(long orderId);

    void cancelOrderAndItems(long orderId, long changedBy);

    record CancellableOrder(long orderId, long userId, String status) {
    }
}
