package edu.sysu.snowfield.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentMapper {

    @Select("""
            SELECT payment_id, payment_no, order_id, status, amount, request_fingerprint, paid_at
            FROM payment
            WHERE order_id = #{orderId} AND idempotency_key = #{idempotencyKey}
            """)
    PaymentRow findByIdempotencyKey(@Param("orderId") long orderId,
                                    @Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT payment_id, payment_no, order_id, status, amount, request_fingerprint, paid_at
            FROM payment
            WHERE order_id = #{orderId} AND status = 'SUCCEEDED'
            """)
    PaymentRow findSucceeded(@Param("orderId") long orderId);

    @Insert("""
            INSERT INTO payment
                (order_id, payment_no, status, amount, method,
                 idempotency_key, request_fingerprint, paid_at)
            VALUES (#{orderId}, #{paymentNo}, #{status}, #{amount}, 'SIMULATED',
                    #{idempotencyKey}, #{requestFingerprint}, #{paidAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "paymentId", keyColumn = "payment_id")
    int insert(PaymentParams params);

    @Update("UPDATE payment SET status = 'REFUNDED' WHERE order_id = #{orderId} AND status = 'SUCCEEDED'")
    int refundSucceeded(@Param("orderId") long orderId);

    @Select("SELECT count(*) FROM payment WHERE order_id = #{orderId} AND status = 'REFUNDED'")
    long countRefunded(@Param("orderId") long orderId);

    class PaymentParams {
        public Long paymentId;
        public long orderId;
        public String paymentNo;
        public String status;
        public BigDecimal amount;
        public String idempotencyKey;
        public String requestFingerprint;
        public OffsetDateTime paidAt;
    }

    record PaymentRow(Long paymentId, String paymentNo, Long orderId, String status,
                      BigDecimal amount, String requestFingerprint, OffsetDateTime paidAt) {
    }
}
