package edu.sysu.snowfield.booking.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 创建住宿订单的用例输入（booking 模块的 application 层输入模型，
 * 与 api 层 DTO 分离，保证 Controller 不把 HTTP DTO 直接透传进事务）。
 */
public record CreateLodgingOrderCommand(
        long userId,
        String idempotencyKey,
        List<LodgingLine> lodgings,
        String expectedTotal) {

    /** 一行住宿选择：对应 API 契约的 `LodgingSelection`（无 quantity，数量由数组长度表达）。 */
    public record LodgingLine(
            long resourceUnitId,
            String stayMode,
            java.time.OffsetDateTime startAt,
            java.time.OffsetDateTime endAt,
            int guestCount) {
    }

    /**
     * 规范化请求指纹：同键不同请求必须能被识别（契约：IDEMPOTENCY_CONFLICT）。
     *
     * <p>返回 **SHA-256 十六进制摘要（64 字符）**，而非原始拼接串：</p>
     * <ul>
     *   <li>`customer_order.create_request_fingerprint` 是 `varchar(128)`，多间房的原始拼接串会超长；</li>
     *   <li>摘要定长，且不把用户输入的原始内容散落到数据库列里。</li>
     * </ul>
     * <p>比较前先对行序排序，保证同一购物车的不同提交顺序得到相同指纹。</p>
     */
    public String fingerprint() {
        StringBuilder raw = new StringBuilder();
        raw.append("expectedTotal=").append(expectedTotal == null ? "" : expectedTotal).append('|');
        List<String> lines = new ArrayList<>();
        for (LodgingLine line : lodgings) {
            lines.add(line.resourceUnitId() + ":" + line.stayMode() + ":"
                    + line.startAt() + ":" + line.endAt() + ":" + line.guestCount());
        }
        lines.sort(String::compareTo);
        raw.append(String.join(",", lines));
        return sha256Hex(raw.toString());
    }

    private static String sha256Hex(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            // SHA-256 是 JLS 要求每个 JVM 必须提供的算法，此处不可达。
            throw new IllegalStateException("JVM 缺少 SHA-256", ex);
        }
    }

    /**
     * 按营地业务时区展开服务日。
     * OVERNIGHT 使用半开住宿夜区间；DAY_PASS 在同一业务日内计一个服务日。
     */
    public static List<LocalDate> serviceDates(String stayMode,
                                               OffsetDateTime startAt,
                                               OffsetDateTime endAt,
                                               ZoneId businessZone) {
        LocalDate from = startAt.atZoneSameInstant(businessZone).toLocalDate();
        LocalDate to = endAt.atZoneSameInstant(businessZone).toLocalDate();
        if ("DAY_PASS".equals(stayMode)) {
            return from.equals(to) ? List.of(from) : List.of();
        }
        if (!"OVERNIGHT".equals(stayMode)) {
            return List.of();
        }
        Map<LocalDate, Boolean> dates = new LinkedHashMap<>();
        for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
            dates.put(d, Boolean.TRUE);
        }
        return List.copyOf(dates.keySet());
    }

    /** 空值安全的可选访问，避免编排层散落 null 判断。 */
    public static <T> Optional<T> opt(T value) {
        return Optional.ofNullable(value);
    }

    /** 便利：把可空金额转成非空。 */
    public static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
