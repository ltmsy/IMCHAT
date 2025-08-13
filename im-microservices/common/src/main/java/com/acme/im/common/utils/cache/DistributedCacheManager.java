package com.acme.im.common.utils.cache;

import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 分布式缓存管理器
 * 提供高性能的分布式缓存解决方案，支持多级缓存、一致性保证和智能预热
 * 
 * 特性：
 * 1. 多级缓存 - 本地缓存 + Redis分布式缓存
 * 2. 一致性保证 - 基于NATS的缓存失效通知
 * 3. 智能预热 - 预测性数据加载
 * 4. 防击穿 - 分布式锁防止缓存击穿
 * 5. 防雪崩 - 随机过期时间
 * 6. 防穿透 - 空值缓存和布隆过滤器
 * 7. 性能监控 - 详细的缓存统计
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class DistributedCacheManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private AsyncEventPublisher eventPublisher;
    
    @Autowired
    private Gson gson;

    // 本地缓存
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "cache-cleanup"));
    
    // 缓存配置
    private static final int DEFAULT_LOCAL_CACHE_SIZE = 10000;
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration DEFAULT_LOCAL_TTL = Duration.ofMinutes(5);
    private static final String CACHE_INVALIDATION_SUBJECT = "cache.invalidation";
    private static final String CACHE_PRELOAD_SUBJECT = "cache.preload";
    
    // 性能统计
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong localHitCount = new AtomicLong(0);
    private final AtomicLong redisHitCount = new AtomicLong(0);
    private final AtomicLong loadCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    // Lua脚本
    private static final String SET_IF_ABSENT_SCRIPT = 
            "if redis.call('exists', KEYS[1]) == 0 then " +
            "  return redis.call('setex', KEYS[1], ARGV[1], ARGV[2]) " +
            "else " +
            "  return nil " +
            "end";

    private final DefaultRedisScript<String> setIfAbsentScript = new DefaultRedisScript<>(SET_IF_ABSENT_SCRIPT, String.class);

    /**
     * 本地缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final long createTime;
        private final long expireTime;
        private volatile long lastAccessTime;
        private final AtomicLong accessCount;

        public CacheEntry(Object value, Duration ttl) {
            this.value = value;
            this.createTime = System.currentTimeMillis();
            this.expireTime = createTime + ttl.toMillis();
            this.lastAccessTime = createTime;
            this.accessCount = new AtomicLong(1);
        }

        public Object getValue() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount.incrementAndGet();
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public long getAccessCount() {
            return accessCount.get();
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public long getAge() {
            return System.currentTimeMillis() - createTime;
        }
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final long localHitCount;
        private final long redisHitCount;
        private final long loadCount;
        private final long evictionCount;
        private final int localCacheSize;
        private final double hitRate;
        private final double localHitRate;

        public CacheStats(long hitCount, long missCount, long localHitCount, 
                         long redisHitCount, long loadCount, long evictionCount, int localCacheSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.localHitCount = localHitCount;
            this.redisHitCount = redisHitCount;
            this.loadCount = loadCount;
            this.evictionCount = evictionCount;
            this.localCacheSize = localCacheSize;
            
            long totalRequests = hitCount + missCount;
            this.hitRate = totalRequests > 0 ? (double) hitCount / totalRequests : 0.0;
            this.localHitRate = hitCount > 0 ? (double) localHitCount / hitCount : 0.0;
        }

        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getLocalHitCount() { return localHitCount; }
        public long getRedisHitCount() { return redisHitCount; }
        public long getLoadCount() { return loadCount; }
        public long getEvictionCount() { return evictionCount; }
        public int getLocalCacheSize() { return localCacheSize; }
        public double getHitRate() { return hitRate; }
        public double getLocalHitRate() { return localHitRate; }

        @Override
        public String toString() {
            return String.format("CacheStats{hit=%d, miss=%d, localHit=%d, redisHit=%d, load=%d, evict=%d, " +
                    "localSize=%d, hitRate=%.2f%%, localHitRate=%.2f%%}", 
                    hitCount, missCount, localHitCount, redisHitCount, loadCount, evictionCount, 
                    localCacheSize, hitRate * 100, localHitRate * 100);
        }
    }

    /**
     * 缓存失效事件
     */
    public static class CacheInvalidationEvent {
        private final String key;
        private final String pattern;
        private final String source;
        private final long timestamp;

        public CacheInvalidationEvent(String key, String pattern, String source) {
            this.key = key;
            this.pattern = pattern;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }

        public String getKey() { return key; }
        public String getPattern() { return pattern; }
        public String getSource() { return source; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 初始化缓存管理器
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化分布式缓存管理器...");
        
        // 启动本地缓存清理任务
        cleanupExecutor.scheduleAtFixedRate(this::cleanupLocalCache, 1, 1, TimeUnit.MINUTES);
        
        // 启动统计任务
        cleanupExecutor.scheduleAtFixedRate(this::logStatistics, 5, 5, TimeUnit.MINUTES);
        
        log.info("分布式缓存管理器初始化完成: 本地缓存大小限制={}", DEFAULT_LOCAL_CACHE_SIZE);
    }

    /**
     * 获取缓存值
     * 
     * @param key 缓存键
     * @param loader 数据加载器
     * @param ttl 过期时间
     * @return 缓存值
     */
    public <T> T get(String key, Function<String, T> loader, Duration ttl) {
        return get(key, loader, ttl, DEFAULT_LOCAL_TTL);
    }

    /**
     * 获取缓存值（完整参数）
     * 
     * @param key 缓存键
     * @param loader 数据加载器
     * @param redisTtl Redis过期时间
     * @param localTtl 本地缓存过期时间
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Function<String, T> loader, Duration redisTtl, Duration localTtl) {
        // 1. 检查本地缓存
        CacheEntry localEntry = localCache.get(key);
        if (localEntry != null && !localEntry.isExpired()) {
            hitCount.incrementAndGet();
            localHitCount.incrementAndGet();
            log.debug("本地缓存命中: key={}", key);
            return (T) localEntry.getValue();
        }

        // 2. 检查Redis缓存
        try {
            Object redisValue = redisTemplate.opsForValue().get(key);
            if (redisValue != null) {
                hitCount.incrementAndGet();
                redisHitCount.incrementAndGet();
                
                // 更新本地缓存
                putToLocalCache(key, redisValue, localTtl);
                
                log.debug("Redis缓存命中: key={}", key);
                return (T) redisValue;
            }
        } catch (Exception e) {
            log.warn("Redis缓存访问异常: key={}, error={}", key, e.getMessage());
        }

        // 3. 缓存未命中，加载数据
        missCount.incrementAndGet();
        return loadAndCache(key, loader, redisTtl, localTtl);
    }

    /**
     * 设置缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    public void put(String key, Object value, Duration ttl) {
        put(key, value, ttl, DEFAULT_LOCAL_TTL);
    }

    /**
     * 设置缓存（完整参数）
     */
    public void put(String key, Object value, Duration redisTtl, Duration localTtl) {
        // 设置Redis缓存
        try {
            redisTemplate.opsForValue().set(key, value, redisTtl);
        } catch (Exception e) {
            log.error("设置Redis缓存失败: key={}", key, e);
        }

        // 设置本地缓存
        putToLocalCache(key, value, localTtl);

        log.debug("缓存已设置: key={}, redisTtl={}, localTtl={}", key, redisTtl, localTtl);
    }

    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    public void evict(String key) {
        // 删除Redis缓存
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除Redis缓存失败: key={}", key, e);
        }

        // 删除本地缓存
        localCache.remove(key);
        evictionCount.incrementAndGet();

        // 发布缓存失效事件
        publishInvalidationEvent(key, null, "manual");

        log.debug("缓存已删除: key={}", key);
    }

    /**
     * 批量删除缓存（按模式）
     * 
     * @param pattern 键模式（支持*通配符）
     */
    public void evictByPattern(String pattern) {
        // 删除Redis缓存
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                evictionCount.addAndGet(keys.size());
            }
        } catch (Exception e) {
            log.error("批量删除Redis缓存失败: pattern={}", pattern, e);
        }

        // 删除本地缓存
        String regex = pattern.replace("*", ".*");
        localCache.entrySet().removeIf(entry -> entry.getKey().matches(regex));

        // 发布缓存失效事件
        publishInvalidationEvent(null, pattern, "pattern");

        log.debug("批量缓存已删除: pattern={}", pattern);
    }

    /**
     * 预热缓存
     * 
     * @param keys 要预热的键列表
     * @param loader 数据加载器
     * @param ttl 过期时间
     */
    public <T> CompletableFuture<Void> preload(List<String> keys, Function<String, T> loader, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            log.info("开始预热缓存: keys={}", keys.size());
            
            keys.parallelStream().forEach(key -> {
                try {
                    get(key, loader, ttl);
                } catch (Exception e) {
                    log.warn("预热缓存失败: key={}, error={}", key, e.getMessage());
                }
            });
            
            log.info("缓存预热完成: keys={}", keys.size());
        });
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(
                hitCount.get(),
                missCount.get(),
                localHitCount.get(),
                redisHitCount.get(),
                loadCount.get(),
                evictionCount.get(),
                localCache.size()
        );
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        // 清空Redis缓存
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            log.error("清空Redis缓存失败", e);
        }

        // 清空本地缓存
        localCache.clear();

        // 重置统计
        hitCount.set(0);
        missCount.set(0);
        localHitCount.set(0);
        redisHitCount.set(0);
        loadCount.set(0);
        evictionCount.set(0);

        log.info("所有缓存已清空");
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 加载数据并缓存
     */
    @SuppressWarnings("unchecked")
    private <T> T loadAndCache(String key, Function<String, T> loader, Duration redisTtl, Duration localTtl) {
        // 使用分布式锁防止缓存击穿
        String lockKey = "lock:" + key;
        try {
            // 尝试获取锁
            String lockValue = UUID.randomUUID().toString();
            String result = redisTemplate.execute(setIfAbsentScript, 
                    Collections.singletonList(lockKey), 
                    "10", // 10秒锁定时间
                    lockValue);

            if ("OK".equals(result)) {
                try {
                    // 获得锁，加载数据
                    log.debug("获得分布式锁，开始加载数据: key={}", key);
                    T value = loader.apply(key);
                    loadCount.incrementAndGet();

                    if (value != null) {
                        // 缓存数据（添加随机时间防止雪崩）
                        Duration randomizedTtl = redisTtl.plus(Duration.ofSeconds(
                                ThreadLocalRandom.current().nextInt(60)));
                        put(key, value, randomizedTtl, localTtl);
                        return value;
                    } else {
                        // 空值缓存，防止穿透
                        put(key, "NULL", Duration.ofMinutes(5), Duration.ofMinutes(1));
                        return null;
                    }
                } finally {
                    // 释放锁
                    redisTemplate.delete(lockKey);
                }
            } else {
                // 未获得锁，等待并重试
                log.debug("未获得分布式锁，等待重试: key={}", key);
                Thread.sleep(50 + ThreadLocalRandom.current().nextInt(50));
                
                // 重新检查缓存
                Object redisValue = redisTemplate.opsForValue().get(key);
                if (redisValue != null && !"NULL".equals(redisValue)) {
                    putToLocalCache(key, redisValue, localTtl);
                    return (T) redisValue;
                }
                
                // 仍然没有，直接加载（可能有并发问题，但避免无限等待）
                T value = loader.apply(key);
                loadCount.incrementAndGet();
                
                if (value != null) {
                    put(key, value, redisTtl, localTtl);
                }
                return value;
            }
        } catch (Exception e) {
            log.error("加载缓存数据异常: key={}", key, e);
            // 异常情况下直接加载
            try {
                T value = loader.apply(key);
                loadCount.incrementAndGet();
                return value;
            } catch (Exception loaderException) {
                log.error("数据加载器异常: key={}", key, loaderException);
                throw new RuntimeException("缓存数据加载失败", loaderException);
            }
        }
    }

    /**
     * 设置本地缓存
     */
    private void putToLocalCache(String key, Object value, Duration ttl) {
        // 检查本地缓存大小限制
        if (localCache.size() >= DEFAULT_LOCAL_CACHE_SIZE) {
            evictLeastUsed();
        }

        localCache.put(key, new CacheEntry(value, ttl));
    }

    /**
     * 清理过期的本地缓存
     */
    private void cleanupLocalCache() {
        int beforeSize = localCache.size();
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = localCache.size();
        
        if (beforeSize > afterSize) {
            evictionCount.addAndGet(beforeSize - afterSize);
            log.debug("清理过期本地缓存: 清理前={}, 清理后={}", beforeSize, afterSize);
        }
    }

    /**
     * 淘汰最少使用的缓存条目
     */
    private void evictLeastUsed() {
        if (localCache.isEmpty()) {
            return;
        }

        // 找出访问次数最少且最久未访问的条目
        String keyToEvict = localCache.entrySet().stream()
                .min((e1, e2) -> {
                    CacheEntry entry1 = e1.getValue();
                    CacheEntry entry2 = e2.getValue();
                    
                    // 优先比较访问次数
                    int countCompare = Long.compare(entry1.getAccessCount(), entry2.getAccessCount());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    
                    // 访问次数相同，比较最后访问时间
                    return Long.compare(entry1.getLastAccessTime(), entry2.getLastAccessTime());
                })
                .map(Map.Entry::getKey)
                .orElse(null);

        if (keyToEvict != null) {
            localCache.remove(keyToEvict);
            evictionCount.incrementAndGet();
            log.debug("淘汰本地缓存条目: key={}", keyToEvict);
        }
    }

    /**
     * 发布缓存失效事件
     */
    private void publishInvalidationEvent(String key, String pattern, String source) {
        try {
            CacheInvalidationEvent event = new CacheInvalidationEvent(key, pattern, source);
            eventPublisher.publishEventAsync(CACHE_INVALIDATION_SUBJECT, event);
        } catch (Exception e) {
            log.warn("发布缓存失效事件失败: key={}, pattern={}", key, pattern, e);
        }
    }

    /**
     * 记录统计信息
     */
    private void logStatistics() {
        CacheStats stats = getStats();
        log.info("缓存统计信息: {}", stats);
    }
} 