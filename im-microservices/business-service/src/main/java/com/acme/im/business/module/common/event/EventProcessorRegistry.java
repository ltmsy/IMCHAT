package com.acme.im.business.module.common.event;

import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件处理器注册器
 * 自动注册和管理所有事件处理器
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventProcessorRegistry {

    private final AsyncEventPublisher eventPublisher;

    @Autowired
    private List<EventProcessor> eventProcessors;

    // 存储已注册的处理器，按主题分组
    private final Map<String, List<EventProcessor>> processorsBySubject = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("开始初始化事件处理器注册器...");
        
        // 按优先级排序处理器
        eventProcessors.sort(Comparator.comparingInt(EventProcessor::getPriority));
        
        // 按主题分组处理器
        for (EventProcessor processor : eventProcessors) {
            String subject = processor.getSupportedSubject();
            processorsBySubject.computeIfAbsent(subject, k -> new java.util.ArrayList<>()).add(processor);
            
            log.info("注册事件处理器: subject={}, processor={}, priority={}", 
                    subject, processor.getProcessorName(), processor.getPriority());
        }

        // 为每个主题注册事件处理器
        for (Map.Entry<String, List<EventProcessor>> entry : processorsBySubject.entrySet()) {
            String subject = entry.getKey();
            List<EventProcessor> processors = entry.getValue();
            
            // 注册事件处理器，使用响应映射器模式
            // TODO: 实现事件订阅逻辑，暂时注释掉
            // eventPublisher.publishToJetStream(subject, event -> {
            //     return processEventWithHandlers(event, processors);
            // });
            
            log.info("为主题 {} 注册了 {} 个事件处理器", subject, processors.size());
        }
        
        log.info("事件处理器注册器初始化完成，共注册 {} 个主题，{} 个处理器", 
                processorsBySubject.size(), eventProcessors.size());
    }

    /**
     * 使用多个处理器处理事件
     */
    private BaseEvent<?> processEventWithHandlers(BaseEvent<?> event, List<EventProcessor> processors) {
        try {
            log.debug("使用 {} 个处理器处理事件: eventId={}, subject={}", 
                    processors.size(), event.getEventId(), event.getSubject());

            // 按优先级顺序处理事件
            for (EventProcessor processor : processors) {
                try {
                    if (processor.supportsEvent(event)) {
                        log.debug("处理器 {} 开始处理事件: eventId={}", 
                                processor.getProcessorName(), event.getEventId());
                        
                        BaseEvent<?> result = processor.processEvent(event);
                        
                        if (result != null) {
                            log.debug("处理器 {} 返回响应: eventId={}, status={}", 
                                    processor.getProcessorName(), result.getEventId(), result.getStatus());
                            return result;
                        }
                    }
                } catch (Exception e) {
                    log.error("处理器 {} 处理事件异常: eventId={}", 
                            processor.getProcessorName(), event.getEventId(), e);
                    
                    // 如果处理器异常，创建错误响应
                    return createErrorResponse(event, "处理器异常: " + e.getMessage(), "PROCESSOR_ERROR");
                }
            }

            // 如果没有处理器返回响应，创建默认响应
            log.warn("没有处理器返回响应: eventId={}, subject={}", event.getEventId(), event.getSubject());
            return createErrorResponse(event, "没有处理器返回响应", "NO_RESPONSE");

        } catch (Exception e) {
            log.error("处理事件异常: eventId={}", event.getEventId(), e);
            return createErrorResponse(event, "事件处理异常: " + e.getMessage(), "EVENT_PROCESSING_ERROR");
        }
    }

    /**
     * 创建错误响应
     */
    private BaseEvent<?> createErrorResponse(BaseEvent<?> originalEvent, String errorMessage, String errorCode) {
        Map<String, Object> errorData = Map.of(
            "errorMessage", errorMessage,
            "errorCode", errorCode,
            "originalEventId", originalEvent.getEventId()
        );

        return BaseEvent.createResponse(EventTopics.Common.Message.RESULT, errorData)
                .fromService("business-service", "default")
                .toService(originalEvent.getSourceService(), originalEvent.getSourceInstance())
                .withUser(originalEvent.getUserId(), originalEvent.getDeviceId(), originalEvent.getSessionId())
                .failure(errorMessage, errorCode);
    }

    /**
     * 获取指定主题的处理器列表
     */
    public List<EventProcessor> getProcessorsForSubject(String subject) {
        return processorsBySubject.getOrDefault(subject, new java.util.ArrayList<>());
    }

    /**
     * 获取所有已注册的主题
     */
    public List<String> getRegisteredSubjects() {
        return new java.util.ArrayList<>(processorsBySubject.keySet());
    }

    /**
     * 公共方法：处理事件
     * 供外部调用，将事件路由到对应的处理器
     * 
     * @param event 要处理的事件
     * @return 处理结果
     */
    public BaseEvent<?> processEvent(BaseEvent<?> event) {
        String subject = event.getSubject();
        List<EventProcessor> processors = processorsBySubject.get(subject);
        
        if (processors == null || processors.isEmpty()) {
            log.warn("没有找到主题 {} 的处理器: eventId={}", subject, event.getEventId());
            return createErrorResponse(event, "没有找到对应的处理器", "NO_PROCESSOR_FOUND");
        }
        
        return processEventWithHandlers(event, processors);
    }

    /**
     * 获取处理器统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalProcessors", eventProcessors.size());
        stats.put("totalSubjects", processorsBySubject.size());
        stats.put("subjects", getRegisteredSubjects());
        
        // 按主题统计处理器数量
        Map<String, Integer> processorCounts = new java.util.HashMap<>();
        for (Map.Entry<String, List<EventProcessor>> entry : processorsBySubject.entrySet()) {
            processorCounts.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("processorCounts", processorCounts);
        
        return stats;
    }
} 