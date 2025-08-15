package com.acme.im.communication.repository;

import com.acme.im.common.infrastructure.database.MessageShardingStrategy;
import com.acme.im.common.infrastructure.database.annotation.DataSource;
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
 * 
 * 数据源策略：
 * - 所有方法都是读操作，统一使用从库(SECONDARY)
 * - 主要用于消息查询、历史记录获取等读操作
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@DataSource(type = DataSource.DataSourceType.SECONDARY)
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
     * 根据会话ID查找消息列表（支持分页） - 读操作，使用从库
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
     * 根据会话ID查找最新消息 - 读操作，使用从库
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
        
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(messages.get(0));
    }
    
    /**
     * 根据会话ID查找指定时间范围的消息 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findByConversationIdAndTimeRange(Long conversationId, 
                                                        LocalDateTime startTime, 
                                                        LocalDateTime endTime, 
                                                        int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND created_at BETWEEN ? AND ? " +
            "AND status = 1 ORDER BY seq DESC LIMIT ?",
            tableName
        );
        
        log.debug("查询会话 {} 在时间范围 {} - {} 的消息，表: {}", 
                 conversationId, startTime, endTime, tableName);
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, 
                                conversationId, startTime, endTime, limit);
    }
    
    /**
     * 根据发送者ID查找消息 - 读操作，使用从库
     * 
     * @param senderId 发送者ID
     * @param conversationId 会话ID（可选，用于限制范围）
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findBySenderId(Long senderId, Long conversationId, int limit) {
        String sql;
        Object[] params;
        
        if (conversationId != null) {
            String tableName = shardingStrategy.getTableName(conversationId);
            sql = String.format(
                "SELECT * FROM %s WHERE sender_id = ? AND conversation_id = ? " +
                "AND status = 1 ORDER BY seq DESC LIMIT ?",
                tableName
            );
            params = new Object[]{senderId, conversationId, limit};
        } else {
            // 跨表查询，需要查询所有分表
            // 这里简化处理，只查询第一个表作为示例
            String tableName = shardingStrategy.getTableName(0L);
            sql = String.format(
                "SELECT * FROM %s WHERE sender_id = ? AND status = 1 " +
                "ORDER BY seq DESC LIMIT ?",
                tableName
            );
            params = new Object[]{senderId, limit};
        }
        
        log.debug("查询发送者 {} 的消息，SQL: {}", senderId, sql);
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, params);
    }
    
    /**
     * 根据消息类型查找消息 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param msgType 消息类型
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findByMessageType(Long conversationId, int msgType, int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND msg_type = ? " +
            "AND status = 1 ORDER BY seq DESC LIMIT ?",
            tableName
        );
        
        log.debug("查询会话 {} 中类型为 {} 的消息，表: {}", conversationId, msgType, tableName);
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, msgType, limit);
    }
    
    /**
     * 搜索消息内容 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> searchByContent(Long conversationId, String keyword, int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND content LIKE ? " +
            "AND status = 1 ORDER BY seq DESC LIMIT ?",
            tableName
        );
        
        String searchPattern = "%" + keyword + "%";
        log.debug("在会话 {} 中搜索关键词 '{}'，表: {}", conversationId, keyword, tableName);
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, searchPattern, limit);
    }
    
    /**
     * 获取会话中指定用户被提及的消息 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findMentionedMessages(Long conversationId, Long userId, int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND mentions LIKE ? " +
            "AND status = 1 ORDER BY seq DESC LIMIT ?",
            tableName
        );
        
        String mentionPattern = "%" + userId + "%";
        log.debug("查询会话 {} 中用户 {} 被提及的消息，表: {}", conversationId, userId, tableName);
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, mentionPattern, limit);
    }
    
    /**
     * 获取会话中回复指定消息的消息 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param replyToId 被回复的消息ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findReplyMessages(Long conversationId, Long replyToId, int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? AND reply_to_id = ? " +
            "AND status = 1 ORDER BY seq ASC LIMIT ?",
            tableName
        );
        
        log.debug("查询会话 {} 中回复消息 {} 的消息，表: {}", conversationId, replyToId, tableName);
        
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, conversationId, replyToId, limit);
    }
    
    /**
     * 统计会话中指定类型的消息数量 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param msgType 消息类型
     * @return 消息数量
     */
    public long countByMessageType(Long conversationId, int msgType) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE conversation_id = ? AND msg_type = ? AND status = 1",
            tableName
        );
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class, conversationId, msgType);
        return count != null ? count : 0L;
    }
    
    /**
     * 获取会话中指定时间段的活跃度统计 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息数量
     */
    public long getActivityCount(Long conversationId, LocalDateTime startTime, LocalDateTime endTime) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE conversation_id = ? " +
            "AND created_at BETWEEN ? AND ? AND status = 1",
            tableName
        );
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class, conversationId, startTime, endTime);
        return count != null ? count : 0L;
    }
} 