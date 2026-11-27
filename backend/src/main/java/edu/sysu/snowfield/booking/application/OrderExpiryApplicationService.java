package edu.sysu.snowfield.booking.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.sysu.snowfield.lodging.application.LodgingReservationPort;
import edu.sysu.snowfield.order.application.OrderExpiryPort;

/** 待支付订单过期扫描；每批使用行锁与 SKIP LOCKED 支持多实例安全竞争。 */
@Service
public class OrderExpiryApplicationService {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final OrderExpiryPort orderExpiry;
    private final LodgingReservationPort lodgingReservation;

    public OrderExpiryApplicationService(OrderExpiryPort orderExpiry,
                                         LodgingReservationPort lodgingReservation) {
        this.orderExpiry = orderExpiry;
        this.lodgingReservation = lodgingReservation;
    }

    @Scheduled(initialDelayString = "${snowfield.order-expiry-initial-delay-ms:60000}",
               fixedDelayString = "${snowfield.order-expiry-scan-ms:60000}")
    @Transactional
    public int expireDueOrders() {
        int expired = 0;
        for (long orderId : orderExpiry.lockDueOrderIds(DEFAULT_BATCH_SIZE)) {
            orderExpiry.expireOrderAndItems(orderId);
            lodgingReservation.releaseForOrder(orderId, "ORDER_EXPIRED");
            expired++;
        }
        return expired;
    }
}
