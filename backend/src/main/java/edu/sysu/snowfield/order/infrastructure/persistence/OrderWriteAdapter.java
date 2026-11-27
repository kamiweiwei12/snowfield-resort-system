package edu.sysu.snowfield.order.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.sysu.snowfield.order.application.OrderWritePort;

/**
 * order 写端口的 MyBatis 适配器。
 */
@Component
public class OrderWriteAdapter implements OrderWritePort {

    private final OrderMapper orderMapper;

    public OrderWriteAdapter(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public void lockCreateIdempotencyKey(long userId, String idempotencyKey) {
        orderMapper.lockCreateIdempotencyKey(userId, idempotencyKey);
    }

    @Override
    public long createOrder(NewOrder order) {
        OrderMapper.NewOrderParams params = new OrderMapper.NewOrderParams();
        params.orderNo = order.orderNo();
        params.userId = order.userId();
        params.campgroundId = order.campgroundId();
        params.status = order.status();
        params.totalAmount = order.totalAmount();
        params.idempotencyKey = order.idempotencyKey();
        params.requestFingerprint = order.requestFingerprint();
        params.expiresAt = order.expiresAt();
        orderMapper.insertOrder(params);
        return params.orderId;
    }

    @Override
    public long createItem(NewItem item) {
        OrderMapper.NewItemParams params = new OrderMapper.NewItemParams();
        params.orderId = item.orderId();
        params.itemType = item.itemType();
        params.itemNameSnapshot = item.itemNameSnapshot();
        params.status = item.status();
        params.quantity = item.quantity();
        params.unitPrice = item.unitPrice();
        params.subtotal = item.subtotal();
        params.priceExplanationJson = item.priceExplanationJson();
        orderMapper.insertItem(params);
        return params.orderItemId;
    }

    @Override
    public void createSnapshotLines(List<SnapshotLine> lines) {
        for (SnapshotLine line : lines) {
            OrderMapper.SnapshotParams params = new OrderMapper.SnapshotParams();
            params.orderItemId = line.orderItemId();
            params.serviceDate = line.serviceDate();
            params.ruleId = line.ruleId();
            params.ruleNameSnapshot = line.ruleNameSnapshot();
            params.operationSnapshot = line.operationSnapshot();
            params.ruleValueSnapshot = line.ruleValueSnapshot();
            params.amount = line.amount();
            orderMapper.insertSnapshotLine(params);
        }
    }

    @Override
    public Optional<ExistingOrder> findByUserAndIdempotencyKey(long userId, String idempotencyKey) {
        OrderMapper.ExistingOrderRow row = orderMapper.findByIdempotencyKey(userId, idempotencyKey);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new ExistingOrder(
                row.orderId(), row.orderNo(), row.createRequestFingerprint(), row.status()));
    }

    @Override
    public Optional<java.math.BigDecimal> findTotalByOrderId(long orderId) {
        return Optional.ofNullable(orderMapper.findTotalByOrderId(orderId));
    }
}
