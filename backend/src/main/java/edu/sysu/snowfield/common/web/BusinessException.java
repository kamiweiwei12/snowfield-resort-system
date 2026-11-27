package edu.sysu.snowfield.common.web;

import java.util.List;
import java.util.Map;

/**
 * 业务异常：携带稳定 {@link ErrorCode} 与可选的字段级/结构化细节，由
 * {@link GlobalExceptionHandler} 统一转换为 {@link ErrorResponse}。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ApiError.FieldError> fieldErrors;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
        this.details = Map.of();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
        this.details = Map.of();
    }

    /**
     * 带自定义消息与结构化细节（如 {@code PRICE_CHANGED} 附上新报价值）。
     * 字段级错误列表为空，需要字段错误时用三参构造。
     */
    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
        this.details = details == null ? Map.of() : details;
    }

    public BusinessException(ErrorCode errorCode, List<ApiError.FieldError> fieldErrors,
                             Map<String, Object> details) {
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
        this.details = details == null ? Map.of() : details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiError.FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
