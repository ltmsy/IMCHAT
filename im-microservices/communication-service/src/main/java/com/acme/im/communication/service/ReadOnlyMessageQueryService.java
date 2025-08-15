package com.acme.im.communication.service;

import com.acme.im.common.infrastructure.database.MessageShardingUtils;
import com.acme.im.common.infrastructure.database.MessageQueryBuilder;
import com.acme.im.common.infrastructure.database.annotation.DataSource;
import com.acme.im.communication.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 只读消息查询服务
 * 专门负责消息查询操作，不进行写操作
 * 
 * 数据源策略：
 * - 所有方法都是读操作，统一使用从库(SECONDARY)
 * - 通过@Transactional(readOnly = true)进一步确保只读特性
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true) // 明确指定只读事务
@DataSource(type = DataSource.DataSourceType.SECONDARY) // 使用从库
public class ReadOnlyMessageQueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 消息行映射器
     */
    private final RowMapper<Message> messageRowMapper = new RowMapper<Message>() {
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
            message.setLastEditAt(rs.getObject("last_edit_at", LocalDateTime.class));
            message.setIsRecalled(rs.getBoolean("is_recalled") ? 1 : 0);
            message.setRecallReason(rs.getString("recall_reason"));
            message.setRecalledAt(rs.getObject("recalled_at", LocalDateTime.class));
            message.setStatus(rs.getInt("status"));
            message.setServerTimestamp(rs.getObject("server_timestamp", LocalDateTime.class));
            message.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
            message.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
            return message;
        }
    };

    /**
     * 查询会话的最新消息 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findLatestMessages(Long conversationId, int limit) {
        String sql = MessageQueryBuilder.buildShardedQuery(
            conversationId,
            "status = 1", // 只查询正常状态的消息
            "seq DESC",
            limit
        );
        
        return jdbcTemplate.query(sql, messageRowMapper);
    }

    /**
     * 分页查询会话消息 - 读操作，使用从库
     * 
     * @param conversationId 会话ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findMessagesByPage(Long conversationId, int offset, int limit) {
        String sql = MessageQueryBuilder.buildPaginationQuery(
            conversationId,
            "status = 1",
            "seq DESC",
            offset,
            limit
        );
        
        return jdbcTemplate.query(sql, messageRowMapper);
    }

    /**
     * 根据消息ID查询消息
     * 
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 消息对象
     */
    public Optional<Message> findMessageById(Long conversationId, Long messageId) {
        String sql = MessageQueryBuilder.buildShardedQuery(
            conversationId,
            "id = ? AND status = 1",
            null,
            1
        );
        
        List<Message> messages = jdbcTemplate.query(sql, messageRowMapper, messageId);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    /**
     * 搜索会话内的消息
     * 
     * @param conversationId 会话ID
     * @param searchTerm 搜索关键词
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> searchMessages(Long conversationId, String searchTerm, int limit) {
        String sql = MessageQueryBuilder.buildSearchQuery(conversationId, searchTerm, limit);
        
        return jdbcTemplate.query(sql, messageRowMapper, searchTerm, conversationId);
    }

    /**
     * 查询用户发送的消息
     * 
     * @param conversationId 会话ID
     * @param senderId 发送者ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findMessagesBySender(Long conversationId, Long senderId, int limit) {
        String sql = MessageQueryBuilder.buildShardedQuery(
            conversationId,
            "sender_id = ? AND status = 1",
            "seq DESC",
            limit
        );
        
        return jdbcTemplate.query(sql, messageRowMapper, senderId);
    }

    /**
     * 查询置顶消息
     * 
     * @param conversationId 会话ID
     * @return 置顶消息列表
     */
    public List<Message> findPinnedMessages(Long conversationId) {
        String sql = MessageQueryBuilder.buildShardedQuery(
            conversationId,
            "is_pinned = 1 AND status = 1",
            "seq DESC",
            null
        );
        
        return jdbcTemplate.query(sql, messageRowMapper);
    }

    /**
     * 统计会话消息数量
     * 
     * @param conversationId 会话ID
     * @return 消息数量
     */
    public long countMessages(Long conversationId) {
        String sql = MessageQueryBuilder.buildCountQuery(conversationId, "status = 1");
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * 统计用户未读消息数量
     * 
     * @param conversationId 会话ID
     * @param lastReadSeq 最后已读序号
     * @return 未读消息数量
     */
    public long countUnreadMessages(Long conversationId, Long lastReadSeq) {
        String sql = MessageQueryBuilder.buildCountQuery(
            conversationId,
            "seq > ? AND status = 1"
        );
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class, lastReadSeq);
        return count != null ? count : 0L;
    }

    /**
     * 查询消息历史（按时间范围）
     * 
     * @param conversationId 会话ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<Message> findMessagesByTimeRange(Long conversationId, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        String sql = MessageQueryBuilder.buildShardedQuery(
            conversationId,
            "created_at BETWEEN ? AND ? AND status = 1",
            "seq ASC",
            limit
        );
        
        return jdbcTemplate.query(sql, messageRowMapper, startTime, endTime);
    }

    /**
     * 获取会话消息序列号信息
     * 
     * @param conversationId 会话ID
     * @return 最新序列号
     */
    public Long getLatestSequenceNumber(Long conversationId) {
        String sql = MessageQueryBuilder.buildShardedQuery(
            conversationId,
            "status = 1",
            "seq DESC",
            1
        );
        
        List<Message> messages = jdbcTemplate.query(sql, messageRowMapper);
        return messages.isEmpty() ? 0L : messages.get(0).getSeq();
    }

    /**
     * 验证分表配置
     * 
     * @return 分表信息
     */
    public String getShardingInfo() {
        return MessageShardingUtils.getShardingInfo();
    }

    /**
     * 获取分表统计信息
     * 
     * @return 分表统计
     */
    public List<String> getAllTableNames() {
        return MessageShardingUtils.getAllTableNames();
    }
} 