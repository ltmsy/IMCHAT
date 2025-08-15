package com.acme.im.business.module.common.service;

import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.entity.EventRecord;
import com.acme.im.business.module.common.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事件持久化服务（优化版）
 * 使用批量处理、缓存机制和异步队列，减少数据库操作频率
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPersistenceService {

    private final ObjectMapper objectMapper;
    private final EventRepository eventRepository;

    // 事件缓存队列（内存缓存，减少数据库写入）
    private final ConcurrentLinkedQueue<EventRecord> eventCache = new ConcurrentLinkedQueue<>();
    
    // 批量处理配置
    private static final int BATCH_SIZE = 100; // 批量写入大小
    private static final int MAX_CACHE_SIZE = 1000; // 最大缓存大小
    private static final long BATCH_INTERVAL_MS = 5000; // 批量处理间隔（毫秒）
    
    // 统计信息
    private final AtomicInteger cacheHitCount = new AtomicInteger(0);
    private final AtomicInteger dbWriteCount = new AtomicInteger(0);
    private final AtomicInteger batchProcessCount = new AtomicInteger(0);

    /**
     * 异步持久化事件（优化版）
     * 优先使用缓存，批量写入数据库
     */
    @Async
    public CompletableFuture<Boolean> persistEventAsync(BaseEvent<?> event) {
        try {
            // 检查是否为重要事件
            if (!shouldPersistEvent(event)) {
                log.debug("事件不需要持久化: eventId={}, subject={}", event.getEventId(), event.getSubject());
                return CompletableFuture.completedFuture(true);
            }

            // 转换为EventRecord
            EventRecord eventRecord = convertToEventRecord(event);
            
            // 添加到缓存
            addToCache(eventRecord);
            
            if (eventCache.size() >= BATCH_SIZE) {
                triggerBatchProcessing();
            }
            
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("异步持久化事件失败: eventId={}", event.getEventId(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 同步持久化事件（直接写入，用于重要事件）
     */
    public boolean persistEvent(BaseEvent<?> event) {
        try {
            // 检查是否为重要事件
            if (!shouldPersistEvent(event)) {
                log.debug("事件不需要持久化: eventId={}, subject={}", event.getEventId(), event.getSubject());
                return true;
            }

            // 对于高优先级或失败事件，直接写入数据库
            if (isHighPriorityEvent(event) || isFailureEvent(event)) {
                EventRecord eventRecord = convertToEventRecord(event);
                return persistEventDirectly(eventRecord);
            }

            // 其他事件使用异步批量处理
            return persistEventAsync(event).get();

        } catch (Exception e) {
            log.error("同步持久化事件失败: eventId={}", event.getEventId(), e);
            return false;
        }
    }

    /**
     * 检查事件是否需要持久化
     */
    private boolean shouldPersistEvent(BaseEvent<?> event) {
        // 系统事件和配置事件不需要持久化
        if (event.getSubject().startsWith("im.system.") || 
            event.getSubject().startsWith("im.config.")) {
            return false;
        }
        
        // 低优先级事件不持久化
        if ("LOW".equals(event.getPriority())) {
            return false;
        }
        
        return true;
    }

    /**
     * 检查是否为高优先级事件
     */
    private boolean isHighPriorityEvent(BaseEvent<?> event) {
        return "URGENT".equals(event.getPriority()) || "HIGH".equals(event.getPriority());
    }

    /**
     * 检查是否为失败事件
     */
    private boolean isFailureEvent(BaseEvent<?> event) {
        return "FAILURE".equals(event.getStatus());
    }

    /**
     * 将事件添加到缓存
     */
    private void addToCache(EventRecord eventRecord) {
        // 限制缓存大小
        if (eventCache.size() >= MAX_CACHE_SIZE) {
            EventRecord removed = eventCache.poll();
            log.debug("缓存已满，移除最旧的事件: eventId={}", removed != null ? removed.getEventId() : "null");
        }
        
        eventCache.offer(eventRecord);
        cacheHitCount.incrementAndGet();
        log.debug("事件已添加到缓存: eventId={}, cacheSize={}", eventRecord.getEventId(), eventCache.size());
    }

    /**
     * 触发批量处理
     */
    private void triggerBatchProcessing() {
        try {
            List<EventRecord> batch = extractBatch();
            if (!batch.isEmpty()) {
                persistBatch(batch);
                batchProcessCount.incrementAndGet();
                log.info("批量处理完成: batchSize={}, totalProcessed={}", batch.size(), batchProcessCount.get());
            }
        } catch (Exception e) {
            log.error("批量处理失败", e);
        }
    }

    /**
     * 提取批量数据
     */
    private List<EventRecord> extractBatch() {
        List<EventRecord> batch = new java.util.ArrayList<>();
        for (int i = 0; i < BATCH_SIZE && !eventCache.isEmpty(); i++) {
            EventRecord record = eventCache.poll();
            if (record != null) {
                batch.add(record);
            }
        }
        return batch;
    }

    /**
     * 批量持久化
     */
    private void persistBatch(List<EventRecord> batch) {
        try {
            // 使用MyBatis-Plus的批量插入
            for (EventRecord record : batch) {
                eventRepository.insert(record);
            }
            dbWriteCount.addAndGet(batch.size());
            log.debug("批量持久化成功: batchSize={}", batch.size());
        } catch (Exception e) {
            log.error("批量持久化失败: batchSize={}", batch.size(), e);
            // 失败时重新放回缓存
            eventCache.addAll(batch);
        }
    }

    /**
     * 直接持久化单个事件
     */
    private boolean persistEventDirectly(EventRecord eventRecord) {
        try {
            eventRepository.insert(eventRecord);
            dbWriteCount.incrementAndGet();
            log.debug("直接持久化成功: eventId={}", eventRecord.getEventId());
            return true;
        } catch (Exception e) {
            log.error("直接持久化失败: eventId={}", eventRecord.getEventId(), e);
            return false;
        }
    }

    /**
     * 转换BaseEvent为EventRecord
     */
    private EventRecord convertToEventRecord(BaseEvent<?> event) {
        EventRecord record = new EventRecord();
        record.setEventId(event.getEventId());
        record.setSubject(event.getSubject());
        record.setEventType(event.getEventType());
        record.setStatus(event.getStatus());
        record.setPriority(event.getPriority());
        record.setSourceService(event.getSourceService());
        record.setSourceInstance(event.getSourceInstance());
        record.setUserId(event.getUserId());
        record.setDeviceId(event.getDeviceId());
        record.setCreatedAt(event.getCreatedAt());
        
        // 设置事件数据
        try {
            String eventData = objectMapper.writeValueAsString(event.getData());
            record.setEventData(eventData);
        } catch (Exception e) {
            log.warn("序列化事件数据失败: eventId={}", event.getEventId(), e);
        }
        
        return record;
    }

    /**
     * 定时批量处理（兜底机制）
     */
    @Scheduled(fixedDelay = BATCH_INTERVAL_MS)
    public void scheduledBatchProcessing() {
        if (!eventCache.isEmpty()) {
            log.debug("定时批量处理: cacheSize={}", eventCache.size());
            triggerBatchProcessing();
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "cacheSize", eventCache.size(),
            "cacheHitCount", cacheHitCount.get(),
            "dbWriteCount", dbWriteCount.get(),
            "batchProcessCount", batchProcessCount.get()
        );
    }

    /**
     * 清理过期事件记录
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupExpiredEvents() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7); // 保留7天
            int deletedCount = eventRepository.deleteByExpiresAtBefore(cutoffTime);
            log.info("清理过期事件记录完成: deletedCount={}", deletedCount);
        } catch (Exception e) {
            log.error("清理过期事件记录失败", e);
        }
    }
} 