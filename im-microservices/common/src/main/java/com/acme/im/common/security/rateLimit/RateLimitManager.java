package com.acme.im.common.security.rateLimit;

import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流管理器
 * 支持令牌桶、滑动窗口、分布式限流等算法
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class RateLimitManager {

    @Autowired
    private AsyncEventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 限流器缓存
    private final Map<String, RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 限流器接口
     */
    public interface RateLimiter {
        boolean tryAcquire();
        boolean tryAcquire(int permits);
        boolean tryAcquire(int permits, long timeout, TimeUnit unit);
        double getRate();
        void setRate(double rate);
        String getLimiterType();
        RateLimitStatistics getStatistics();
    }

    /**
     * 限流配置
     */
    @Data
    public static class RateLimitConfig {
        private String key;
        private String type; // TOKEN_BUCKET, SLIDING_WINDOW, LEAKY_BUCKET
        private double rate; // 每秒请求数
        private int capacity; // 容量
        private int burstSize; // 突发大小
        private long windowSize; // 窗口大小（毫秒）
        private int windowCount; // 窗口数量
        private boolean distributed; // 是否分布式
        private String redisKeyPrefix; // Redis键前缀
        private long expireTime; // 过期时间（秒）
        private boolean enabled; // 是否启用
        private Map<String, Object> attributes; // 扩展属性
    }

    /**
     * 限流统计信息
     */
    @Data
    public static class RateLimitStatistics {
        private String key;
        private long totalRequests;
        private long allowedRequests;
        private long rejectedRequests;
        private double currentRate;
        private double allowedRate;
        private long lastRequestTime;
        private long lastResetTime;
        private Map<String, Object> metrics;
    }

    /**
     * 限流事件
     */
    @Data
    public static class RateLimitEvent {
        private String eventType;
        private String key;
        private String userId;
        private String clientIp;
        private String userAgent;
        private long timestamp;
        private boolean allowed;
        private String reason;
        private Map<String, Object> context;
    }

    /**
     * 初始化限流管理器
     */
    public void initialize() {
        try {
            // 启动清理任务
            startCleanupTask();
            
            log.info("限流管理器初始化成功");
        } catch (Exception e) {
            log.error("限流管理器初始化失败", e);
        }
    }

    /**
     * 创建限流器
     * 
     * @param config 限流配置
     * @return 限流器实例
     */
    public RateLimiter createRateLimiter(RateLimitConfig config) {
        try {
            if (config == null || config.getKey() == null) {
                log.warn("限流配置无效");
                return null;
            }

            RateLimiter rateLimiter;
            switch (config.getType().toUpperCase()) {
                case "TOKEN_BUCKET":
                    rateLimiter = new TokenBucketRateLimiter(config);
                    break;
                case "SLIDING_WINDOW":
                    rateLimiter = new SlidingWindowRateLimiter(config);
                    break;
                case "LEAKY_BUCKET":
                    rateLimiter = new LeakyBucketRateLimiter(config);
                    break;
                default:
                    log.warn("不支持的限流类型: {}", config.getType());
                    return null;
            }

            rateLimiterCache.put(config.getKey(), rateLimiter);
            log.info("限流器创建成功: key={}, type={}, rate={}", 
                    config.getKey(), config.getType(), config.getRate());
            
            return rateLimiter;
        } catch (Exception e) {
            log.error("创建限流器失败: key={}, type={}", config.getKey(), config.getType(), e);
            return null;
        }
    }

    /**
     * 获取限流器
     * 
     * @param key 限流器键
     * @return 限流器实例
     */
    public RateLimiter getRateLimiter(String key) {
        return rateLimiterCache.get(key);
    }

    /**
     * 检查限流
     * 
     * @param key 限流器键
     * @param userId 用户ID
     * @param clientIp 客户端IP
     * @param userAgent 用户代理
     * @return 是否允许请求
     */
    public boolean checkRateLimit(String key, String userId, String clientIp, String userAgent) {
        return checkRateLimit(key, userId, clientIp, userAgent, null);
    }

    /**
     * 检查限流（带上下文）
     * 
     * @param key 限流器键
     * @param userId 用户ID
     * @param clientIp 客户端IP
     * @param userAgent 用户代理
     * @param context 上下文信息
     * @return 是否允许请求
     */
    public boolean checkRateLimit(String key, String userId, String clientIp, String userAgent, Map<String, Object> context) {
        try {
            RateLimiter rateLimiter = getRateLimiter(key);
            if (rateLimiter == null) {
                log.warn("限流器不存在: {}", key);
                return true; // 没有限流器时默认允许
            }

            boolean allowed = rateLimiter.tryAcquire();
            
            // 记录限流事件
            recordRateLimitEvent(key, userId, clientIp, userAgent, allowed, context);
            
            if (allowed) {
                log.debug("限流检查通过: key={}, userId={}", key, userId);
            } else {
                log.warn("限流检查拒绝: key={}, userId={}", key, userId);
            }
            
            return allowed;
        } catch (Exception e) {
            log.error("限流检查异常: key={}, userId={}", key, userId, e);
            return true; // 异常时默认允许
        }
    }

    /**
     * 令牌桶限流器实现
     */
    private class TokenBucketRateLimiter implements RateLimiter {
        private final RateLimitConfig config;
        private final AtomicLong tokens;
        private final AtomicLong lastRefillTime;
        private final RateLimitStatistics statistics;

        public TokenBucketRateLimiter(RateLimitConfig config) {
            this.config = config;
            this.tokens = new AtomicLong(config.getCapacity());
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.statistics = new RateLimitStatistics();
            this.statistics.setKey(config.getKey());
            this.statistics.setAllowedRate(config.getRate());
            this.statistics.setMetrics(new ConcurrentHashMap<>());
        }

        @Override
        public boolean tryAcquire() {
            return tryAcquire(1);
        }

        @Override
        public boolean tryAcquire(int permits) {
            refillTokens();
            
            long currentTokens = tokens.get();
            if (currentTokens >= permits) {
                if (tokens.addAndGet(-permits) >= 0) {
                    updateStatistics(true, permits);
                    return true;
                } else {
                    tokens.addAndGet(permits); // 回滚
                    updateStatistics(false, permits);
                    return false;
                }
            } else {
                updateStatistics(false, permits);
                return false;
            }
        }

        @Override
        public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
            long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
            while (System.currentTimeMillis() < endTime) {
                if (tryAcquire(permits)) {
                    return true;
                }
                try {
                    Thread.sleep(10); // 短暂等待
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        @Override
        public double getRate() {
            return config.getRate();
        }

        @Override
        public void setRate(double rate) {
            config.setRate(rate);
            statistics.setAllowedRate(rate);
        }

        @Override
        public String getLimiterType() {
            return "TOKEN_BUCKET";
        }

        @Override
        public RateLimitStatistics getStatistics() {
            return statistics;
        }

        private void refillTokens() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            long timePassed = now - lastRefill;
            
            if (timePassed >= 1000) { // 每秒补充一次
                long tokensToAdd = (long) (timePassed / 1000.0 * config.getRate());
                long currentTokens = tokens.get();
                long newTokens = Math.min(currentTokens + tokensToAdd, config.getCapacity());
                
                if (tokens.compareAndSet(currentTokens, newTokens)) {
                    lastRefillTime.set(now);
                }
            }
        }

        private void updateStatistics(boolean allowed, int permits) {
            statistics.setTotalRequests(statistics.getTotalRequests() + permits);
            if (allowed) {
                statistics.setAllowedRequests(statistics.getAllowedRequests() + permits);
            } else {
                statistics.setRejectedRequests(statistics.getRejectedRequests() + permits);
            }
            statistics.setLastRequestTime(System.currentTimeMillis());
            statistics.setCurrentRate((double) statistics.getAllowedRequests() / 
                    Math.max(1, (System.currentTimeMillis() - statistics.getLastResetTime()) / 1000.0));
        }
    }

    /**
     * 滑动窗口限流器实现
     */
    private class SlidingWindowRateLimiter implements RateLimiter {
        private final RateLimitConfig config;
        private final long[] windowCounts;
        private final AtomicLong currentWindowIndex;
        private final AtomicLong lastWindowTime;
        private final RateLimitStatistics statistics;

        public SlidingWindowRateLimiter(RateLimitConfig config) {
            this.config = config;
            this.windowCounts = new long[config.getWindowCount()];
            this.currentWindowIndex = new AtomicLong(0);
            this.lastWindowTime = new AtomicLong(System.currentTimeMillis());
            this.statistics = new RateLimitStatistics();
            this.statistics.setKey(config.getKey());
            this.statistics.setAllowedRate(config.getRate());
            this.statistics.setMetrics(new ConcurrentHashMap<>());
        }

        @Override
        public boolean tryAcquire() {
            return tryAcquire(1);
        }

        @Override
        public boolean tryAcquire(int permits) {
            long now = System.currentTimeMillis();
            long windowIndex = (now / config.getWindowSize()) % config.getWindowCount();
            
            // 检查是否需要滑动窗口
            if (windowIndex != currentWindowIndex.get()) {
                slideWindow(windowIndex);
            }
            
            // 检查当前窗口是否允许请求
            long currentCount = windowCounts[(int) windowIndex];
            if (currentCount + permits <= config.getCapacity()) {
                windowCounts[(int) windowIndex] += permits;
                updateStatistics(true, permits);
                return true;
            } else {
                updateStatistics(false, permits);
                return false;
            }
        }

        @Override
        public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
            long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
            while (System.currentTimeMillis() < endTime) {
                if (tryAcquire(permits)) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        @Override
        public double getRate() {
            return config.getRate();
        }

        @Override
        public void setRate(double rate) {
            config.setRate(rate);
            statistics.setAllowedRate(rate);
        }

        @Override
        public String getLimiterType() {
            return "SLIDING_WINDOW";
        }

        @Override
        public RateLimitStatistics getStatistics() {
            return statistics;
        }

        private void slideWindow(long newWindowIndex) {
            long oldWindowIndex = currentWindowIndex.get();
            if (newWindowIndex != oldWindowIndex) {
                // 清空旧窗口
                for (int i = 0; i < windowCounts.length; i++) {
                    if (i != newWindowIndex) {
                        windowCounts[i] = 0;
                    }
                }
                currentWindowIndex.set(newWindowIndex);
                lastWindowTime.set(System.currentTimeMillis());
            }
        }

        private void updateStatistics(boolean allowed, int permits) {
            statistics.setTotalRequests(statistics.getTotalRequests() + permits);
            if (allowed) {
                statistics.setAllowedRequests(statistics.getAllowedRequests() + permits);
            } else {
                statistics.setRejectedRequests(statistics.getRejectedRequests() + permits);
            }
            statistics.setLastRequestTime(System.currentTimeMillis());
        }
    }

    /**
     * 漏桶限流器实现
     */
    private class LeakyBucketRateLimiter implements RateLimiter {
        private final RateLimitConfig config;
        private final AtomicLong currentWater;
        private final AtomicLong lastLeakTime;
        private final RateLimitStatistics statistics;

        public LeakyBucketRateLimiter(RateLimitConfig config) {
            this.config = config;
            this.currentWater = new AtomicLong(0);
            this.lastLeakTime = new AtomicLong(System.currentTimeMillis());
            this.statistics = new RateLimitStatistics();
            this.statistics.setKey(config.getKey());
            this.statistics.setAllowedRate(config.getRate());
            this.statistics.setMetrics(new ConcurrentHashMap<>());
        }

        @Override
        public boolean tryAcquire() {
            return tryAcquire(1);
        }

        @Override
        public boolean tryAcquire(int permits) {
            leakWater();
            
            long currentWaterLevel = currentWater.get();
            if (currentWaterLevel + permits <= config.getCapacity()) {
                currentWater.addAndGet(permits);
                updateStatistics(true, permits);
                return true;
            } else {
                updateStatistics(false, permits);
                return false;
            }
        }

        @Override
        public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
            long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
            while (System.currentTimeMillis() < endTime) {
                if (tryAcquire(permits)) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        @Override
        public double getRate() {
            return config.getRate();
        }

        @Override
        public void setRate(double rate) {
            config.setRate(rate);
            statistics.setAllowedRate(rate);
        }

        @Override
        public String getLimiterType() {
            return "LEAKY_BUCKET";
        }

        @Override
        public RateLimitStatistics getStatistics() {
            return statistics;
        }

        private void leakWater() {
            long now = System.currentTimeMillis();
            long lastLeak = lastLeakTime.get();
            long timePassed = now - lastLeak;
            
            if (timePassed >= 1000) { // 每秒漏水一次
                long waterToLeak = (long) (timePassed / 1000.0 * config.getRate());
                long currentWaterLevel = currentWater.get();
                long newWaterLevel = Math.max(0, currentWaterLevel - waterToLeak);
                
                if (currentWater.compareAndSet(currentWaterLevel, newWaterLevel)) {
                    lastLeakTime.set(now);
                }
            }
        }

        private void updateStatistics(boolean allowed, int permits) {
            statistics.setTotalRequests(statistics.getTotalRequests() + permits);
            if (allowed) {
                statistics.setAllowedRequests(statistics.getAllowedRequests() + permits);
            } else {
                statistics.setRejectedRequests(statistics.getRejectedRequests() + permits);
            }
            statistics.setLastRequestTime(System.currentTimeMillis());
        }
    }

    /**
     * 记录限流事件
     */
    private void recordRateLimitEvent(String key, String userId, String clientIp, String userAgent, 
                                    boolean allowed, Map<String, Object> context) {
        try {
            RateLimitEvent event = new RateLimitEvent();
            event.setEventType(allowed ? "RATE_LIMIT_ALLOWED" : "RATE_LIMIT_REJECTED");
            event.setKey(key);
            event.setUserId(userId);
            event.setClientIp(clientIp);
            event.setUserAgent(userAgent);
            event.setTimestamp(System.currentTimeMillis());
            event.setAllowed(allowed);
            event.setReason(allowed ? "请求通过" : "请求被限流");
            event.setContext(context != null ? context : new ConcurrentHashMap<>());

            String subject = "im.ratelimit.event";
            eventPublisher.publishToJetStream(subject, event);
            log.debug("限流事件发布: key={}, userId={}, allowed={}", key, userId, allowed);
        } catch (Exception e) {
            log.error("记录限流事件异常: key={}, userId={}", key, userId, e);
        }
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 清理过期的限流器
                long now = System.currentTimeMillis();
                rateLimiterCache.entrySet().removeIf(entry -> {
                    RateLimiter rateLimiter = entry.getValue();
                    RateLimitStatistics stats = rateLimiter.getStatistics();
                    // 如果超过1小时没有请求，则清理
                    return (now - stats.getLastRequestTime()) > TimeUnit.HOURS.toMillis(1);
                });
                
                log.debug("限流器清理完成: 剩余数量={}", rateLimiterCache.size());
            } catch (Exception e) {
                log.error("限流器清理任务执行异常", e);
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 获取限流统计信息
     */
    public String getRateLimitStatistics() {
        return String.format("RateLimitStatistics{totalLimiters=%d, types=%s}", 
                rateLimiterCache.size(), 
                rateLimiterCache.values().stream()
                        .map(RateLimiter::getLimiterType)
                        .distinct()
                        .reduce((a, b) -> a + "," + b)
                        .orElse("none"));
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info("限流管理器资源已释放");
        } catch (Exception e) {
            log.error("限流管理器资源释放异常", e);
        }
    }
} 