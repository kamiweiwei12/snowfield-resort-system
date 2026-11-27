package edu.sysu.snowfield.common.web;

import org.springframework.http.HttpStatus;

/**
 * 稳定业务错误码，与 {@code docs/API_CONTRACT.md} 第 4 节的错误码一一对应。
 * 数据库唯一/排斥约束必须映射为这里的稳定码，不得把 SQL 细节返回前端。
 */
public enum ErrorCode {

    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数校验失败"),
    INVALID_TIME_RANGE("INVALID_TIME_RANGE", HttpStatus.BAD_REQUEST, "时间区间无效"),
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", HttpStatus.BAD_REQUEST, "非法状态转换"),

    AUTH_REQUIRED("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "需要认证"),
    TOKEN_INVALID("TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "令牌无效"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "令牌已过期"),

    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "无访问权限"),

    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "订单不存在"),

    BOOKING_CONFLICT("BOOKING_CONFLICT", HttpStatus.CONFLICT, "所选资源已被预订"),
    CAPACITY_EXCEEDED("CAPACITY_EXCEEDED", HttpStatus.CONFLICT, "容量不足"),
    INVENTORY_INSUFFICIENT("INVENTORY_INSUFFICIENT", HttpStatus.CONFLICT, "库存不足"),
    ALREADY_VERIFIED("ALREADY_VERIFIED", HttpStatus.CONFLICT, "已核销"),
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT, "幂等冲突"),
    PRICE_CHANGED("PRICE_CHANGED", HttpStatus.CONFLICT, "价格已变化"),
    PRICE_RULE_CONFLICT("PRICE_RULE_CONFLICT", HttpStatus.CONFLICT, "价格规则冲突"),

    ORDER_EXPIRED("ORDER_EXPIRED", HttpStatus.UNPROCESSABLE_CONTENT, "订单已过期"),
    RESOURCE_NOT_OPEN("RESOURCE_NOT_OPEN", HttpStatus.UNPROCESSABLE_CONTENT, "资源未开放"),
    PACKAGE_NOT_AVAILABLE("PACKAGE_NOT_AVAILABLE", HttpStatus.UNPROCESSABLE_CONTENT, "套餐不可用"),

    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
