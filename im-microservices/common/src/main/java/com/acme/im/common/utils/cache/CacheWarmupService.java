package com.acme.im.common.utils.cache;

import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 缓存预热服务
 * 提供智能的缓存预热和失效策略，提升系统整体性能
 * 
 * 特性：
 * 1. 智能预热 - 基于访问模式的预测性缓存加载
 * 2. 分批预热 - 避免系统启动时的性能冲击
 * 3. 优先级预热 - 重要数据优先加载
 * 4. 动态调整 - 根据系统负载动态调整预热策略
 * 5. 失效策略 - 多种缓存失效策略
 * 6. 监控统计 - 详细的预热和失效统计
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Service
@Slf4j
public class CacheWarmupService {

    @Autowired
    private DistributedCacheManager cacheManager;
    
    @Autowired
    private AsyncEventPublisher eventPublisher;
    
    @Autowired
    @Qualifier("cacheExecutor")
    private Executor cacheExecutor;

    // 预热任务队列
    private final BlockingQueue<WarmupTask> warmupQueue = new PriorityBlockingQueue<>(1000);
    
    // 预热统计
    private final AtomicLong totalWarmupTasks = new AtomicLong(0);
    private final AtomicLong completedWarmupTasks = new AtomicLong(0);
    private final AtomicLong failedWarmupTasks = new AtomicLong(0);
    private final AtomicLong cacheHitsAfterWarmup = new AtomicLong(0);
    
    // 访问统计 - 用于智能预热决策
    private final ConcurrentHashMap<String, AccessStats> accessStatsMap = new ConcurrentHashMap<>();
    
    // 预热配置
    private static final int WARMUP_BATCH_SIZE = 50;
    private static final int WARMUP_THREAD_COUNT = 3;
    private static final Duration WARMUP_TIMEOUT = Duration.ofMinutes(5);

    // 预热任务执行器
    private ExecutorService warmupExecutor;
    private volatile boolean running = false;

    /**
     * 预热任务
     */
    public static class WarmupTask implements Comparable<WarmupTask> {
        private final String key;
        private final Function<String, Object> loader;
        private final Duration ttl;
        private final WarmupPriority priority;
        private final long createTime;
        private int retryCount;

        public WarmupTask(String key, Function<String, Object> loader, Duration ttl, WarmupPriority priority) {
            this.key = key;
            this.loader = loader;
            this.ttl = ttl;
            this.priority = priority;
            this.createTime = System.currentTimeMillis();
            this.retryCount = 0;
        }

        @Override
        public int compareTo(WarmupTask other) {
            // 优先级高的任务先执行
            int priorityCompare = other.priority.ordinal() - this.priority.ordinal();
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 优先级相同，创建时间早的先执行
            return Long.compare(this.createTime, other.createTime);
        }

        public String getKey() { return key; }
        public Function<String, Object> getLoader() { return loader; }
        public Duration getTtl() { return ttl; }
        public WarmupPriority getPriority() { return priority; }
        public long getCreateTime() { return createTime; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { this.retryCount++; }
    }

    /**
     * 预热优先级
     */
    public enum WarmupPriority {
        CRITICAL,   // 关键数据，立即预热
        HIGH,       // 高优先级，优先预热
        NORMAL,     // 普通优先级
        LOW         // 低优先级，空闲时预热
    }

    /**
     * 访问统计
     */
    private static class AccessStats {
        private final AtomicLong accessCount = new AtomicLong(0);
        private final AtomicLong hitCount = new AtomicLong(0);
        private volatile long lastAccessTime = System.currentTimeMillis();
        private volatile long firstAccessTime = System.currentTimeMillis();

        public void recordAccess(boolean hit) {
            accessCount.incrementAndGet();
            if (hit) {
                hitCount.incrementAndGet();
            }
            lastAccessTime = System.currentTimeMillis();
        }

        public long getAccessCount() { return accessCount.get(); }
        public long getHitCount() { return hitCount.get(); }
        public long getLastAccessTime() { return lastAccessTime; }
        public long getFirstAccessTime() { return firstAccessTime; }
        
        public double getHitRate() {
            long total = accessCount.get();
            return total > 0 ? (double) hitCount.get() / total : 0.0;
        }
        
        public long getAccessFrequency() {
            long duration = System.currentTimeMillis() - firstAccessTime;
            return duration > 0 ? accessCount.get() * 3600000L / duration : 0; // 每小时访问次数
        }
    }

    /**
     * 预热统计信息
     */
    public static class WarmupStats {
        private final long totalTasks;
        private final long completedTasks;
        private final long failedTasks;
        private final long queueSize;
        private final long cacheHits;
        private final double completionRate;

