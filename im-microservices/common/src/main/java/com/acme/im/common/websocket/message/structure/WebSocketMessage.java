package com.acme.im.common.websocket.message.structure;

import com.acme.im.common.websocket.message.types.MessageType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket消息结构
 * 定义统一的消息格式
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
public class WebSocketMessage<T> {

    /**
     * 消息ID（全局唯一）
     */
    private String messageId;

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 消息类型代码（兼容性）
     */
    private String typeCode;

    /**
     * 消息状态
     */
    private MessageStatus status;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 接收者ID（可以是用户ID、群组ID等）
     */
    private String receiverId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息内容
     */
    private T payload;

    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 消息序列号（会话内唯一）
     */
    private Long sequence;

    /**
     * 客户端消息ID（幂等性）
     */
    private String clientMessageId;

    /**
     * 消息版本
     */
    private String version = "1.0";

    /**
     * 消息来源
     */
    private String source;

    /**
     * 消息目标
     */
    private String target;

    /**
     * 消息优先级
     */
    private MessagePriority priority = MessagePriority.NORMAL;

    /**
     * 消息标签
     */
    private String[] tags;

    /**
     * 扩展字段
     */
    private Map<String, Object> extensions;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 构造函数
     */
    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.PENDING;
        this.priority = MessagePriority.NORMAL;
    }

    /**
     * 构造函数
     */
    public WebSocketMessage(MessageType type, T payload) {
        this();
        this.type = type;
        this.typeCode = type.getCode();
        this.payload = payload;
    }

    /**
     * 构造函数
     */
    public WebSocketMessage(MessageType type, String senderId, String receiverId, T payload) {
        this(type, payload);
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    /**
     * 构造函数
     */
    public WebSocketMessage(MessageType type, String senderId, String receiverId, String conversationId, T payload) {
        this(type, senderId, receiverId, payload);
        this.conversationId = conversationId;
    }

    /**
     * 设置消息类型
     */
    public void setType(MessageType type) {
        this.type = type;
        this.typeCode = type != null ? type.getCode() : null;
    }

    /**
     * 设置消息状态
     */
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /**
     * 设置消息优先级
     */
    public void setPriority(MessagePriority priority) {
        this.priority = priority;
    }

    /**
     * 添加扩展字段
     */
    public void addExtension(String key, Object value) {
        if (this.extensions == null) {
            this.extensions = new java.util.HashMap<>();
        }
        this.extensions.put(key, value);
    }

    /**
     * 获取扩展字段
     */
    public Object getExtension(String key) {
        return this.extensions != null ? this.extensions.get(key) : null;
    }

    /**
     * 检查是否为系统消息
     */
    public boolean isSystemMessage() {
        return this.type != null && this.type.isSystemMessage();
    }

    /**
     * 检查是否为用户消息
     */
    public boolean isUserMessage() {
        return this.type != null && this.type.isUserMessage();
    }

    /**
     * 检查是否为消息相关
     */
    public boolean isMessageRelated() {
        return this.type != null && this.type.isMessageRelated();
    }

    /**
     * 检查是否为会话相关
     */
    public boolean isConversationRelated() {
        return this.type != null && this.type.isConversationRelated();
    }

    /**
     * 检查是否为群组相关
     */
    public boolean isGroupRelated() {
        return this.type != null && this.type.isGroupRelated();
    }

    /**
     * 检查是否为好友相关
     */
    public boolean isFriendRelated() {
        return this.type != null && this.type.isFriendRelated();
    }

    /**
     * 检查是否为文件相关
     */
    public boolean isFileRelated() {
        return this.type != null && this.type.isFileRelated();
    }

    /**
     * 检查是否为通知相关
     */
    public boolean isNotificationRelated() {
        return this.type != null && this.type.isNotificationRelated();
    }

    /**
     * 检查是否为搜索相关
     */
    public boolean isSearchRelated() {
        return this.type != null && this.type.isSearchRelated();
    }

    /**
     * 检查是否为高优先级消息
     */
    public boolean isHighPriority() {
        return this.priority == MessagePriority.HIGH || this.priority == MessagePriority.URGENT;
    }

    /**
     * 检查是否为低优先级消息
     */
    public boolean isLowPriority() {
        return this.priority == MessagePriority.LOW;
    }

    /**
     * 检查消息是否成功
     */
    public boolean isSuccess() {
        return this.status == MessageStatus.SUCCESS;
    }

    /**
     * 检查消息是否失败
     */
    public boolean isFailed() {
        return this.status == MessageStatus.FAILED;
    }

    /**
     * 检查消息是否待处理
     */
    public boolean isPending() {
        return this.status == MessageStatus.PENDING;
    }

    /**
     * 检查消息是否已发送
     */
    public boolean isSent() {
        return this.status == MessageStatus.SENT;
    }

    /**
     * 检查消息是否已送达
     */
    public boolean isDelivered() {
        return this.status == MessageStatus.DELIVERED;
    }

    /**
     * 检查消息是否已读
     */
    public boolean isRead() {
        return this.status == MessageStatus.READ;
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount != null && this.maxRetryCount != null && 
               this.retryCount < this.maxRetryCount;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        this.retryCount++;
    }

    /**
     * 创建成功响应
     */
    public static <T> WebSocketMessage<T> success(MessageType type, T payload) {
        WebSocketMessage<T> message = new WebSocketMessage<>(type, payload);
        message.setStatus(MessageStatus.SUCCESS);
        return message;
    }

    /**
     * 创建失败响应
     */
    public static <T> WebSocketMessage<T> error(MessageType type, String errorMessage, String errorCode) {
        WebSocketMessage<T> message = new WebSocketMessage<>(type, null);
        message.setStatus(MessageStatus.FAILED);
        message.setErrorMessage(errorMessage);
        message.setErrorCode(errorCode);
        return message;
    }

    /**
     * 创建心跳消息
     */
    public static WebSocketMessage<String> heartbeat(String senderId) {
        return new WebSocketMessage<>(MessageType.HEARTBEAT, senderId, null, "ping");
    }

    /**
     * 创建心跳响应
     */
    public static WebSocketMessage<String> heartbeatAck(String senderId) {
        return new WebSocketMessage<>(MessageType.HEARTBEAT_ACK, senderId, null, "pong");
    }

    @Override
    public String toString() {
        return String.format("WebSocketMessage{id=%s, type=%s, sender=%s, receiver=%s, status=%s, timestamp=%s}", 
                messageId, type, senderId, receiverId, status, timestamp);
    }
} 