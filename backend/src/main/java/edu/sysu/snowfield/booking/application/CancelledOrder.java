package edu.sysu.snowfield.booking.application;

/** 整单取消结果。 */
public record CancelledOrder(long orderId, String status, boolean refunded, boolean replayed) {
}