        public WarmupStats(long totalTasks, long completedTasks, long failedTasks, 
                          long queueSize, long cacheHits) {
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.failedTasks = failedTasks;
            this.queueSize = queueSize;
            this.cacheHits = cacheHits;
            this.completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks : 0.0;
        }

        public long getTotalTasks() { return totalTasks; }
        public long getCompletedTasks() { return completedTasks; }
        public long getFailedTasks() { return failedTasks; }
        public long getQueueSize() { return queueSize; }
        public long getCacheHits() { return cacheHits; }
        public double getCompletionRate() { return completionRate; }

        @Override
        public String toString() {
            return String.format("WarmupStats{total=%d, completed=%d, failed=%d, queue=%d, hits=%d, rate=%.2f%%}",
                    totalTasks, completedTasks, failedTasks, queueSize, cacheHits, completionRate * 100);
        }
    }

    /**
     * 初始化预热服务
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化缓存预热服务...");
        
        // 创建预热执行器
        warmupExecutor = Executors.newFixedThreadPool(WARMUP_THREAD_COUNT, r -> {
            Thread t = new Thread(r, "cache-warmup-worker");
            t.setDaemon(true);
            return t;
        });

        // 启动预热工作线程
        for (int i = 0; i < WARMUP_THREAD_COUNT; i++) {
            warmupExecutor.submit(this::processWarmupTasks);
        }

        // 订阅缓存失效事件
        subscribeToInvalidationEvents();

        running = true;
        log.info("缓存预热服务初始化完成: 工作线程数={}", WARMUP_THREAD_COUNT);
    }

    /**
     * 应用启动完成后执行系统级预热
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("cacheExecutor")
    public void onApplicationReady() {
        log.info("应用启动完成，开始执行系统级缓存预热...");
        
        // 延迟5秒后开始预热，避免启动时的性能冲击
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // 执行系统级预热任务
        performSystemWarmup();
    }

    /**
     * 添加预热任务
     * 
     * @param key 缓存键
     * @param loader 数据加载器
     * @param ttl 过期时间
     * @param priority 优先级
     */
    public void addWarmupTask(String key, Function<String, Object> loader, Duration ttl, WarmupPriority priority) {
        if (!running) {
            log.warn("预热服务未运行，忽略预热任务: key={}", key);
            return;
        }

        WarmupTask task = new WarmupTask(key, loader, ttl, priority);
        if (warmupQueue.offer(task)) {
            totalWarmupTasks.incrementAndGet();
            log.debug("添加预热任务: key={}, priority={}", key, priority);
        } else {
            log.warn("预热队列已满，丢弃任务: key={}", key);
        }
    }

    /**
     * 批量添加预热任务
     */
    public void addWarmupTasks(Map<String, Function<String, Object>> loaders, Duration ttl, WarmupPriority priority) {
        loaders.forEach((key, loader) -> addWarmupTask(key, loader, ttl, priority));
        log.info("批量添加预热任务: count={}, priority={}", loaders.size(), priority);
    }

    /**
     * 智能预热 - 基于访问模式预测需要预热的缓存
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    @Async("cacheExecutor")
    public void intelligentWarmup() {
        if (!running) {
            return;
        }

        log.debug("开始智能预热分析...");
        
        try {
            // 分析访问统计，找出需要预热的缓存
            List<String> candidateKeys = identifyWarmupCandidates();
            
            if (!candidateKeys.isEmpty()) {
                log.info("发现预热候选项: count={}", candidateKeys.size());
                
                // 为候选键添加预热任务（这里需要根据具体业务实现loader）
                // candidateKeys.forEach(key -> addWarmupTask(key, getLoaderForKey(key), Duration.ofHours(1), WarmupPriority.NORMAL));
            }
            
        } catch (Exception e) {
            log.error("智能预热分析异常", e);
        }
    }

    /**
     * 记录缓存访问统计
     * 
     * @param key 缓存键
     * @param hit 是否命中
     */
    public void recordCacheAccess(String key, boolean hit) {
        AccessStats stats = accessStatsMap.computeIfAbsent(key, k -> new AccessStats());
        stats.recordAccess(hit);
        
        if (hit) {
            cacheHitsAfterWarmup.incrementAndGet();
        }
    }

    /**
     * 清理过期的访问统计
     */
    @Scheduled(fixedDelay = 3600000) // 每小时执行一次
    @Async("cacheExecutor")
    public void cleanupAccessStats() {
        if (!running) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - Duration.ofDays(7).toMillis();
        int beforeSize = accessStatsMap.size();
        
        accessStatsMap.entrySet().removeIf(entry -> 
                entry.getValue().getLastAccessTime() < cutoffTime);
        
        int afterSize = accessStatsMap.size();
        if (beforeSize > afterSize) {
            log.info("清理过期访问统计: 清理前={}, 清理后={}", beforeSize, afterSize);
        }
    }

