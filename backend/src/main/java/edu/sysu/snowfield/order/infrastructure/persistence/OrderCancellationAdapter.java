package edu.sysu.snowfield.order.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.sysu.snowfield.order.application.OrderCancellationPort;

@Component
public class OrderCancellationAdapter implements OrderCancellationPort {

    private final OrderCancellationMapper mapper;

    public OrderCancellationAdapter(OrderCancellationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<CancellableOrder> lockOwnedOrder(long orderId, long userId) {
        OrderCancellationMapper.CancellableOrderRow row = mapper.lockOwnedOrder(orderId, userId);
        return row == null ? Optional.empty()
                : Optional.of(new CancellableOrder(row.orderId(), row.userId(), row.status()));
    }

    @Override
    public boolean hasStartedItems(long orderId) {
        return mapper.countStartedItems(orderId) > 0;
    }

    @Override
    public void cancelOrderAndItems(long orderId, long changedBy) {
        mapper.insertOrderHistory(orderId, changedBy);
        mapper.insertItemHistories(orderId, changedBy);
        mapper.cancelItems(orderId);
        mapper.cancelOrder(orderId);
    }
}
