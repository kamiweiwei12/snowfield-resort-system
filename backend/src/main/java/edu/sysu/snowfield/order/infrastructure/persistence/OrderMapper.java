package edu.sysu.snowfield.order.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * order 持久化：订单主体、明细、价格快照。
 */
@Mapper
public interface OrderMapper {

    /**
     * PostgreSQL 事务级 advisory lock：锁随事务提交/回滚自动释放。
     * 64 位 hash 碰撞只会造成无害的额外串行化，不影响正确性。
     */
    @Select("""
            SELECT pg_advisory_xact_lock(
                hashtextextended(CAST(#{userId} AS text) || ':' || #{idempotencyKey}, 0)
            )
            """)
    String lockCreateIdempotencyKey(@Param("userId") long userId,
                                    @Param("idempotencyKey") String idempotencyKey);

    class NewOrderParams {
        public Long orderId;
        public String orderNo;
        public long userId;
        public long campgroundId;
        public String status;
        public BigDecimal totalAmount;
        public String idempotencyKey;
        public String requestFingerprint;
        public OffsetDateTime expiresAt;
    }

    @Insert("""
            INSERT INTO customer_order
                (order_no, user_id, campground_id, status, total_amount,
                 create_idempotency_key, create_request_fingerprint, expires_at)
            VALUES (#{orderNo}, #{userId}, #{campgroundId}, #{status}, #{totalAmount},
                    #{idempotencyKey}, #{requestFingerprint}, #{expiresAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "orderId", keyColumn = "order_id")
    int insertOrder(NewOrderParams params);

    class NewItemParams {
        public Long orderItemId;
        public long orderId;
        public String itemType;
        public String itemNameSnapshot;
        public String status;
        public int quantity;
        public BigDecimal unitPrice;
        public BigDecimal subtotal;
        public String priceExplanationJson;
    }

    @Insert("""
            INSERT INTO order_item
                (order_id, item_type, item_name_snapshot, status, quantity, unit_price, subtotal, price_explanation)
            VALUES (#{orderId}, #{itemType}, #{itemNameSnapshot}, #{status}, #{quantity}, #{unitPrice}, #{subtotal},
                    #{priceExplanationJson}::jsonb)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "orderItemId", keyColumn = "order_item_id")
    int insertItem(NewItemParams params);

    class SnapshotParams {
        public long orderItemId;
        public LocalDate serviceDate;
        public Long ruleId;
        public String ruleNameSnapshot;
        public String operationSnapshot;
        public BigDecimal ruleValueSnapshot;
        public BigDecimal amount;
    }

    @Insert("""
            INSERT INTO price_snapshot_line
                (order_item_id, service_date, rule_id, rule_name_snapshot,
                 operation_snapshot, rule_value_snapshot, amount)
            VALUES (#{orderItemId}, #{serviceDate}, #{ruleId}, #{ruleNameSnapshot},
                    #{operationSnapshot}, #{ruleValueSnapshot}, #{amount})
            """)
    int insertSnapshotLine(SnapshotParams params);

    /** 幂等重放：按 (user_id, key) 查既有订单。 */
    @Select("""
            SELECT order_id, order_no, create_request_fingerprint, status
            FROM customer_order
            WHERE user_id = #{userId} AND create_idempotency_key = #{idempotencyKey}
            """)
    ExistingOrderRow findByIdempotencyKey(@Param("userId") long userId,
                                          @Param("idempotencyKey") String idempotencyKey);

    record ExistingOrderRow(Long orderId, String orderNo, String createRequestFingerprint, String status) {
    }

    /** 取订单总额（幂等重放用；不重算，避免规则变更影响历史单）。 */
    @Select("SELECT total_amount FROM customer_order WHERE order_id = #{orderId}")
    BigDecimal findTotalByOrderId(@Param("orderId") long orderId);
}
