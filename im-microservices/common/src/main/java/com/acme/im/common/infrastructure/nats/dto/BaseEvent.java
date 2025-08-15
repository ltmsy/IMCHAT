package com.acme.im.common.infrastructure.nats.dto;

import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 基础事件类
 * 所有NATS事件的基础结构
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseEvent<T> {

    /**
     * 事件ID - 唯一标识
     */
    private String eventId;

    /**
     * 事件主题
     */
    private String subject;

    /**
     * 事件类型：REQUEST, RESPONSE, NOTIFICATION, BROADCAST
     */
    private String eventType;

    /**
     * 事件状态：SUCCESS, FAILURE, PENDING, TIMEOUT
     */
    private String status;

    /**
     * 事件优先级：URGENT, HIGH, MEDIUM, LOW
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
     * 目标服务名称（可选）
     */
    private String targetService;

    /**
     * 目标实例ID（可选）
     */
    private String targetInstance;

    /**
     * 用户ID（可选）
     */
    private String userId;

    /**
     * 设备ID（可选）
     */
    private String deviceId;

    /**
     * 会话ID（可选）
     */
    private String sessionId;

    /**
     * 事件数据
     */
    private T data;

    /**
     * 事件元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间（可选）
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
     * 错误信息（可选）
     */
    private String errorMessage;

    /**
     * 错误代码（可选）
     */
    private String errorCode;

    /**
     * 创建新事件
     */
    public static <T> BaseEvent<T> create(String subject, String eventType, T data) {
        return BaseEvent.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .subject(subject)
                .eventType(eventType)
                .status(EventTopics.EventStatus.PENDING)
                .priority(EventTopics.EventPriority.MEDIUM)
                .data(data)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    /**
     * 创建请求事件
     */
    public static <T> BaseEvent<T> createRequest(String subject, T data) {
        return create(subject, EventTopics.EventType.REQUEST, data);
    }

    /**
     * 创建响应事件
     */
    public static <T> BaseEvent<T> createResponse(String subject, T data) {
        return create(subject, EventTopics.EventType.RESPONSE, data);
    }

    /**
     * 创建通知事件
     */
    public static <T> BaseEvent<T> createNotification(String subject, T data) {
        return create(subject, EventTopics.EventType.NOTIFICATION, data);
    }

    /**
     * 创建广播事件
     */
    public static <T> BaseEvent<T> createBroadcast(String subject, T data) {
        return create(subject, EventTopics.EventType.BROADCAST, data);
    }

    /**
     * 设置成功状态
     */
    public BaseEvent<T> success() {
        this.status = EventTopics.EventStatus.SUCCESS;
        return this;
    }

    /**
     * 设置失败状态
     */
    public BaseEvent<T> failure(String errorMessage, String errorCode) {
        this.status = EventTopics.EventStatus.FAILURE;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        return this;
    }

    /**
     * 设置高优先级
     */
    public BaseEvent<T> highPriority() {
        this.priority = EventTopics.EventPriority.HIGH;
        return this;
    }

    /**
     * 设置紧急优先级
     */
    public BaseEvent<T> urgentPriority() {
        this.priority = EventTopics.EventPriority.URGENT;
        return this;
    }

    /**
     * 添加元数据
     */
    public BaseEvent<T> addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * 设置源服务信息
     */
    public BaseEvent<T> fromService(String serviceName, String instanceId) {
        this.sourceService = serviceName;
        this.sourceInstance = instanceId;
        return this;
    }

    /**
     * 设置目标服务信息
     */
    public BaseEvent<T> toService(String serviceName, String instanceId) {
        this.targetService = serviceName;
        this.targetInstance = instanceId;
        return this;
    }

    /**
     * 设置用户上下文
     */
    public BaseEvent<T> withUser(String userId, String deviceId, String sessionId) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.sessionId = sessionId;
        return this;
    }

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
        return retryCount < maxRetries && status.equals(EventTopics.EventStatus.FAILURE);
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
} 