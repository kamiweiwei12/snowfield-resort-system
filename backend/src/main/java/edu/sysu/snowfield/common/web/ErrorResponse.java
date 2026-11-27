package edu.sysu.snowfield.common.web;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一错误响应体，符合 {@code docs/API_CONTRACT.md} 的错误结构：
 * {@code { success:false, error:{ code, message, fieldErrors, details }, traceId, timestamp }}。
 */
public record ErrorResponse(boolean success, ApiError error, String traceId, OffsetDateTime timestamp) {

    public static ErrorResponse of(String code, String message,
                                   List<ApiError.FieldError> fieldErrors,
                                   Map<String, Object> details) {
        List<ApiError.FieldError> fe = fieldErrors == null ? List.of() : fieldErrors;
        Map<String, Object> d = details == null ? Map.of() : details;
        return new ErrorResponse(false, new ApiError(code, message, fe, d),
                UUID.randomUUID().toString(), OffsetDateTime.now(ZoneId.of("Asia/Shanghai")));
    }
}
