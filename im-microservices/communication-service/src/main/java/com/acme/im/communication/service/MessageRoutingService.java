package com.acme.im.communication.service;

import com.acme.im.communication.entity.Message;
import com.acme.im.communication.event.MessageEditEvent;
import com.acme.im.communication.event.NewMessageEvent;
import com.acme.im.communication.event.MessageDeleteEvent;
import com.acme.im.communication.event.MessagePinEvent;
import com.acme.im.common.websocket.proto.WebSocketMessage;
import com.acme.im.common.websocket.proto.ChatMessage;
import com.acme.im.common.websocket.proto.MessageType;
import com.acme.im.common.websocket.proto.MessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息路由服务
 * 负责消息的实时推送、多端同步、状态变更通知等
 * 
 * @author IM开发团队
 * @since 2.0.0
 */
@Service
@Slf4j
public class MessageRoutingService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // 用户会话映射：userId -> sessionIds
    private final Map<String, List<String>> userSessions = new ConcurrentHashMap<>();
    
    // 会话用户映射：conversationId -> userIds
    private final Map<String, List<String>> conversationUsers = new ConcurrentHashMap<>();
    
    // 异步执行器
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // ================================
    // 事件监听器 - 替代直接依赖
    // ================================
    
    /**
     * 监听新消息事件
     */
    @EventListener
    public void handleNewMessage(NewMessageEvent event) {
        log.info("收到新消息事件: messageId={}", event.getMessage().getId());
        pushNewMessage(event.getMessage());
    }
    
    /**
     * 监听消息编辑事件
     */
    @EventListener
    public void handleMessageEdit(MessageEditEvent event) {
        log.info("收到消息编辑事件: messageId={}", event.getEditedMessage().getId());
        pushMessageEdit(event.getOriginalMessage(), event.getEditedMessage());
    }
    
    /**
     * 监听消息删除事件
     */
    @EventListener
    public void handleMessageDelete(MessageDeleteEvent event) {
        log.info("收到消息删除事件: messageId={}", event.getMessage().getId());
        pushMessageDelete(event.getMessage(), event.getDeleteReason(), event.getDeleteScope());
    }
    
    /**
     * 监听消息置顶事件
     */
    @EventListener
    public void handleMessagePin(MessagePinEvent event) {
        log.info("收到消息置顶事件: messageId={}", event.getMessage().getId());
        pushMessagePin(event.getMessage(), event.getPinScope());
    }

    // ================================
    // 消息推送
    // ================================
    
    /**
     * 推送新消息到会话
     */
    public void pushNewMessage(Message message) {
        try {
            String conversationId = message.getConversationId().toString();
            
            // 1. 构建WebSocket消息
            WebSocketMessage wsMessage = buildWebSocketMessage(message);
            
            // 2. 推送到会话主题
            String topicDestination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(topicDestination, wsMessage);
            
            // 3. 推送给会话中的在线用户
            pushToConversationUsers(conversationId, wsMessage);
            
            log.info("新消息推送完成: conversationId={}, messageId={}", conversationId, message.getId());
            
        } catch (Exception e) {
            log.error("推送新消息失败: messageId={}, error={}", message.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 推送消息状态变更
     */
    public void pushMessageStatusChange(Message message, String changeType) {
        try {
            String conversationId = message.getConversationId().toString();
            
            // 构建状态变更消息
            WebSocketMessage statusMessage = buildEventMessage(
                "EVENT_MESSAGE_STATUS_CHANGE", 
                Map.of(
                    "messageId", message.getId(),
                    "changeType", changeType,
                    "timestamp", System.currentTimeMillis(),
                    "details", buildStatusChangeDetails(message, changeType)
                )
            );
            
            // 推送到会话
            String topicDestination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(topicDestination, statusMessage);
            
            log.info("消息状态变更推送完成: conversationId={}, messageId={}, changeType={}", 
                    conversationId, message.getId(), changeType);
            
        } catch (Exception e) {
            log.error("推送消息状态变更失败: messageId={}, changeType={}, error={}", 
                    message.getId(), changeType, e.getMessage(), e);
        }
    }
    
    /**
     * 推送消息编辑通知
     */
    public void pushMessageEdit(Message originalMessage, Message editMessage) {
        try {
            String conversationId = originalMessage.getConversationId().toString();
            
            // 构建编辑通知消息
            WebSocketMessage editNotification = buildEventMessage(
                "EVENT_MESSAGE_EDIT",
                Map.of(
                    "originalMessageId", originalMessage.getId(),
                    "editMessageId", editMessage.getId(),
                    "editorId", editMessage.getSenderId(),
                    "editReason", editMessage.getEditReason(),
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // 推送到会话
            String topicDestination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(topicDestination, editNotification);
            
            log.info("消息编辑通知推送完成: conversationId={}, originalMessageId={}, editMessageId={}",
                    conversationId, originalMessage.getId(), editMessage.getId());
            
        } catch (Exception e) {
            log.error("推送消息编辑通知失败: originalMessageId={}, editMessageId={}, error={}",
                    originalMessage.getId(), editMessage.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 推送消息删除通知
     */
    public void pushMessageDelete(Message message, String deleteReason, Integer deleteScope) {
        try {
            String conversationId = message.getConversationId().toString();
            
            // 构建删除通知消息
            WebSocketMessage deleteNotification = buildEventMessage(
                "EVENT_MESSAGE_DELETE",
                Map.of(
                    "messageId", message.getId(),
                    "deleteReason", deleteReason,
                    "deleteScope", deleteScope,
                    "deletedBy", message.getDeletedBy(),
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // 推送到会话
            String topicDestination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(topicDestination, deleteNotification);
            
            log.info("消息删除通知推送完成: conversationId={}, messageId={}, deleteScope={}",
                    conversationId, message.getId(), deleteScope);
            
        } catch (Exception e) {
            log.error("推送消息删除通知失败: messageId={}, error={}", message.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 推送消息置顶通知
     */
    public void pushMessagePin(Message message, Integer pinScope) {
        try {
            String conversationId = message.getConversationId().toString();
            
            // 构建置顶通知消息
            WebSocketMessage pinNotification = buildEventMessage(
                "EVENT_MESSAGE_PIN",
                Map.of(
                    "messageId", message.getId(),
                    "pinScope", pinScope,
                    "pinnedBy", message.getPinnedBy(),
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // 推送到会话
            String topicDestination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(topicDestination, pinNotification);
            
            log.info("消息置顶通知推送完成: conversationId={}, messageId={}, pinScope={}",
                    conversationId, message.getId(), pinScope);
            
        } catch (Exception e) {
            log.error("推送消息置顶通知失败: messageId={}, error={}", message.getId(), e.getMessage(), e);
        }
    }

    // ================================
    // 多端同步
    // ================================
    
    /**
     * 同步消息到用户的所有设备
     */
    public void syncToUserDevices(String userId, Object message, String destination) {
        try {
            // 获取用户的所有会话
            List<String> sessions = userSessions.get(userId);
            if (sessions != null && !sessions.isEmpty()) {
                for (String sessionId : sessions) {
                    messagingTemplate.convertAndSendToUser(sessionId, destination, message);
                }
                log.debug("消息已同步到用户所有设备: userId={}, destination={}, sessionCount={}",
                        userId, destination, sessions.size());
            }
        } catch (Exception e) {
            log.error("同步消息到用户设备失败: userId={}, destination={}, error={}",
                    userId, destination, e.getMessage(), e);
        }
    }
    
    /**
     * 同步消息状态变更到所有相关设备
     */
    public void syncMessageStatusToAllDevices(Message message, String changeType) {
        try {
            String conversationId = message.getConversationId().toString();
            
            // 获取会话中的所有用户
            List<String> userIds = conversationUsers.get(conversationId);
            if (userIds != null && !userIds.isEmpty()) {
                for (String userId : userIds) {
                    syncToUserDevices(userId, buildStatusChangeMessage(message, changeType), "/queue/message-status");
                }
                log.debug("消息状态变更已同步到所有相关设备: conversationId={}, changeType={}, userCount={}",
                        conversationId, changeType, userIds.size());
            }
        } catch (Exception e) {
            log.error("同步消息状态变更到所有设备失败: conversationId={}, changeType={}, error={}",
                    message.getConversationId(), changeType, e.getMessage(), e);
        }
    }

    // ================================
    // 会话管理
    // ================================
    
    /**
     * 用户加入会话
     */
    public void userJoinConversation(String userId, String conversationId, String sessionId) {
        try {
            // 添加到用户会话映射
            userSessions.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(sessionId);
            
            // 添加到会话用户映射
            conversationUsers.computeIfAbsent(conversationId, k -> new java.util.ArrayList<>()).add(userId);
            
            // 推送用户加入通知
            WebSocketMessage joinNotification = buildEventMessage(
                "EVENT_USER_JOIN_CONVERSATION",
                Map.of(
                    "userId", userId,
                    "conversationId", conversationId,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            String topicDestination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(topicDestination, joinNotification);
            
            log.info("用户加入会话: userId={}, conversationId={}, sessionId={}", userId, conversationId, sessionId);
            
        } catch (Exception e) {
            log.error("用户加入会话失败: userId={}, conversationId={}, sessionId={}, error={}",
                    userId, conversationId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 用户离开会话
     */
    public void userLeaveConversation(String userId, String conversationId, String sessionId) {
        try {
            // 从用户会话映射中移除
            List<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            
            // 从会话用户映射中移除
            List<String> userIds = conversationUsers.get(conversationId);
            if (userIds != null) {
                userIds.remove(userId);
                if (userIds.isEmpty()) {
                    conversationUsers.remove(conversationId);
                }
            }
            
            // 推送用户离开通知
            WebSocketMessage leaveNotification = buildEventMessage(
                "EVENT_USER_LEAVE_CONVERSATION",
                Map.of(
                    "userId", userId,
                    "conversationId", conversationId,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            String topicDestination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(topicDestination, leaveNotification);
            
            log.info("用户离开会话: userId={}, conversationId={}, sessionId={}", userId, conversationId, sessionId);
            
        } catch (Exception e) {
            log.error("用户离开会话失败: userId={}, conversationId={}, sessionId={}, error={}",
                    userId, conversationId, sessionId, e.getMessage(), e);
        }
    }

    // ================================
    // 工具方法
    // ================================
    
    /**
     * 构建WebSocket消息
     */
    private WebSocketMessage buildWebSocketMessage(Message message) {
        // 根据消息类型构建不同的payload
        Object payload;
        MessageType messageType;

        if (message.isEditMessage()) {
            messageType = MessageType.CHAT_EDIT;
            payload = Map.of(
                "content", message.getContent(),
                "originalMessageId", message.getOriginalMessageId(),
                "editReason", message.getEditReason()
            );
        } else if (message.isQuoteMessage()) {
            messageType = MessageType.CHAT_QUOTE;
            payload = Map.of(
                "content", message.getContent(),
                "quotedMessageId", message.getQuotedMessageId(),
                "quotedContent", message.getQuotedContent()
            );
        } else if (message.isForwardMessage()) {
            messageType = MessageType.CHAT_FORWARD;
            payload = Map.of(
                "content", message.getContent(),
                "originalMessageId", message.getOriginalMessageId(),
                "forwardReason", message.getForwardReason()
            );
        } else {
            // 普通消息
            messageType = getMessageType(message.getMsgType());
            payload = Map.of(
                "content", message.getContent(),
                "contentExtra", message.getContentExtra()
            );
        }

        WebSocketMessage.Builder wsMessageBuilder = WebSocketMessage.newBuilder();
        wsMessageBuilder.setMessageId(message.getId().toString());
        wsMessageBuilder.setSenderId(message.getSenderId().toString());
        wsMessageBuilder.setConversationId(message.getConversationId().toString());
        wsMessageBuilder.setClientMessageId(message.getClientMsgId());
        wsMessageBuilder.setTimestamp(message.getServerTimestamp() != null ? 
            message.getServerTimestamp().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() : 
            System.currentTimeMillis());
        wsMessageBuilder.setDeviceId(message.getDeviceId());
        wsMessageBuilder.setSource(message.getSource());
        wsMessageBuilder.setVersion(message.getVersion());
        
        // 设置消息类型
        wsMessageBuilder.setType(messageType);
        wsMessageBuilder.setStatus(MessageStatus.MESSAGE_NORMAL);
        
        // 设置聊天消息payload
        ChatMessage.Builder chatBuilder = ChatMessage.newBuilder();
        chatBuilder.setContent(payload.toString());
        wsMessageBuilder.setChat(chatBuilder.build());
        
        return wsMessageBuilder.build();
    }
    
    /**
     * 构建事件消息
     */
    private WebSocketMessage buildEventMessage(String eventType, Map<String, Object> eventData) {
        WebSocketMessage.Builder builder = WebSocketMessage.newBuilder();
        builder.setMessageId("event_" + System.currentTimeMillis());
        builder.setType(MessageType.EVENT_USER_STATUS_CHANGE); // 默认事件类型
        builder.setStatus(MessageStatus.MESSAGE_NORMAL);
        builder.setSenderId("system");
        builder.setTimestamp(System.currentTimeMillis());
        builder.setSequence(System.currentTimeMillis());
        builder.setClientMessageId("event_" + System.currentTimeMillis());
        builder.setVersion("2.0.0");
        
        // 构建简单的聊天消息作为事件payload
        ChatMessage.Builder chatBuilder = ChatMessage.newBuilder();
        chatBuilder.setContent(eventData.toString());
        builder.setChat(chatBuilder.build());
        
        return builder.build();
    }
    
    /**
     * 获取消息类型
     */
    private MessageType getMessageType(Integer msgType) {
        if (msgType == null) return MessageType.CHAT_TEXT;

        switch (msgType) {
            case 0: return MessageType.CHAT_TEXT;
            case 1: return MessageType.CHAT_IMAGE;
            case 2: return MessageType.CHAT_FILE;
            case 3: return MessageType.CHAT_VOICE;
            case 4: return MessageType.CHAT_VIDEO;
            case 5: return MessageType.CHAT_LOCATION;
            case 6: return MessageType.CHAT_CARD;
            default: return MessageType.CHAT_TEXT;
        }
    }
    
    /**
     * 构建状态变更详情
     */
    private Map<String, Object> buildStatusChangeDetails(Message message, String changeType) {
        Map<String, Object> details = new java.util.HashMap<>();
        details.put("messageId", message.getId());
        details.put("conversationId", message.getConversationId());
        details.put("changeType", changeType);

        switch (changeType) {
            case "EDIT":
                details.put("editCount", message.getEditCount());
                details.put("lastEditAt", message.getLastEditAt());
                break;
            case "PIN":
                details.put("pinScope", message.getPinScope());
                details.put("pinnedBy", message.getPinnedBy());
                details.put("pinnedAt", message.getPinnedAt());
                break;
            case "DELETE":
                details.put("deleteScope", message.getDeleteScope());
                details.put("deletedBy", message.getDeletedBy());
                details.put("deletedAt", message.getDeletedAt());
                break;
        }

        return details;
    }
    
    /**
     * 构建状态变更消息
     */
    private WebSocketMessage buildStatusChangeMessage(Message message, String changeType) {
        return buildEventMessage(
            "EVENT_MESSAGE_STATUS_CHANGE",
            buildStatusChangeDetails(message, changeType)
        );
    }
    
    /**
     * 推送给会话用户
     */
    private void pushToConversationUsers(String conversationId, WebSocketMessage message) {
        try {
            List<String> userIds = conversationUsers.get(conversationId);
            if (userIds != null && !userIds.isEmpty()) {
                for (String userId : userIds) {
                    List<String> sessions = userSessions.get(userId);
                    if (sessions != null) {
                        for (String sessionId : sessions) {
                            messagingTemplate.convertAndSendToUser(sessionId, "/queue/new-messages", message);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("推送给会话用户失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
        }
    }
} 