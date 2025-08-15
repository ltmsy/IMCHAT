package com.acme.im.common.infrastructure.nats.entity;

import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事件记录实体
 * 用于持久化存储重要事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRecord {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件主题
     */
    private String subject;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件状态
     */
    private String status;

    /**
     * 事件优先级
     */
    private String priority;

    /**
     * 源服务名称
     */
    private String sourceService;

    /**
     * 源实例ID
     */
    private String sourceInstance;

    /**
     * 目标服务名称
     */
    private String targetService;

    /**
     * 目标实例ID
     */
    private String targetInstance;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 事件数据（JSON格式）
     */
    private String eventData;

    /**
     * 事件元数据（JSON格式）
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;

    /**
     * 是否已持久化
     */
    private Boolean persisted;

    /**
     * 持久化时间
     */
    private LocalDateTime persistedAt;

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries && EventTopics.EventStatus.FAILURE.equals(status);
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * 标记为已持久化
     */
    public void markAsPersisted() {
        this.persisted = true;
        this.persistedAt = LocalDateTime.now();
    }

    /**
     * 检查是否为重要事件（用于决定是否持久化）
     */
    public boolean isImportantEvent() {
        return EventTopics.Common.Auth.VALIDATE.equals(subject) ||
               EventTopics.Common.Auth.RESULT.equals(subject) ||
               EventTopics.Admin.System.NOTIFICATION.equals(subject) ||
               EventTopics.EventStatus.FAILURE.equals(status) ||
               EventTopics.EventPriority.HIGH.equals(priority) ||
               EventTopics.EventPriority.URGENT.equals(priority);
    }
} 