package edu.sysu.snowfield.order.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.sysu.snowfield.order.application.OrderPaymentPort;

@Component
public class OrderPaymentAdapter implements OrderPaymentPort {

    private final OrderPaymentMapper mapper;

    public OrderPaymentAdapter(OrderPaymentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<PayableOrder> lockOwnedOrder(long orderId, long userId) {
        OrderPaymentMapper.PayableOrderRow row = mapper.lockOwnedOrder(orderId, userId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new PayableOrder(
                row.orderId(), row.userId(), row.status(), row.totalAmount(), row.expiresAt()));
    }

    @Override
    public void confirmOrderAndItems(long orderId, long changedBy) {
        mapper.insertOrderHistory(orderId, changedBy);
        mapper.insertItemHistories(orderId, changedBy);
        mapper.confirmItems(orderId);
        mapper.confirmOrder(orderId);
    }
}
