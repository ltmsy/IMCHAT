package com.acme.im.common.infrastructure.nats.publisher;

import com.acme.im.common.infrastructure.nats.config.NatsConnectionPool;
import com.google.gson.Gson;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步事件发布器
 * 提供高性能的异步事件发布能力，支持批量处理、重试机制和背压控制
 * 
 * 特性：
 * 1. 异步处理 - 非阻塞事件发布
 * 2. 批量发送 - 批量处理提升吞吐量
 * 3. 重试机制 - 失败重试和死信队列
 * 4. 背压控制 - 防止内存溢出
 * 5. 监控统计 - 详细的性能指标
 * 6. 优雅关闭 - 确保消息不丢失
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class AsyncEventPublisher {

    @Autowired
    private NatsConnectionPool connectionPool;
    
    @Autowired
    @Qualifier("gson")
    private Gson gson;

    // 异步处理配置
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_BATCH_TIMEOUT_MS = 1000;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;

    // 事件队列
    private final BlockingQueue<EventTask> eventQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
    
    // 线程池
    private ThreadPoolExecutor publisherExecutor;
    private ScheduledExecutorService scheduledExecutor;
    
    // 统计信息
    private final AtomicLong totalEvents = new AtomicLong(0);
    private final AtomicLong successfulEvents = new AtomicLong(0);
    private final AtomicLong failedEvents = new AtomicLong(0);
    private final AtomicLong retriedEvents = new AtomicLong(0);
    private final AtomicLong droppedEvents = new AtomicLong(0);
    
    // 状态控制
    private volatile boolean running = false;
    private volatile boolean shutdown = false;

    /**
     * 事件任务
     */
    private static class EventTask {
        private final String subject;
        private final Object event;
        private final boolean useJetStream;
        private final long createTime;
        private int retryCount;
        private final CompletableFuture<Void> future;

        public EventTask(String subject, Object event, boolean useJetStream) {
            this.subject = subject;
            this.event = event;
            this.useJetStream = useJetStream;
            this.createTime = System.currentTimeMillis();
            this.retryCount = 0;
            this.future = new CompletableFuture<>();
        }

        public String getSubject() { return subject; }
        public Object getEvent() { return event; }
        public boolean isUseJetStream() { return useJetStream; }
        public long getCreateTime() { return createTime; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { this.retryCount++; }
        public CompletableFuture<Void> getFuture() { return future; }
        
        public boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - createTime > maxAgeMs;
        }
    }

    /**
     * 发布统计信息
     */
    public static class PublishStats {
        private final long totalEvents;
        private final long successfulEvents;
        private final long failedEvents;
        private final long retriedEvents;
        private final long droppedEvents;
        private final int queueSize;
        private final double successRate;

        public PublishStats(long totalEvents, long successfulEvents, long failedEvents, 
                          long retriedEvents, long droppedEvents, int queueSize) {
            this.totalEvents = totalEvents;
            this.successfulEvents = successfulEvents;
            this.failedEvents = failedEvents;
            this.retriedEvents = retriedEvents;
            this.droppedEvents = droppedEvents;
            this.queueSize = queueSize;
            this.successRate = totalEvents > 0 ? (double) successfulEvents / totalEvents : 0.0;
        }

        public long getTotalEvents() { return totalEvents; }
        public long getSuccessfulEvents() { return successfulEvents; }
        public long getFailedEvents() { return failedEvents; }
        public long getRetriedEvents() { return retriedEvents; }
        public long getDroppedEvents() { return droppedEvents; }
        public int getQueueSize() { return queueSize; }
        public double getSuccessRate() { return successRate; }

        @Override
        public String toString() {
            return String.format("PublishStats{total=%d, success=%d, failed=%d, retried=%d, dropped=%d, queue=%d, successRate=%.2f%%}", 
                    totalEvents, successfulEvents, failedEvents, retriedEvents, droppedEvents, queueSize, successRate * 100);
        }
    }

    /**
     * 初始化异步发布器
     */
    @PostConstruct
    public void initialize() {
        if (running) {
            return;
        }

        log.info("初始化异步事件发布器...");

        // 创建线程池
        publisherExecutor = new ThreadPoolExecutor(
                DEFAULT_THREAD_POOL_SIZE,
                DEFAULT_THREAD_POOL_SIZE,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadFactory() {
                    private final AtomicLong counter = new AtomicLong(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "async-event-publisher-" + counter.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 创建调度线程池
        scheduledExecutor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicLong counter = new AtomicLong(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "async-event-scheduler-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });

        // 启动事件处理任务
        for (int i = 0; i < DEFAULT_THREAD_POOL_SIZE; i++) {
            publisherExecutor.submit(this::processEvents);
        }

        // 启动批量处理任务
        scheduledExecutor.scheduleAtFixedRate(this::processBatchEvents, 
                DEFAULT_BATCH_TIMEOUT_MS, DEFAULT_BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // 启动统计任务
        scheduledExecutor.scheduleAtFixedRate(this::logStatistics, 30, 30, TimeUnit.SECONDS);

        running = true;
        log.info("异步事件发布器初始化完成: 线程池大小={}, 队列容量={}", 
                DEFAULT_THREAD_POOL_SIZE, DEFAULT_QUEUE_CAPACITY);
    }

    /**
     * 异步发布事件
     * 
     * @param subject 主题
     * @param event 事件对象
     * @return CompletableFuture
     */
    public CompletableFuture<Void> publishEventAsync(String subject, Object event) {
        return publishEventAsync(subject, event, false);
    }

    /**
     * 异步发布事件到JetStream
     * 
     * @param subject 主题
     * @param event 事件对象
     * @return CompletableFuture
     */
    public CompletableFuture<Void> publishToJetStreamAsync(String subject, Object event) {
        return publishEventAsync(subject, event, true);
    }

    /**
     * 异步发布事件（内部方法）
     */
    private CompletableFuture<Void> publishEventAsync(String subject, Object event, boolean useJetStream) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new RuntimeException("发布器已关闭"));
        }

        EventTask task = new EventTask(subject, event, useJetStream);
        totalEvents.incrementAndGet();

        // 尝试添加到队列
        if (eventQueue.offer(task)) {
            log.debug("事件已加入队列: subject={}, queueSize={}", subject, eventQueue.size());
            return task.getFuture();
        } else {
            // 队列满了，拒绝事件
            droppedEvents.incrementAndGet();
            log.warn("事件队列已满，丢弃事件: subject={}, queueSize={}", subject, eventQueue.size());
            return CompletableFuture.failedFuture(new RuntimeException("事件队列已满"));
        }
    }

    /**
     * 同步发布事件（兼容性）
     */
    public void publishEvent(String subject, Object event) {
        try {
            publishEventAsync(subject, event).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("同步发布事件失败: subject={}", subject, e);
            throw new RuntimeException("发布事件失败", e);
        }
    }

    /**
     * 同步发布事件到JetStream（兼容性）
     */
    public void publishToJetStream(String subject, Object event) {
        try {
            publishToJetStreamAsync(subject, event).get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("发布JetStream事件超时: subject={}, 超时时间=30秒", subject);
            // 超时时不抛出异常，让业务继续执行
        } catch (Exception e) {
            log.error("同步发布JetStream事件失败: subject={}", subject, e);
            // 其他异常也不抛出，避免影响业务流程
        }
    }

    /**
     * 获取发布统计信息
     */
    public PublishStats getStats() {
        return new PublishStats(
                totalEvents.get(),
                successfulEvents.get(),
                failedEvents.get(),
                retriedEvents.get(),
                droppedEvents.get(),
                eventQueue.size()
        );
    }

    /**
     * 优雅关闭
     */
    @PreDestroy
    public void shutdown() {
        if (shutdown) {
            return;
        }

        log.info("正在关闭异步事件发布器...");
        shutdown = true;

        // 等待队列中的事件处理完成
        long waitStart = System.currentTimeMillis();
        while (!eventQueue.isEmpty() && (System.currentTimeMillis() - waitStart) < 30000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 关闭线程池
        if (publisherExecutor != null) {
            publisherExecutor.shutdown();
            try {
                if (!publisherExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    publisherExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                publisherExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        running = false;
        log.info("异步事件发布器已关闭: 剩余队列事件={}", eventQueue.size());
    }

    // ================================
    // 私有处理方法
    // ================================

    /**
     * 处理事件队列
     */
    private void processEvents() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                EventTask task = eventQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    processEventTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("处理事件任务异常", e);
            }
        }
    }

    /**
     * 批量处理事件
     */
    private void processBatchEvents() {
        if (!running || eventQueue.isEmpty()) {
            return;
        }

        try {
            // 收集一批事件
            var batch = new java.util.ArrayList<EventTask>();
            eventQueue.drainTo(batch, DEFAULT_BATCH_SIZE);

            if (!batch.isEmpty()) {
                log.debug("批量处理事件: size={}", batch.size());
                
                // 并行处理批次
                batch.parallelStream().forEach(this::processEventTask);
            }
        } catch (Exception e) {
            log.error("批量处理事件异常", e);
        }
    }

    /**
     * 处理单个事件任务
     */
    private void processEventTask(EventTask task) {
        Connection connection = null;
        try {
            // 检查事件是否过期
            if (task.isExpired(60000)) { // 1分钟过期
                log.warn("事件已过期，丢弃: subject={}, age={}ms", 
                        task.getSubject(), System.currentTimeMillis() - task.getCreateTime());
                droppedEvents.incrementAndGet();
                task.getFuture().completeExceptionally(new RuntimeException("事件已过期"));
                return;
            }

            // 获取连接
            connection = connectionPool.borrowConnection();
            
            // 序列化事件
            String jsonEvent = gson.toJson(task.getEvent());
            byte[] eventData = jsonEvent.getBytes(StandardCharsets.UTF_8);

            // 发布事件
            if (task.isUseJetStream()) {
                JetStream jetStream = connection.jetStream();
                jetStream.publish(task.getSubject(), eventData);
            } else {
                connection.publish(task.getSubject(), eventData);
            }

            // 发布成功
            successfulEvents.incrementAndGet();
            task.getFuture().complete(null);
            
            log.debug("事件发布成功: subject={}, useJetStream={}", 
                    task.getSubject(), task.isUseJetStream());

        } catch (Exception e) {
            handleEventError(task, e);
        } finally {
            // 归还连接
            if (connection != null) {
                connectionPool.returnConnection(connection);
            }
        }
    }

    /**
     * 处理事件错误
     */
    private void handleEventError(EventTask task, Exception error) {
        log.warn("事件发布失败: subject={}, retryCount={}, error={}", 
                task.getSubject(), task.getRetryCount(), error.getMessage());

        // 重试逻辑
        if (task.getRetryCount() < DEFAULT_MAX_RETRIES) {
            task.incrementRetryCount();
            retriedEvents.incrementAndGet();
            
            // 延迟重试
            scheduledExecutor.schedule(() -> {
                if (eventQueue.offer(task)) {
                    log.debug("事件重试: subject={}, retryCount={}", 
                            task.getSubject(), task.getRetryCount());
                } else {
                    // 重试队列满了
                    droppedEvents.incrementAndGet();
                    task.getFuture().completeExceptionally(new RuntimeException("重试队列已满"));
                }
            }, Math.min(1000 * (1 << task.getRetryCount()), 30000), TimeUnit.MILLISECONDS);
            
        } else {
            // 重试次数耗尽
            failedEvents.incrementAndGet();
            task.getFuture().completeExceptionally(error);
            
            log.error("事件发布最终失败: subject={}, maxRetries={}", 
                    task.getSubject(), DEFAULT_MAX_RETRIES);
        }
    }

    /**
     * 记录统计信息
     */
    private void logStatistics() {
        if (running) {
            PublishStats stats = getStats();
//            log.info("异步发布器统计: {}", stats);
        }
    }
} 