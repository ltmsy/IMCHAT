package com.acme.im.communication.plugin;

import java.util.Map;

/**
 * 消息路由扩展点接口
 * 专注于消息路由逻辑，不涉及业务认证
 * 
 * 使用场景：
 * 1. 消息分发 - 根据消息类型分发到不同处理器
 * 2. 负载均衡 - 在多实例间分配消息
 * 3. 优先级路由 - 根据消息优先级选择处理路径
 * 4. 条件路由 - 根据消息内容选择处理路径
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public interface MessageRouter {

    /**
     * 路由消息
     * 
     * @param message 消息对象
     * @param context 路由上下文
     * @return 路由结果
     */
    RoutingResult routeMessage(Object message, RoutingContext context);

    /**
     * 获取路由器名称
     * 
     * @return 路由器名称
     */
    String getRouterName();

    /**
     * 获取路由器优先级
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
     * 路由上下文
     */
    class RoutingContext {
        private final String sessionId;
        private final String messageType;
        private final String targetUserId;
        private final String conversationId;
        private final Map<String, Object> attributes;

        public RoutingContext(String sessionId, String messageType, String targetUserId, String conversationId) {
            this.sessionId = sessionId;
            this.messageType = messageType;
            this.targetUserId = targetUserId;
            this.conversationId = conversationId;
            this.attributes = new java.util.HashMap<>();
        }

        public String getSessionId() { return sessionId; }
        public String getMessageType() { return messageType; }
        public String getTargetUserId() { return targetUserId; }
        public String getConversationId() { return conversationId; }
        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttribute(String key, Object value) { attributes.put(key, value); }
        public Object getAttribute(String key) { return attributes.get(key); }
    }

    /**
     * 路由结果
     */
    class RoutingResult {
        private final boolean success;
        private final String targetService;
        private final String targetInstance;
        private final String reason;
        private final Map<String, Object> metadata;

        private RoutingResult(boolean success, String targetService, String targetInstance, String reason, Map<String, Object> metadata) {
            this.success = success;
            this.targetService = targetService;
            this.targetInstance = targetInstance;
            this.reason = reason;
            this.metadata = metadata != null ? metadata : new java.util.HashMap<>();
        }

        public static RoutingResult success(String targetService, String targetInstance) {
            return new RoutingResult(true, targetService, targetInstance, null, null);
        }

        public static RoutingResult success(String targetService, String targetInstance, Map<String, Object> metadata) {
            return new RoutingResult(true, targetService, targetInstance, null, metadata);
        }

        public static RoutingResult failure(String reason) {
            return new RoutingResult(false, null, null, reason, null);
        }

        public boolean isSuccess() { return success; }
        public String getTargetService() { return targetService; }
        public String getTargetInstance() { return targetInstance; }
        public String getReason() { return reason; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
} 