package com.acme.im.communication.realtime;

import com.acme.im.communication.service.MessageService;
import com.acme.im.communication.entity.Message;
import com.acme.im.common.websocket.message.structure.WebSocketMessage;
import com.acme.im.common.websocket.message.types.MessageType;
import com.acme.im.common.websocket.constants.WebSocketConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket消息处理器
 * 处理WebSocket连接建立、消息接收和发送
 * 使用公共模块的WebSocketMessage统一消息格式
 * 
 * 支持的消息类型：
 * 1. 认证消息 - 建立连接后的身份验证
 * 2. 心跳消息 - 保持连接活跃
 * 3. 聊天消息 - 文本、图片、文件等消息
 * 4. 状态消息 - 在线状态、输入状态等
 * 5. 控制消息 - 消息撤回、编辑等操作
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageHandler {

    private final WebSocketConnectionManager connectionManager;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    /**
     * 处理接收到的消息
     */
    public void handleIncomingMessage(WebSocketSession session, org.springframework.web.reactive.socket.WebSocketMessage message) {
        try {
            String payload = message.getPayloadAsText();
            log.debug("收到WebSocket消息: sessionId={}, payload={}", session.getId(), payload);

            // 解析消息 - 使用公共模块的WebSocketMessage
            WebSocketMessage<?> wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            
            // 根据消息类型处理
            MessageType msgType = wsMessage.getType();
            if (msgType == null) {
                log.warn("消息类型为空: sessionId={}", session.getId());
                return;
            }

            switch (msgType) {
                case CONNECT:
                    handleAuthMessage(session, wsMessage);
                    break;
                case HEARTBEAT:
                    handleHeartbeatMessage(session, wsMessage);
                    break;
                case MESSAGE_SEND:
                    handleChatMessage(session, wsMessage);
                    break;
                case MESSAGE_RECALL:
                case MESSAGE_EDIT:
                case CONVERSATION_PIN:
                    handleControlMessage(session, wsMessage);
                    break;
                default:
                    log.warn("未知消息类型: sessionId={}, type={}", session.getId(), msgType);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 处理认证消息
     */
    private void handleAuthMessage(WebSocketSession session, WebSocketMessage<?> wsMessage) {
        try {
            // 从消息中提取认证信息
            Map<String, Object> authData = (Map<String, Object>) wsMessage.getPayload();
            String token = (String) authData.get("token");
            String deviceId = (String) authData.get("deviceId");
            
            // 这里应该验证JWT token，获取用户信息
            // 简化处理，假设token有效且包含用户ID
            Long userId = extractUserIdFromToken(token);
            
            if (userId != null) {
                // 注册连接
                String connectionId = connectionManager.registerConnection(session, userId, deviceId);
                
                if (connectionId != null) {
                    // 认证成功响应
                    log.info("用户认证成功: userId={}, connectionId={}", userId, connectionId);
                } else {
                    log.warn("连接注册失败: userId={}", userId);
                }
            } else {
                log.warn("无效的认证token: sessionId={}", session.getId());
            }
        } catch (Exception e) {
            log.error("处理认证消息异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeatMessage(WebSocketSession session, WebSocketMessage<?> wsMessage) {
        try {
            // 更新连接的最后活跃时间
            connectionManager.updateHeartbeat(session.getId());
            log.debug("收到心跳消息: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("处理心跳消息异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, WebSocketMessage<?> wsMessage) {
        try {
            // 处理聊天消息逻辑
            log.debug("收到聊天消息: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("处理聊天消息异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 处理控制消息
     */
    private void handleControlMessage(WebSocketSession session, WebSocketMessage<?> wsMessage) {
        try {
            // 处理控制消息逻辑
            log.debug("收到控制消息: sessionId={}, type={}", session.getId(), wsMessage.getType());
        } catch (Exception e) {
            log.error("处理控制消息异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 处理连接关闭
     */
    public void handleConnectionClose(WebSocketSession session) {
        try {
            connectionManager.unregisterConnection(session.getId());
            log.info("WebSocket连接已关闭: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("处理连接关闭异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 处理错误
     */
    public void handleError(WebSocketSession session, Throwable error) {
        log.error("WebSocket连接错误: sessionId={}", session.getId(), error);
        handleConnectionClose(session);
    }

    /**
     * 从token中提取用户ID
     */
    private Long extractUserIdFromToken(String token) {
        // 这里应该实现JWT token解析逻辑
        // 简化实现，返回一个模拟的用户ID
        try {
            // 实际应该解析JWT token
            return 1L; // 模拟用户ID
        } catch (Exception e) {
            log.error("解析token失败", e);
            return null;
    }
} 
} 