package edu.sysu.snowfield.order.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderCancellationMapper {

    @Select("""
            SELECT order_id, user_id, status FROM customer_order
            WHERE order_id = #{orderId} AND user_id = #{userId}
            FOR UPDATE
            """)
    CancellableOrderRow lockOwnedOrder(@Param("orderId") long orderId, @Param("userId") long userId);

    @Select("""
            SELECT count(*) FROM order_item
            WHERE order_id = #{orderId} AND status IN ('IN_USE', 'FULFILLED')
            """)
    long countStartedItems(@Param("orderId") long orderId);

    @Insert("""
            INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, reason)
            SELECT order_id, status, 'CANCELLED', #{changedBy}, 'ORDER_CANCELLED'
            FROM customer_order
            WHERE order_id = #{orderId} AND status IN ('PENDING_PAYMENT', 'CONFIRMED')
            """)
    int insertOrderHistory(@Param("orderId") long orderId, @Param("changedBy") long changedBy);

    @Insert("""
            INSERT INTO order_item_status_history (order_item_id, from_status, to_status, changed_by, reason)
            SELECT order_item_id, status, 'CANCELLED', #{changedBy}, 'ORDER_CANCELLED'
            FROM order_item
            WHERE order_id = #{orderId} AND status IN ('HELD', 'CONFIRMED')
            """)
    int insertItemHistories(@Param("orderId") long orderId, @Param("changedBy") long changedBy);

    @Update("""
            UPDATE order_item SET status = 'CANCELLED'
            WHERE order_id = #{orderId} AND status IN ('HELD', 'CONFIRMED')
            """)
    int cancelItems(@Param("orderId") long orderId);

    @Update("""
            UPDATE customer_order SET status = 'CANCELLED', version = version + 1, updated_at = now()
            WHERE order_id = #{orderId} AND status IN ('PENDING_PAYMENT', 'CONFIRMED')
            """)
    int cancelOrder(@Param("orderId") long orderId);

    record CancellableOrderRow(Long orderId, Long userId, String status) {
    }
}
