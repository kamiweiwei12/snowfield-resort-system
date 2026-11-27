package edu.sysu.snowfield.catalog.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import edu.sysu.snowfield.catalog.application.CatalogQueryPort;
import edu.sysu.snowfield.catalog.application.ResourceSnapshot;

/**
 * catalog 查询端口的 MyBatis 适配器。
 *
 * <p>只把「当前可售」的资源单元视为存在，停用/退役返回 empty，
 * 由 booking 转成 {@code RESOURCE_NOT_FOUND}。</p>
 */
@Component
public class CatalogQueryAdapter implements CatalogQueryPort {

    private final ResourceUnitMapper resourceUnitMapper;

    public CatalogQueryAdapter(ResourceUnitMapper resourceUnitMapper) {
        this.resourceUnitMapper = resourceUnitMapper;
    }

    @Override
    public Optional<ResourceSnapshot> findResourceUnit(long resourceUnitId) {
        ResourceUnitMapper.ResourceUnitRow row = resourceUnitMapper.findRowById(resourceUnitId);
        if (row == null || !isSellable(row.unitStatus()) || !isSellable(row.typeStatus())) {
            return Optional.empty();
        }
        return Optional.of(new ResourceSnapshot(
                row.resourceUnitId(),
                row.resourceTypeId(),
                row.campgroundId(),
                row.unitName(),
                null, // stayMode 由调用方按选择项校验，见 supportsStayMode
                row.capacity(),
                row.basePrice(),
                true));
    }

    @Override
    public boolean supportsStayMode(long resourceTypeId, String stayMode) {
        if (stayMode == null || stayMode.isBlank()) {
            return false;
        }
        return resourceUnitMapper.countStayModeSupport(resourceTypeId, stayMode) > 0;
    }

    @Override
    public boolean hasActiveMaintenanceOverlap(long resourceUnitId,
                                               OffsetDateTime startAt,
                                               OffsetDateTime endAt) {
        return resourceUnitMapper.countActiveMaintenanceOverlap(resourceUnitId, startAt, endAt) > 0;
    }

    private static boolean isSellable(String status) {
        // ResourceStatus 枚举只有 ACTIVE/MAINTENANCE/INACTIVE/RETIRED（docs/ENUMS.md）；
        // 只有 ACTIVE 可售，其余一律视为不可预订。
        return "ACTIVE".equals(status);
    }
}
