package com.acme.im.business.module.message.event;

import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.constants.EventErrorCodes;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息事件处理器
 * 使用注解驱动的方式处理各类消息事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventHandler {

    private final AsyncEventPublisher eventPublisher;

    /**
     * 处理聊天消息事件
     */
    @NatsEventHandler(
        value = EventTopics.Common.Message.CHAT,
        priority = 50,
        description = "处理聊天消息发送请求"
    )
    public void handleChatMessage(BaseEvent<?> event) {
        try {
            log.info("处理聊天消息事件: eventId={}, userId={}", event.getEventId(), event.getUserId());

            Object data = event.getData();
            if (data == null) {
                log.warn("聊天消息数据为空: eventId={}", event.getEventId());
                publishMessageFailureResponse(event, "消息数据为空", EventErrorCodes.MESSAGE_DATA_NULL);
                return;
            }

            Map<String, Object> messageData = (Map<String, Object>) data;
            String content = (String) messageData.get("content");
            String conversationId = (String) messageData.get("conversationId");
            String messageType = (String) messageData.get("messageType");

            if (content == null || conversationId == null) {
                log.warn("聊天消息参数不完整: eventId={}", event.getEventId());
                publishMessageFailureResponse(event, "消息内容或会话ID为空", EventErrorCodes.MESSAGE_PARAMS_INCOMPLETE);
                return;
            }

            // 这里应该调用实际的消息服务处理逻辑
            // messageService.saveMessage(...)
            
            // 构建消息处理成功响应
            MessageResult messageResult = MessageResult.builder()
                    .success(true)
                    .messageId(generateMessageId())
                    .conversationId(conversationId)
                    .content(content)
                    .messageType(messageType != null ? messageType : "TEXT")
                    .sendTime(LocalDateTime.now())
                    .message("消息发送成功")
                    .build();

            publishMessageSuccessResponse(event, messageResult);
            
            log.info("聊天消息处理成功: eventId={}, conversationId={}", event.getEventId(), conversationId);

        } catch (Exception e) {
            log.error("处理聊天消息异常: eventId={}", event.getEventId(), e);
            publishMessageFailureResponse(event, "消息处理异常: " + e.getMessage(), EventErrorCodes.MESSAGE_PROCESSING_ERROR);
        }
    }

    /**
     * 处理控制消息事件
     */
    @NatsEventHandler(
        value = EventTopics.Common.Message.CONTROL,
        priority = 60,
        description = "处理控制消息（撤回、编辑等）"
    )
    public void handleControlMessage(BaseEvent<?> event) {
        try {
            log.info("处理控制消息事件: eventId={}, userId={}", event.getEventId(), event.getUserId());

            Object data = event.getData();
            if (data == null) {
                log.warn("控制消息数据为空: eventId={}", event.getEventId());
                publishMessageFailureResponse(event, "控制消息数据为空", "CONTROL_DATA_NULL");
                return;
            }

            Map<String, Object> controlData = (Map<String, Object>) data;
            String action = (String) controlData.get("action");
            String messageId = (String) controlData.get("messageId");
            String conversationId = (String) controlData.get("conversationId");

            if (action == null || messageId == null) {
                log.warn("控制消息参数不完整: eventId={}", event.getEventId());
                publishMessageFailureResponse(event, "操作类型或消息ID为空", "CONTROL_PARAMS_INCOMPLETE");
                return;
            }

            // 处理不同的控制操作
            switch (action.toLowerCase()) {
                case "recall":
                    handleMessageRecall(messageId, conversationId, event);
                    break;
                case "edit":
                    handleMessageEdit(controlData, event);
                    break;
                case "pin":
                    handleMessagePin(messageId, conversationId, event);
                    break;
                default:
                    log.warn("不支持的控制操作: eventId={}, action={}", event.getEventId(), action);
                    publishMessageFailureResponse(event, "不支持的操作类型: " + action, "UNSUPPORTED_ACTION");
                    return;
            }

            log.info("控制消息处理成功: eventId={}, action={}, messageId={}", 
                    event.getEventId(), action, messageId);

        } catch (Exception e) {
            log.error("处理控制消息异常: eventId={}", event.getEventId(), e);
            publishMessageFailureResponse(event, "控制消息处理异常: " + e.getMessage(), "CONTROL_PROCESSING_ERROR");
        }
    }

    /**
     * 处理消息撤回
     */
    private void handleMessageRecall(String messageId, String conversationId, BaseEvent<?> event) {
        // 这里应该调用实际的消息服务撤回逻辑
        // messageService.recallMessage(messageId, event.getUserId())
        
        MessageResult result = MessageResult.builder()
                .success(true)
                .messageId(messageId)
                .conversationId(conversationId)
                .messageType("RECALL")
                .message("消息撤回成功")
                .build();
                
        publishMessageSuccessResponse(event, result);
    }

    /**
     * 处理消息编辑
     */
    private void handleMessageEdit(Map<String, Object> controlData, BaseEvent<?> event) {
        String messageId = (String) controlData.get("messageId");
        String newContent = (String) controlData.get("newContent");
        String conversationId = (String) controlData.get("conversationId");
        
        if (newContent == null) {
            publishMessageFailureResponse(event, "新消息内容为空", "EDIT_CONTENT_NULL");
            return;
        }
        
        // 这里应该调用实际的消息服务编辑逻辑
        // messageService.editMessage(messageId, newContent, event.getUserId())
        
        MessageResult result = MessageResult.builder()
                .success(true)
                .messageId(messageId)
                .conversationId(conversationId)
                .content(newContent)
                .messageType("EDIT")
                .message("消息编辑成功")
                .build();
                
        publishMessageSuccessResponse(event, result);
    }

    /**
     * 处理消息置顶
     */
    private void handleMessagePin(String messageId, String conversationId, BaseEvent<?> event) {
        // 这里应该调用实际的消息服务置顶逻辑
        // messageService.pinMessage(messageId, conversationId, event.getUserId())
        
        MessageResult result = MessageResult.builder()
                .success(true)
                .messageId(messageId)
                .conversationId(conversationId)
                .messageType("PIN")
                .message("消息置顶成功")
                .build();
                
        publishMessageSuccessResponse(event, result);
    }

    /**
     * 发布消息处理成功响应
     */
    private void publishMessageSuccessResponse(BaseEvent<?> originalEvent, MessageResult result) {
        try {
            BaseEvent<MessageResult> responseEvent = BaseEvent.createResponse(EventTopics.Common.Message.RESULT, result)
                    .fromService("business-service", "default")
                    .withUser(originalEvent.getUserId(), originalEvent.getDeviceId(), originalEvent.getSessionId())
                    .success();

            eventPublisher.publishEvent(EventTopics.Common.Message.RESULT, responseEvent);
            
        } catch (Exception e) {
            log.error("发布消息成功响应异常: eventId={}", originalEvent.getEventId(), e);
        }
    }

    /**
     * 发布消息处理失败响应
     */
    private void publishMessageFailureResponse(BaseEvent<?> originalEvent, String errorMessage, String errorCode) {
        try {
            MessageResult result = MessageResult.builder()
                    .success(false)
                    .message(errorMessage)
                    .errorCode(errorCode)
                    .build();

            BaseEvent<MessageResult> responseEvent = BaseEvent.createResponse(EventTopics.Common.Message.RESULT, result)
                    .fromService("business-service", "default")
                    .withUser(originalEvent.getUserId(), originalEvent.getDeviceId(), originalEvent.getSessionId())
                    .failure(errorMessage, errorCode);

            eventPublisher.publishEvent(EventTopics.Common.Message.RESULT, responseEvent);
            
        } catch (Exception e) {
            log.error("发布消息失败响应异常: eventId={}", originalEvent.getEventId(), e);
        }
    }

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 消息处理结果数据类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageResult {
        private boolean success;
        private String messageId;
        private String conversationId;
        private String content;
        private String messageType;
        private LocalDateTime sendTime;
        private String message;
        private String errorCode;
    }
} 