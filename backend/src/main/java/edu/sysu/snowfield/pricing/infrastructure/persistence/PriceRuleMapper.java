package edu.sysu.snowfield.pricing.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * pricing 持久化：匹配某日生效的价格规则。
 */
@Mapper
public interface PriceRuleMapper {

    /**
     * 筛选某日对某资源生效的规则。
     *
     * <p>匹配条件（契约 §价格）：</p>
     * <ul>
     *   <li>同营地、已启用、日期在有效期内</li>
     *   <li>资源类型为本类型，或为 NULL（D-001 方案B 全营地规则）</li>
     *   <li>stay_mode 为本模式，或为 NULL</li>
     *   <li>若规则配置了星期限制，则当日星期必须命中</li>
     *   <li>若规则配置了特定日期，则当日必须命中</li>
     * </ul>
     */
    @Select("""
            SELECT pr.price_rule_id, pr.description, pr.rule_type, pr.operation, pr.value, pr.priority
            FROM price_rule pr
            WHERE pr.campground_id = #{campgroundId}
              AND pr.enabled
              AND pr.valid_from <= #{serviceDate}
              AND pr.valid_to   >= #{serviceDate}
              AND (pr.resource_type_id = #{resourceTypeId} OR pr.resource_type_id IS NULL)
              AND (pr.stay_mode = #{stayMode} OR pr.stay_mode IS NULL)
              AND (NOT EXISTS (SELECT 1 FROM price_rule_weekday w WHERE w.price_rule_id = pr.price_rule_id)
                   OR EXISTS (SELECT 1 FROM price_rule_weekday w
                              WHERE w.price_rule_id = pr.price_rule_id
                                AND w.weekday = #{isoWeekday}))
              AND (NOT EXISTS (SELECT 1 FROM price_rule_date d WHERE d.price_rule_id = pr.price_rule_id)
                   OR EXISTS (SELECT 1 FROM price_rule_date d
                              WHERE d.price_rule_id = pr.price_rule_id
                                AND d.service_date = #{serviceDate}))
            ORDER BY pr.priority DESC, pr.price_rule_id
            """)
    List<PriceRuleRow> findMatching(@Param("campgroundId") long campgroundId,
                                    @Param("resourceTypeId") long resourceTypeId,
                                    @Param("stayMode") String stayMode,
                                    @Param("serviceDate") LocalDate serviceDate,
                                    @Param("isoWeekday") int isoWeekday);

    record PriceRuleRow(
            Long priceRuleId,
            String description,
            String ruleType,
            String operation,
            BigDecimal value,
            Integer priority) {
    }
}
