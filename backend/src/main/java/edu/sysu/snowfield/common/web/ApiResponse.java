package edu.sysu.snowfield.common.web;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 统一成功响应体，符合 {@code docs/API_CONTRACT.md} 的成功结构：
 * {@code { success, data, traceId, timestamp }}。
 *
 * <p>时间采用业务时区 Asia/Shanghai（中国大陆营地，无夏令时，恒为 +08:00）。</p>
 */
public record ApiResponse<T>(boolean success, T data, String traceId, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, newTraceId(), now());
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString();
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneId.of("Asia/Shanghai"));
    }
}
