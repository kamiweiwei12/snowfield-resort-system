package edu.sysu.snowfield.booking.application;

/**
 * 创建住宿订单的结果（应用层输出，api 层再映射为 OrderResponse）。
 */
public record CreatedLodgingOrder(
        long orderId,
        String orderNo,
        String status,
        String totalAmount,
        boolean replayed) {
}
