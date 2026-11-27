package edu.sysu.snowfield.payment.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.sysu.snowfield.payment.application.PaymentWritePort;

@Component
public class PaymentWriteAdapter implements PaymentWritePort {

    private final PaymentMapper mapper;

    public PaymentWriteAdapter(PaymentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ExistingPayment> findByOrderAndIdempotencyKey(long orderId, String idempotencyKey) {
        return map(mapper.findByIdempotencyKey(orderId, idempotencyKey));
    }

    @Override
    public Optional<ExistingPayment> findSucceededByOrder(long orderId) {
        return map(mapper.findSucceeded(orderId));
    }

    @Override
    public long create(NewPayment payment) {
        PaymentMapper.PaymentParams params = new PaymentMapper.PaymentParams();
        params.orderId = payment.orderId();
        params.paymentNo = payment.paymentNo();
        params.status = payment.status();
        params.amount = payment.amount();
        params.idempotencyKey = payment.idempotencyKey();
        params.requestFingerprint = payment.requestFingerprint();
        params.paidAt = payment.paidAt();
        mapper.insert(params);
        return params.paymentId;
    }

    @Override
    public int refundSucceededForOrder(long orderId) {
        return mapper.refundSucceeded(orderId);
    }

    @Override
    public boolean hasRefundedPayment(long orderId) {
        return mapper.countRefunded(orderId) > 0;
    }

    private static Optional<ExistingPayment> map(PaymentMapper.PaymentRow row) {
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new ExistingPayment(row.paymentId(), row.paymentNo(), row.orderId(),
                row.status(), row.amount(), row.requestFingerprint(), row.paidAt()));
    }
}
