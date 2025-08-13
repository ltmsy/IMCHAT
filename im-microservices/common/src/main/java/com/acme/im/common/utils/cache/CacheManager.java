package com.acme.im.common.utils.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存管理组件
 * 提供统一的缓存操作接口，支持Redis缓存
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class CacheManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("设置缓存成功: key={}, value={}", key, value);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, error={}", key, e.getMessage(), e);
        }
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("设置缓存成功: key={}, value={}, timeout={} {}", key, value, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, error={}", key, e.getMessage(), e);
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("获取缓存: key={}, value={}", key, value);
            return value;
        } catch (Exception e) {
            log.error("获取缓存失败: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取缓存并转换为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value != null && clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("删除缓存: key={}, result={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("删除缓存失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            log.debug("检查缓存存在: key={}, result={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查缓存存在失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            log.debug("设置缓存过期时间: key={}, timeout={} {}, result={}", key, timeout, unit, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置缓存过期时间失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取缓存过期时间
     */
    public long getExpire(String key, TimeUnit unit) {
        try {
            Long expire = redisTemplate.getExpire(key, unit);
            log.debug("获取缓存过期时间: key={}, expire={} {}", key, expire, unit);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("获取缓存过期时间失败: key={}, error={}", key, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 批量删除缓存
     */
    public long deleteBatch(String... keys) {
        try {
            Long count = redisTemplate.delete(java.util.Arrays.asList(keys));
            log.debug("批量删除缓存: keys={}, count={}", java.util.Arrays.toString(keys), count);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("批量删除缓存失败: keys={}, error={}", java.util.Arrays.toString(keys), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("清空所有缓存成功");
        } catch (Exception e) {
            log.error("清空所有缓存失败: error={}", e.getMessage(), e);
        }
    }
} 