package com.acme.im.communication.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * WebSocket事件监听器
 * 监听WebSocket连接、断开、订阅、取消订阅等事件
 * 
 * @author acme
 * @since 1.0.0
 */
@Component
@Slf4j
public class WebSocketEventListener {

    /**
     * 处理WebSocket连接建立事件
     * 
     * @param event 连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("WebSocket连接建立 - 会话ID: {}", sessionId);
        
        // 这里可以添加连接建立后的逻辑
        // 例如：记录用户在线状态、初始化用户会话等
    }

    /**
     * 处理WebSocket连接断开事件
     * 
     * @param event 断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("WebSocket连接断开 - 会话ID: {}", sessionId);
        
        // 这里可以添加连接断开后的逻辑
        // 例如：清理用户会话、更新用户在线状态等
    }

    /**
     * 处理WebSocket订阅事件
     * 
     * @param event 订阅事件
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        log.info("WebSocket订阅 - 会话ID: {}, 目标: {}", sessionId, destination);
        
        // 这里可以添加订阅后的逻辑
        // 例如：记录用户订阅的频道、权限验证等
    }

    /**
     * 处理WebSocket取消订阅事件
     * 
     * @param event 取消订阅事件
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();
        
        log.info("WebSocket取消订阅 - 会话ID: {}, 订阅ID: {}", sessionId, subscriptionId);
        
        // 这里可以添加取消订阅后的逻辑
        // 例如：清理用户订阅记录等
    }
} 