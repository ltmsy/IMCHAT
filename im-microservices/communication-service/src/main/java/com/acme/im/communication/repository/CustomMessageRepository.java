package com.acme.im.communication.repository;

import com.acme.im.common.infrastructure.database.MessageShardingStrategy;
import com.acme.im.communication.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 自定义消息Repository实现
 * 支持分表查询和操作
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CustomMessageRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final MessageShardingStrategy shardingStrategy;
    
    /**
     * 消息行映射器
     */
    private static final RowMapper<Message> MESSAGE_ROW_MAPPER = new RowMapper<Message>() {
        @Override
        public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
            Message message = new Message();
            message.setId(rs.getLong("id"));
            message.setConversationId(rs.getLong("conversation_id"));
            message.setSeq(rs.getLong("seq"));
            message.setClientMsgId(rs.getString("client_msg_id"));
            message.setSenderId(rs.getLong("sender_id"));
            message.setMsgType(rs.getInt("msg_type"));
            message.setContent(rs.getString("content"));
            message.setContentExtra(rs.getString("content_extra"));
            message.setReplyToId(rs.getLong("reply_to_id"));
            message.setForwardFromId(rs.getLong("forward_from_id"));
            message.setMentions(rs.getString("mentions"));
            message.setIsPinned(rs.getBoolean("is_pinned") ? 1 : 0);
            message.setIsEdited(rs.getBoolean("is_edited") ? 1 : 0);
            message.setEditCount(rs.getInt("edit_count"));
            message.setLastEditAt(rs.getTimestamp("last_edit_at") != null ? 
                rs.getTimestamp("last_edit_at").toLocalDateTime() : null);
            message.setIsRecalled(rs.getBoolean("is_recalled") ? 1 : 0);
            message.setRecallReason(rs.getString("recall_reason"));
            message.setRecalledAt(rs.getTimestamp("recalled_at") != null ? 
                rs.getTimestamp("recalled_at").toLocalDateTime() : null);
            message.setStatus(rs.getInt("status"));
            message.setServerTimestamp(rs.getTimestamp("server_timestamp") != null ? 
                rs.getTimestamp("server_timestamp").toLocalDateTime() : null);
            message.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toLocalDateTime() : null);
            message.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return message;
        }
    };
    
    /**
     * 根据会话ID查找消息列表（支持分页）
     * 
     * @param conversationId 会话ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 消息列表
     */
    public List<Message> findByConversationId(Long conversationId, int limit, int offset) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? ORDER BY seq DESC LIMIT ? OFFSET ?",
            tableName
        );
        
        log.debug("查询会话 {} 的消息，表: {}, SQL: {}", conversationId, tableName, sql);
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, limit, offset);
    }
    
    /**
     * 根据会话ID查找最新消息
     * 
     * @param conversationId 会话ID
     * @return 最新消息
     */
    public Optional<Message> findLatestMessageByConversationId(Long conversationId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? ORDER BY seq DESC LIMIT 1",
            tableName
        );
        
        List<Message> messages = jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }
    
    /**
     * 根据会话ID查找指定序号范围的消息
     * 
     * @param conversationId 会话ID
     * @param startSeq 起始序号
     * @param endSeq 结束序号
     * @return 消息列表
     */
    public List<Message> findByConversationIdAndSeqBetween(Long conversationId, Long startSeq, Long endSeq) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND seq BETWEEN ? AND ? ORDER BY seq",
            tableName
        );
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, startSeq, endSeq);
    }
    
    /**
     * 根据会话ID查找指定时间范围的消息
     * 
     * @param conversationId 会话ID
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    public List<Message> findByConversationIdAndTimeBetween(Long conversationId, LocalDateTime startTime, LocalDateTime endTime) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND server_timestamp BETWEEN ? AND ? ORDER BY server_timestamp",
            tableName
        );
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, startTime, endTime);
    }
    
    /**
     * 根据客户端消息ID查找消息
     * 
     * @param conversationId 会话ID
     * @param clientMsgId 客户端消息ID
     * @return 消息
     */
    public Optional<Message> findByConversationIdAndClientMsgId(Long conversationId, String clientMsgId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND client_msg_id = ?",
            tableName
        );
        
        List<Message> messages = jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, clientMsgId);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }
    
    /**
     * 插入消息
     * 
     * @param message 消息对象
     * @return 插入结果
     */
    public boolean insertMessage(Message message) {
        String tableName = shardingStrategy.getTableName(message.getConversationId());
        String sql = String.format(
            "INSERT INTO %s (conversation_id, seq, client_msg_id, sender_id, msg_type, content, " +
            "content_extra, reply_to_id, forward_from_id, mentions, is_pinned, is_edited, " +
            "edit_count, last_edit_at, is_recalled, recall_reason, recalled_at, status, " +
            "server_timestamp, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            tableName
        );
        
        int result = jdbcTemplate.update(sql,
            message.getConversationId(),
            message.getSeq(),
            message.getClientMsgId(),
            message.getSenderId(),
            message.getMsgType(),
            message.getContent(),
            message.getContentExtra(),
            message.getReplyToId(),
            message.getForwardFromId(),
            message.getMentions(),
            message.getIsPinned(),
            message.getIsEdited(),
            message.getEditCount(),
            message.getLastEditAt(),
            message.getIsRecalled(),
            message.getRecallReason(),
            message.getRecalledAt(),
            message.getStatus(),
            message.getServerTimestamp(),
            message.getCreatedAt(),
            message.getUpdatedAt()
        );
        
        log.debug("插入消息到表 {}，结果: {}", tableName, result > 0);
        return result > 0;
    }
    
    /**
     * 更新消息
     * 
     * @param message 消息对象
     * @return 更新结果
     */
    public boolean updateMessage(Message message) {
        String tableName = shardingStrategy.getTableName(message.getConversationId());
        String sql = String.format(
            "UPDATE %s SET content = ?, content_extra = ?, is_pinned = ?, is_edited = ?, " +
            "edit_count = ?, last_edit_at = ?, is_recalled = ?, recall_reason = ?, " +
            "recalled_at = ?, status = ?, updated_at = ? WHERE id = ?",
            tableName
        );
        
        int result = jdbcTemplate.update(sql,
            message.getContent(),
            message.getContentExtra(),
            message.getIsPinned(),
            message.getIsEdited(),
            message.getEditCount(),
            message.getLastEditAt(),
            message.getIsRecalled(),
            message.getRecallReason(),
            message.getRecalledAt(),
            message.getStatus(),
            LocalDateTime.now(),
            message.getId()
        );
        
        log.debug("更新消息到表 {}，结果: {}", tableName, result > 0);
        return result > 0;
    }
    
    /**
     * 删除消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 删除结果
     */
    public boolean deleteMessage(Long conversationId, Long messageId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        
        int result = jdbcTemplate.update(sql, messageId);
        log.debug("从表 {} 删除消息 {}，结果: {}", tableName, messageId, result > 0);
        return result > 0;
    }
    
    /**
     * 根据会话ID统计消息数量
     * 
     * @param conversationId 会话ID
     * @return 消息数量
     */
    public long countByConversationId(Long conversationId) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE conversation_id = ?", tableName);
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class, conversationId);
        return count != null ? count : 0L;
    }
    
    /**
     * 根据会话ID和状态统计消息数量
     * 
     * @param conversationId 会话ID
     * @param status 消息状态
     * @return 消息数量
     */
    public long countByConversationIdAndStatus(Long conversationId, Integer status) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE conversation_id = ? AND status = ?", tableName);
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class, conversationId, status);
        return count != null ? count : 0L;
    }
} 