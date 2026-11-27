package edu.sysu.snowfield.lodging.infrastructure.persistence;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * lodging 持久化：住宿占用写入。
 */
@Mapper
public interface LodgingReservationMapper {

    /**
     * 插入 HELD 占用。重叠时由排斥约束 {@code ex_lodging_reservation_no_overlap}
     * 抛出 SQLState 23P01，由适配器翻译。
     *
     * <p>时间区间用半开 `[start, end)`（AGENTS.md §3）。</p>
     */
    @Insert("""
            INSERT INTO lodging_reservation
                (order_item_id, resource_unit_id, stay_mode, stay_period, guest_count, occupancy_status)
            VALUES (#{orderItemId}, #{resourceUnitId}, #{stayMode},
                    tstzrange(#{startAt}, #{endAt}, '[)'), #{guestCount}, 'HELD')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "reservationId", keyColumn = "reservation_id")
    int insertHold(HoldParams params);

    /** 用参数对象承载生成键回填（MyBatis 注解 SQL 需要可变对象）。 */
    class HoldParams {
        public Long reservationId;
        public long orderItemId;
        public long resourceUnitId;
        public String stayMode;
        public OffsetDateTime startAt;
        public OffsetDateTime endAt;
        public int guestCount;
    }

    /** 释放占用：RELEASED 是终态，且会从排斥谓词中排除（契约 §住宿/日营占用）。 */
    @Insert("""
            UPDATE lodging_reservation
            SET occupancy_status = 'RELEASED', updated_at = now()
            WHERE reservation_id = #{reservationId}
              AND occupancy_status IN ('HELD','CONFIRMED','IN_USE')
            """)
    int release(@Param("reservationId") long reservationId);

    @Insert("""
            INSERT INTO lodging_reservation_status_history
                (reservation_id, from_status, to_status, reason)
            SELECT r.reservation_id, r.occupancy_status, 'CONFIRMED', 'SIMULATED_PAYMENT_SUCCEEDED'
            FROM lodging_reservation r
            JOIN order_item i ON i.order_item_id = r.order_item_id
            WHERE i.order_id = #{orderId} AND r.occupancy_status = 'HELD'
            """)
    int insertConfirmHistories(@Param("orderId") long orderId);

    @Update("""
            UPDATE lodging_reservation r
            SET occupancy_status = 'CONFIRMED', updated_at = now()
            FROM order_item i
            WHERE i.order_item_id = r.order_item_id
              AND i.order_id = #{orderId}
              AND r.occupancy_status = 'HELD'
            """)
    int confirmForOrder(@Param("orderId") long orderId);

    @Insert("""
            INSERT INTO lodging_reservation_status_history
                (reservation_id, from_status, to_status, reason)
            SELECT r.reservation_id, r.occupancy_status, 'RELEASED', #{reason}
            FROM lodging_reservation r
            JOIN order_item i ON i.order_item_id = r.order_item_id
            WHERE i.order_id = #{orderId}
              AND r.occupancy_status IN ('HELD', 'CONFIRMED', 'IN_USE')
            """)
    int insertReleaseHistories(@Param("orderId") long orderId, @Param("reason") String reason);

    @Update("""
            UPDATE lodging_reservation r
            SET occupancy_status = 'RELEASED', updated_at = now()
            FROM order_item i
            WHERE i.order_item_id = r.order_item_id
              AND i.order_id = #{orderId}
              AND r.occupancy_status IN ('HELD', 'CONFIRMED', 'IN_USE')
            """)
    int releaseForOrder(@Param("orderId") long orderId);
}
