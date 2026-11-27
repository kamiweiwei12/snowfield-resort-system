package edu.sysu.snowfield.payment.application;

/** 模拟支付应用命令；userId 来自后续认证上下文，不接受客户端冒充。 */
public record SimulatePaymentCommand(
        long orderId,
        long userId,
        String idempotencyKey,
        String desiredOutcome) {

    public String normalizedOutcome() {
        return desiredOutcome == null ? null : desiredOutcome.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public String fingerprint() {
        return "desiredOutcome=" + normalizedOutcome();
    }
}
