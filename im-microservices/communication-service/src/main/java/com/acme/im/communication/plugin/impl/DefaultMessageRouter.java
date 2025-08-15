package com.acme.im.communication.plugin.impl;

import com.acme.im.communication.plugin.MessageRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 默认消息路由器实现
 * 专注于消息路由逻辑，不涉及业务认证
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class DefaultMessageRouter implements MessageRouter {

    @Override
    public RoutingResult routeMessage(Object message, RoutingContext context) {
        try {
            log.debug("开始路由消息: sessionId={}, messageType={}", context.getSessionId(), context.getMessageType());
            
            // 根据消息类型进行路由
            switch (context.getMessageType()) {
                case "CHAT":
                    return routeChatMessage(message, context);
                case "FILE":
                    return routeFileMessage(message, context);
                case "IMAGE":
                    return routeImageMessage(message, context);
                case "AUDIO":
                    return routeAudioMessage(message, context);
                case "VIDEO":
                    return routeVideoMessage(message, context);
                case "SYSTEM":
                    return routeSystemMessage(message, context);
                default:
                    return routeDefaultMessage(message, context);
            }
        } catch (Exception e) {
            log.error("消息路由异常: sessionId={}, messageType={}", context.getSessionId(), context.getMessageType(), e);
            return RoutingResult.failure("路由异常: " + e.getMessage());
        }
    }

    @Override
    public String getRouterName() {
        return "DefaultMessageRouter";
    }

    @Override
    public int getPriority() {
        return 100; // 中等优先级
    }

    @Override
    public boolean supportsMessageType(String messageType) {
        return true; // 支持所有消息类型
    }

    // ================================
    // 私有路由方法
    // ================================

    /**
     * 路由聊天消息
     */
    private RoutingResult routeChatMessage(Object message, RoutingContext context) {
        // 聊天消息路由到消息处理服务
        Map<String, Object> metadata = Map.of(
            "messageType", "CHAT",
            "priority", "normal",
            "requiresAck", true
        );
        
        return RoutingResult.success("message-service", "default", metadata);
    }

    /**
     * 路由文件消息
     */
    private RoutingResult routeFileMessage(Object message, RoutingContext context) {
        // 文件消息路由到文件处理服务
        Map<String, Object> metadata = Map.of(
            "messageType", "FILE",
            "priority", "high",
            "requiresAck", true,
            "fileProcessing", true
        );
        
        return RoutingResult.success("file-service", "default", metadata);
    }

    /**
     * 路由图片消息
     */
    private RoutingResult routeImageMessage(Object message, RoutingContext context) {
        // 图片消息路由到图片处理服务
        Map<String, Object> metadata = Map.of(
            "messageType", "IMAGE",
            "priority", "normal",
            "requiresAck", true,
            "imageProcessing", true
        );
        
        return RoutingResult.success("image-service", "default", metadata);
    }

    /**
     * 路由音频消息
     */
    private RoutingResult routeAudioMessage(Object message, RoutingContext context) {
        // 音频消息路由到音频处理服务
        Map<String, Object> metadata = Map.of(
            "messageType", "AUDIO",
            "priority", "normal",
            "requiresAck", true,
            "audioProcessing", true
        );
        
        return RoutingResult.success("audio-service", "default", metadata);
    }

    /**
     * 路由视频消息
     */
    private RoutingResult routeVideoMessage(Object message, RoutingContext context) {
        // 视频消息路由到视频处理服务
        Map<String, Object> metadata = Map.of(
            "messageType", "VIDEO",
            "priority", "high",
            "requiresAck", true,
            "videoProcessing", true,
            "videoType", "mp4" // 添加视频类型字段
        );
        
        return RoutingResult.success("video-service", "default", metadata);
    }

    /**
     * 路由系统消息
     */
    private RoutingResult routeSystemMessage(Object message, RoutingContext context) {
        // 系统消息路由到系统处理服务
        Map<String, Object> metadata = Map.of(
            "messageType", "SYSTEM",
            "priority", "critical",
            "requiresAck", false,
            "systemProcessing", true
        );
        
        return RoutingResult.success("system-service", "default", metadata);
    }

    /**
     * 路由默认消息
     */
    private RoutingResult routeDefaultMessage(Object message, RoutingContext context) {
        // 默认消息路由到通用消息处理服务
        Map<String, Object> metadata = Map.of(
            "messageType", "DEFAULT",
            "priority", "low",
            "requiresAck", false
        );
        
        return RoutingResult.success("message-service", "default", metadata);
    }
} 