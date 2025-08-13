package com.acme.im.common.websocket.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * WebSocket请求对象
 * 支持多种请求类型和参数
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketRequest<T> {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 请求类型
     */
    private String type;

    /**
     * 请求子类型
     */
    private String subType;

    /**
     * 请求时间
     */
    private LocalDateTime timestamp;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 请求版本
     */
    private String version;

    /**
     * 请求数据
     */
    private T data;

    /**
     * 请求参数
     */
    private Map<String, Object> parameters;

    /**
     * 请求元数据
     */
    private Map<String, Object> metadata;

    /**
     * 请求标签
     */
    private String[] tags;

    /**
     * 请求优先级
     */
    private Integer priority;

    /**
     * 是否异步
     */
    private Boolean async;

    /**
     * 超时时间（毫秒）
     */
    private Long timeout;

    /**
     * 重试次数
     */
    private Integer retryCount;

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
     * 创建默认请求对象
     */
    public static <T> WebSocketRequest<T> create() {
        return WebSocketRequest.<T>builder()
                .requestId(generateRequestId())
                .timestamp(LocalDateTime.now())
                .version("1.0")
                .parameters(new HashMap<>())
                .metadata(new HashMap<>())
                .tags(new String[0])
                .priority(5)
                .async(false)
                .timeout(30000L)
                .retryCount(0)
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建认证请求
     */
    public static WebSocketRequest<AuthenticationRequest> createAuthRequest(String userId, String token) {
        AuthenticationRequest authData = AuthenticationRequest.builder()
                .userId(userId)
                .token(token)
                .build();

        return WebSocketRequest.<AuthenticationRequest>builder()
                .requestId(generateRequestId())
                .type("AUTHENTICATION")
                .subType("LOGIN")
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .version("1.0")
                .data(authData)
                .parameters(new HashMap<>())
                .metadata(new HashMap<>())
                .tags(new String[]{"security", "authentication"})
                .priority(10)
                .async(false)
                .timeout(10000L)
                .retryCount(0)
                .compressed(false)
                .encrypted(true)
                .build();
    }

    /**
     * 创建消息发送请求
     */
    public static WebSocketRequest<MessageRequest> createMessageRequest(String userId, String targetId, String content, String messageType) {
        MessageRequest messageData = MessageRequest.builder()
                .senderId(userId)
                .targetId(targetId)
                .content(content)
                .messageType(messageType)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketRequest.<MessageRequest>builder()
                .requestId(generateRequestId())
                .type("MESSAGE")
                .subType("SEND")
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .version("1.0")
                .data(messageData)
                .parameters(new HashMap<>())
                .metadata(new HashMap<>())
                .tags(new String[]{"message", "communication"})
                .priority(5)
                .async(false)
                .timeout(30000L)
                .retryCount(3)
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建订阅请求
     */
    public static WebSocketRequest<SubscriptionRequest> createSubscriptionRequest(String userId, String topic, String[] filters) {
        SubscriptionRequest subscriptionData = SubscriptionRequest.builder()
                .userId(userId)
                .topic(topic)
                .filters(filters != null ? filters : new String[0])
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketRequest.<SubscriptionRequest>builder()
                .requestId(generateRequestId())
                .type("SUBSCRIPTION")
                .subType("SUBSCRIBE")
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .version("1.0")
                .data(subscriptionData)
                .parameters(new HashMap<>())
                .metadata(new HashMap<>())
                .tags(new String[]{"subscription", "event"})
                .priority(3)
                .async(false)
                .timeout(10000L)
                .retryCount(0)
                .compressed(false)
                .encrypted(false)
                .build();
    }

    /**
     * 创建心跳请求
     */
    public static WebSocketRequest<HeartbeatRequest> createHeartbeatRequest(String userId, String sessionId) {
        HeartbeatRequest heartbeatData = HeartbeatRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .build();

        return WebSocketRequest.<HeartbeatRequest>builder()
                .requestId(generateRequestId())
                .type("HEARTBEAT")
                .subType("PING")
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .sessionId(sessionId)
                .version("1.0")
                .data(heartbeatData)
                .parameters(new HashMap<>())
                .metadata(new HashMap<>())
                .tags(new String[]{"heartbeat", "connection"})
                .priority(1)
                .async(true)
                .timeout(5000L)
                .retryCount(0)
                .compressed(true)
                .encrypted(false)
                .build();
    }

    /**
     * 添加参数
     */
    public WebSocketRequest<T> addParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
        return this;
    }

    /**
     * 添加元数据
     */
    public WebSocketRequest<T> addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }

    /**
     * 添加标签
     */
    public WebSocketRequest<T> addTag(String tag) {
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
     * 设置高优先级
     */
    public WebSocketRequest<T> setHighPriority() {
        this.priority = 8;
        return this;
    }

    /**
     * 设置紧急优先级
     */
    public WebSocketRequest<T> setUrgentPriority() {
        this.priority = 10;
        return this;
    }

    /**
     * 启用压缩
     */
    public WebSocketRequest<T> enableCompression() {
        this.compressed = true;
        return this;
    }

    /**
     * 启用加密
     */
    public WebSocketRequest<T> enableEncryption() {
        this.encrypted = true;
        return this;
    }

    /**
     * 设置异步
     */
    public WebSocketRequest<T> setAsync() {
        this.async = true;
        return this;
    }

    /**
     * 生成请求ID
     */
    private static String generateRequestId() {
        return "REQ_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 认证请求数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthenticationRequest {
        private String userId;
        private String token;
        private String clientType;
        private String clientVersion;
        private String deviceId;
        private String deviceInfo;
        private String location;
        private String timezone;
    }

    /**
     * 消息请求数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageRequest {
        private String senderId;
        private String targetId;
        private String targetType; // USER, GROUP, BROADCAST
        private String content;
        private String messageType; // TEXT, IMAGE, FILE, VOICE, VIDEO
        private String contentType; // application/json, text/plain, etc.
        private Long contentSize;
        private String contentHash;
        private LocalDateTime timestamp;
        private String[] mentions;
        private String[] tags;
        private Map<String, Object> attributes;
        private Boolean replyTo;
        private String replyToMessageId;
        private Boolean forward;
        private String forwardFromMessageId;
        private Boolean urgent;
        private Boolean silent;
    }

    /**
     * 订阅请求数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubscriptionRequest {
        private String userId;
        private String topic;
        private String[] filters;
        private String subscriptionType; // PERSISTENT, TEMPORARY
        private LocalDateTime timestamp;
        private String[] tags;
        private Map<String, Object> attributes;
        private Boolean enableNotifications;
        private String notificationLevel; // ALL, IMPORTANT, CRITICAL
    }

    /**
     * 心跳请求数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeartbeatRequest {
        private String userId;
        private String sessionId;
        private LocalDateTime timestamp;
        private String clientStatus; // ONLINE, AWAY, BUSY, OFFLINE
        private String deviceStatus; // ACTIVE, IDLE, SLEEP
        private Long batteryLevel;
        private String networkType; // WIFI, MOBILE, ETHERNET
        private String networkQuality; // EXCELLENT, GOOD, FAIR, POOR
        private Map<String, Object> metrics;
    }
} 