package com.acme.im.communication.service;

import com.acme.im.common.infrastructure.database.MessageShardingUtils;
import com.acme.im.common.infrastructure.database.MessageQueryBuilder;
import com.acme.im.communication.entity.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;

/**
 * 消息分表示例服务
 * 展示如何使用分表工具类进行消息操作
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Service
public class MessageShardingExample {
    
    /**
     * 示例：获取消息的分表信息
     */
    public void demonstrateShardingInfo() {
        System.out.println("=== 消息分表示例 ===");
        System.out.println(MessageShardingUtils.getShardingInfo());
        
        // 获取所有分表名称
        List<String> allTables = MessageShardingUtils.getAllTableNames();
        System.out.println("所有分表: " + allTables.subList(0, 5) + "... (共" + allTables.size() + "个)");
        
        // 测试特定会话ID的分表
        Long conversationId = 12345L;
        String tableName = MessageShardingUtils.getTableName(conversationId);
        int shardIndex = MessageShardingUtils.getShardIndex(conversationId);
        
        System.out.println("会话ID " + conversationId + " 的分表信息:");
        System.out.println("  分表名称: " + tableName);
        System.out.println("  分表索引: " + shardIndex);
        System.out.println("  是否有效: " + MessageShardingUtils.isValidShardIndex(shardIndex));
    }
    
    /**
     * 示例：构建分表查询SQL
     */
    public void demonstrateQueryBuilding() {
        System.out.println("\n=== SQL查询构建示例 ===");
        
        Long conversationId = 67890L;
        
        // 构建单表查询
        String singleQuery = MessageQueryBuilder.buildShardedQuery(
            conversationId, 
            "status = 1 AND msg_type = 1", 
            "seq DESC", 
            20
        );
        System.out.println("单表查询SQL: " + singleQuery);
        
        // 构建分页查询
        String paginationQuery = MessageQueryBuilder.buildPaginationQuery(
            conversationId,
            "sender_id = ?",
            "created_at DESC",
            0,
            10
        );
        System.out.println("分页查询SQL: " + paginationQuery);
        
        // 构建搜索查询
        String searchQuery = MessageQueryBuilder.buildSearchQuery(
            conversationId,
            "hello world",
            50
        );
        System.out.println("搜索查询SQL: " + searchQuery);
    }
    
    /**
     * 示例：跨分表查询
     */
    public void demonstrateCrossShardQueries() {
        System.out.println("\n=== 跨分表示例 ===");
        
        List<Long> conversationIds = Arrays.asList(1001L, 2002L, 3003L, 4004L, 5005L);
        
        // 获取涉及的分表
        List<String> involvedTables = MessageShardingUtils.getInvolvedTableNames(conversationIds);
        System.out.println("会话ID列表: " + conversationIds);
        System.out.println("涉及的分表: " + involvedTables);
        
        // 构建跨分表查询
        List<String> crossShardQueries = MessageQueryBuilder.buildCrossShardQueries(
            conversationIds,
            "status = 1",
            "created_at DESC",
            100
        );
        
        System.out.println("跨分表查询SQL数量: " + crossShardQueries.size());
        for (int i = 0; i < Math.min(3, crossShardQueries.size()); i++) {
            System.out.println("  查询" + (i + 1) + ": " + crossShardQueries.get(i));
        }
    }
    
    /**
     * 示例：消息实体分表方法使用
     */
    public void demonstrateMessageEntityMethods() {
        System.out.println("\n=== 消息实体分表方法示例 ===");
        
        // 创建示例消息
        Message message = new Message();
        message.setConversationId(99999L);
        message.setMsgType(1);
        message.setContent("这是一条测试消息");
        message.setStatus(1);
        
        // 使用分表方法
        System.out.println("消息分表信息:");
        System.out.println("  分表名称: " + message.getTableName());
        System.out.println("  分表索引: " + message.getShardIndex());
        System.out.println("  索引有效: " + message.isValidShardIndex());
        System.out.println("  消息类型: " + message.getMessageTypeDescription());
        System.out.println("  可编辑: " + message.isEditable());
        System.out.println("  可撤回: " + message.isRecallable());
        System.out.println("  系统消息: " + message.isSystemMessage());
        System.out.println("  媒体消息: " + message.isMediaMessage());
    }
    
    /**
     * 示例：分表验证和工具方法
     */
    public void demonstrateValidationAndUtilities() {
        System.out.println("\n=== 分表验证和工具方法示例 ===");
        
        // 验证分表名称
        String[] testTableNames = {"messages_00", "messages_15", "messages_31", "messages_32", "invalid_table"};
        
        for (String tableName : testTableNames) {
            boolean isValid = MessageShardingUtils.isValidTableName(tableName);
            int shardIndex = MessageShardingUtils.extractShardIndex(tableName);
            
            System.out.println("表名 '" + tableName + "':");
            System.out.println("  有效: " + isValid);
            System.out.println("  分表索引: " + (shardIndex >= 0 ? shardIndex : "无效"));
        }
        
        // 检查会话是否在同一分表
        Long conv1 = 1001L;
        Long conv2 = 1033L; // 1001 % 32 = 9, 1033 % 32 = 9 (同一分表)
        Long conv3 = 2001L; // 2001 % 32 = 17 (不同分表)
        
        System.out.println("\n会话分表关系:");
        System.out.println("会话 " + conv1 + " 和 " + conv2 + " 在同一分表: " + 
                          MessageShardingUtils.isInSameShard(conv1, conv2));
        System.out.println("会话 " + conv1 + " 和 " + conv3 + " 在同一分表: " + 
                          MessageShardingUtils.isInSameShard(conv1, conv3));
    }
    
    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        demonstrateShardingInfo();
        demonstrateQueryBuilding();
        demonstrateCrossShardQueries();
        demonstrateMessageEntityMethods();
        demonstrateValidationAndUtilities();
        
        System.out.println("\n=== 分表示例完成 ===");
    }
} 