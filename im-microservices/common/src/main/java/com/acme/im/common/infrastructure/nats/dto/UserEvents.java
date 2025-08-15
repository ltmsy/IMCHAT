package com.acme.im.common.infrastructure.nats.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户相关事件DTO类
 * 统一管理所有用户相关的事件数据结构
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class UserEvents {

    /**
     * 用户上线事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOnlineEvent {
        private Long userId;
        private String deviceId;
        private String sessionId;
        private String clientType; // web, mobile, desktop
        private String clientVersion;
        private String ipAddress;
        private LocalDateTime onlineTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户离线事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOfflineEvent {
        private Long userId;
        private String deviceId;
        private String sessionId;
        private String offlineReason; // logout, timeout, error
        private LocalDateTime offlineTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户状态变更事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusChangeEvent {
        private Long userId;
        private String oldStatus;
        private String newStatus;
        private String reason;
        private String operatorId; // 操作者ID，如果是管理员操作
        private LocalDateTime changeTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户信息更新事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileUpdateEvent {
        private Long userId;
        private String updateField; // nickname, avatar, status, etc.
        private String oldValue;
        private String newValue;
        private String operatorId;
        private LocalDateTime updateTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户权限变更事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPermissionChangeEvent {
        private Long userId;
        private String permissionType; // role, feature, access
        private String oldPermission;
        private String newPermission;
        private String reason;
        private String operatorId;
        private LocalDateTime changeTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户设备管理事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDeviceEvent {
        private Long userId;
        private String deviceId;
        private String eventType; // register, unregister, update
        private String deviceInfo; // 设备信息JSON
        private LocalDateTime eventTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户会话事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSessionEvent {
        private Long userId;
        private String sessionId;
        private String eventType; // create, close, timeout
        private String deviceId;
        private String clientType;
        private LocalDateTime eventTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户活动事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityEvent {
        private Long userId;
        private String activityType; // login, message, file_upload, etc.
        private String activityDetails;
        private Map<String, Object> metadata;
        private LocalDateTime activityTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户注册完成事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegistrationCompletedEvent {
        private Long userId;
        private String username;
        private String email;
        private LocalDateTime registrationTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户信息更新事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileUpdatedEvent {
        private Long userId;
        private String field;
        private String oldValue;
        private String newValue;
        private LocalDateTime updateTime;
        private LocalDateTime timestamp;
    }

    /**
     * 用户状态变更事件（业务层使用）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusChangedEvent {
        private Long userId;
        private String oldStatus;
        private String newStatus;
        private String reason;
        private LocalDateTime changeTime;
        private LocalDateTime timestamp;
    }

    /**
     * Token验证请求事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenValidationRequest {
        private String requestId;
        private String token;
        private String deviceId;
        private Long timestamp;
    }

    /**
     * Token验证响应事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenValidationResponse {
        private String requestId;
        private boolean valid;
        private Long userId;
        private String username;
        private String deviceId;
        private String errorMessage;
        private Long timestamp;
    }
} 