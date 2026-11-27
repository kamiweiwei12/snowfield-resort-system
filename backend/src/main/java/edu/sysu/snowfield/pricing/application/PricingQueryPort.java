package edu.sysu.snowfield.pricing.application;

import java.time.LocalDate;
import java.util.List;

import edu.sysu.snowfield.pricing.domain.PriceCalculator;

/**
 * pricing 模块对外的报价端口：给定资源与日期，返回该日匹配的价格规则。
 *
 * <p>匹配范围按契约 §价格：营地、资源类型/住宿模式、有效期、星期/日期。
 * D-001 方案B：`resource_type_id IS NULL` 的规则属于全营地范围，也参与匹配；
 * 调用方需保证资源类型实际支持该 `stay_mode`。</p>
 */
public interface PricingQueryPort {

    /** 取某日对某资源生效的全部规则（未做优先级过滤，由领域层判定冲突）。 */
    List<PriceCalculator.Rule> findRulesFor(
            long campgroundId,
            long resourceTypeId,
            String stayMode,
            LocalDate serviceDate);
}