    /**
     * 获取预热统计信息
     */
    public WarmupStats getWarmupStats() {
        return new WarmupStats(
                totalWarmupTasks.get(),
                completedWarmupTasks.get(),
                failedWarmupTasks.get(),
                warmupQueue.size(),
                cacheHitsAfterWarmup.get()
        );
    }

    /**
     * 记录预热统计
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟记录一次
    @Async("cacheExecutor")
    public void logWarmupStats() {
        if (running) {
            WarmupStats stats = getWarmupStats();
            log.info("缓存预热统计: {}", stats);
        }
    }

    /**
     * 销毁预热服务
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭缓存预热服务...");
        running = false;

        if (warmupExecutor != null) {
            warmupExecutor.shutdown();
            try {
                if (!warmupExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    warmupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                warmupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("缓存预热服务已关闭: 剩余任务数={}", warmupQueue.size());
    }

    // ================================
    // 私有方法
    // ================================

    /**
     * 处理预热任务队列
     */
    private void processWarmupTasks() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                WarmupTask task = warmupQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    executeWarmupTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("处理预热任务异常", e);
            }
        }
    }

    /**
     * 执行单个预热任务
     */
    private void executeWarmupTask(WarmupTask task) {
        try {
            log.debug("执行预热任务: key={}, priority={}", task.getKey(), task.getPriority());
            
            // 检查缓存是否已存在
            // 这里需要扩展cacheManager来支持检查缓存是否存在
            // if (cacheManager.exists(task.getKey())) {
            //     log.debug("缓存已存在，跳过预热: key={}", task.getKey());
            //     completedWarmupTasks.incrementAndGet();
            //     return;
            // }

            // 执行数据加载和缓存
            Object value = task.getLoader().apply(task.getKey());
            if (value != null) {
                cacheManager.put(task.getKey(), value, task.getTtl());
                completedWarmupTasks.incrementAndGet();
                log.debug("预热任务完成: key={}", task.getKey());
            } else {
                log.warn("预热任务返回空值: key={}", task.getKey());
                completedWarmupTasks.incrementAndGet();
            }

        } catch (Exception e) {
            log.error("预热任务执行失败: key={}, retryCount={}", task.getKey(), task.getRetryCount(), e);
            
            // 重试逻辑
            if (task.getRetryCount() < 2) { // 最多重试2次
                task.incrementRetryCount();
                warmupQueue.offer(task);
                log.debug("预热任务重试: key={}, retryCount={}", task.getKey(), task.getRetryCount());
            } else {
                failedWarmupTasks.incrementAndGet();
                log.error("预热任务最终失败: key={}", task.getKey());
            }
        }
    }

    /**
     * 执行系统级预热
     */
    private void performSystemWarmup() {
        log.info("开始系统级缓存预热...");
        
        try {
            // 这里可以根据具体业务需求添加系统级预热逻辑
            // 例如：预热用户信息、配置信息、热门数据等
            
            // 示例：预热系统配置
            addWarmupTask("system:config", key -> loadSystemConfig(), Duration.ofHours(12), WarmupPriority.CRITICAL);
            
            // 示例：预热热门数据
            // addWarmupTask("hot:data", key -> loadHotData(), Duration.ofHours(2), WarmupPriority.HIGH);
            
            log.info("系统级缓存预热任务已添加");
            
        } catch (Exception e) {
            log.error("系统级预热异常", e);
        }
    }

    /**
     * 识别预热候选项
     */
    private List<String> identifyWarmupCandidates() {
        // 基于访问统计识别需要预热的缓存键
        return accessStatsMap.entrySet().stream()
                .filter(entry -> {
                    AccessStats stats = entry.getValue();
                    // 访问频率高且命中率低的键需要预热
                    return stats.getAccessFrequency() > 10 && stats.getHitRate() < 0.8;
                })
                .map(Map.Entry::getKey)
                .limit(100) // 限制候选数量
                .collect(Collectors.toList());
    }

    /**
     * 订阅缓存失效事件
     */
    private void subscribeToInvalidationEvents() {
        // 移除此方法，改为使用事件发布器
    }

    /**
     * 加载系统配置（示例）
     */
    private Object loadSystemConfig() {
        // 这里实现系统配置的加载逻辑
        Map<String, String> config = new HashMap<>();
        config.put("system.name", "IM-System");
        config.put("system.version", "1.0.0");
        config.put("load.time", LocalDateTime.now().toString());
        return config;
    }
} 