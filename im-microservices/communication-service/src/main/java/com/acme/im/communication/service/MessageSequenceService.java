package com.acme.im.communication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 消息序列管理服务
 * 负责为每个会话生成递增的消息序列号
 * 
 * 序列号特点：
 * 1. 会话内全局递增
 * 2. 从1开始编号
 * 3. 支持高并发生成
 * 4. Redis缓存 + MySQL持久化
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSequenceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Redis键前缀
     */
    private static final String REDIS_KEY_PREFIX = "comm:seq:";
    
    /**
     * 序列号缓存过期时间（小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 24;
    
    /**
     * 序列号增长步长
     */
    private static final long INCREMENT_STEP = 1L;

    /**
     * 获取下一个消息序列号
     * 
     * @param conversationId 会话ID
     * @return 消息序列号
     */
    public Long getNextSequence(Long conversationId) {
        String redisKey = REDIS_KEY_PREFIX + conversationId;
        
        try {
            // 先尝试从Redis获取并递增
            Long nextSeq = redisTemplate.opsForValue().increment(redisKey, INCREMENT_STEP);
            
            if (nextSeq == 1L) {
                // 如果是第一次获取，需要从数据库同步当前序列号
                Long dbSeq = getCurrentSequenceFromDB(conversationId);
                if (dbSeq > 0) {
                    // 数据库中已有序列号，设置Redis为数据库值+1
                    nextSeq = dbSeq + INCREMENT_STEP;
                    redisTemplate.opsForValue().set(redisKey, nextSeq);
                }
            }
            
            // 设置过期时间
            redisTemplate.expire(redisKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 异步更新数据库序列号
            updateSequenceInDB(conversationId, nextSeq);
            
            log.debug("生成消息序列号: conversationId={}, seq={}", conversationId, nextSeq);
            return nextSeq;
            
        } catch (Exception e) {
            log.error("生成消息序列号失败: conversationId={}", conversationId, e);
            // 降级到数据库方式
            return getNextSequenceFromDB(conversationId);
        }
    }

    /**
     * 获取当前会话的最新序列号
     * 
     * @param conversationId 会话ID
     * @return 当前序列号，如果没有消息则返回0
     */
    public Long getCurrentSequence(Long conversationId) {
        String redisKey = REDIS_KEY_PREFIX + conversationId;
        
        try {
            // 先从Redis获取
            Object value = redisTemplate.opsForValue().get(redisKey);
            if (value != null) {
                Long seq = Long.valueOf(value.toString());
                // Redis中存储的是下一个序列号，当前序列号需要减1
                return seq > 0 ? seq - 1 : 0;
            }
            
            // Redis中没有，从数据库获取
            return getCurrentSequenceFromDB(conversationId);
            
        } catch (Exception e) {
            log.error("获取当前序列号失败: conversationId={}", conversationId, e);
            return getCurrentSequenceFromDB(conversationId);
        }
    }

    /**
     * 重置会话序列号（谨慎使用）
     * 
     * @param conversationId 会话ID
     * @param sequence 重置的序列号
     */
    public void resetSequence(Long conversationId, Long sequence) {
        String redisKey = REDIS_KEY_PREFIX + conversationId;
        
        try {
            // 更新Redis
            redisTemplate.opsForValue().set(redisKey, sequence + 1);
            redisTemplate.expire(redisKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 更新数据库
            updateSequenceInDB(conversationId, sequence);
            
            log.info("重置会话序列号: conversationId={}, sequence={}", conversationId, sequence);
            
        } catch (Exception e) {
            log.error("重置会话序列号失败: conversationId={}, sequence={}", conversationId, sequence, e);
            throw new RuntimeException("重置序列号失败", e);
        }
    }

    /**
     * 预热序列号缓存
     * 将活跃会话的序列号加载到Redis中
     * 
     * @param conversationIds 会话ID列表
     */
    public void preloadSequences(Long... conversationIds) {
        if (conversationIds == null || conversationIds.length == 0) {
            return;
        }
        
        for (Long conversationId : conversationIds) {
            try {
                String redisKey = REDIS_KEY_PREFIX + conversationId;
                
                // 检查Redis中是否已存在
                if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
                    // 从数据库获取当前序列号
                    Long currentSeq = getCurrentSequenceFromDB(conversationId);
                    
                    // 设置到Redis（下一个序列号）
                    redisTemplate.opsForValue().set(redisKey, currentSeq + 1);
                    redisTemplate.expire(redisKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                    
                    log.debug("预热序列号缓存: conversationId={}, currentSeq={}", conversationId, currentSeq);
                }
            } catch (Exception e) {
                log.error("预热序列号缓存失败: conversationId={}", conversationId, e);
            }
        }
    }

    /**
     * 清理序列号缓存
     * 
     * @param conversationId 会话ID
     */
    public void clearSequenceCache(Long conversationId) {
        String redisKey = REDIS_KEY_PREFIX + conversationId;
        try {
            redisTemplate.delete(redisKey);
            log.debug("清理序列号缓存: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("清理序列号缓存失败: conversationId={}", conversationId, e);
        }
    }

    // ================================
    // 私有方法 - 数据库操作
    // ================================

    /**
     * 从数据库获取当前序列号
     */
    private Long getCurrentSequenceFromDB(Long conversationId) {
        try {
            String sql = """
                SELECT current_seq FROM conversation_sequences 
                WHERE conversation_id = ?
                """;
            
            Long seq = jdbcTemplate.queryForObject(sql, Long.class, conversationId);
            return seq != null ? seq : 0L;
            
        } catch (Exception e) {
            log.debug("会话序列号记录不存在，返回0: conversationId={}", conversationId);
            return 0L;
        }
    }

    /**
     * 从数据库获取下一个序列号（降级方案）
     */
    private Long getNextSequenceFromDB(Long conversationId) {
        try {
            // 使用MySQL的原子操作获取下一个序列号
            String upsertSql = """
                INSERT INTO conversation_sequences (conversation_id, current_seq, last_message_at, updated_at) 
                VALUES (?, 1, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                    current_seq = current_seq + 1,
                    last_message_at = VALUES(last_message_at),
                    updated_at = VALUES(updated_at)
                """;
            
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(upsertSql, conversationId, now, now);
            
            // 获取更新后的序列号
            return getCurrentSequenceFromDB(conversationId);
            
        } catch (Exception e) {
            log.error("从数据库获取序列号失败: conversationId={}", conversationId, e);
            throw new RuntimeException("获取消息序列号失败", e);
        }
    }

    /**
     * 异步更新数据库序列号
     */
    private void updateSequenceInDB(Long conversationId, Long sequence) {
        try {
            String upsertSql = """
                INSERT INTO conversation_sequences (conversation_id, current_seq, last_message_at, updated_at) 
                VALUES (?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                    current_seq = GREATEST(current_seq, VALUES(current_seq)),
                    last_message_at = VALUES(last_message_at),
                    updated_at = VALUES(updated_at)
                """;
            
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(upsertSql, conversationId, sequence, now, now);
            
        } catch (Exception e) {
            log.error("更新数据库序列号失败: conversationId={}, sequence={}", conversationId, sequence, e);
            // 不抛出异常，避免影响主流程
        }
    }
} 