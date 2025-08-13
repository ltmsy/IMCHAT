package com.acme.im.common.lock.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redis分布式锁实现
 * 支持多种锁策略和重试机制
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class RedisDistributedLock {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 锁释放脚本（Lua脚本，保证原子性）
     */
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";

    /**
     * 锁续期脚本（Lua脚本，保证原子性）
     */
    private static final String RENEW_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('expire', KEYS[1], ARGV[2]) " +
        "else " +
        "    return 0 " +
        "end";

    /**
     * 尝试获取锁
     * 
     * @param lockKey 锁键
     * @param lockValue 锁值（通常是线程ID或请求ID）
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime, TimeUnit timeUnit) {
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime, timeUnit);
            boolean success = Boolean.TRUE.equals(result);
            
            if (success) {
                log.debug("获取锁成功: key={}, value={}, expireTime={} {}", 
                        lockKey, lockValue, expireTime, timeUnit);
            } else {
                log.debug("获取锁失败: key={}, value={}", lockKey, lockValue);
            }
            
            return success;
        } catch (Exception e) {
            log.error("获取锁异常: key={}, value={}", lockKey, lockValue, e);
            return false;
        }
    }

    /**
     * 尝试获取锁（带等待时间）
     * 
     * @param lockKey 锁键
     * @param lockValue 锁值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @param waitTime 等待时间
     * @param waitTimeUnit 等待时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime, TimeUnit timeUnit,
                          long waitTime, TimeUnit waitTimeUnit) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + waitTimeUnit.toMillis(waitTime);
        
        while (System.currentTimeMillis() < endTime) {
            if (tryLock(lockKey, lockValue, expireTime, timeUnit)) {
                return true;
            }
            
            try {
                Thread.sleep(100); // 等待100ms后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        log.debug("获取锁超时: key={}, value={}, waitTime={} {}", 
                lockKey, lockValue, waitTime, waitTimeUnit);
        return false;
    }

    /**
     * 释放锁
     * 
     * @param lockKey 锁键
     * @param lockValue 锁值
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String lockValue) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            
            Long result = redisTemplate.execute(script, 
                    Collections.singletonList(lockKey), lockValue);
            
            boolean success = Long.valueOf(1).equals(result);
            
            if (success) {
                log.debug("释放锁成功: key={}, value={}", lockKey, lockValue);
            } else {
                log.debug("释放锁失败: key={}, value={}", lockKey, lockValue);
            }
            
            return success;
        } catch (Exception e) {
            log.error("释放锁异常: key={}, value={}", lockKey, lockValue, e);
            return false;
        }
    }

    /**
     * 续期锁
     * 
     * @param lockKey 锁键
     * @param lockValue 锁值
     * @param expireTime 新的过期时间
     * @param timeUnit 时间单位
     * @return 是否续期成功
     */
    public boolean renewLock(String lockKey, String lockValue, long expireTime, TimeUnit timeUnit) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(RENEW_SCRIPT);
            script.setResultType(Long.class);
            
            Long result = redisTemplate.execute(script, 
                    Collections.singletonList(lockKey), lockValue, String.valueOf(expireTime));
            
            boolean success = Long.valueOf(1).equals(result);
            
            if (success) {
                log.debug("续期锁成功: key={}, value={}, expireTime={} {}", 
                        lockKey, lockValue, expireTime, timeUnit);
            } else {
                log.debug("续期锁失败: key={}, value={}", lockKey, lockValue);
            }
            
            return success;
        } catch (Exception e) {
            log.error("续期锁异常: key={}, value={}", lockKey, lockValue, e);
            return false;
        }
    }

    /**
     * 检查锁是否存在
     * 
     * @param lockKey 锁键
     * @return 是否存在
     */
    public boolean isLocked(String lockKey) {
        try {
            Boolean result = redisTemplate.hasKey(lockKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查锁状态异常: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 获取锁的剩余过期时间
     * 
     * @param lockKey 锁键
     * @return 剩余过期时间（毫秒），-1表示永不过期，-2表示键不存在
     */
    public long getLockExpireTime(String lockKey) {
        try {
            Long expireTime = redisTemplate.getExpire(lockKey, TimeUnit.MILLISECONDS);
            return expireTime != null ? expireTime : -2;
        } catch (Exception e) {
            log.error("获取锁过期时间异常: key={}", lockKey, e);
            return -2;
        }
    }

    /**
     * 获取锁的值
     * 
     * @param lockKey 锁键
     * @return 锁值
     */
    public String getLockValue(String lockKey) {
        try {
            Object value = redisTemplate.opsForValue().get(lockKey);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("获取锁值异常: key={}", lockKey, e);
            return null;
        }
    }

    /**
     * 强制删除锁（不检查锁值）
     * 
     * @param lockKey 锁键
     * @return 是否删除成功
     */
    public boolean forceUnlock(String lockKey) {
        try {
            Boolean result = redisTemplate.delete(lockKey);
            boolean success = Boolean.TRUE.equals(result);
            
            if (success) {
                log.warn("强制删除锁成功: key={}", lockKey);
            } else {
                log.debug("强制删除锁失败: key={}", lockKey);
            }
            
            return success;
        } catch (Exception e) {
            log.error("强制删除锁异常: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 批量删除锁
     * 
     * @param lockKeys 锁键列表
     * @return 删除成功的数量
     */
    public long batchUnlock(java.util.Collection<String> lockKeys) {
        try {
            Long result = redisTemplate.delete(lockKeys);
            long count = result != null ? result : 0;
            
            log.debug("批量删除锁完成: keys={}, deleted={}", lockKeys, count);
            return count;
        } catch (Exception e) {
            log.error("批量删除锁异常: keys={}", lockKeys, e);
            return 0;
        }
    }

    /**
     * 清理过期的锁
     * 
     * @param pattern 锁键模式
     * @return 清理的数量
     */
    public long cleanExpiredLocks(String pattern) {
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            
            long count = 0;
            for (String key : keys) {
                if (getLockExpireTime(key) == -2) { // 键不存在
                    continue;
                }
                
                if (getLockExpireTime(key) == -1) { // 永不过期
                    continue;
                }
                
                if (getLockExpireTime(key) <= 0) { // 已过期
                    if (forceUnlock(key)) {
                        count++;
                    }
                }
            }
            
            log.info("清理过期锁完成: pattern={}, cleaned={}", pattern, count);
            return count;
        } catch (Exception e) {
            log.error("清理过期锁异常: pattern={}", pattern, e);
            return 0;
        }
    }

    /**
     * 获取锁统计信息
     * 
     * @param pattern 锁键模式
     * @return 统计信息
     */
    public LockStatistics getLockStatistics(String pattern) {
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return new LockStatistics(0, 0, 0, 0);
            }
            
            int totalLocks = keys.size();
            int activeLocks = 0;
            int expiredLocks = 0;
            int permanentLocks = 0;
            
            for (String key : keys) {
                long expireTime = getLockExpireTime(key);
                if (expireTime == -2) { // 键不存在
                    continue;
                } else if (expireTime == -1) { // 永不过期
                    permanentLocks++;
                } else if (expireTime <= 0) { // 已过期
                    expiredLocks++;
                } else { // 活跃锁
                    activeLocks++;
                }
            }
            
            return new LockStatistics(totalLocks, activeLocks, expiredLocks, permanentLocks);
        } catch (Exception e) {
            log.error("获取锁统计信息异常: pattern={}", pattern, e);
            return new LockStatistics(0, 0, 0, 0);
        }
    }

    /**
     * 锁统计信息
     */
    public static class LockStatistics {
        private final int totalLocks;
        private final int activeLocks;
        private final int expiredLocks;
        private final int permanentLocks;

        public LockStatistics(int totalLocks, int activeLocks, int expiredLocks, int permanentLocks) {
            this.totalLocks = totalLocks;
            this.activeLocks = activeLocks;
            this.expiredLocks = expiredLocks;
            this.permanentLocks = permanentLocks;
        }

        public int getTotalLocks() { return totalLocks; }
        public int getActiveLocks() { return activeLocks; }
        public int getExpiredLocks() { return expiredLocks; }
        public int getPermanentLocks() { return permanentLocks; }

        @Override
        public String toString() {
            return String.format("LockStatistics{total=%d, active=%d, expired=%d, permanent=%d}", 
                    totalLocks, activeLocks, expiredLocks, permanentLocks);
        }
    }
} 