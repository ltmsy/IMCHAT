package com.acme.im.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 执行器配置类
 * 提供默认的线程池执行器
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
public class ExecutorConfig {

    /**
     * 创建默认的线程池执行器
     * 用于异步任务处理
     */
    @Bean("customTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("im-task-");
        executor.initialize();
        return executor;
    }
} 