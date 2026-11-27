package edu.sysu.snowfield.booking.application;

/** 整单取消命令；userId 后续必须来自认证上下文。 */
public record CancelOrderCommand(long orderId, long userId, String idempotencyKey) {
}
