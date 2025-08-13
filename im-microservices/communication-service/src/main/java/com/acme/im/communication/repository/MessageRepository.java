package com.acme.im.communication.repository;

import com.acme.im.communication.config.MessageShardingStrategy;
import com.acme.im.communication.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 消息Repository
 * 支持分表的消息CRUD操作
 * 
 * 分表策略：按会话ID取模分表
 * 表名：messages_00 ~ messages_31
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final MessageShardingStrategy shardingStrategy;
    
    /**
     * 保存消息
     * 
     * @param message 消息对象
     * @return 保存后的消息（包含生成的ID）
     */
    public Message save(Message message) {
        String tableName = shardingStrategy.getTableName(message.getConversationId());
        
        String sql = String.format("""
            INSERT INTO %s (
                conversation_id, seq, client_msg_id, sender_id, msg_type, 
                content, content_extra, reply_to_id, forward_from_id, mentions,
                is_pinned, is_edited, edit_count, last_edit_at, is_recalled,
                recall_reason, recalled_at, status, server_timestamp, created_at, updated_at
            ) VALUES (
                :conversationId, :seq, :clientMsgId, :senderId, :msgType,
                :content, :contentExtra, :replyToId, :forwardFromId, :mentions,
                :isPinned, :isEdited, :editCount, :lastEditAt, :isRecalled,
                :recallReason, :recalledAt, :status, :serverTimestamp, :createdAt, :updatedAt
            )
            """, tableName);

        // 设置默认值
        if (message.getServerTimestamp() == null) {
            message.setServerTimestamp(LocalDateTime.now());
        }
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        if (message.getUpdatedAt() == null) {
            message.setUpdatedAt(LocalDateTime.now());
        }
        if (message.getStatus() == null) {
            message.setStatus(1); // 默认正常状态
        }
        if (message.getIsPinned() == null) {
            message.setIsPinned(0);
        }
        if (message.getIsEdited() == null) {
            message.setIsEdited(0);
        }
        if (message.getIsRecalled() == null) {
            message.setIsRecalled(0);
        }
        if (message.getEditCount() == null) {
            message.setEditCount(0);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, new BeanPropertySqlParameterSource(message), keyHolder);
        
        // 设置生成的ID
        Number generatedKey = keyHolder.getKey();
        if (generatedKey != null) {
            message.setId(generatedKey.longValue());
        }
        
        log.debug("保存消息成功: table={}, messageId={}, conversationId={}", 
                 tableName, message.getId(), message.getConversationId());
        
        return message;
    }
    
    /**
     * 根据ID查找消息
     * 
     * @param conversationId 会话ID（用于确定分表）
     * @param messageId 消息ID
     * @return 消息对象
     */
    public Optional<Message> findById(Long conversationId, Long messageId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            SELECT * FROM %s 
            WHERE id = ? AND conversation_id = ?
            """, tableName);
        
        try {
            Message message = jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(Message.class), messageId, conversationId);
            return Optional.ofNullable(message);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 查询会话的最新消息
     * 
     * @param conversationId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findLatestByConversationId(Long conversationId, int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            SELECT * FROM %s 
            WHERE conversation_id = ? AND status = 1
            ORDER BY seq DESC 
            LIMIT ?
            """, tableName);
        
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Message.class), 
                                conversationId, limit);
    }
    
    /**
     * 分页查询会话消息历史
     * 
     * @param conversationId 会话ID
     * @param beforeSeq 在此序号之前的消息（用于分页）
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findHistoryByConversationId(Long conversationId, Long beforeSeq, int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql;
        Object[] params;
        
        if (beforeSeq != null) {
            sql = String.format("""
                SELECT * FROM %s 
                WHERE conversation_id = ? AND seq < ? AND status = 1
                ORDER BY seq DESC 
                LIMIT ?
                """, tableName);
            params = new Object[]{conversationId, beforeSeq, limit};
        } else {
            sql = String.format("""
                SELECT * FROM %s 
                WHERE conversation_id = ? AND status = 1
                ORDER BY seq DESC 
                LIMIT ?
                """, tableName);
            params = new Object[]{conversationId, limit};
        }
        
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Message.class), params);
    }
    
    /**
     * 根据客户端消息ID查找消息（幂等性检查）
     * 
     * @param conversationId 会话ID
     * @param clientMsgId 客户端消息ID
     * @return 消息对象
     */
    public Optional<Message> findByClientMsgId(Long conversationId, String clientMsgId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            SELECT * FROM %s 
            WHERE conversation_id = ? AND client_msg_id = ?
            """, tableName);
        
        try {
            Message message = jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(Message.class), conversationId, clientMsgId);
            return Optional.ofNullable(message);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 更新消息
     * 
     * @param message 消息对象
     * @return 是否更新成功
     */
    public boolean update(Message message) {
        String tableName = shardingStrategy.getTableName(message.getConversationId());
        
        message.setUpdatedAt(LocalDateTime.now());
        
        String sql = String.format("""
            UPDATE %s SET 
                content = :content,
                content_extra = :contentExtra,
                is_pinned = :isPinned,
                is_edited = :isEdited,
                edit_count = :editCount,
                last_edit_at = :lastEditAt,
                is_recalled = :isRecalled,
                recall_reason = :recallReason,
                recalled_at = :recalledAt,
                status = :status,
                updated_at = :updatedAt
            WHERE id = :id AND conversation_id = :conversationId
            """, tableName);
        
        int updatedRows = namedParameterJdbcTemplate.update(sql, new BeanPropertySqlParameterSource(message));
        
        log.debug("更新消息: table={}, messageId={}, updatedRows={}", 
                 tableName, message.getId(), updatedRows);
        
        return updatedRows > 0;
    }
    
    /**
     * 撤回消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param reason 撤回原因
     * @return 是否撤回成功
     */
    public boolean recallMessage(Long conversationId, Long messageId, String reason) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            UPDATE %s SET 
                is_recalled = 1,
                recall_reason = ?,
                recalled_at = ?,
                updated_at = ?
            WHERE id = ? AND conversation_id = ? AND is_recalled = 0
            """, tableName);
        
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(sql, reason, now, now, messageId, conversationId);
        
        log.debug("撤回消息: table={}, messageId={}, updatedRows={}", 
                 tableName, messageId, updatedRows);
        
        return updatedRows > 0;
    }
    
    /**
     * 编辑消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param newContent 新内容
     * @return 是否编辑成功
     */
    public boolean editMessage(Long conversationId, Long messageId, String newContent) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            UPDATE %s SET 
                content = ?,
                is_edited = 1,
                edit_count = edit_count + 1,
                last_edit_at = ?,
                updated_at = ?
            WHERE id = ? AND conversation_id = ? AND is_recalled = 0 AND status = 1
            """, tableName);
        
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(sql, newContent, now, now, messageId, conversationId);
        
        log.debug("编辑消息: table={}, messageId={}, updatedRows={}", 
                 tableName, messageId, updatedRows);
        
        return updatedRows > 0;
    }
    
    /**
     * 置顶/取消置顶消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param pinned 是否置顶
     * @return 是否操作成功
     */
    public boolean pinMessage(Long conversationId, Long messageId, boolean pinned) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            UPDATE %s SET 
                is_pinned = ?,
                updated_at = ?
            WHERE id = ? AND conversation_id = ?
            """, tableName);
        
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(sql, pinned ? 1 : 0, now, messageId, conversationId);
        
        log.debug("{}消息: table={}, messageId={}, updatedRows={}", 
                 pinned ? "置顶" : "取消置顶", tableName, messageId, updatedRows);
        
        return updatedRows > 0;
    }
    
    /**
     * 删除消息（软删除）
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 是否删除成功
     */
    public boolean deleteMessage(Long conversationId, Long messageId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            UPDATE %s SET 
                status = 0,
                updated_at = ?
            WHERE id = ? AND conversation_id = ?
            """, tableName);
        
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(sql, now, messageId, conversationId);
        
        log.debug("删除消息: table={}, messageId={}, updatedRows={}", 
                 tableName, messageId, updatedRows);
        
        return updatedRows > 0;
    }
    
    /**
     * 获取会话中置顶的消息
     * 
     * @param conversationId 会话ID
     * @return 置顶消息列表
     */
    public List<Message> findPinnedMessages(Long conversationId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            SELECT * FROM %s 
            WHERE conversation_id = ? AND is_pinned = 1 AND status = 1
            ORDER BY seq DESC
            """, tableName);
        
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Message.class), conversationId);
    }
    
    /**
     * 统计会话消息数量
     * 
     * @param conversationId 会话ID
     * @return 消息数量
     */
    public long countByConversationId(Long conversationId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        
        String sql = String.format("""
            SELECT COUNT(*) FROM %s 
            WHERE conversation_id = ? AND status = 1
            """, tableName);
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class, conversationId);
        return count != null ? count : 0L;
    }
} 