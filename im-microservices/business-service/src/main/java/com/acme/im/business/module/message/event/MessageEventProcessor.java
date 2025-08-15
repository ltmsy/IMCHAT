package com.acme.im.business.module.message.event;

import com.acme.im.business.module.common.event.EventProcessor;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息事件处理器
 * 处理聊天消息、控制消息等事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventProcessor implements EventProcessor {

    @Override
    public String getProcessorName() {
        return "MessageEventProcessor";
    }

    @Override
    public String getSupportedSubject() {
        return EventTopics.Common.Message.CHAT;
    }

    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public BaseEvent<?> processEvent(BaseEvent<?> event) {
        try {
            log.info("处理消息事件: eventId={}, subject={}, userId={}", 
                    event.getEventId(), event.getSubject(), event.getUserId());

            // 根据事件主题处理不同类型的消息
            switch (event.getSubject()) {
                case EventTopics.Common.Message.CHAT:
                    return processChatMessage(event);
                case EventTopics.Common.Message.CONTROL:
                    return processControlMessage(event);
                default:
                    log.warn("不支持的消息事件主题: eventId={}, subject={}", 
                            event.getEventId(), event.getSubject());
                    return createFailureResponse(event, "不支持的消息事件主题", "UNSUPPORTED_SUBJECT");
            }

        } catch (Exception e) {
            log.error("处理消息事件异常: eventId={}", event.getEventId(), e);
            return createFailureResponse(event, "消息处理异常: " + e.getMessage(), "MESSAGE_PROCESSING_ERROR");
        }
    }

    /**
     * 处理聊天消息
     */
    private BaseEvent<?> processChatMessage(BaseEvent<?> event) {
        try {
            Object data = event.getData();
            if (!(data instanceof Map)) {
                log.error("聊天消息数据格式错误: eventId={}", event.getEventId());
                return createFailureResponse(event, "数据格式错误", "INVALID_DATA_FORMAT");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> chatData = (Map<String, Object>) data;
            String content = (String) chatData.get("content");
            String conversationId = (String) chatData.get("conversationId");
            String userId = (String) chatData.get("userId");

            if (content == null || content.trim().isEmpty()) {
                log.warn("聊天消息内容为空: eventId={}, userId={}", event.getEventId(), userId);
                return createFailureResponse(event, "消息内容不能为空", "EMPTY_MESSAGE_CONTENT");
            }

            if (conversationId == null || conversationId.trim().isEmpty()) {
                log.warn("会话ID为空: eventId={}, userId={}", event.getEventId(), userId);
                return createFailureResponse(event, "会话ID不能为空", "EMPTY_CONVERSATION_ID");
            }

            // 这里应该调用消息服务处理聊天消息
            // 由于MessageService在通信层，这里只是示例
            log.info("处理聊天消息: eventId={}, userId={}, conversationId={}, content={}", 
                    event.getEventId(), userId, conversationId, content);

            // 创建消息处理结果
            MessageResult messageResult = new MessageResult(
                event.getEventId(),
                "CHAT",
                true,
                null,
                null,
                System.currentTimeMillis()
            );

            return BaseEvent.createResponse(EventTopics.Common.Message.RESULT, messageResult)
                    .fromService("business-service", "default")
                    .toService(event.getSourceService(), event.getSourceInstance())
                    .withUser(userId, event.getDeviceId(), event.getSessionId())
                    .success()
                    .addMetadata("messageType", "CHAT")
                    .addMetadata("conversationId", conversationId)
                    .addMetadata("processedTime", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("处理聊天消息异常: eventId={}", event.getEventId(), e);
            return createFailureResponse(event, "聊天消息处理异常: " + e.getMessage(), "CHAT_MESSAGE_ERROR");
        }
    }

    /**
     * 处理控制消息
     */
    private BaseEvent<?> processControlMessage(BaseEvent<?> event) {
        try {
            Object data = event.getData();
            if (!(data instanceof Map)) {
                log.error("控制消息数据格式错误: eventId={}", event.getEventId());
                return createFailureResponse(event, "数据格式错误", "INVALID_DATA_FORMAT");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> controlData = (Map<String, Object>) data;
            String controlType = (String) controlData.get("controlType");
            String userId = (String) controlData.get("userId");

            if (controlType == null || controlType.trim().isEmpty()) {
                log.warn("控制类型为空: eventId={}, userId={}", event.getEventId(), userId);
                return createFailureResponse(event, "控制类型不能为空", "EMPTY_CONTROL_TYPE");
            }

            log.info("处理控制消息: eventId={}, userId={}, controlType={}", 
                    event.getEventId(), userId, controlType);

            // 根据控制类型处理不同的控制逻辑
            switch (controlType) {
                case "ONLINE_STATUS":
                    // 处理在线状态变更
                    handleOnlineStatusChange(controlData);
                    break;
                case "READ_RECEIPT":
                    // 处理已读回执
                    handleReadReceipt(controlData);
                    break;
                case "TYPING_INDICATOR":
                    // 处理正在输入指示器
                    handleTypingIndicator(controlData);
                    break;
                default:
                    log.warn("不支持的控制类型: eventId={}, controlType={}", 
                            event.getEventId(), controlType);
                    return createFailureResponse(event, "不支持的控制类型: " + controlType, "UNSUPPORTED_CONTROL_TYPE");
            }

            // 创建控制消息处理结果
            MessageResult messageResult = new MessageResult(
                event.getEventId(),
                "CONTROL",
                true,
                null,
                null,
                System.currentTimeMillis()
            );

            return BaseEvent.createResponse(EventTopics.Common.Message.RESULT, messageResult)
                    .fromService("business-service", "default")
                    .toService(event.getSourceService(), event.getSourceInstance())
                    .withUser(userId, event.getDeviceId(), event.getSessionId())
                    .success()
                    .addMetadata("messageType", "CONTROL")
                    .addMetadata("controlType", controlType)
                    .addMetadata("processedTime", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("处理控制消息异常: eventId={}", event.getEventId(), e);
            return createFailureResponse(event, "控制消息处理异常: " + e.getMessage(), "CONTROL_MESSAGE_ERROR");
        }
    }

    /**
     * 处理在线状态变更
     */
    private void handleOnlineStatusChange(Map<String, Object> controlData) {
        // TODO: 实现在线状态变更逻辑
        log.debug("处理在线状态变更: {}", controlData);
    }

    /**
     * 处理已读回执
     */
    private void handleReadReceipt(Map<String, Object> controlData) {
        // TODO: 实现已读回执逻辑
        log.debug("处理已读回执: {}", controlData);
    }

    /**
     * 处理正在输入指示器
     */
    private void handleTypingIndicator(Map<String, Object> controlData) {
        // TODO: 实现正在输入指示器逻辑
        log.debug("处理正在输入指示器: {}", controlData);
    }

    /**
     * 创建失败响应
     */
    private BaseEvent<?> createFailureResponse(BaseEvent<?> originalEvent, String errorMessage, String errorCode) {
        MessageResult messageResult = new MessageResult(
            originalEvent.getEventId(),
            "UNKNOWN",
            false,
            errorMessage,
            errorCode,
            System.currentTimeMillis()
        );

        return BaseEvent.createResponse(EventTopics.Common.Message.RESULT, messageResult)
                .fromService("business-service", "default")
                .toService(originalEvent.getSourceService(), originalEvent.getSourceInstance())
                .withUser(originalEvent.getUserId(), originalEvent.getDeviceId(), originalEvent.getSessionId())
                .failure(errorMessage, errorCode);
    }

    /**
     * 消息处理结果
     */
    public static class MessageResult {
        private final String messageId;
        private final String messageType;
        private final boolean success;
        private final String errorMessage;
        private final String errorCode;
        private final long processedTime;

        public MessageResult(String messageId, String messageType, boolean success, 
                           String errorMessage, String errorCode, long processedTime) {
            this.messageId = messageId;
            this.messageType = messageType;
            this.success = success;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
            this.processedTime = processedTime;
        }

        // Getters
        public String getMessageId() { return messageId; }
        public String getMessageType() { return messageType; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorCode() { return errorCode; }
        public long getProcessedTime() { return processedTime; }
    }
} 