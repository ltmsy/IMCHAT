package com.acme.im.common.websocket.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * WebSocket响应对象
 * 支持多种响应类型和状态
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketResponse<T> {

    /**
     * 响应ID
     */
    private String responseId;

    /**
     * 对应的请求ID
     */
    private String requestId;

    /**
     * 响应类型
     */
    private String type;

    /**
     * 响应子类型
     */
    private String subType;

    /**
     * 响应时间
     */
    private LocalDateTime timestamp;

    /**
     * 响应状态
     */
    private String status; // SUCCESS, FAILED, PENDING, TIMEOUT

    /**
     * 响应代码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 错误详情
     */
    private ErrorDetail error;

    /**
     * 响应元数据
     */
    private Map<String, Object> metadata;

    /**
     * 响应标签
     */
    private String[] tags;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;

    /**
     * 服务器ID
     */
    private String serverId;

    /**
     * 响应版本
     */
    private String version;

    /**
     * 是否压缩
     */
    private Boolean compressed;

    /**
     * 是否加密
     */
    private Boolean encrypted;

    /**
     * 签名
     */
    private String signature;

    /**
     * 创建成功响应
     */
    public static <T> WebSocketResponse<T> success(String requestId, T data) {
        return WebSocketResponse.<T>builder()
                .responseId(generateResponseId())
                .requestId(requestId)
                .type("RESPONSE")
                .subType("SUCCESS")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .code(0)
                .message("操作成功")
                .data(data)
                .metadata(new HashMap<>())
                .tags(new String[0])
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static <T> WebSocketResponse<T> failure(String requestId, Integer code, String message) {
        return WebSocketResponse.<T>builder()
                .responseId(generateResponseId())
                .requestId(requestId)
                .type("RESPONSE")
                .subType("FAILURE")
                .timestamp(LocalDateTime.now())
                .status("FAILED")
                .code(code)
                .message(message)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build())
                .metadata(new HashMap<>())
                .tags(new String[0])
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建认证成功响应
     */
    public static WebSocketResponse<AuthenticationResponse> authSuccess(String requestId, String userId, String sessionId, String token) {
        AuthenticationResponse authData = AuthenticationResponse.builder()
                .userId(userId)
                .sessionId(sessionId)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .permissions(new String[]{"message:send", "message:receive", "user:read"})
                .build();

        return WebSocketResponse.<AuthenticationResponse>builder()
                .responseId(generateResponseId())
                .requestId(requestId)
                .type("AUTHENTICATION")
                .subType("LOGIN_SUCCESS")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .code(0)
                .message("认证成功")
                .data(authData)
                .metadata(new HashMap<>())
                .tags(new String[]{"security", "authentication"})
                .version("1.0")
                .compressed(false)
                .encrypted(true)
                .build();
    }

    /**
     * 创建认证失败响应
     */
    public static WebSocketResponse<AuthenticationResponse> authFailure(String requestId, Integer code, String message) {
        return WebSocketResponse.<AuthenticationResponse>builder()
                .responseId(generateResponseId())
                .requestId(requestId)
                .type("AUTHENTICATION")
                .subType("LOGIN_FAILURE")
                .timestamp(LocalDateTime.now())
                .status("FAILED")
                .code(code)
                .message(message)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build())
                .metadata(new HashMap<>())
                .tags(new String[]{"security", "authentication"})
                .version("1.0")
                .compressed(false)
                .encrypted(true)
                .build();
    }

    /**
     * 创建消息发送成功响应
     */
    public static WebSocketResponse<MessageResponse> messageSuccess(String requestId, String messageId, String status) {
        MessageResponse messageData = MessageResponse.builder()
                .messageId(messageId)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketResponse.<MessageResponse>builder()
                .responseId(generateResponseId())
                .requestId(requestId)
                .type("MESSAGE")
                .subType("SEND_SUCCESS")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .code(0)
                .message("消息发送成功")
                .data(messageData)
                .metadata(new HashMap<>())
                .tags(new String[]{"message", "communication"})
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建订阅成功响应
     */
    public static WebSocketResponse<SubscriptionResponse> subscriptionSuccess(String requestId, String subscriptionId, String topic) {
        SubscriptionResponse subscriptionData = SubscriptionResponse.builder()
                .subscriptionId(subscriptionId)
                .topic(topic)
                .status("ACTIVE")
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketResponse.<SubscriptionResponse>builder()
                .responseId(generateResponseId())
                .requestId(requestId)
                .type("SUBSCRIPTION")
                .subType("SUBSCRIBE_SUCCESS")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .code(0)
                .message("订阅成功")
                .data(subscriptionData)
                .metadata(new HashMap<>())
                .tags(new String[]{"subscription", "event"})
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建心跳响应
     */
    public static WebSocketResponse<HeartbeatResponse> heartbeatResponse(String requestId, String userId, String sessionId) {
        HeartbeatResponse heartbeatData = HeartbeatResponse.builder()
                .userId(userId)
                .sessionId(sessionId)
                .status("ALIVE")
                .timestamp(LocalDateTime.now())
                .serverTime(LocalDateTime.now())
                .build();

        return WebSocketResponse.<HeartbeatResponse>builder()
                .responseId(generateResponseId())
                .requestId(requestId)
                .type("HEARTBEAT")
                .subType("PONG")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .code(0)
                .message("心跳响应")
                .data(heartbeatData)
                .metadata(new HashMap<>())
                .tags(new String[]{"heartbeat", "connection"})
                .version("1.0")
                .compressed(true)
                .encrypted(false)
                .build();
    }

    /**
     * 创建通知响应
     */
    public static WebSocketResponse<NotificationResponse> notification(String userId, String type, String title, String content) {
        NotificationResponse notificationData = NotificationResponse.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        return WebSocketResponse.<NotificationResponse>builder()
                .responseId(generateResponseId())
                .requestId(null) // 通知不需要请求ID
                .type("NOTIFICATION")
                .subType(type.toUpperCase())
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .code(0)
                .message("通知")
                .data(notificationData)
                .metadata(new HashMap<>())
                .tags(new String[]{"notification", "system"})
                .version("1.0")
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 添加元数据
     */
    public WebSocketResponse<T> addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }

    /**
     * 添加标签
     */
    public WebSocketResponse<T> addTag(String tag) {
        if (tags == null) {
            tags = new String[0];
        }
        String[] newTags = new String[tags.length + 1];
        System.arraycopy(tags, 0, newTags, 0, tags.length);
        newTags[tags.length] = tag;
        this.tags = newTags;
        return this;
    }

    /**
     * 设置处理时间
     */
    public WebSocketResponse<T> setProcessingTime(long startTime) {
        this.processingTime = System.currentTimeMillis() - startTime;
        return this;
    }

    /**
     * 设置服务器ID
     */
    public WebSocketResponse<T> setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    /**
     * 启用压缩
     */
    public WebSocketResponse<T> enableCompression() {
        this.compressed = true;
        return this;
    }

    /**
     * 启用加密
     */
    public WebSocketResponse<T> enableEncryption() {
        this.encrypted = true;
        return this;
    }

    /**
     * 生成响应ID
     */
    private static String generateResponseId() {
        return "RESP_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 错误详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDetail {
        private Integer code;
        private String message;
        private String details;
        private String stackTrace;
        private LocalDateTime timestamp;
        private String errorType;
        private String errorCategory;
        private Map<String, Object> context;
    }

    /**
     * 认证响应数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthenticationResponse {
        private String userId;
        private String sessionId;
        private String token;
        private LocalDateTime expiresAt;
        private String[] permissions;
        private String[] roles;
        private String clientId;
        private String deviceId;
        private LocalDateTime lastLoginTime;
        private String loginLocation;
        private Map<String, Object> attributes;
    }

    /**
     * 消息响应数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private String messageId;
        private String status;
        private LocalDateTime timestamp;
        private String deliveryStatus;
        private LocalDateTime deliveredAt;
        private LocalDateTime readAt;
        private String[] recipients;
        private Map<String, Object> attributes;
    }

    /**
     * 订阅响应数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubscriptionResponse {
        private String subscriptionId;
        private String topic;
        private String status;
        private LocalDateTime timestamp;
        private LocalDateTime expiresAt;
        private String[] filters;
        private String notificationLevel;
        private Map<String, Object> attributes;
    }

    /**
     * 心跳响应数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeartbeatResponse {
        private String userId;
        private String sessionId;
        private String status;
        private LocalDateTime timestamp;
        private LocalDateTime serverTime;
        private Long latency;
        private String serverStatus;
        private Map<String, Object> metrics;
    }

    /**
     * 通知响应数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationResponse {
        private String userId;
        private String type;
        private String title;
        private String content;
        private LocalDateTime timestamp;
        private Boolean read;
        private String priority;
        private String[] actions;
        private Map<String, Object> data;
        private String[] tags;
    }
} 