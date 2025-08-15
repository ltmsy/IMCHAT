package com.acme.im.business.module.user.event;

import com.acme.im.business.module.user.service.UserService;
import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.WebSocketEvents;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * WebSocket连接状态事件处理器（简化版）
 * 只处理连接建立和断开2个核心事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final UserService userService;
    private final AsyncEventPublisher eventPublisher;

    /**
     * 处理所有WebSocket事件（统一处理连接建立和断开）
     */
    @NatsEventHandler(
        value = EventTopics.Common.WebSocket.ALL,
        priority = 100,
        description = "处理WebSocket连接状态事件"
    )
    public void handleWebSocketEvents(BaseEvent<WebSocketEvents.ConnectionEvent> event) {
        try {
            WebSocketEvents.ConnectionEvent data = event.getData();
            log.info("处理WebSocket事件: action={}, userId={}, deviceId={}", 
                    data.getAction(), data.getUserId(), data.getDeviceId());

            if (data.getUserId() == null) {
                log.warn("WebSocket事件中用户ID为空: sessionId={}", data.getSessionId());
                return;
            }

            if ("CONNECTED".equals(data.getAction())) {
                // 连接建立：更新在线状态
                handleConnectionEstablished(data);
            } else if ("DISCONNECTED".equals(data.getAction())) {
                // 连接断开：更新离线状态
                handleConnectionDisconnected(data);
            }

            log.info("WebSocket事件处理完成: action={}, userId={}", data.getAction(), data.getUserId());

        } catch (Exception e) {
            log.error("处理WebSocket事件异常: eventId={}", event.getEventId(), e);
        }
    }

    /**
     * 处理连接建立事件
     */
    private void handleConnectionEstablished(WebSocketEvents.ConnectionEvent data) {
        try {
            // 更新用户在线状态
            updateUserOnlineStatus(data.getUserId(), true);
            
            // 更新设备在线状态
            updateDeviceOnlineStatus(data.getUserId(), data.getDeviceId(), true);
            
            // 发布用户在线状态变更事件
            publishUserOnlineStatusChanged(data.getUserId(), data.getDeviceId(), true, "WebSocket连接建立");
            
            log.info("用户连接建立处理完成: userId={}, deviceId={}", data.getUserId(), data.getDeviceId());
            
        } catch (Exception e) {
            log.error("处理连接建立事件异常: userId={}, error: {}", data.getUserId(), e.getMessage(), e);
        }
    }

    /**
     * 处理连接断开事件
     */
    private void handleConnectionDisconnected(WebSocketEvents.ConnectionEvent data) {
        try {
            // 更新用户在线状态
            updateUserOnlineStatus(data.getUserId(), false);
            
            // 更新设备在线状态
            updateDeviceOnlineStatus(data.getUserId(), data.getDeviceId(), false);
            
            // 发布用户在线状态变更事件
            publishUserOnlineStatusChanged(data.getUserId(), data.getDeviceId(), false, "WebSocket连接断开");
            
            log.info("用户连接断开处理完成: userId={}, deviceId={}", data.getUserId(), data.getDeviceId());
            
        } catch (Exception e) {
            log.error("处理连接断开事件异常: userId={}, error: {}", data.getUserId(), e.getMessage(), e);
        }
    }

    // ================================
    // 辅助方法（简化版）
    // ================================

    /**
     * 更新用户在线状态
     */
    private void updateUserOnlineStatus(Long userId, boolean isOnline) {
        try {
            // 这里可以调用UserService更新用户在线状态
            // 例如：userService.updateUserOnlineStatus(userId, isOnline);
            log.debug("更新用户在线状态: userId={}, isOnline={}", userId, isOnline);
        } catch (Exception e) {
            log.error("更新用户在线状态失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 更新设备在线状态
     */
    private void updateDeviceOnlineStatus(Long userId, String deviceId, boolean isOnline) {
        try {
            // 这里可以调用UserService更新设备在线状态
            // 例如：userService.updateDeviceOnlineStatus(userId, deviceId, isOnline);
            log.debug("更新设备在线状态: userId={}, deviceId={}, isOnline={}", userId, deviceId, isOnline);
        } catch (Exception e) {
            log.error("更新设备在线状态失败: userId={}, deviceId={}, error: {}", userId, deviceId, e.getMessage(), e);
        }
    }

    /**
     * 发布用户在线状态变更事件
     */
    private void publishUserOnlineStatusChanged(Long userId, String deviceId, boolean isOnline, String reason) {
        try {
            // 这里可以发布用户在线状态变更事件
            // 例如：通过UserEventPublisher发布事件
            log.debug("发布用户在线状态变更事件: userId={}, deviceId={}, isOnline={}, reason={}", 
                    userId, deviceId, isOnline, reason);
        } catch (Exception e) {
            log.error("发布用户在线状态变更事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }
} 