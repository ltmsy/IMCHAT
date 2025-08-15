package com.acme.im.common.infrastructure.nats.exception;

import lombok.Getter;

/**
 * 事件处理异常
 * 用于统一处理事件处理过程中的各种异常情况
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Getter
public class EventProcessingException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 事件主题
     */
    private final String eventSubject;

    /**
     * 事件ID
     */
    private final String eventId;

    /**
     * 是否可重试
     */
    private final boolean retryable;

    /**
     * 构造函数
     */
    public EventProcessingException(String message, String errorCode, String eventSubject, String eventId) {
        this(message, errorCode, eventSubject, eventId, false, null);
    }

    /**
     * 构造函数
     */
    public EventProcessingException(String message, String errorCode, String eventSubject, String eventId, boolean retryable) {
        this(message, errorCode, eventSubject, eventId, retryable, null);
    }

    /**
     * 构造函数
     */
    public EventProcessingException(String message, String errorCode, String eventSubject, String eventId, boolean retryable, Throwable cause) {
        super(String.format("事件处理失败 [%s]: %s (事件主题: %s, 事件ID: %s)", errorCode, message, eventSubject, eventId), cause);
        this.errorCode = errorCode;
        this.eventSubject = eventSubject;
        this.eventId = eventId;
        this.retryable = retryable;
    }

    /**
     * 创建数据验证异常
     */
    public static EventProcessingException dataValidationError(String message, String eventSubject, String eventId) {
        return new EventProcessingException(message, "DATA_VALIDATION_ERROR", eventSubject, eventId, false);
    }

    /**
     * 创建业务逻辑异常
     */
    public static EventProcessingException businessLogicError(String message, String eventSubject, String eventId) {
        return new EventProcessingException(message, "BUSINESS_LOGIC_ERROR", eventSubject, eventId, false);
    }

    /**
     * 创建系统异常（可重试）
     */
    public static EventProcessingException systemError(String message, String eventSubject, String eventId, Throwable cause) {
        return new EventProcessingException(message, "SYSTEM_ERROR", eventSubject, eventId, true, cause);
    }

    /**
     * 创建超时异常（可重试）
     */
    public static EventProcessingException timeoutError(String message, String eventSubject, String eventId) {
        return new EventProcessingException(message, "TIMEOUT_ERROR", eventSubject, eventId, true);
    }

    /**
     * 创建权限异常
     */
    public static EventProcessingException permissionError(String message, String eventSubject, String eventId) {
        return new EventProcessingException(message, "PERMISSION_ERROR", eventSubject, eventId, false);
    }
} 