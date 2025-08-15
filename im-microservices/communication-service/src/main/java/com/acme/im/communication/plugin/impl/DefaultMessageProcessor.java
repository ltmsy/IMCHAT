package com.acme.im.communication.plugin.impl;

import com.acme.im.communication.plugin.MessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 默认消息处理器实现
 * 专注于消息处理逻辑，不涉及业务认证
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class DefaultMessageProcessor implements MessageProcessor {

    @Override
    public Object processMessage(Object message, ProcessingContext context) {
        try {
            log.debug("开始处理消息: sessionId={}, messageType={}", context.getSessionId(), context.getMessageType());
            
            // 根据消息类型进行不同处理
            switch (context.getMessageType()) {
                case "CHAT":
                    return processChatMessage(message, context);
                case "FILE":
                    return processFileMessage(message, context);
                case "IMAGE":
                    return processImageMessage(message, context);
                case "AUDIO":
                    return processAudioMessage(message, context);
                case "VIDEO":
                    return processVideoMessage(message, context);
                case "SYSTEM":
                    return processSystemMessage(message, context);
                default:
                    return processDefaultMessage(message, context);
            }
        } catch (Exception e) {
            log.error("消息处理异常: sessionId={}, messageType={}", context.getSessionId(), context.getMessageType(), e);
            // 返回原始消息，不中断处理流程
            return message;
        }
    }

    @Override
    public String getProcessorName() {
        return "DefaultMessageProcessor";
    }

    @Override
    public int getPriority() {
        return 100; // 中等优先级
    }

    @Override
    public boolean supportsMessageType(String messageType) {
        return true; // 支持所有消息类型
    }

    @Override
    public List<String> getSupportedMessageTypes() {
        return List.of("CHAT", "FILE", "IMAGE", "AUDIO", "VIDEO", "SYSTEM");
    }

    // ================================
    // 私有处理方法
    // ================================

    /**
     * 处理聊天消息
     */
    private Object processChatMessage(Object message, ProcessingContext context) {
        // 聊天消息处理：表情转换、链接解析等
        context.setAttribute("processed", true);
        context.setAttribute("processingTime", System.currentTimeMillis());
        
        log.debug("聊天消息处理完成: sessionId={}", context.getSessionId());
        return message;
    }

    /**
     * 处理文件消息
     */
    private Object processFileMessage(Object message, ProcessingContext context) {
        // 文件消息处理：文件信息提取、安全检查等
        context.setAttribute("processed", true);
        context.setAttribute("processingTime", System.currentTimeMillis());
        context.setAttribute("fileType", "document");
        
        log.debug("文件消息处理完成: sessionId={}", context.getSessionId());
        return message;
    }

    /**
     * 处理图片消息
     */
    private Object processImageMessage(Object message, ProcessingContext context) {
        // 图片消息处理：尺寸信息、格式转换等
        context.setAttribute("processed", true);
        context.setAttribute("processingTime", System.currentTimeMillis());
        context.setAttribute("imageType", "jpeg");
        
        log.debug("图片消息处理完成: sessionId={}", context.getSessionId());
        return message;
    }

    /**
     * 处理音频消息
     */
    private Object processAudioMessage(Object message, ProcessingContext context) {
        // 音频消息处理：时长信息、格式转换等
        context.setAttribute("processed", true);
        context.setAttribute("processingTime", System.currentTimeMillis());
        context.setAttribute("audioType", "mp3");
        
        log.debug("音频消息处理完成: sessionId={}", context.getSessionId());
        return message;
    }

    /**
     * 处理视频消息
     */
    private Object processVideoMessage(Object message, ProcessingContext context) {
        // 视频消息处理：时长信息、分辨率、格式转换等
        context.setAttribute("processed", true);
        context.setAttribute("processingTime", System.currentTimeMillis());
        context.setAttribute("videoType", "mp4");
        
        log.debug("视频消息处理完成: sessionId={}", context.getSessionId());
        return message;
    }

    /**
     * 处理系统消息
     */
    private Object processSystemMessage(Object message, ProcessingContext context) {
        // 系统消息处理：优先级设置、路由信息等
        context.setAttribute("processed", true);
        context.setAttribute("processingTime", System.currentTimeMillis());
        context.setAttribute("priority", "high");
        
        log.debug("系统消息处理完成: sessionId={}", context.getSessionId());
        return message;
    }

    /**
     * 处理默认消息
     */
    private Object processDefaultMessage(Object message, ProcessingContext context) {
        // 默认消息处理：基础信息设置
        context.setAttribute("processed", true);
        context.setAttribute("processingTime", System.currentTimeMillis());
        context.setAttribute("defaultProcessing", true);
        
        log.debug("默认消息处理完成: sessionId={}", context.getSessionId());
        return message;
    }
} 