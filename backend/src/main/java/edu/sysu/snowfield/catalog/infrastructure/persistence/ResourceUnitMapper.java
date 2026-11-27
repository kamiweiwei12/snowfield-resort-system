package edu.sysu.snowfield.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * catalog 持久化：资源单元读取（只在本模块内使用，不对外暴露）。
 */
@Mapper
public interface ResourceUnitMapper {

    /**
     * 取资源单元及其类型基础价。`stay_mode` 取该资源类型当前支持的住宿模式中
     * 与查询一致的记录；此处按调用方传入的 stayMode 校验支持性由上层完成，
     * 本查询返回类型的基础价与容量。
     */
    @Select("""
            SELECT ru.resource_unit_id   AS resource_unit_id,
                   rt.resource_type_id   AS resource_type_id,
                   rt.campground_id      AS campground_id,
                   ru.name               AS unit_name,
                   ru.capacity           AS capacity,
                   rt.base_price         AS base_price,
                   ru.status             AS unit_status,
                   rt.status             AS type_status
            FROM resource_unit ru
            JOIN resource_type rt ON rt.resource_type_id = ru.resource_type_id
            WHERE ru.resource_unit_id = #{resourceUnitId}
            """)
    ResourceUnitRow findRowById(@Param("resourceUnitId") long resourceUnitId);

    /** 资源类型是否支持某住宿模式（D-001 要求发布/报价时校验）。 */
    @Select("""
            SELECT count(*) FROM resource_type_stay_mode
            WHERE resource_type_id = #{resourceTypeId} AND stay_mode = #{stayMode}
            """)
    long countStayModeSupport(@Param("resourceTypeId") long resourceTypeId,
                              @Param("stayMode") String stayMode);

    /** 有效维修（OPEN/IN_PROGRESS）与请求半开区间重叠的记录数。 */
    @Select("""
            SELECT count(*)
            FROM resource_maintenance
            WHERE resource_unit_id = #{resourceUnitId}
              AND status IN ('OPEN', 'IN_PROGRESS')
              AND period && tstzrange(#{startAt}, #{endAt}, '[)')
            """)
    long countActiveMaintenanceOverlap(@Param("resourceUnitId") long resourceUnitId,
                                       @Param("startAt") OffsetDateTime startAt,
                                       @Param("endAt") OffsetDateTime endAt);

    /** MyBatis 行记录，仅本模块内部使用。 */
    record ResourceUnitRow(
            Long resourceUnitId,
            Long resourceTypeId,
            Long campgroundId,
            String unitName,
            Integer capacity,
            BigDecimal basePrice,
            String unitStatus,
            String typeStatus) {
    }
}
