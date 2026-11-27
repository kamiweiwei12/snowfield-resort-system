package edu.sysu.snowfield.catalog.application;

import java.math.BigDecimal;

/**
 * catalog 模块对外的只读公开契约：其他模块（如 booking）经此取资源信息，
 * 不得直接访问 catalog 的 Mapper（ARCHITECTURE.md §2.2 R10）。
 *
 * @param resourceUnitId 资源单元 ID
 * @param resourceTypeId 所属资源类型 ID
 * @param campgroundId   所属营地 ID
 * @param name           具体资源名称（用于订单历史快照）
 * @param stayMode       该资源的住宿模式
 * @param capacity       单元容量
 * @param basePrice      资源类型基础价（唯一基础价来源，契约 §价格）
 * @param active         是否可售（非 RETIRED/INACTIVE）
 */
public record ResourceSnapshot(
        long resourceUnitId,
        long resourceTypeId,
        long campgroundId,
        String name,
        String stayMode,
        int capacity,
        BigDecimal basePrice,
        boolean active) {
}
