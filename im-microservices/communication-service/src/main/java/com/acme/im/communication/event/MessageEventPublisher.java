package com.acme.im.communication.event;

import com.acme.im.communication.entity.Message;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.MessageEvents;
import com.acme.im.common.infrastructure.nats.dto.UserEvents;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消息事件发布器
 * 基于公共模块的EventPublisher，发布消息相关事件
 * 
 * 事件类型：
 * 1. MESSAGE_CREATED - 消息创建事件
 * 2. MESSAGE_RECALLED - 消息撤回事件
 * 3. MESSAGE_EDITED - 消息编辑事件
 * 4. MESSAGE_PINNED - 消息置顶事件
 * 5. MESSAGE_DELETED - 消息删除事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventPublisher {

    private final AsyncEventPublisher eventPublisher;

    /**
     * 消息事件主题 - 使用统一的事件主题常量
     */
    private static final String MESSAGE_CREATED_SUBJECT = EventTopics.Communication.Message.SENT;
    private static final String MESSAGE_RECALLED_SUBJECT = EventTopics.Communication.Message.RECALLED;
    private static final String MESSAGE_EDITED_SUBJECT = EventTopics.Communication.Message.EDITED;
    private static final String MESSAGE_PINNED_SUBJECT = EventTopics.Communication.Message.PINNED;
    private static final String MESSAGE_DELETED_SUBJECT = EventTopics.Communication.Message.DELETED;

    /**
     * 发布消息创建事件
     * 
     * @param message 创建的消息
     */
    public void publishMessageCreated(Message message) {
        try {
            MessageEvent event = MessageEvent.builder()
                    .eventType("MESSAGE_CREATED")
                    .messageId(message.getId())
                    .conversationId(message.getConversationId())
                    .senderId(message.getSenderId())
                    .seq(message.getSeq())
                    .msgType(message.getMsgType())
                    .content(message.getContent())
                    .contentExtra(message.getContentExtra())
                    .replyToId(message.getReplyToId())
                    .forwardFromId(message.getForwardFromId())
                    .mentions(message.getMentions())
                    .serverTimestamp(message.getServerTimestamp())
                    .timestamp(LocalDateTime.now())
                    .build();

            // 使用公共模块的EventPublisher
            eventPublisher.publishToJetStream(MESSAGE_CREATED_SUBJECT, event);
            
            log.info("发布消息创建事件: messageId={}, conversationId={}, senderId={}", 
                    message.getId(), message.getConversationId(), message.getSenderId());

        } catch (Exception e) {
            log.error("发布消息创建事件失败: messageId={}", message.getId(), e);
        }
    }

    /**
     * 发布消息撤回事件
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     * @param reason 撤回原因
     */
    public void publishMessageRecalled(Long conversationId, Long messageId, Long operatorId, String reason) {
        try {
            MessageEvents.MessageRecalledEvent eventData = MessageEvents.MessageRecalledEvent.builder()
                    .conversationId(conversationId)
                    .messageId(messageId)
                    .operatorId(operatorId)
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();

            BaseEvent<MessageEvents.MessageRecalledEvent> baseEvent = BaseEvent.createNotification(
                    MESSAGE_RECALLED_SUBJECT, eventData)
                    .fromService("communication-service", "default")
                    .withUser(operatorId.toString(), null, null);

            eventPublisher.publishToJetStream(MESSAGE_RECALLED_SUBJECT, baseEvent);
            
            log.info("发布消息撤回事件: messageId={}, conversationId={}, operatorId={}", 
                    messageId, conversationId, operatorId);

        } catch (Exception e) {
            log.error("发布消息撤回事件失败: messageId={}", messageId, e);
        }
    }

    /**
     * 发布消息编辑事件
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     * @param newContent 新内容
     */
    public void publishMessageEdited(Long conversationId, Long messageId, Long operatorId, String newContent) {
        try {
            MessageEvents.MessageEditedEvent eventData = MessageEvents.MessageEditedEvent.builder()
                    .conversationId(conversationId)
                    .messageId(messageId)
                    .operatorId(operatorId)
                    .newContent(newContent)
                    .timestamp(LocalDateTime.now())
                    .build();

            BaseEvent<MessageEvents.MessageEditedEvent> baseEvent = BaseEvent.createNotification(
                    MESSAGE_EDITED_SUBJECT, eventData)
                    .fromService("communication-service", "default")
                    .withUser(operatorId.toString(), null, null);

            eventPublisher.publishToJetStream(MESSAGE_EDITED_SUBJECT, baseEvent);
            
            log.info("发布消息编辑事件: messageId={}, conversationId={}, operatorId={}", 
                    messageId, conversationId, operatorId);

        } catch (Exception e) {
            log.error("发布消息编辑事件失败: messageId={}", messageId, e);
        }
    }

    /**
     * 发布消息置顶事件
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param pinned 是否置顶
     */
    public void publishMessagePinned(Long conversationId, Long messageId, boolean pinned) {
        try {
            MessageEvents.MessagePinnedEvent eventData = MessageEvents.MessagePinnedEvent.builder()
                    .conversationId(conversationId)
                    .messageId(messageId)
                    .operatorId(null) // 这里可以传入操作者ID
                    .pinned(pinned)
                    .timestamp(LocalDateTime.now())
                    .build();

            BaseEvent<MessageEvents.MessagePinnedEvent> baseEvent = BaseEvent.createNotification(
                    MESSAGE_PINNED_SUBJECT, eventData)
                    .fromService("communication-service", "default");

            eventPublisher.publishToJetStream(MESSAGE_PINNED_SUBJECT, baseEvent);
            
            log.info("发布消息置顶事件: messageId={}, conversationId={}, pinned={}", 
                    messageId, conversationId, pinned);

        } catch (Exception e) {
            log.error("发布消息置顶事件失败: messageId={}", messageId, e);
        }
    }

    /**
     * 发布消息删除事件
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param operatorId 操作者ID
     */
    public void publishMessageDeleted(Long conversationId, Long messageId, Long operatorId) {
        try {
            MessageEvents.MessageDeletedEvent eventData = MessageEvents.MessageDeletedEvent.builder()
                    .conversationId(conversationId)
                    .messageId(messageId)
                    .operatorId(operatorId)
                    .reason("用户删除")
                    .timestamp(LocalDateTime.now())
                    .build();

            BaseEvent<MessageEvents.MessageDeletedEvent> baseEvent = BaseEvent.createNotification(
                    MESSAGE_DELETED_SUBJECT, eventData)
                    .fromService("communication-service", "default")
                    .withUser(operatorId.toString(), null, null);

            eventPublisher.publishToJetStream(MESSAGE_DELETED_SUBJECT, baseEvent);
            
            log.info("发布消息删除事件: messageId={}, conversationId={}, operatorId={}", 
                    messageId, conversationId, operatorId);

        } catch (Exception e) {
            log.error("发布消息删除事件失败: messageId={}", messageId, e);
        }
    }

    /**
     * 发布用户状态事件
     * 
     * @param userId 用户ID
     * @param status 状态（online/offline）
     */
    public void publishUserStatusEvent(Long userId, String status) {
        try {
            UserEvents.UserStatusChangeEvent eventData = UserEvents.UserStatusChangeEvent.builder()
                    .userId(userId)
                    .oldStatus(null)
                    .newStatus(status)
                    .reason("状态变更")
                    .changeTime(LocalDateTime.now())
                    .timestamp(LocalDateTime.now())
                    .build();

            BaseEvent<UserEvents.UserStatusChangeEvent> baseEvent = BaseEvent.createNotification(
                    EventTopics.Business.User.STATUS_CHANGED, eventData)
                    .fromService("communication-service", "default")
                    .withUser(userId.toString(), null, null);

            eventPublisher.publishEvent(EventTopics.Business.User.STATUS_CHANGED, baseEvent);
            
            log.info("发布用户状态事件: userId={}, status={}", userId, status);

        } catch (Exception e) {
            log.error("发布用户状态事件失败: userId={}", userId, e);
        }
    }

    /**
     * 发布连接事件
     * 
     * @param userId 用户ID
     * @param connectionId 连接ID
     * @param eventType 事件类型（connected/disconnected）
     */
    public void publishConnectionEvent(Long userId, String connectionId, String eventType) {
        try {
            UserEvents.UserSessionEvent eventData = UserEvents.UserSessionEvent.builder()
                    .userId(userId)
                    .sessionId(connectionId)
                    .eventType(eventType)
                    .deviceId(null)
                    .clientType(null)
                    .eventTime(LocalDateTime.now())
                    .timestamp(LocalDateTime.now())
                    .build();

            BaseEvent<UserEvents.UserSessionEvent> baseEvent = BaseEvent.createNotification(
                    EventTopics.Communication.Connection.ESTABLISHED, eventData)
                    .fromService("communication-service", "default")
                    .withUser(userId.toString(), null, connectionId);

            eventPublisher.publishEvent(EventTopics.Communication.Connection.ESTABLISHED, baseEvent);
            
            log.info("发布连接事件: userId={}, connectionId={}, eventType={}", 
                    userId, connectionId, eventType);

        } catch (Exception e) {
            log.error("发布连接事件失败: userId={}, connectionId={}", userId, connectionId, e);
        }
    }

    /**
     * 消息事件数据对象
     */
    public static class MessageEvent {
        private String eventType;
        private Long messageId;
        private Long conversationId;
        private Long senderId;
        private Long seq;
        private Integer msgType;
        private String content;
        private String contentExtra;
        private Long replyToId;
        private Long forwardFromId;
        private String mentions;
        private LocalDateTime serverTimestamp;
        private LocalDateTime timestamp;

        // 构造器
        private MessageEvent() {}

        public static MessageEventBuilder builder() {
            return new MessageEventBuilder();
        }

        // Getters and Setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
        
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        
        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }
        
        public Long getSeq() { return seq; }
        public void setSeq(Long seq) { this.seq = seq; }
        
        public Integer getMsgType() { return msgType; }
        public void setMsgType(Integer msgType) { this.msgType = msgType; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getContentExtra() { return contentExtra; }
        public void setContentExtra(String contentExtra) { this.contentExtra = contentExtra; }
        
        public Long getReplyToId() { return replyToId; }
        public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }
        
        public Long getForwardFromId() { return forwardFromId; }
        public void setForwardFromId(Long forwardFromId) { this.forwardFromId = forwardFromId; }
        
        public String getMentions() { return mentions; }
        public void setMentions(String mentions) { this.mentions = mentions; }
        
        public LocalDateTime getServerTimestamp() { return serverTimestamp; }
        public void setServerTimestamp(LocalDateTime serverTimestamp) { this.serverTimestamp = serverTimestamp; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        // Builder类
        public static class MessageEventBuilder {
            private final MessageEvent event = new MessageEvent();

            public MessageEventBuilder eventType(String eventType) {
                event.setEventType(eventType);
                return this;
            }

            public MessageEventBuilder messageId(Long messageId) {
                event.setMessageId(messageId);
                return this;
            }

            public MessageEventBuilder conversationId(Long conversationId) {
                event.setConversationId(conversationId);
                return this;
            }

            public MessageEventBuilder senderId(Long senderId) {
                event.setSenderId(senderId);
                return this;
            }

            public MessageEventBuilder seq(Long seq) {
                event.setSeq(seq);
                return this;
            }

            public MessageEventBuilder msgType(Integer msgType) {
                event.setMsgType(msgType);
                return this;
            }

            public MessageEventBuilder content(String content) {
                event.setContent(content);
                return this;
            }

            public MessageEventBuilder contentExtra(String contentExtra) {
                event.setContentExtra(contentExtra);
                return this;
            }

            public MessageEventBuilder replyToId(Long replyToId) {
                event.setReplyToId(replyToId);
                return this;
            }

            public MessageEventBuilder forwardFromId(Long forwardFromId) {
                event.setForwardFromId(forwardFromId);
                return this;
            }

            public MessageEventBuilder mentions(String mentions) {
                event.setMentions(mentions);
                return this;
            }

            public MessageEventBuilder serverTimestamp(LocalDateTime serverTimestamp) {
                event.setServerTimestamp(serverTimestamp);
                return this;
            }

            public MessageEventBuilder timestamp(LocalDateTime timestamp) {
                event.setTimestamp(timestamp);
                return this;
            }

            public MessageEvent build() {
                return event;
            }
        }
    }
} 