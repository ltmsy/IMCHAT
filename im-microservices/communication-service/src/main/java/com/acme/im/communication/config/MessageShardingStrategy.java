package com.acme.im.communication.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 消息分表策略
 * 按会话ID取模分表，共32张表
 * 严格按照 messages_sharding_design.sql 中的分表规则
 * 
 * 分表规则：conversation_id % 32
 * 表名格式：messages_00, messages_01, ..., messages_31
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
public class MessageShardingStrategy {

    /**
     * 分表数量，默认32张表
     */
    @Value("${message.sharding.table-count:32}")
    private int shardCount;

    /**
     * 表名前缀
     */
    private static final String TABLE_PREFIX = "messages_";

    /**
     * 根据会话ID获取分表名
     * 
     * @param conversationId 会话ID
     * @return 分表名，如：messages_00, messages_01, ..., messages_31
     */
    public String getTableName(Long conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        int shardIndex = getShardIndex(conversationId);
        return String.format("%s%02d", TABLE_PREFIX, shardIndex);
    }

    /**
     * 根据会话ID获取分表索引
     * 
     * @param conversationId 会话ID
     * @return 分表索引 (0-31)
     */
    public int getShardIndex(Long conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        return (int) (conversationId % shardCount);
    }

    /**
     * 验证分表索引是否有效
     * 
     * @param shardIndex 分表索引
     * @return 是否有效
     */
    public boolean isValidShardIndex(int shardIndex) {
        return shardIndex >= 0 && shardIndex < shardCount;
    }

    /**
     * 获取所有分表名列表
     * 
     * @return 分表名列表
     */
    public String[] getAllTableNames() {
        String[] tableNames = new String[shardCount];
        for (int i = 0; i < shardCount; i++) {
            tableNames[i] = String.format("%s%02d", TABLE_PREFIX, i);
        }
        return tableNames;
    }

    /**
     * 根据分表索引获取表名
     * 
     * @param shardIndex 分表索引
     * @return 分表名
     */
    public String getTableNameByIndex(int shardIndex) {
        if (!isValidShardIndex(shardIndex)) {
            throw new IllegalArgumentException("无效的分表索引: " + shardIndex);
        }
        
        return String.format("%s%02d", TABLE_PREFIX, shardIndex);
    }

    /**
     * 获取分表数量
     * 
     * @return 分表数量
     */
    public int getShardCount() {
        return shardCount;
    }

    /**
     * 获取表名前缀
     * 
     * @return 表名前缀
     */
    public String getTablePrefix() {
        return TABLE_PREFIX;
    }

    /**
     * 计算会话ID的哈希值（用于一致性哈希等场景）
     * 
     * @param conversationId 会话ID
     * @return 哈希值
     */
    public int getHashCode(Long conversationId) {
        if (conversationId == null) {
            return 0;
        }
        return conversationId.hashCode();
    }

    /**
     * 根据表名提取分表索引
     * 
     * @param tableName 表名
     * @return 分表索引
     */
    public int extractShardIndex(String tableName) {
        if (tableName == null || !tableName.startsWith(TABLE_PREFIX)) {
            throw new IllegalArgumentException("无效的表名: " + tableName);
        }
        
        String indexStr = tableName.substring(TABLE_PREFIX.length());
        try {
            int index = Integer.parseInt(indexStr);
            if (!isValidShardIndex(index)) {
                throw new IllegalArgumentException("无效的分表索引: " + index);
            }
            return index;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法解析分表索引: " + indexStr);
        }
    }

    /**
     * 检查表名是否为有效的消息分表名
     * 
     * @param tableName 表名
     * @return 是否有效
     */
    public boolean isValidTableName(String tableName) {
        if (tableName == null || !tableName.startsWith(TABLE_PREFIX)) {
            return false;
        }
        
        try {
            int index = extractShardIndex(tableName);
            return isValidShardIndex(index);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 