package edu.sysu.snowfield.lodging.application;

/**
 * 住宿时段冲突：由数据库排斥约束触发，翻译为稳定业务错误码前的中途异常。
 *
 * <p>不继承 {@code BusinessException}，以保持 lodging 模块不依赖 common.web 协议层；
 * 由 booking 编排层翻译为 {@code BOOKING_CONFLICT}。</p>
 */
public class LodgingConflictException extends RuntimeException {

    public LodgingConflictException(String message) {
        super(message);
    }
}
