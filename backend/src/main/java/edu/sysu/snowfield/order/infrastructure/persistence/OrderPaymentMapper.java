package edu.sysu.snowfield.order.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderPaymentMapper {

    @Select("""
            SELECT order_id, user_id, status, total_amount, expires_at
            FROM customer_order
            WHERE order_id = #{orderId} AND user_id = #{userId}
            FOR UPDATE
            """)
    PayableOrderRow lockOwnedOrder(@Param("orderId") long orderId, @Param("userId") long userId);

    @Insert("""
            INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, reason)
            SELECT order_id, status, 'CONFIRMED', #{changedBy}, 'SIMULATED_PAYMENT_SUCCEEDED'
            FROM customer_order WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT'
            """)
    int insertOrderHistory(@Param("orderId") long orderId, @Param("changedBy") long changedBy);

    @Insert("""
            INSERT INTO order_item_status_history (order_item_id, from_status, to_status, changed_by, reason)
            SELECT order_item_id, status, 'CONFIRMED', #{changedBy}, 'SIMULATED_PAYMENT_SUCCEEDED'
            FROM order_item WHERE order_id = #{orderId} AND status = 'HELD'
            """)
    int insertItemHistories(@Param("orderId") long orderId, @Param("changedBy") long changedBy);

    @Update("UPDATE order_item SET status = 'CONFIRMED' WHERE order_id = #{orderId} AND status = 'HELD'")
    int confirmItems(@Param("orderId") long orderId);

    @Update("""
            UPDATE customer_order
            SET status = 'CONFIRMED', version = version + 1, updated_at = now()
            WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT'
            """)
    int confirmOrder(@Param("orderId") long orderId);

    record PayableOrderRow(Long orderId, Long userId, String status,
                           BigDecimal totalAmount, OffsetDateTime expiresAt) {
    }
}
