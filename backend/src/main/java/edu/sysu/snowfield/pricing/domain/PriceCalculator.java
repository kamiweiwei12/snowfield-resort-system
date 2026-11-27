package edu.sysu.snowfield.pricing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 住宿逐日价格计算（纯领域逻辑，不依赖 Spring/MyBatis/DB，符合 R9）。
 *
 * <p>契约 §价格：`base_price` 是唯一基础价；先筛选营地/资源类型/住宿模式/有效期/星期/日期，
 * 再取**唯一**最高优先级规则胜出；同一有效报价单元 `(campgroundId, resourceTypeId, stayMode, serviceDate)`
 * 在最高优先级上出现多条即为冲突，**不得**以创建时间或主键静默选胜者。</p>
 */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    /** 一条价格规则（领域视图）。 */
    public record Rule(
            long ruleId,
            String name,
            String ruleType,
            String operation,
            BigDecimal value,
            int priority) {
    }

    /** 冲突信息：同一优先级上有多条规则。 */
    public record Conflict(long priority, List<String> ruleNames) {
    }

    /**
     * 计算单日最终价。
     *
     * @param basePrice 资源类型基础价
     * @param matching  该日已过滤的有效规则
     * @return 价格与胜出规则；若冲突则返回 conflict
     */
    public static Result priceFor(BigDecimal basePrice, List<Rule> matching) {
        if (matching == null || matching.isEmpty()) {
            return Result.priced(basePrice.setScale(2, RoundingMode.HALF_UP), null);
        }

        int maxPriority = matching.stream().mapToInt(Rule::priority).max().orElseThrow();
        List<Rule> top = matching.stream().filter(r -> r.priority() == maxPriority).toList();

        if (top.size() > 1) {
            return Result.conflicted(
                    basePrice.setScale(2, RoundingMode.HALF_UP),
                    new Conflict(maxPriority, top.stream().map(Rule::name).toList()));
        }
        Rule winner = top.getFirst();
        BigDecimal price = apply(basePrice, winner);
        return Result.priced(price.setScale(2, RoundingMode.HALF_UP), winner);
    }

    /** FIXED：最终价 = value；MULTIPLIER：base × value；ADDITION：base + value。 */
    public static BigDecimal apply(BigDecimal basePrice, Rule rule) {
        return switch (rule.operation()) {
            case "FIXED" -> rule.value();
            case "MULTIPLIER" -> basePrice.multiply(rule.value());
            case "ADDITION" -> basePrice.add(rule.value());
            default -> throw new IllegalArgumentException("未知价格操作：" + rule.operation());
        };
    }

    /**
     * 结果：价格 + 胜出规则；conflict 非空表示冲突。
     *
     * <p>用静态工厂而不是重载构造，避免 {@code new Result(price, null)} 的歧义。</p>
     */
    public record Result(BigDecimal unitPrice, Rule winner, Conflict conflict) {

        public static Result priced(BigDecimal unitPrice, Rule winner) {
            return new Result(unitPrice, winner, null);
        }

        public static Result conflicted(BigDecimal unitPrice, Conflict conflict) {
            return new Result(unitPrice, null, conflict);
        }

        public boolean hasConflict() {
            return conflict != null;
        }

        /** 可解释价格说明（写入 order_item.price_explanation）。 */
        public String explanation(LocalDate date) {
            if (winner == null) {
                return "基础价";
            }
            return "%s（%s）：%s".formatted(winner.name(), winner.operation(), winner.value().toPlainString());
        }
    }

    /** 供调用方判断是否命中某条规则的有效期。 */
    public static boolean coversDate(LocalDate validFrom, LocalDate validTo, LocalDate date) {
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }

    /** 便利：把可能为空的规则包装成 Optional，避免调用点散落 null 判断。 */
    public static Optional<Rule> orEmpty(Rule rule) {
        return Optional.ofNullable(rule);
    }
}
