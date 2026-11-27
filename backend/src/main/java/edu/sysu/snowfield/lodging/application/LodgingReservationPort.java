package edu.sysu.snowfield.lodging.application;

import java.time.OffsetDateTime;

/**
 * lodging 模块对外端口：写入住宿占用。
 *
 * <p>并发正确性由数据库排斥约束
 * {@code ex_lodging_reservation_no_overlap} 保证（ARCHITECTURE.md ADR-003），
 * 应用层只负责把冲突翻译成 {@code BOOKING_CONFLICT}，不得用「先查后写」替代约束。</p>
 */
public interface LodgingReservationPort {

    /**
     * 建立一条 HELD 占用。
     *
     * @return reservationId
     * @throws LodgingConflictException 时间区间与既有有效占用重叠
     */
    long hold(long orderItemId, long resourceUnitId, String stayMode,
              OffsetDateTime startAt, OffsetDateTime endAt, int guestCount);

    /** 支付成功后，把订单全部 HELD 住宿占用确认；同时写状态历史。 */
    void confirmForOrder(long orderId);

    /** 取消/过期时释放订单全部有效住宿占用，并写状态历史。 */
    void releaseForOrder(long orderId, String reason);
}
