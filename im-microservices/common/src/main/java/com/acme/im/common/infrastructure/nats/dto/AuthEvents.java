package com.acme.im.common.infrastructure.nats.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 认证相关事件DTO类
 * 统一管理所有认证相关的事件数据结构
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class AuthEvents {

    /**
     * 认证请求事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticationRequest {
        private String token;
        private String deviceId;
        private String sessionId;
        private String clientType;
        private String clientVersion;
        private String ipAddress;
        private LocalDateTime requestTime;
        private LocalDateTime timestamp;
    }

    /**
     * 认证结果事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticationResult {
        private String sessionId;
        private String deviceId;
        private Long userId;
        private boolean success;
        private String errorCode;
        private String errorMessage;
        private String userRole;
        private String userPermissions;
        private LocalDateTime authTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户登出事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLogoutEvent {
        private Long userId;
        private String deviceId;
        private String sessionId;
        private String logoutReason; // user_initiated, admin_forced, timeout
        private String operatorId; // 操作者ID，如果是管理员强制登出
        private LocalDateTime logoutTime;
        private LocalDateTime timestamp;
    }

    /**
     * Token刷新事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRefreshEvent {
        private Long userId;
        private String deviceId;
        private String oldToken;
        private String newToken;
        private LocalDateTime refreshTime;
        private LocalDateTime expiresAt;
        private LocalDateTime timestamp;
    }

    /**
     * Token失效事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenInvalidationEvent {
        private Long userId;
        private String deviceId;
        private String token;
        private String invalidationReason; // expired, compromised, admin_revoked
        private String operatorId;
        private LocalDateTime invalidationTime;
        private LocalDateTime timestamp;
    }

    /**
     * 权限验证事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionCheckEvent {
        private Long userId;
        private String resourceType; // message, file, conversation, etc.
        private String resourceId;
        private String action; // read, write, delete, etc.
        private boolean allowed;
        private String reason;
        private LocalDateTime checkTime;
        private LocalDateTime timestamp;
    }

    /**
     * 安全事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityEvent {
        private Long userId;
        private String eventType; // failed_login, suspicious_activity, brute_force
        private String details;
        private String ipAddress;
        private String userAgent;
        private String riskLevel; // low, medium, high, critical
        private LocalDateTime eventTime;
        private LocalDateTime timestamp;
    }
} 