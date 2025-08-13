package com.acme.im.common.resilience.retry.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 重试配置类
 * 支持多种重试策略和配置选项
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class RetryConfig {

    /**
     * 重试属性配置
     */
    @Bean
    @ConfigurationProperties(prefix = "resilience.retry")
    public RetryProperties retryProperties() {
        return new RetryProperties();
    }

    /**
     * 默认重试配置
     */
    @Bean
    public io.github.resilience4j.retry.RetryConfig defaultRetryConfig() {
        return io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    /**
     * 快速重试配置
     */
    @Bean
    public io.github.resilience4j.retry.RetryConfig fastRetryConfig() {
        return io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    /**
     * 持久重试配置
     */
    @Bean
    public io.github.resilience4j.retry.RetryConfig persistentRetryConfig() {
        return io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    /**
     * 重试属性配置类
     */
    @Data
    public static class RetryProperties {
        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 默认配置名称
         */
        private String defaultConfig = "default";

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 等待时间（毫秒）
         */
        private long waitDuration = 1000;

        /**
         * 最大等待时间（毫秒）
         */
        private long maxWaitDuration = 10000;

        /**
         * 是否启用指数退避
         */
        private boolean enableExponentialBackoff = true;

        /**
         * 指数退避乘数
         */
        private double exponentialBackoffMultiplier = 2.0;

        /**
         * 是否启用抖动
         */
        private boolean enableJitter = true;

        /**
         * 抖动因子
         */
        private double jitterFactor = 0.1;

        /**
         * 是否启用固定等待时间
         */
        private boolean enableFixedWait = false;

        /**
         * 固定等待时间（毫秒）
         */
        private long fixedWaitDuration = 1000;

        /**
         * 是否启用随机等待时间
         */
        private boolean enableRandomWait = false;

        /**
         * 随机等待时间范围（毫秒）
         */
        private long randomWaitMin = 500;
        private long randomWaitMax = 2000;

        /**
         * 是否启用Fibonacci退避
         */
        private boolean enableFibonacciBackoff = false;

        /**
         * Fibonacci退避乘数
         */
        private double fibonacciBackoffMultiplier = 1.0;

        /**
         * 是否启用自定义退避策略
         */
        private boolean enableCustomBackoff = false;

        /**
         * 自定义退避策略类
         */
        private String customBackoffStrategyClass;

        /**
         * 重试异常类型
         */
        private String[] retryExceptions = {"Exception"};

        /**
         * 忽略的异常类型
         */
        private String[] ignoreExceptions = {"IllegalArgumentException"};

        /**
         * 是否启用结果重试
         */
        private boolean enableResultRetry = false;

        /**
         * 结果重试条件
         */
        private String resultRetryCondition;

        /**
         * 是否启用异常重试
         */
        private boolean enableExceptionRetry = true;

        /**
         * 异常重试条件
         */
        private String exceptionRetryCondition;

        /**
         * 是否启用超时重试
         */
        private boolean enableTimeoutRetry = true;

        /**
         * 超时时间（毫秒）
         */
        private long timeoutDuration = 5000;

        /**
         * 是否启用熔断器集成
         */
        private boolean enableCircuitBreakerIntegration = true;

        /**
         * 熔断器配置名称
         */
        private String circuitBreakerConfigName = "default";

        /**
         * 是否启用限流器集成
         */
        private boolean enableRateLimiterIntegration = true;

        /**
         * 限流器配置名称
         */
        private String rateLimiterConfigName = "default";

        /**
         * 是否启用舱壁隔离集成
         */
        private boolean enableBulkheadIntegration = true;

        /**
         * 舱壁隔离配置名称
         */
        private String bulkheadConfigName = "default";

        /**
         * 是否启用降级集成
         */
        private boolean enableFallbackIntegration = true;

        /**
         * 降级策略
         */
        private String fallbackStrategy = "DEFAULT";

        /**
         * 是否启用监控
         */
        private boolean enableMonitoring = true;

        /**
         * 是否启用指标
         */
        private boolean enableMetrics = true;

        /**
         * 是否启用事件发布
         */
        private boolean enableEventPublishing = true;

        /**
         * 是否启用健康检查
         */
        private boolean enableHealthCheck = true;

        /**
         * 是否启用日志
         */
        private boolean enableLogging = true;

        /**
         * 日志级别
         */
        private String logLevel = "INFO";

        /**
         * 是否启用审计
         */
        private boolean enableAudit = true;

        /**
         * 是否启用追踪
         */
        private boolean enableTracing = true;

        /**
         * 追踪采样率
         */
        private double tracingSampleRate = 0.1;

        /**
         * 是否启用告警
         */
        private boolean enableAlerting = true;

        /**
         * 告警阈值
         */
        private int alertThreshold = 100;

        /**
         * 是否启用缓存
         */
        private boolean enableCache = true;

        /**
         * 缓存大小
         */
        private int cacheSize = 1000;

        /**
         * 缓存过期时间（分钟）
         */
        private int cacheExpiration = 60;

        /**
         * 是否启用异步重试
         */
        private boolean enableAsyncRetry = false;

        /**
         * 异步重试线程池大小
         */
        private int asyncRetryThreadPoolSize = 10;

        /**
         * 异步重试队列大小
         */
        private int asyncRetryQueueSize = 1000;

        /**
         * 是否启用批量重试
         */
        private boolean enableBatchRetry = false;

        /**
         * 批量重试大小
         */
        private int batchRetrySize = 100;

        /**
         * 批量重试间隔（毫秒）
         */
        private long batchRetryInterval = 1000;

        /**
         * 是否启用条件重试
         */
        private boolean enableConditionalRetry = false;

        /**
         * 条件重试表达式
         */
        private String conditionalRetryExpression;

        /**
         * 是否启用智能重试
         */
        private boolean enableSmartRetry = false;

        /**
         * 智能重试算法
         */
        private String smartRetryAlgorithm = "ADAPTIVE";

        /**
         * 是否启用学习重试
         */
        private boolean enableLearningRetry = false;

        /**
         * 学习重试模型
         */
        private String learningRetryModel = "REINFORCEMENT";

        /**
         * 是否启用预测重试
         */
        private boolean enablePredictiveRetry = false;

        /**
         * 预测重试模型
         */
        private String predictiveRetryModel = "LSTM";

        /**
         * 是否启用自适应重试
         */
        private boolean enableAdaptiveRetry = false;

        /**
         * 自适应重试参数
         */
        private String adaptiveRetryParameters;

        /**
         * 是否启用动态重试
         */
        private boolean enableDynamicRetry = false;

        /**
         * 动态重试配置
         */
        private String dynamicRetryConfig;

        /**
         * 是否启用配置热更新
         */
        private boolean enableHotReload = false;

        /**
         * 配置热更新间隔（秒）
         */
        private int hotReloadInterval = 30;

        /**
         * 是否启用配置验证
         */
        private boolean enableConfigValidation = true;

        /**
         * 配置验证规则
         */
        private String configValidationRules;

        /**
         * 是否启用性能优化
         */
        private boolean enablePerformanceOptimization = true;

        /**
         * 性能优化策略
         */
        private String performanceOptimizationStrategy = "DEFAULT";

        /**
         * 是否启用资源管理
         */
        private boolean enableResourceManagement = true;

        /**
         * 资源管理策略
         */
        private String resourceManagementStrategy = "CONSERVATIVE";

        /**
         * 是否启用故障注入
         */
        private boolean enableFaultInjection = false;

        /**
         * 故障注入配置
         */
        private String faultInjectionConfig;

        /**
         * 是否启用混沌工程
         */
        private boolean enableChaosEngineering = false;

        /**
         * 混沌工程配置
         */
        private String chaosEngineeringConfig;
    }
} 