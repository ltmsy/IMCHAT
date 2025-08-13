package com.acme.im.common.infrastructure.database;

import org.springframework.stereotype.Component;

/**
 * 消息分表策略
 * 基于 messages_sharding_design.sql 的分表设计
 * 分表策略：按会话ID取模分表，共32张表
 */
@Component
public class MessageShardingStrategy {
    
    /**
     * 分表数量
     */
    private static final int SHARD_COUNT = 32;
    
    /**
     * 根据会话ID获取分表名
     * 
     * @param conversationId 会话ID
     * @return 分表名，格式：messages_00, messages_01, ..., messages_31
     */
    public String getTableName(Long conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        int shardIndex = (int) (conversationId % SHARD_COUNT);
        return String.format("messages_%02d", shardIndex);
    }
    
    /**
     * 根据会话ID字符串获取分表名
     * 
     * @param conversationId 会话ID字符串
     * @return 分表名
     */
    public String getTableName(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        try {
            Long id = Long.parseLong(conversationId);
            return getTableName(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的会话ID格式: " + conversationId);
        }
    }
    
    /**
     * 获取分表数量
     * 
     * @return 分表数量
     */
    public int getShardCount() {
        return SHARD_COUNT;
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
        
        return (int) (conversationId % SHARD_COUNT);
    }
    
    /**
     * 验证分表名是否有效
     * 
     * @param tableName 表名
     * @return 是否有效
     */
    public boolean isValidTableName(String tableName) {
        if (tableName == null || !tableName.startsWith("messages_")) {
            return false;
        }
        
        try {
            String suffix = tableName.substring("messages_".length());
            int index = Integer.parseInt(suffix);
            return index >= 0 && index < SHARD_COUNT;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 获取所有分表名列表
     * 
     * @return 分表名数组
     */
    public String[] getAllTableNames() {
        String[] tableNames = new String[SHARD_COUNT];
        for (int i = 0; i < SHARD_COUNT; i++) {
            tableNames[i] = String.format("messages_%02d", i);
        }
        return tableNames;
    }
} 