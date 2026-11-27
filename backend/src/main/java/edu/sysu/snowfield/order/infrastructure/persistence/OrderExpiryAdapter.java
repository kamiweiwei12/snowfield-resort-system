package edu.sysu.snowfield.order.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.sysu.snowfield.order.application.OrderExpiryPort;

@Component
public class OrderExpiryAdapter implements OrderExpiryPort {

    private final OrderExpiryMapper mapper;

    public OrderExpiryAdapter(OrderExpiryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Long> lockDueOrderIds(int limit) {
        return mapper.lockDueOrderIds(limit);
    }

    @Override
    public void expireOrderAndItems(long orderId) {
        mapper.insertOrderHistory(orderId);
        mapper.insertItemHistories(orderId);
        mapper.expireItems(orderId);
        mapper.expireOrder(orderId);
    }
}
