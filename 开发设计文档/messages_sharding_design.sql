-- IM系统消息表分表设计方案
-- 版本: 1.0
-- 说明: 针对messages表进行水平分表，提高查询性能和存储效率

SET NAMES utf8mb4;

-- ================================
-- 消息表分表策略
-- ================================

-- 分表策略：按会话ID取模分表 (推荐)
-- 优势：
-- 1. 同一会话的消息在同一张表，查询效率高
-- 2. 避免跨表查询历史消息
-- 3. 数据分布相对均匀
-- 4. 便于按会话维护和清理数据

-- 分表数量：32张表 (可根据实际需求调整)
-- 分表规则：conversation_id % 32

-- ================================
-- 创建分表
-- ================================

-- 消息分表 0-31
CREATE TABLE `messages_00` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `seq` BIGINT NOT NULL COMMENT '消息序号',
    `client_msg_id` VARCHAR(64) NOT NULL COMMENT '客户端消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `msg_type` TINYINT NOT NULL COMMENT '消息类型：1-文本，2-图片，3-文件，4-语音，5-视频，6-位置，7-名片，8-系统消息',
    `content` TEXT COMMENT '消息内容',
    `content_extra` JSON COMMENT '扩展内容（文件信息、位置信息等）',
    `reply_to_id` BIGINT COMMENT '回复消息ID',
    `forward_from_id` BIGINT COMMENT '转发来源消息ID',
    `mentions` JSON COMMENT '提及的用户ID列表',
    `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `is_edited` TINYINT DEFAULT 0 COMMENT '是否已编辑',
    `edit_count` INT DEFAULT 0 COMMENT '编辑次数',
    `last_edit_at` DATETIME COMMENT '最后编辑时间',
    `is_recalled` TINYINT DEFAULT 0 COMMENT '是否已撤回',
    `recall_reason` VARCHAR(255) COMMENT '撤回原因',
    `recalled_at` DATETIME COMMENT '撤回时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-已删除，1-正常，2-审核中，3-已拒绝',
    `server_timestamp` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '服务器时间戳',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY `uk_conv_seq` (`conversation_id`, `seq`),
    UNIQUE KEY `uk_conv_client_id` (`conversation_id`, `client_msg_id`),
    INDEX `idx_conversation_time` (`conversation_id`, `server_timestamp`),
    INDEX `idx_sender` (`sender_id`),
    INDEX `idx_type` (`msg_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_pinned` (`conversation_id`, `is_pinned`),
    FULLTEXT KEY `ft_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表分片00';

