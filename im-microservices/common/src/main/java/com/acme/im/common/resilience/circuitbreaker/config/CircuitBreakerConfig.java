package com.acme.im.common.resilience.circuitbreaker.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 熔断器配置类
 * 支持多种熔断策略和配置选项
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {

    /**
     * 熔断器属性配置
     */
    @Bean
    @ConfigurationProperties(prefix = "resilience.circuitbreaker")
    public CircuitBreakerProperties circuitBreakerProperties() {
        return new CircuitBreakerProperties();
    }

    /**
     * 默认熔断器配置
     */
    @Bean
    public io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slowCallRateThreshold(100.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build();
    }

    /**
     * 严格熔断器配置（快速失败）
     */
    @Bean
    public io.github.resilience4j.circuitbreaker.CircuitBreakerConfig strictCircuitBreakerConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .failureRateThreshold(30.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(1))
                .build();
    }

    /**
     * 宽松熔断器配置（容忍更多失败）
     */
    @Bean
    public io.github.resilience4j.circuitbreaker.CircuitBreakerConfig lenientCircuitBreakerConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .failureRateThreshold(70.0f)
                .waitDurationInOpenState(Duration.ofSeconds(120))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slowCallRateThreshold(100.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .build();
    }

    /**
     * 时间窗口熔断器配置
     */
    @Bean
    public io.github.resilience4j.circuitbreaker.CircuitBreakerConfig timeBasedCircuitBreakerConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.TIME_BASED)
                .slidingWindowSize(60) // 60秒
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slowCallRateThreshold(100.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build();
    }

    /**
     * 熔断器属性配置类
     */
    @Data
    public static class CircuitBreakerProperties {
        /**
         * 是否启用熔断器
         */
        private boolean enabled = true;

        /**
         * 默认配置名称
         */
        private String defaultConfig = "default";

        /**
         * 滑动窗口类型
         */
        private String slidingWindowType = "COUNT_BASED";

        /**
         * 滑动窗口大小
         */
        private int slidingWindowSize = 10;

        /**
         * 失败率阈值（百分比）
         */
        private float failureRateThreshold = 50.0f;

        /**
         * 慢调用率阈值（百分比）
         */
        private float slowCallRateThreshold = 100.0f;

        /**
         * 慢调用持续时间阈值（毫秒）
         */
        private long slowCallDurationThreshold = 2000;

        /**
         * 半开状态允许的调用次数
         */
        private int permittedNumberOfCallsInHalfOpenState = 5;

        /**
         * 打开状态的等待时间（秒）
         */
        private int waitDurationInOpenState = 60;

        /**
         * 最小调用次数（用于计算失败率）
         */
        private int minimumNumberOfCalls = 10;

        /**
         * 是否启用自动转换
         */
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;

        /**
         * 是否记录异常
         */
        private boolean recordExceptions = true;

        /**
         * 是否记录失败结果
         */
        private boolean recordFailure = true;

        /**
         * 是否记录成功结果
         */
        private boolean recordSuccess = false;

        /**
         * 是否记录慢调用
         */
        private boolean recordSlowCalls = true;

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
         * 是否启用缓存
         */
        private boolean enableCache = true;

        /**
         * 缓存大小
         */
        private int cacheSize = 1000;

        /**
         * 缓存过期时间（秒）
         */
        private int cacheExpiration = 300;

        /**
         * 是否启用重试
         */
        private boolean enableRetry = true;

        /**
         * 最大重试次数
         */
        private int maxRetryAttempts = 3;

        /**
         * 重试等待时间（毫秒）
         */
        private long retryWaitDuration = 1000;

        /**
         * 是否启用退避策略
         */
        private boolean enableBackoff = true;

        /**
         * 退避乘数
         */
        private double backoffMultiplier = 2.0;

        /**
         * 最大退避时间（毫秒）
         */
        private long maxBackoffDuration = 10000;

        /**
         * 是否启用抖动
         */
        private boolean enableJitter = true;

        /**
         * 抖动因子
         */
        private double jitterFactor = 0.1;

        /**
         * 是否启用超时
         */
        private boolean enableTimeout = true;

        /**
         * 超时时间（毫秒）
         */
        private long timeoutDuration = 5000;

        /**
         * 是否启用并发限制
         */
        private boolean enableConcurrencyLimit = true;

        /**
         * 最大并发调用数
         */
        private int maxConcurrentCalls = 100;

        /**
         * 是否启用限流
         */
        private boolean enableRateLimiting = true;

        /**
         * 限流速率（每秒请求数）
         */
        private int rateLimitPerSecond = 1000;

        /**
         * 限流突发大小
         */
        private int rateLimitBurstSize = 100;

        /**
         * 是否启用熔断器链
         */
        private boolean enableChain = false;

        /**
         * 熔断器链配置
         */
        private String chainConfig = "";

        /**
         * 是否启用降级
         */
        private boolean enableFallback = true;

        /**
         * 降级策略
         */
        private String fallbackStrategy = "DEFAULT";

        /**
         * 是否启用监控
         */
        private boolean enableMonitoring = true;

        /**
         * 监控间隔（秒）
         */
        private int monitoringInterval = 30;

        /**
         * 是否启用告警
         */
        private boolean enableAlerting = true;

        /**
         * 告警阈值
         */
        private float alertThreshold = 80.0f;

        /**
         * 是否启用日志
         */
        private boolean enableLogging = true;

        /**
         * 日志级别
         */
        private String logLevel = "INFO";

        /**
         * 是否启用追踪
         */
        private boolean enableTracing = true;

        /**
         * 追踪采样率
         */
        private double tracingSampleRate = 0.1;
    }
} 