package com.acme.im.common.exception;

import com.acme.im.common.response.ResponseCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑异常
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误状态码
     */
    private final int code;

    /**
     * 错误状态码枚举
     */
    private final ResponseCode responseCode;

    /**
     * 构造函数
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResponseCode.ERROR.getCode();
        this.responseCode = ResponseCode.ERROR;
    }

    /**
     * 构造函数
     */
    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
        this.responseCode = responseCode;
    }

    /**
     * 构造函数
     */
    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
        this.responseCode = responseCode;
    }

    /**
     * 构造函数
     */
    public BusinessException(ResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.code = responseCode.getCode();
        this.responseCode = responseCode;
    }

    /**
     * 构造函数
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.responseCode = ResponseCode.fromCode(code);
    }

    /**
     * 构造函数
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.responseCode = ResponseCode.fromCode(code);
    }

    /**
     * 创建用户不存在异常
     */
    public static BusinessException userNotFound() {
        return new BusinessException(ResponseCode.USER_NOT_FOUND);
    }

    /**
     * 创建用户不存在异常
     */
    public static BusinessException userNotFound(String message) {
        return new BusinessException(ResponseCode.USER_NOT_FOUND, message);
    }

    /**
     * 创建用户已存在异常
     */
    public static BusinessException userAlreadyExists() {
        return new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
    }

    /**
     * 创建用户已存在异常
     */
    public static BusinessException userAlreadyExists(String message) {
        return new BusinessException(ResponseCode.USER_ALREADY_EXISTS, message);
    }

    /**
     * 创建无效凭据异常
     */
    public static BusinessException invalidCredentials() {
        return new BusinessException(ResponseCode.INVALID_CREDENTIALS);
    }

    /**
     * 创建无效凭据异常
     */
    public static BusinessException invalidCredentials(String message) {
        return new BusinessException(ResponseCode.INVALID_CREDENTIALS, message);
    }

    /**
     * 创建权限不足异常
     */
    public static BusinessException insufficientPermissions() {
        return new BusinessException(ResponseCode.INSUFFICIENT_PERMISSIONS);
    }

    /**
     * 创建权限不足异常
     */
    public static BusinessException insufficientPermissions(String message) {
        return new BusinessException(ResponseCode.INSUFFICIENT_PERMISSIONS, message);
    }

    /**
     * 创建令牌过期异常
     */
    public static BusinessException tokenExpired() {
        return new BusinessException(ResponseCode.TOKEN_EXPIRED);
    }

    /**
     * 创建令牌过期异常
     */
    public static BusinessException tokenExpired(String message) {
        return new BusinessException(ResponseCode.TOKEN_EXPIRED, message);
    }

    /**
     * 创建令牌无效异常
     */
    public static BusinessException tokenInvalid() {
        return new BusinessException(ResponseCode.TOKEN_INVALID);
    }

    /**
     * 创建令牌无效异常
     */
    public static BusinessException tokenInvalid(String message) {
        return new BusinessException(ResponseCode.TOKEN_INVALID, message);
    }

    /**
     * 创建消息发送失败异常
     */
    public static BusinessException messageSendFailed() {
        return new BusinessException(ResponseCode.MESSAGE_SEND_FAILED);
    }

    /**
     * 创建消息发送失败异常
     */
    public static BusinessException messageSendFailed(String message) {
        return new BusinessException(ResponseCode.MESSAGE_SEND_FAILED, message);
    }

    /**
     * 创建文件上传失败异常
     */
    public static BusinessException fileUploadFailed() {
        return new BusinessException(ResponseCode.FILE_UPLOAD_FAILED);
    }

    /**
     * 创建文件上传失败异常
     */
    public static BusinessException fileUploadFailed(String message) {
        return new BusinessException(ResponseCode.FILE_UPLOAD_FAILED, message);
    }

    /**
     * 创建文件不存在异常
     */
    public static BusinessException fileNotFound() {
        return new BusinessException(ResponseCode.FILE_NOT_FOUND);
    }

    /**
     * 创建文件不存在异常
     */
    public static BusinessException fileNotFound(String message) {
        return new BusinessException(ResponseCode.FILE_NOT_FOUND, message);
    }

    /**
     * 创建数据库错误异常
     */
    public static BusinessException databaseError(String message) {
        return new BusinessException(ResponseCode.DATABASE_ERROR, message);
    }

    /**
     * 创建缓存错误异常
     */
    public static BusinessException cacheError(String message) {
        return new BusinessException(ResponseCode.CACHE_ERROR, message);
    }

    /**
     * 创建外部服务错误异常
     */
    public static BusinessException externalServiceError(String message) {
        return new BusinessException(ResponseCode.EXTERNAL_SERVICE_ERROR, message);
    }

    /**
     * 创建网络错误异常
     */
    public static BusinessException networkError(String message) {
        return new BusinessException(ResponseCode.NETWORK_ERROR, message);
    }

    /**
     * 创建超时错误异常
     */
    public static BusinessException timeoutError(String message) {
        return new BusinessException(ResponseCode.TIMEOUT_ERROR, message);
    }
} 