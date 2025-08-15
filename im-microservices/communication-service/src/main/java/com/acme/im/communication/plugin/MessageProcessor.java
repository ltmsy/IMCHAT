package com.acme.im.communication.plugin;

import java.util.Map;

/**
 * 消息处理器扩展点接口
 * 专注于消息处理逻辑，不涉及业务认证
 * 
 * 使用场景：
 * 1. 消息格式转换 - 转换不同格式的消息
 * 2. 消息内容处理 - 处理消息内容（如表情转换、链接解析等）
 * 3. 消息路由 - 根据消息内容路由到不同处理器
 * 4. 消息统计 - 统计消息数量、类型等
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public interface MessageProcessor {

    /**
     * 处理消息
     * 
     * @param message 原始消息
     * @param context 处理上下文
     * @return 处理后的消息
     */
    Object processMessage(Object message, ProcessingContext context);

    /**
     * 获取处理器名称
     * 
     * @return 处理器名称
     */
    String getProcessorName();

    /**
     * 获取处理器优先级
     * 
     * @return 优先级，数值越小优先级越高
     */
    int getPriority();

    /**
     * 检查是否支持该消息类型
     * 
     * @param messageType 消息类型
     * @return 是否支持
     */
    boolean supportsMessageType(String messageType);

    /**
     * 获取处理器支持的消息类型
     * 
     * @return 支持的消息类型列表
     */
    java.util.List<String> getSupportedMessageTypes();

    /**
     * 消息处理上下文
     */
    class ProcessingContext {
        private final String sessionId;
        private final String messageType;
        private final Long timestamp;
        private final Map<String, Object> attributes;

        public ProcessingContext(String sessionId, String messageType) {
            this.sessionId = sessionId;
            this.messageType = messageType;
            this.timestamp = System.currentTimeMillis();
            this.attributes = new java.util.HashMap<>();
        }

        public String getSessionId() { return sessionId; }
        public String getMessageType() { return messageType; }
        public Long getTimestamp() { return timestamp; }
        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttribute(String key, Object value) { attributes.put(key, value); }
        public Object getAttribute(String key) { return attributes.get(key); }
    }
} 