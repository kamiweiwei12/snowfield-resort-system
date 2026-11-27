package edu.sysu.snowfield.common.web;

import java.util.List;
import java.util.Map;

/**
 * 错误详情对象：稳定错误码、面向用户的消息、字段级校验错误与附加细节。
 * {@code details} 用于返回结构化业务上下文（如 PRICE_CHANGED 时的新报价）。
 */
public record ApiError(String code, String message, List<FieldError> fieldErrors, Map<String, Object> details) {

    /**
     * 单个字段的校验错误。
     */
    public record FieldError(String field, String message) {
    }
}
