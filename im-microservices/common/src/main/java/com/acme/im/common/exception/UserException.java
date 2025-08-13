package com.acme.im.common.exception;

/**
 * 用户相关异常
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class UserException extends RuntimeException {
    
    private final String errorCode;
    
    public UserException(String message) {
        super(message);
        this.errorCode = "USER_ERROR";
    }
    
    public UserException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public UserException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "USER_ERROR";
    }
    
    public UserException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
} 