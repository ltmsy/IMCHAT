package com.acme.im.common.infrastructure.database;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息查询构建器
 * 支持分表查询的SQL构建
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class MessageQueryBuilder {
    
    /**
     * 构建单表查询SQL
     * 
     * @param tableName 表名
     * @param conditions 查询条件
     * @param orderBy 排序
     * @param limit 限制数量
     * @return 完整的SQL语句
     */
    public static String buildSingleTableQuery(String tableName, String conditions, String orderBy, Integer limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        
        if (conditions != null && !conditions.trim().isEmpty()) {
            sql.append(" WHERE ").append(conditions);
        }
        
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        
        return sql.toString();
    }
    
    /**
     * 构建分表查询SQL（查询单个会话的消息）
     * 
     * @param conversationId 会话ID
     * @param conditions 查询条件
     * @param orderBy 排序
     * @param limit 限制数量
     * @return 完整的SQL语句
     */
    public static String buildShardedQuery(Long conversationId, String conditions, String orderBy, Integer limit) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        return buildSingleTableQuery(tableName, conditions, orderBy, limit);
    }
    
    /**
     * 构建跨分表查询SQL（查询多个会话的消息）
     * 
     * @param conversationIds 会话ID列表
     * @param conditions 查询条件
     * @param orderBy 排序
     * @param limit 限制数量
     * @return 跨分表查询的SQL语句列表
     */
    public static List<String> buildCrossShardQueries(List<Long> conversationIds, String conditions, String orderBy, Integer limit) {
        List<String> tableNames = MessageShardingUtils.getInvolvedTableNames(conversationIds);
        
        return tableNames.stream()
                .map(tableName -> buildSingleTableQuery(tableName, conditions, orderBy, limit))
                .collect(Collectors.toList());
    }
    
    /**
     * 构建插入消息SQL
     * 
     * @param conversationId 会话ID
     * @return 插入SQL语句
     */
    public static String buildInsertQuery(Long conversationId) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        return String.format(
            "INSERT INTO %s (conversation_id, seq, client_msg_id, sender_id, msg_type, content, " +
            "content_extra, reply_to_id, forward_from_id, mentions, is_pinned, is_edited, " +
            "edit_count, last_edit_at, is_recalled, recall_reason, recalled_at, status, " +
            "server_timestamp, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            tableName
        );
    }
    
    /**
     * 构建更新消息SQL
     * 
     * @param conversationId 会话ID
     * @param fields 要更新的字段
     * @return 更新SQL语句
     */
    public static String buildUpdateQuery(Long conversationId, List<String> fields) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        
        String setClause = fields.stream()
                .map(field -> field + " = ?")
                .collect(Collectors.joining(", "));
        sql.append(setClause);
        
        sql.append(" WHERE id = ? AND conversation_id = ?");
        
        return sql.toString();
    }
    
    /**
     * 构建删除消息SQL
     * 
     * @param conversationId 会话ID
     * @return 删除SQL语句
     */
    public static String buildDeleteQuery(Long conversationId) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        return String.format("DELETE FROM %s WHERE id = ? AND conversation_id = ?", tableName);
    }
    
    /**
     * 构建消息计数SQL
     * 
     * @param conversationId 会话ID
     * @param conditions 查询条件
     * @return 计数SQL语句
     */
    public static String buildCountQuery(Long conversationId, String conditions) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableName);
        
        if (conditions != null && !conditions.trim().isEmpty()) {
            sql.append(" WHERE ").append(conditions);
        }
        
        return sql.toString();
    }
    
    /**
     * 构建分页查询SQL
     * 
     * @param conversationId 会话ID
     * @param conditions 查询条件
     * @param orderBy 排序
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 分页查询SQL语句
     */
    public static String buildPaginationQuery(Long conversationId, String conditions, String orderBy, Integer offset, Integer limit) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        
        if (conditions != null && !conditions.trim().isEmpty()) {
            sql.append(" WHERE ").append(conditions);
        }
        
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        if (offset != null && offset >= 0) {
            sql.append(" OFFSET ").append(offset);
        }
        
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        
        return sql.toString();
    }
    
    /**
     * 构建搜索查询SQL（全文搜索）
     * 
     * @param conversationId 会话ID
     * @param searchTerm 搜索关键词
     * @param limit 限制数量
     * @return 搜索SQL语句
     */
    public static String buildSearchQuery(Long conversationId, String searchTerm, Integer limit) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        sql.append(" WHERE MATCH(content) AGAINST(? IN BOOLEAN MODE)");
        sql.append(" AND conversation_id = ?");
        sql.append(" AND status = 1"); // 只搜索正常状态的消息
        
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        
        return sql.toString();
    }
    
    /**
     * 构建批量操作SQL（如批量更新状态）
     * 
     * @param conversationId 会话ID
     * @param messageIds 消息ID列表
     * @param field 要更新的字段
     * @return 批量更新SQL语句
     */
    public static String buildBatchUpdateQuery(Long conversationId, List<Long> messageIds, String field) {
        String tableName = MessageShardingUtils.getTableName(conversationId);
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(field).append(" = ?");
        sql.append(" WHERE conversation_id = ? AND id IN (");
        
        String placeholders = messageIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));
        sql.append(placeholders).append(")");
        
        return sql.toString();
    }
} 