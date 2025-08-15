package com.acme.im.common.infrastructure.nats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 多端设备同步事件DTO
 * 专门处理需要多端同步的业务场景
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class MultiDeviceEvents {

    /**
     * 多端同步事件基类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultiDeviceSyncEvent {
        private String eventType;           // 事件类型
        private Long userId;                // 目标用户ID
        private List<String> targetDevices; // 目标设备列表（空表示所有设备）
        private String excludeDeviceId;     // 排除的设备ID（避免自己收到自己的操作）
        private Map<String, Object> data;  // 事件数据
        private LocalDateTime timestamp;    // 事件时间
        private String sourceDeviceId;      // 源设备ID（触发事件的设备）
        private String sourceService;       // 源服务
        private String sourceInstance;      // 源实例
    }

    /**
     * 用户信息同步事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileSyncEvent {
        private Long userId;
        private String field;           // 变更字段
        private String oldValue;        // 旧值
        private String newValue;        // 新值
        private String sourceDeviceId;  // 源设备ID
        private LocalDateTime updateTime;
    }

    /**
     * 消息状态同步事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageStatusSyncEvent {
        private Long messageId;
        private Long conversationId;
        private String status;          // 消息状态：SENT, DELIVERED, READ, RECALLED, EDITED, DELETED
        private String oldStatus;       // 旧状态
        private Long operatorId;        // 操作者ID
        private String sourceDeviceId;  // 源设备ID
        private LocalDateTime updateTime;
        private Map<String, Object> metadata; // 额外信息
    }

    /**
     * 会话状态同步事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationSyncEvent {
        private Long conversationId;
        private String eventType;       // 事件类型：CREATED, UPDATED, DELETED, MEMBER_ADDED, MEMBER_REMOVED
        private Long operatorId;        // 操作者ID
        private String sourceDeviceId;  // 源设备ID
        private Map<String, Object> data; // 事件数据
        private LocalDateTime eventTime;
    }

    /**
     * 好友关系同步事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendshipSyncEvent {
        private Long userId1;
        private Long userId2;
        private String eventType;       // 事件类型：ESTABLISHED, TERMINATED, STATUS_CHANGED
        private String sourceDeviceId;  // 源设备ID
        private Map<String, Object> data; // 事件数据
        private LocalDateTime eventTime;
    }

    /**
     * 用户状态同步事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusSyncEvent {
        private Long userId;
        private String oldStatus;
        private String newStatus;
        private String reason;
        private String sourceDeviceId;  // 源设备ID
        private LocalDateTime changeTime;
    }

    /**
     * 系统通知同步事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemNotificationSyncEvent {
        private String notificationType;    // 通知类型
        private Long targetUserId;          // 目标用户ID
        private String title;               // 通知标题
        private String content;             // 通知内容
        private String priority;            // 优先级：LOW, NORMAL, HIGH, URGENT
        private Map<String, Object> data;  // 通知数据
        private LocalDateTime createTime;
        private LocalDateTime expireTime;   // 过期时间
    }

    /**
     * 设备管理同步事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceManagementSyncEvent {
        private Long userId;
        private String deviceId;
        private String eventType;       // 事件类型：REGISTERED, UNREGISTERED, STATUS_CHANGED
        private String deviceInfo;      // 设备信息
        private String sourceDeviceId;  // 源设备ID
        private LocalDateTime eventTime;
    }
} 