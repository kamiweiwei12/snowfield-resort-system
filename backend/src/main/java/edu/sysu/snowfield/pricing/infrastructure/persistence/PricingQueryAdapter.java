package edu.sysu.snowfield.pricing.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import edu.sysu.snowfield.pricing.application.PricingQueryPort;
import edu.sysu.snowfield.pricing.domain.PriceCalculator;

/**
 * pricing 查询端口的 MyBatis 适配器。
 */
@Component
public class PricingQueryAdapter implements PricingQueryPort {

    private final PriceRuleMapper priceRuleMapper;

    public PricingQueryAdapter(PriceRuleMapper priceRuleMapper) {
        this.priceRuleMapper = priceRuleMapper;
    }

    @Override
    public List<PriceCalculator.Rule> findRulesFor(long campgroundId, long resourceTypeId,
                                                   String stayMode, LocalDate serviceDate) {
        int isoWeekday = serviceDate.getDayOfWeek().getValue();
        return priceRuleMapper
                .findMatching(campgroundId, resourceTypeId, stayMode, serviceDate, isoWeekday)
                .stream()
                .map(r -> new PriceCalculator.Rule(
                        r.priceRuleId(),
                        r.description() == null ? "(未命名规则)" : r.description(),
                        r.ruleType(),
                        r.operation(),
                        r.value(),
                        r.priority()))
                .toList();
    }
}
