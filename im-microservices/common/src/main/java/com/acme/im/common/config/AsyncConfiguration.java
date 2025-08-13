package com.acme.im.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理配置
 * 提供高性能的异步任务执行能力，支持多种场景的线程池配置
 * 
 * 特性：
 * 1. 多线程池配置 - 不同业务使用不同线程池
 * 2. 动态调整 - 支持运行时调整线程池参数
 * 3. 监控统计 - 详细的线程池统计信息
 * 4. 异常处理 - 统一的异步异常处理机制
 * 5. 优雅关闭 - 确保任务完成后关闭
 * 6. 背压控制 - 防止任务队列溢出
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    // 默认线程池配置
    @Value("${performance.async.core-pool-size:4}")
    private int defaultCorePoolSize;
    
    @Value("${performance.async.max-pool-size:20}")
    private int defaultMaxPoolSize;
    
    @Value("${performance.async.queue-capacity:1000}")
    private int defaultQueueCapacity;
    
    @Value("${performance.async.keep-alive-seconds:60}")
    private int defaultKeepAliveSeconds;
    
    @Value("${performance.async.thread-name-prefix:async-}")
    private String defaultThreadNamePrefix;

    // 事件处理线程池配置
    @Value("${performance.async.event.core-pool-size:2}")
    private int eventCorePoolSize;
    
    @Value("${performance.async.event.max-pool-size:8}")
    private int eventMaxPoolSize;
    
    @Value("${performance.async.event.queue-capacity:5000}")
    private int eventQueueCapacity;
    
    @Value("${performance.async.event.thread-name-prefix:event-}")
    private String eventThreadNamePrefix;

    /**
     * 默认异步线程池
     * 用于一般的异步任务处理
     */
    @Override
    @Bean("taskExecutor")
    public Executor getAsyncExecutor() {
        log.info("创建默认异步线程池: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                defaultCorePoolSize, defaultMaxPoolSize, defaultQueueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置
        executor.setCorePoolSize(defaultCorePoolSize);
        executor.setMaxPoolSize(defaultMaxPoolSize);
        executor.setQueueCapacity(defaultQueueCapacity);
        executor.setKeepAliveSeconds(defaultKeepAliveSeconds);
        executor.setThreadNamePrefix(defaultThreadNamePrefix);
        
        // 拒绝策略 - 调用者运行策略，确保任务不会丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("默认异步线程池创建完成: {}", executor.getThreadPoolExecutor().toString());
        return executor;
    }

    /**
     * 事件处理线程池
     * 专门用于处理NATS事件发布和订阅
     */
    @Bean("eventExecutor")
    public Executor eventExecutor() {
        log.info("创建事件处理线程池: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                eventCorePoolSize, eventMaxPoolSize, eventQueueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置
        executor.setCorePoolSize(eventCorePoolSize);
        executor.setMaxPoolSize(eventMaxPoolSize);
        executor.setQueueCapacity(eventQueueCapacity);
        executor.setKeepAliveSeconds(defaultKeepAliveSeconds);
        executor.setThreadNamePrefix(eventThreadNamePrefix);
        
        // 拒绝策略 - 丢弃最旧的任务，适合事件处理场景
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("事件处理线程池创建完成: {}", executor.getThreadPoolExecutor().toString());
        return executor;
    }

    /**
     * 缓存操作线程池
     * 专门用于缓存预热、失效等操作
     */
    @Bean("cacheExecutor")
    public Executor cacheExecutor() {
        log.info("创建缓存操作线程池");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置 - 缓存操作通常不需要太多线程
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(2000);
        executor.setKeepAliveSeconds(120); // 更长的空闲时间
        executor.setThreadNamePrefix("cache-");
        
        // 拒绝策略 - 调用者运行，确保缓存操作不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(45);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("缓存操作线程池创建完成: {}", executor.getThreadPoolExecutor().toString());
        return executor;
    }

    /**
     * 消息处理线程池
     * 专门用于消息的异步处理（发送、推送等）
     */
    @Bean("messageExecutor")
    public Executor messageExecutor() {
        log.info("创建消息处理线程池");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置 - 消息处理需要较高并发
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(3000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("message-");
        
        // 拒绝策略 - 丢弃最旧的任务，避免消息积压
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("消息处理线程池创建完成: {}", executor.getThreadPoolExecutor().toString());
        return executor;
    }

    /**
     * 文件处理线程池
     * 专门用于文件上传、下载、转换等I/O密集型操作
     */
    @Bean("fileExecutor")
    public Executor fileExecutor() {
        log.info("创建文件处理线程池");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置 - I/O密集型，可以设置更多线程
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(180); // 更长的空闲时间，因为I/O操作可能较慢
        executor.setThreadNamePrefix("file-");
        
        // 拒绝策略 - 调用者运行，确保文件操作不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭 - 文件操作需要更长时间
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        // 不允许核心线程超时，保持基础处理能力
        executor.setAllowCoreThreadTimeOut(false);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("文件处理线程池创建完成: {}", executor.getThreadPoolExecutor().toString());
        return executor;
    }

    /**
     * 监控统计线程池
     * 专门用于系统监控、统计等后台任务
     */
    @Bean("monitorExecutor")
    public Executor monitorExecutor() {
        log.info("创建监控统计线程池");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置 - 监控任务通常不需要太多资源
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(300); // 5分钟空闲时间
        executor.setThreadNamePrefix("monitor-");
        
        // 拒绝策略 - 丢弃任务，监控任务丢失不会影响主要功能
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(false); // 监控任务可以中断
        executor.setAwaitTerminationSeconds(10);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("监控统计线程池创建完成: {}", executor.getThreadPoolExecutor().toString());
        return executor;
    }

    /**
     * 异步异常处理器
     * 统一处理异步方法中的未捕获异常
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncUncaughtExceptionHandler();
    }

    /**
     * 自定义异步异常处理器
     */
    private static class CustomAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
        
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("异步方法执行异常: method={}, params={}", 
                    method.getName(), 
                    params != null ? params.length : 0, 
                    ex);
            
            // 可以在这里添加异常通知、告警等逻辑
            // 例如：发送异常通知到监控系统
            try {
                // 这里可以集成异常通知系统
                notifyException(method, ex, params);
            } catch (Exception notifyEx) {
                log.warn("发送异常通知失败", notifyEx);
            }
        }
        
        /**
         * 异常通知（可扩展）
         */
        private void notifyException(Method method, Throwable ex, Object... params) {
            // 实现异常通知逻辑
            // 例如：发送到告警系统、记录到特殊日志等
            log.warn("异步异常通知: {}#{} - {}", 
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    ex.getMessage());
        }
    }
} 