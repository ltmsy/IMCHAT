package com.acme.im.common.infrastructure.nats.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 事件持久化配置
 * 提供可配置的持久化策略参数
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "im.event.persistence")
public class EventPersistenceConfig {

    /**
     * 是否启用事件持久化
     */
    private boolean enabled = true;

    /**
     * 批量处理配置
     */
    private BatchConfig batch = new BatchConfig();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 持久化策略配置
     */
    private StrategyConfig strategy = new StrategyConfig();

    /**
     * 批量处理配置
     */
    @Data
    public static class BatchConfig {
        /**
         * 批量写入大小
         */
        private int size = 100;

        /**
         * 批量处理间隔（毫秒）
         */
        private long intervalMs = 5000;

        /**
         * 最大批量大小
         */
        private int maxSize = 500;

        /**
         * 是否启用异步批量处理
         */
        private boolean asyncEnabled = true;
    }

    /**
     * 缓存配置
     */
    @Data
    public static class CacheConfig {
        /**
         * 最大缓存大小
         */
        private int maxSize = 1000;

        /**
         * 缓存过期时间（秒）
         */
        private long expireSeconds = 300;

        /**
         * 是否启用缓存
         */
        private boolean enabled = true;

        /**
         * 缓存清理间隔（毫秒）
         */
        private long cleanupIntervalMs = 60000;
    }

    /**
     * 持久化策略配置
     */
    @Data
    public static class StrategyConfig {
        private boolean persistFailure = true;
        private boolean persistHighPriority = true;
        private boolean persistAuthEvents = true;
        private boolean persistSystemEvents = true;
        private boolean persistChatEvents = false;
        private boolean persistAll = false;
    }

    /**
     * 获取批量大小
     */
    public int getBatchSize() {
        return Math.min(batch.getSize(), batch.getMaxSize());
    }

    /**
     * 获取批量处理间隔
     */
    public long getBatchIntervalMs() {
        return Math.max(batch.getIntervalMs(), 1000); // 最小1秒
    }

    /**
     * 获取最大缓存大小
     */
    public int getMaxCacheSize() {
        return Math.max(cache.getMaxSize(), 100); // 最小100
    }

    /**
     * 检查是否应该持久化事件
     */
    public boolean shouldPersistEvent(String subject, String status, String priority) {
        if (!enabled) {
            return false;
        }

        // 1. 失败事件 - 必须持久化（用于故障排查）
        if (strategy.isPersistFailure() && "FAILURE".equals(status)) {
            return true;
        }

        // 2. 高优先级事件 - 必须持久化（业务关键操作）
        if (strategy.isPersistHighPriority() && 
            ("HIGH".equals(priority) || "URGENT".equals(priority))) {
            return true;
        }

        // 3. 认证相关事件 - 安全审计需要
        if (strategy.isPersistAuthEvents() && 
            (subject.startsWith("auth.") || subject.startsWith("user."))) {
            return true;
        }

        // 4. 系统事件 - 运维监控需要
        if (strategy.isPersistSystemEvents() && 
            subject.startsWith("system.")) {
            return true;
        }

        // 5. 聊天消息事件 - 通常不持久化（量大且不重要）
        if (strategy.isPersistChatEvents() && 
            subject.startsWith("message.")) {
            return true;
        }

        // 6. 配置要求持久化所有事件（开发/测试环境）
        if (strategy.isPersistAll()) {
            return true;
        }

        // 默认不持久化
        return false;
    }

    /**
     * 获取事件保留策略
     */
    public RetentionStrategy getRetentionStrategy(String subject, String status, String priority) {
        // 认证失败事件 - 长期保留（安全审计）
        if (subject.startsWith("auth.") && "FAILURE".equals(status)) {
            return new RetentionStrategy(365, true); // 保留1年，不删除
        }

        // 系统异常事件 - 中期保留（故障排查）
        if (subject.startsWith("system.") && "FAILURE".equals(status)) {
            return new RetentionStrategy(90, false); // 保留3个月
        }

        // 高优先级事件 - 中期保留（业务追踪）
        if ("HIGH".equals(priority) || "URGENT".equals(priority)) {
            return new RetentionStrategy(180, false); // 保留6个月
        }

        // 普通失败事件 - 短期保留（问题定位）
        if ("FAILURE".equals(status)) {
            return new RetentionStrategy(30, false); // 保留1个月
        }

        // 其他事件 - 短期保留
        return new RetentionStrategy(7, false); // 保留1周
    }

    /**
     * 事件保留策略
     */
    @Data
    public static class RetentionStrategy {
        private final int retentionDays;
        private final boolean permanent; // 是否永久保留

        public RetentionStrategy(int retentionDays, boolean permanent) {
            this.retentionDays = retentionDays;
            this.permanent = permanent;
        }
    }
} 