package com.acme.im.communication.websocket;

import com.acme.im.common.websocket.proto.WebSocketMessage;
import com.acme.im.common.websocket.proto.ChatMessage;
import com.acme.im.common.websocket.proto.MessageType;
import com.acme.im.common.websocket.proto.MessageStatus;
import com.acme.im.communication.service.MessageService;
import com.acme.im.communication.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * WebSocket消息处理器
 * 处理WebSocket消息的接收、处理和发送
 * 支持新的消息格式和特殊操作
 * 
 * @author acme
 * @since 2.0.0
 */
@Controller
@Slf4j
public class WebSocketMessageHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private MessageService messageService;

    // ================================
    // 聊天消息处理
    // ================================
    
    /**
     * 处理聊天消息
     * 
     * @param message 消息内容
     * @param headerAccessor 消息头访问器
     * @return 处理后的消息
     */
    @MessageMapping("/chat")
    @SendTo("/topic/public")
    public Object handleChatMessage(@Payload Object message, 
                                   SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到聊天消息: {}", message);
        
        try {
            // 记录用户会话信息
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            log.debug("用户会话ID: {}, 用户ID: {}", sessionId, userId);
            
            // 这里可以添加消息处理逻辑，如消息验证、过滤等
            
            // 返回一个简单的响应消息
            return Map.of(
                "type", "CHAT_RESPONSE",
                "messageId", "resp_" + System.currentTimeMillis(),
                "content", "消息已收到",
                "timestamp", System.currentTimeMillis(),
                "sessionId", sessionId,
                "userId", userId
            );
        } catch (Exception e) {
            log.error("处理聊天消息失败: {}", e.getMessage(), e);
            return Map.of("error", "消息处理失败", "details", e.getMessage());
        }
    }

    /**
     * 处理私聊消息
     * 
     * @param message 消息内容
     * @param headerAccessor 消息头访问器
     */
    @MessageMapping("/private-message")
    public void handlePrivateMessage(@Payload Object message, 
                                   SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到私聊消息: {}", message);
        
        try {
            // 发送私聊消息给指定用户
            // 这里需要从消息中提取接收者ID，暂时发送给发送者自己
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/private-messages", 
                Map.of(
                    "type", "PRIVATE_MESSAGE_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "content", "私聊消息已收到",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("私聊消息响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理私聊消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理群聊消息
     * 
     * @param message 消息内容
     * @param headerAccessor 消息头访问器
     */
    @MessageMapping("/group-message")
    public void handleGroupMessage(@Payload Object message, 
                                 SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到群聊消息: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 这里应该从消息中提取群组ID，然后广播给群组成员
            // 暂时发送给发送者自己作为确认
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/group-messages", 
                Map.of(
                    "type", "GROUP_MESSAGE_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "content", "群聊消息已收到",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("群聊消息响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理群聊消息失败: {}", e.getMessage(), e);
        }
    }

    // ================================
    // 特殊操作消息处理
    // ================================
    
    /**
     * 处理编辑消息
     */
    @MessageMapping("/edit-message")
    public void handleEditMessage(@Payload Object message, 
                                SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到编辑消息请求: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 这里应该解析消息内容，调用MessageService.editMessage()
            // 暂时发送确认响应
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/edit-responses", 
                Map.of(
                    "type", "EDIT_MESSAGE_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "content", "编辑消息请求已收到",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("编辑消息响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理编辑消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理引用消息
     */
    @MessageMapping("/quote-message")
    public void handleQuoteMessage(@Payload Object message, 
                                 SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到引用消息请求: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 这里应该解析消息内容，调用MessageService.quoteMessage()
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/quote-responses", 
                Map.of(
                    "type", "QUOTE_MESSAGE_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "content", "引用消息请求已收到",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("引用消息响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理引用消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理转发消息
     */
    @MessageMapping("/forward-message")
    public void handleForwardMessage(@Payload Object message, 
                                   SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到转发消息请求: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 这里应该解析消息内容，调用MessageService.forwardMessage()
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/forward-responses", 
                Map.of(
                    "type", "FORWARD_MESSAGE_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "content", "转发消息请求已收到",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("转发消息响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理转发消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理删除消息
     */
    @MessageMapping("/delete-message")
    public void handleDeleteMessage(@Payload Object message, 
                                  SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到删除消息请求: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 这里应该解析消息内容，调用MessageService.deleteMessage()
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/delete-responses", 
                Map.of(
                    "type", "DELETE_MESSAGE_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "content", "删除消息请求已收到",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("删除消息响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理删除消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理置顶消息
     */
    @MessageMapping("/pin-message")
    public void handlePinMessage(@Payload Object message, 
                                SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到置顶消息请求: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 这里应该解析消息内容，调用MessageService.pinMessage()
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/pin-responses", 
                Map.of(
                    "type", "PIN_MESSAGE_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "content", "置顶消息请求已收到",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("置顶消息响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理置顶消息失败: {}", e.getMessage(), e);
        }
    }

    // ================================
    // 系统消息处理
    // ================================
    
    /**
     * 处理心跳消息
     */
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload Object message, 
                               SimpMessageHeaderAccessor headerAccessor) {
        log.debug("收到心跳消息: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 发送心跳响应
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/heartbeat", 
                Map.of(
                    "type", "HEARTBEAT_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
        } catch (Exception e) {
            log.error("处理心跳消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理认证消息
     */
    @MessageMapping("/auth")
    public void handleAuth(@Payload Object message, 
                          SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到认证消息: {}", message);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
            
            // 这里应该进行用户认证
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/queue/auth", 
                Map.of(
                    "type", "AUTH_RESPONSE",
                    "messageId", "resp_" + System.currentTimeMillis(),
                    "status", "authenticated",
                    "timestamp", System.currentTimeMillis(),
                    "userId", userId
                )
            );
            log.debug("认证响应已发送给会话: {}", sessionId);
        } catch (Exception e) {
            log.error("处理认证消息失败: {}", e.getMessage(), e);
        }
    }

    // ================================
    // 消息路由和分发
    // ================================
    
    /**
     * 广播消息到指定会话的所有用户
     */
    public void broadcastToConversation(String conversationId, Object message) {
        try {
            String destination = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(destination, message);
            log.debug("消息已广播到会话: {}", conversationId);
        } catch (Exception e) {
            log.error("广播消息到会话失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
        }
    }
    
    /**
     * 发送消息给指定用户
     */
    public void sendToUser(String userId, String destination, Object message) {
        try {
            messagingTemplate.convertAndSendToUser(userId, destination, message);
            log.debug("消息已发送给用户: userId={}, destination={}", userId, destination);
        } catch (Exception e) {
            log.error("发送消息给用户失败: userId={}, destination={}, error={}", 
                     userId, destination, e.getMessage(), e);
        }
    }
    
    /**
     * 发送消息给指定会话的所有在线用户
     */
    public void sendToConversationUsers(String conversationId, String destination, Object message) {
        try {
            // 这里应该查询会话中的所有在线用户，然后逐个发送
            // 暂时使用广播方式
            String broadcastDestination = "/topic/conversation/" + conversationId + destination;
            messagingTemplate.convertAndSend(broadcastDestination, message);
            log.debug("消息已发送到会话用户: conversationId={}, destination={}", conversationId, destination);
        } catch (Exception e) {
            log.error("发送消息到会话用户失败: conversationId={}, destination={}, error={}", 
                     conversationId, destination, e.getMessage(), e);
        }
    }

    // ================================
    // 异常处理
    // ================================
    
    /**
     * 处理消息处理过程中的异常
     */
    @MessageExceptionHandler
    public void handleException(Throwable exception, SimpMessageHeaderAccessor headerAccessor) {
        log.error("WebSocket消息处理异常: {}", exception.getMessage(), exception);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                messagingTemplate.convertAndSendToUser(
                    sessionId, 
                    "/queue/errors", 
                    Map.of(
                        "type", "ERROR",
                        "messageId", "error_" + System.currentTimeMillis(),
                        "error", "消息处理失败",
                        "details", exception.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    )
                );
            }
        } catch (Exception e) {
            log.error("发送错误响应失败: {}", e.getMessage(), e);
        }
    }
} 