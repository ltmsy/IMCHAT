package com.acme.im.common.infrastructure.nats.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * WebSocket连接状态事件数据传输对象（简化版）
 * 只保留连接建立和断开2个核心事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class WebSocketEvents {

    /**
     * WebSocket连接状态事件（统一处理连接建立和断开）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConnectionEvent {
        /**
         * 会话ID
         */
        private String sessionId;
        
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 设备ID
         */
        private String deviceId;
        
        /**
         * 动作：CONNECTED（连接建立）或 DISCONNECTED（连接断开）
         */
        private String action;
        
        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
        
        /**
         * 连接来源
         */
        private String source;
    }
} 