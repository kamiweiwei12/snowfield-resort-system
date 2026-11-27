package edu.sysu.snowfield.common.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把异常统一映射为 {@link ErrorResponse}，不向客户端泄漏堆栈、SQL 或内部路径。
 * 数据库唯一/排斥约束的 SQL 异常由各模块适配为 {@link BusinessException} 后在此返回稳定错误码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getErrorCode().defaultMessage();
        return build(ex.getErrorCode(), message, ex.getFieldErrors(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, null, fieldErrors, Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, null, fieldErrors, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.VALIDATION_FAILED, null, List.of(), Map.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(ErrorCode.VALIDATION_FAILED, null,
                List.of(new ApiError.FieldError(ex.getHeaderName(), "请求头不能为空")), Map.of());
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMethodValidation(Exception ex) {
        return build(ErrorCode.VALIDATION_FAILED, null, List.of(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, null, List.of(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        // S1：仅返回稳定 500；详细日志与审计在 S2/S3 接入 SLF4J 后再补。
        return build(ErrorCode.INTERNAL_ERROR, null, List.of(), Map.of());
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String messageOverride,
                                                List<ApiError.FieldError> fieldErrors,
                                                Map<String, Object> details) {
        String message = messageOverride != null ? messageOverride : code.defaultMessage();
        return ResponseEntity.status(code.httpStatus())
                .body(ErrorResponse.of(code.code(), message, fieldErrors, details));
    }
}
