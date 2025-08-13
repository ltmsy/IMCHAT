package com.acme.im.common.infrastructure.database;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;
import java.util.Set;

/**
 * 消息分表工具类
 * 提供分表相关的静态方法和常量
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class MessageShardingUtils {
    
    /**
     * 分表总数
     */
    public static final int SHARD_COUNT = 32;
    
    /**
     * 分表名称前缀
     */
    public static final String TABLE_PREFIX = "messages_";
    
    /**
     * 分表名称格式
     */
    public static final String TABLE_NAME_FORMAT = "messages_%02d";
    
    /**
     * 根据会话ID获取分表名称
     * 
     * @param conversationId 会话ID
     * @return 分表名称，如：messages_00, messages_01, ..., messages_31
     */
    public static String getTableName(Long conversationId) {
        if (conversationId == null) {
            return getDefaultTableName();
        }
        int shardIndex = getShardIndex(conversationId);
        return String.format(TABLE_NAME_FORMAT, shardIndex);
    }
    
    /**
     * 根据会话ID获取分表索引
     * 
     * @param conversationId 会话ID
     * @return 分表索引 (0-31)
     */
    public static int getShardIndex(Long conversationId) {
        if (conversationId == null) {
            return 0;
        }
        return (int) (conversationId % SHARD_COUNT);
    }
    
    /**
     * 检查分表索引是否有效
     * 
     * @param shardIndex 分表索引
     * @return 是否为有效分表索引
     */
    public static boolean isValidShardIndex(int shardIndex) {
        return shardIndex >= 0 && shardIndex < SHARD_COUNT;
    }
    
    /**
     * 获取默认分表名称
     * 
     * @return 默认分表名称
     */
    public static String getDefaultTableName() {
        return String.format(TABLE_NAME_FORMAT, 0);
    }
    
    /**
     * 获取所有分表名称
     * 
     * @return 所有分表名称列表
     */
    public static List<String> getAllTableNames() {
        List<String> tableNames = new ArrayList<>();
        for (int i = 0; i < SHARD_COUNT; i++) {
            tableNames.add(String.format(TABLE_NAME_FORMAT, i));
        }
        return tableNames;
    }
    
    /**
     * 获取指定范围的分表名称
     * 
     * @param startIndex 开始索引（包含）
     * @param endIndex 结束索引（不包含）
     * @return 指定范围的分表名称列表
     */
    public static List<String> getTableNamesInRange(int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex > SHARD_COUNT || startIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid range: startIndex=" + startIndex + ", endIndex=" + endIndex);
        }
        
        List<String> tableNames = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            tableNames.add(String.format(TABLE_NAME_FORMAT, i));
        }
        return tableNames;
    }
    
    /**
     * 根据会话ID列表获取涉及的分表名称
     * 
     * @param conversationIds 会话ID列表
     * @return 涉及的分表名称列表（去重）
     */
    public static List<String> getInvolvedTableNames(List<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Arrays.asList(getDefaultTableName());
        }
        
        Set<Integer> shardIndices = new HashSet<>();
        for (Long conversationId : conversationIds) {
            shardIndices.add(getShardIndex(conversationId));
        }
        
        List<String> tableNames = new ArrayList<>();
        for (Integer index : shardIndices) {
            tableNames.add(String.format(TABLE_NAME_FORMAT, index));
        }
        Collections.sort(tableNames);
        return tableNames;
    }
    
    /**
     * 检查两个会话是否在同一分表
     * 
     * @param conversationId1 会话ID1
     * @param conversationId2 会话ID2
     * @return 是否在同一分表
     */
    public static boolean isInSameShard(Long conversationId1, Long conversationId2) {
        if (conversationId1 == null || conversationId2 == null) {
            return false;
        }
        return getShardIndex(conversationId1) == getShardIndex(conversationId2);
    }
    
    /**
     * 获取分表统计信息
     * 
     * @return 分表统计信息字符串
     */
    public static String getShardingInfo() {
        return String.format("Message Sharding: %d tables (0-%d), prefix: %s", 
                SHARD_COUNT, SHARD_COUNT - 1, TABLE_PREFIX);
    }
    
    /**
     * 验证分表名称格式
     * 
     * @param tableName 表名
     * @return 是否为有效的分表名称
     */
    public static boolean isValidTableName(String tableName) {
        if (tableName == null || !tableName.startsWith(TABLE_PREFIX)) {
            return false;
        }
        
        try {
            String suffix = tableName.substring(TABLE_PREFIX.length());
            int index = Integer.parseInt(suffix);
            return isValidShardIndex(index);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 从分表名称提取分表索引
     * 
     * @param tableName 分表名称
     * @return 分表索引，如果不是有效分表名称则返回-1
     */
    public static int extractShardIndex(String tableName) {
        if (!isValidTableName(tableName)) {
            return -1;
        }
        
        try {
            String suffix = tableName.substring(TABLE_PREFIX.length());
            return Integer.parseInt(suffix);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
} 