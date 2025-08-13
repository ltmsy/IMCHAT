package com.acme.im.communication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 消息幂等性处理服务
 * 防止重复消息，确保消息的唯一性
 * 
 * 幂等性策略：
 * 1. 基于客户端消息ID + 会话ID的唯一性约束
 * 2. Redis缓存快速检查 + MySQL持久化存储
 * 3. 支持消息重试和重复检测
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageIdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Redis键前缀
     */
    private static final String REDIS_KEY_PREFIX = "comm:idem:";
    
    /**
     * 幂等性记录缓存过期时间（小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * 检查消息是否已存在（幂等性检查）
     * 
     * @param conversationId 会话ID
     * @param clientMsgId 客户端消息ID
     * @return 如果消息已存在，返回服务端消息ID；否则返回null
     */
    public Long checkMessageExists(Long conversationId, String clientMsgId) {
        String redisKey = buildRedisKey(conversationId, clientMsgId);
        
        try {
            // 先从Redis检查
            Object value = redisTemplate.opsForValue().get(redisKey);
            if (value != null) {
                Long serverMsgId = Long.valueOf(value.toString());
                log.debug("Redis幂等性检查命中: conversationId={}, clientMsgId={}, serverMsgId={}", 
                         conversationId, clientMsgId, serverMsgId);
                return serverMsgId;
            }
            
            // Redis中没有，从数据库检查
            Long serverMsgId = checkMessageExistsInDB(conversationId, clientMsgId);
            if (serverMsgId != null) {
                // 将结果缓存到Redis
                redisTemplate.opsForValue().set(redisKey, serverMsgId, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                log.debug("数据库幂等性检查命中: conversationId={}, clientMsgId={}, serverMsgId={}", 
                         conversationId, clientMsgId, serverMsgId);
            }
            
            return serverMsgId;
            
        } catch (Exception e) {
            log.error("幂等性检查失败: conversationId={}, clientMsgId={}", conversationId, clientMsgId, e);
            // 发生异常时，从数据库检查
            return checkMessageExistsInDB(conversationId, clientMsgId);
        }
    }

    /**
     * 记录消息幂等性信息
     * 
     * @param conversationId 会话ID
     * @param clientMsgId 客户端消息ID
     * @param serverMsgId 服务端消息ID
     * @param senderId 发送者ID
     * @return 是否记录成功
     */
    public boolean recordMessageIdempotency(Long conversationId, String clientMsgId, 
                                          Long serverMsgId, Long senderId) {
        try {
            // 先记录到数据库
            boolean dbSuccess = recordMessageIdempotencyInDB(conversationId, clientMsgId, serverMsgId, senderId);
            
            if (dbSuccess) {
                // 数据库记录成功后，缓存到Redis
                String redisKey = buildRedisKey(conversationId, clientMsgId);
                redisTemplate.opsForValue().set(redisKey, serverMsgId, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                
                log.debug("记录消息幂等性成功: conversationId={}, clientMsgId={}, serverMsgId={}", 
                         conversationId, clientMsgId, serverMsgId);
            }
            
            return dbSuccess;
            
        } catch (DuplicateKeyException e) {
            log.warn("消息幂等性记录重复: conversationId={}, clientMsgId={}", conversationId, clientMsgId);
            return false;
        } catch (Exception e) {
            log.error("记录消息幂等性失败: conversationId={}, clientMsgId={}, serverMsgId={}", 
                     conversationId, clientMsgId, serverMsgId, e);
            return false;
        }
    }

    /**
     * 检查并记录消息幂等性（原子操作）
     * 如果消息不存在则记录，如果已存在则返回现有的服务端消息ID
     * 
     * @param conversationId 会话ID
     * @param clientMsgId 客户端消息ID
     * @param serverMsgId 服务端消息ID
     * @param senderId 发送者ID
     * @return 服务端消息ID，如果是新消息返回传入的serverMsgId，如果是重复消息返回现有的serverMsgId
     */
    public Long checkAndRecordIdempotency(Long conversationId, String clientMsgId, 
                                        Long serverMsgId, Long senderId) {
        // 先检查是否已存在
        Long existingMsgId = checkMessageExists(conversationId, clientMsgId);
        if (existingMsgId != null) {
            return existingMsgId;
        }
        
        // 不存在，尝试记录
        boolean recorded = recordMessageIdempotency(conversationId, clientMsgId, serverMsgId, senderId);
        if (recorded) {
            return serverMsgId;
        } else {
            // 记录失败，可能是并发插入导致的重复，再次检查
            existingMsgId = checkMessageExists(conversationId, clientMsgId);
            return existingMsgId != null ? existingMsgId : serverMsgId;
        }
    }

    /**
     * 删除幂等性记录（谨慎使用）
     * 
     * @param conversationId 会话ID
     * @param clientMsgId 客户端消息ID
     */
    public void removeIdempotencyRecord(Long conversationId, String clientMsgId) {
        try {
            // 删除Redis缓存
            String redisKey = buildRedisKey(conversationId, clientMsgId);
            redisTemplate.delete(redisKey);
            
            // 删除数据库记录
            String deleteSql = """
                DELETE FROM message_idempotency 
                WHERE conversation_id = ? AND client_msg_id = ?
                """;
            
            int deletedRows = jdbcTemplate.update(deleteSql, conversationId, clientMsgId);
            
            log.debug("删除幂等性记录: conversationId={}, clientMsgId={}, deletedRows={}", 
                     conversationId, clientMsgId, deletedRows);
            
        } catch (Exception e) {
            log.error("删除幂等性记录失败: conversationId={}, clientMsgId={}", conversationId, clientMsgId, e);
        }
    }

    /**
     * 批量清理过期的幂等性记录
     * 
     * @param beforeTime 清理此时间之前的记录
     * @return 清理的记录数量
     */
    public int cleanExpiredRecords(LocalDateTime beforeTime) {
        try {
            String deleteSql = """
                DELETE FROM message_idempotency 
                WHERE created_at < ?
                LIMIT 1000
                """;
            
            int deletedRows = jdbcTemplate.update(deleteSql, beforeTime);
            
            log.info("清理过期幂等性记录: beforeTime={}, deletedRows={}", beforeTime, deletedRows);
            return deletedRows;
            
        } catch (Exception e) {
            log.error("清理过期幂等性记录失败: beforeTime={}", beforeTime, e);
            return 0;
        }
    }

    /**
     * 获取会话的幂等性记录数量
     * 
     * @param conversationId 会话ID
     * @return 记录数量
     */
    public long countIdempotencyRecords(Long conversationId) {
        try {
            String countSql = """
                SELECT COUNT(*) FROM message_idempotency 
                WHERE conversation_id = ?
                """;
            
            Long count = jdbcTemplate.queryForObject(countSql, Long.class, conversationId);
            return count != null ? count : 0L;
            
        } catch (Exception e) {
            log.error("统计幂等性记录失败: conversationId={}", conversationId, e);
            return 0L;
        }
    }

    // ================================
    // 私有方法
    // ================================

    /**
     * 构建Redis键
     */
    private String buildRedisKey(Long conversationId, String clientMsgId) {
        return REDIS_KEY_PREFIX + conversationId + ":" + clientMsgId;
    }

    /**
     * 从数据库检查消息是否存在
     */
    private Long checkMessageExistsInDB(Long conversationId, String clientMsgId) {
        try {
            String sql = """
                SELECT server_msg_id FROM message_idempotency 
                WHERE conversation_id = ? AND client_msg_id = ?
                """;
            
            return jdbcTemplate.queryForObject(sql, Long.class, conversationId, clientMsgId);
            
        } catch (Exception e) {
            // 记录不存在或查询异常
            return null;
        }
    }

    /**
     * 在数据库中记录幂等性信息
     */
    private boolean recordMessageIdempotencyInDB(Long conversationId, String clientMsgId, 
                                                Long serverMsgId, Long senderId) {
        try {
            String insertSql = """
                INSERT INTO message_idempotency 
                (conversation_id, client_msg_id, server_msg_id, sender_id, created_at) 
                VALUES (?, ?, ?, ?, ?)
                """;
            
            int insertedRows = jdbcTemplate.update(insertSql, conversationId, clientMsgId, 
                                                  serverMsgId, senderId, LocalDateTime.now());
            
            return insertedRows > 0;
            
        } catch (DuplicateKeyException e) {
            // 唯一约束冲突，消息已存在
            throw e;
        } catch (Exception e) {
            log.error("数据库记录幂等性失败: conversationId={}, clientMsgId={}, serverMsgId={}", 
                     conversationId, clientMsgId, serverMsgId, e);
            return false;
        }
    }
} 