package edu.sysu.snowfield.lodging.infrastructure.persistence;

import java.sql.SQLException;
import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import edu.sysu.snowfield.lodging.application.LodgingConflictException;
import edu.sysu.snowfield.lodging.application.LodgingReservationPort;

/**
 * 住宿占用端口实现：把数据库排斥约束冲突翻译为 {@link LodgingConflictException}。
 *
 * <p>关键点：**不在应用层先查询再插入**。排斥约束是并发正确性的唯一来源，
 * 先查后写存在竞态（DEVELOPMENT.md 风险 3）。</p>
 */
@Component
public class LodgingReservationAdapter implements LodgingReservationPort {

    private static final String EXCLUSION_VIOLATION = "23P01";

    private final LodgingReservationMapper mapper;

    public LodgingReservationAdapter(LodgingReservationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long hold(long orderItemId, long resourceUnitId, String stayMode,
                     OffsetDateTime startAt, OffsetDateTime endAt, int guestCount) {
        LodgingReservationMapper.HoldParams params = new LodgingReservationMapper.HoldParams();
        params.orderItemId = orderItemId;
        params.resourceUnitId = resourceUnitId;
        params.stayMode = stayMode;
        params.startAt = startAt;
        params.endAt = endAt;
        params.guestCount = guestCount;
        try {
            mapper.insertHold(params);
        } catch (DataIntegrityViolationException ex) {
            if (isExclusionViolation(ex)) {
                throw new LodgingConflictException(
                        "资源 " + resourceUnitId + " 在所选时段已被占用");
            }
            throw ex;
        }
        return params.reservationId;
    }

    @Override
    public void confirmForOrder(long orderId) {
        mapper.insertConfirmHistories(orderId);
        mapper.confirmForOrder(orderId);
    }

    @Override
    public void releaseForOrder(long orderId, String reason) {
        mapper.insertReleaseHistories(orderId, reason);
        mapper.releaseForOrder(orderId);
    }

    private static boolean isExclusionViolation(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof SQLException sql && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
            if (cursor.getMessage() != null && cursor.getMessage().contains("ex_lodging_reservation_no_overlap")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