-- 批量创建其他分表的存储过程
DELIMITER $$
CREATE PROCEDURE CreateMessageTables()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE table_suffix VARCHAR(2);
    DECLARE sql_stmt TEXT;
    
    WHILE i <= 31 DO
        -- 生成表后缀 (01, 02, ..., 31)
        SET table_suffix = LPAD(i, 2, '0');
        
        SET sql_stmt = CONCAT('
        CREATE TABLE `messages_', table_suffix, '` (
            `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT ''消息ID'',
            `conversation_id` BIGINT NOT NULL COMMENT ''会话ID'',
            `seq` BIGINT NOT NULL COMMENT ''消息序号'',
            `client_msg_id` VARCHAR(64) NOT NULL COMMENT ''客户端消息ID'',
            `sender_id` BIGINT NOT NULL COMMENT ''发送者ID'',
            `msg_type` TINYINT NOT NULL COMMENT ''消息类型：1-文本，2-图片，3-文件，4-语音，5-视频，6-位置，7-名片，8-系统消息'',
            `content` TEXT COMMENT ''消息内容'',
            `content_extra` JSON COMMENT ''扩展内容（文件信息、位置信息等）'',
            `reply_to_id` BIGINT COMMENT ''回复消息ID'',
            `forward_from_id` BIGINT COMMENT ''转发来源消息ID'',
            `mentions` JSON COMMENT ''提及的用户ID列表'',
            `is_pinned` TINYINT DEFAULT 0 COMMENT ''是否置顶'',
            `is_edited` TINYINT DEFAULT 0 COMMENT ''是否已编辑'',
            `edit_count` INT DEFAULT 0 COMMENT ''编辑次数'',
            `last_edit_at` DATETIME COMMENT ''最后编辑时间'',
            `is_recalled` TINYINT DEFAULT 0 COMMENT ''是否已撤回'',
            `recall_reason` VARCHAR(255) COMMENT ''撤回原因'',
            `recalled_at` DATETIME COMMENT ''撤回时间'',
            `status` TINYINT DEFAULT 1 COMMENT ''状态：0-已删除，1-正常，2-审核中，3-已拒绝'',
            `server_timestamp` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''服务器时间戳'',
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
            `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
            
            UNIQUE KEY `uk_conv_seq` (`conversation_id`, `seq`),
            UNIQUE KEY `uk_conv_client_id` (`conversation_id`, `client_msg_id`),
            INDEX `idx_conversation_time` (`conversation_id`, `server_timestamp`),
            INDEX `idx_sender` (`sender_id`),
            INDEX `idx_type` (`msg_type`),
            INDEX `idx_status` (`status`),
            INDEX `idx_pinned` (`conversation_id`, `is_pinned`),
            FULLTEXT KEY `ft_content` (`content`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''消息表分片', table_suffix, ''';
        ');
        
        SET @sql = sql_stmt;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 执行存储过程创建分表
CALL CreateMessageTables();

-- 删除存储过程
DROP PROCEDURE CreateMessageTables;

-- ================================
-- 分表路由函数
-- ================================

-- 创建获取分表名的函数
DELIMITER $$
CREATE FUNCTION GetMessageTableName(conversation_id BIGINT) 
RETURNS VARCHAR(20) 
READS SQL DATA 
DETERMINISTIC
BEGIN
    DECLARE table_suffix VARCHAR(2);
    SET table_suffix = LPAD(conversation_id % 32, 2, '0');
    RETURN CONCAT('messages_', table_suffix);
END$$
DELIMITER ;

-- ================================
-- 分表管理视图
-- ================================

-- 创建消息分表信息视图
CREATE VIEW `message_tables_info` AS
SELECT 
    TABLE_NAME as table_name,
    TABLE_ROWS as row_count,
    ROUND(DATA_LENGTH/1024/1024, 2) as data_size_mb,
    ROUND(INDEX_LENGTH/1024/1024, 2) as index_size_mb,
    ROUND((DATA_LENGTH + INDEX_LENGTH)/1024/1024, 2) as total_size_mb,
    CREATE_TIME as created_at,
    UPDATE_TIME as updated_at
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME LIKE 'messages_%'
ORDER BY TABLE_NAME;

-- ================================
-- 分表数据迁移存储过程
-- ================================

-- 从原messages表迁移数据到分表的存储过程
DELIMITER $$
CREATE PROCEDURE MigrateMessagesToShards()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_conversation_id BIGINT;
    DECLARE v_table_name VARCHAR(20);
    DECLARE migrate_cursor CURSOR FOR 
        SELECT DISTINCT conversation_id FROM messages ORDER BY conversation_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- 检查原messages表是否存在
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_schema = DATABASE() AND table_name = 'messages') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '原messages表不存在，无需迁移';
    END IF;
    
    OPEN migrate_cursor;
    
    migration_loop: LOOP
        FETCH migrate_cursor INTO v_conversation_id;
        IF done THEN
            LEAVE migration_loop;
        END IF;
        
        -- 获取目标分表名
        SET v_table_name = GetMessageTableName(v_conversation_id);
        
        -- 迁移该会话的所有消息
        SET @sql = CONCAT('
            INSERT INTO ', v_table_name, ' 
            SELECT * FROM messages 
            WHERE conversation_id = ', v_conversation_id
        );
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        -- 输出迁移进度
        SELECT CONCAT('已迁移会话 ', v_conversation_id, ' 的消息到表 ', v_table_name) as progress;
        
    END LOOP;
    
    CLOSE migrate_cursor;
    
    SELECT '消息数据迁移完成！' as result;
END$$
DELIMITER ;

-- ================================
-- 分表查询示例
-- ================================

-- 示例1：查询指定会话的最新消息
-- SELECT GetMessageTableName(12345) as table_name; -- 先获取表名
-- SELECT * FROM messages_xx WHERE conversation_id = 12345 ORDER BY seq DESC LIMIT 20;

-- 示例2：查询用户发送的消息（需要查询所有分表）
-- 这种查询建议通过应用层实现，或者建立专门的索引表

-- ================================
-- 分表维护存储过程
-- ================================

-- 清理过期消息的存储过程
DELIMITER $$
CREATE PROCEDURE CleanExpiredMessages(IN days_to_keep INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE table_suffix VARCHAR(2);
    DECLARE cutoff_date DATETIME;
    
    SET cutoff_date = DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    WHILE i < 32 DO
        SET table_suffix = LPAD(i, 2, '0');
        
        SET @sql = CONCAT('
            DELETE FROM messages_', table_suffix, ' 
            WHERE created_at < ''', cutoff_date, '''
            AND status = 0
        ');
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SET i = i + 1;
    END WHILE;
    
    SELECT CONCAT('已清理 ', days_to_keep, ' 天前的过期消息') as result;
END$$
DELIMITER ;

-- 统计所有分表消息数量的存储过程
DELIMITER $$
CREATE PROCEDURE GetMessageStats()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE table_suffix VARCHAR(2);
    DECLARE total_count BIGINT DEFAULT 0;
    DECLARE table_count BIGINT DEFAULT 0;
    
    DROP TEMPORARY TABLE IF EXISTS temp_stats;
    CREATE TEMPORARY TABLE temp_stats (
        table_name VARCHAR(20),
        message_count BIGINT
    );
    
    WHILE i < 32 DO
        SET table_suffix = LPAD(i, 2, '0');
        
        SET @sql = CONCAT('
            INSERT INTO temp_stats 
            SELECT ''messages_', table_suffix, ''', COUNT(*) 
            FROM messages_', table_suffix
        );
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SET i = i + 1;
    END WHILE;
    
    SELECT 
        table_name,
        message_count,
        ROUND(message_count * 100.0 / (SELECT SUM(message_count) FROM temp_stats), 2) as percentage
    FROM temp_stats 
    ORDER BY message_count DESC;
    
    SELECT SUM(message_count) as total_messages FROM temp_stats;
    
    DROP TEMPORARY TABLE temp_stats;
END$$
DELIMITER ;

-- ================================
-- 应用层分表路由逻辑参考
-- ================================

/*
Java代码示例：

@Component
public class MessageShardingStrategy {
    
    private static final int SHARD_COUNT = 32;
    
    public String getTableName(Long conversationId) {
        int shardIndex = (int) (conversationId % SHARD_COUNT);
        return String.format("messages_%02d", shardIndex);
    }
    
    public String getTableName(String conversationId) {
        return getTableName(Long.parseLong(conversationId));
    }
}

@Repository
public class MessageRepository {
    
    @Autowired
    private MessageShardingStrategy shardingStrategy;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public List<Message> findByConversationId(Long conversationId, int limit) {
        String tableName = shardingStrategy.getTableName(conversationId);
        String sql = String.format(
            "SELECT * FROM %s WHERE conversation_id = ? ORDER BY seq DESC LIMIT ?", 
            tableName
        );
        return jdbcTemplate.query(sql, new MessageRowMapper(), conversationId, limit);
    }
    
    public void insertMessage(Message message) {
        String tableName = shardingStrategy.getTableName(message.getConversationId());
        String sql = String.format(
            "INSERT INTO %s (conversation_id, seq, sender_id, content, ...) VALUES (?, ?, ?, ?, ...)",
            tableName
        );
        jdbcTemplate.update(sql, message.getConversationId(), message.getSeq(), ...);
    }
}
*/

-- ================================
-- 分表性能优化建议
-- ================================

/*
1. 索引优化：
   - 每个分表保持相同的索引结构
   - 根据查询模式调整索引策略
   - 定期分析和优化索引

2. 查询优化：
   - 尽量避免跨分表查询
   - 使用会话ID作为查询条件
   - 合理使用LIMIT限制结果集

3. 维护建议：
   - 定期清理过期数据
   - 监控各分表的数据分布
   - 根据业务增长调整分表数量

4. 备份策略：
   - 可以并行备份多个分表
   - 按分表制定不同的备份策略
   - 重要会话可以增加备份频率
*/

-- ================================
-- 分表监控查询
-- ================================

-- 查看分表信息
-- SELECT * FROM message_tables_info;

-- 获取消息统计
-- CALL GetMessageStats();

-- 清理30天前的过期消息
-- CALL CleanExpiredMessages(30);

SELECT '消息表分表设计完成！共32个分表，按会话ID取模路由' as result; 