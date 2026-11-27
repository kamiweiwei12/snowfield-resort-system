package edu.sysu.snowfield.order.infrastructure.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderExpiryMapper {

    @Select("""
            SELECT order_id FROM customer_order
            WHERE status = 'PENDING_PAYMENT' AND expires_at <= now()
            ORDER BY order_id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<Long> lockDueOrderIds(@Param("limit") int limit);

    @Insert("""
            INSERT INTO order_status_history (order_id, from_status, to_status, reason)
            SELECT order_id, status, 'EXPIRED', 'PAYMENT_TIMEOUT'
            FROM customer_order
            WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT'
            """)
    int insertOrderHistory(@Param("orderId") long orderId);

    @Insert("""
            INSERT INTO order_item_status_history (order_item_id, from_status, to_status, reason)
            SELECT order_item_id, status, 'EXPIRED', 'PAYMENT_TIMEOUT'
            FROM order_item
            WHERE order_id = #{orderId} AND status = 'HELD'
            """)
    int insertItemHistories(@Param("orderId") long orderId);

    @Update("UPDATE order_item SET status = 'EXPIRED' WHERE order_id = #{orderId} AND status = 'HELD'")
    int expireItems(@Param("orderId") long orderId);

    @Update("""
            UPDATE customer_order SET status = 'EXPIRED', version = version + 1, updated_at = now()
            WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT'
            """)
    int expireOrder(@Param("orderId") long orderId);
}
