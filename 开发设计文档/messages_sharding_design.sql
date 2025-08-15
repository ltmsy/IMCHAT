-- ================================
-- IM系统消息表分表设计方案（优化版）
-- 版本: 2.0
-- 说明: 针对messages表进行水平分表，提高查询性能和存储效率
-- 优化: 支持新的消息格式，包含设备ID、消息来源、消息版本等字段
-- ================================

SET NAMES utf8mb4;

-- 选择数据库
USE `im_system`;

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
    `receiver_id` BIGINT COMMENT '接收者ID（用户ID或群组ID）',
    
    -- 消息类型和内容
    `msg_type` TINYINT NOT NULL COMMENT '消息类型：0-文本，1-图片，2-文件，3-语音，4-视频，5-位置，6-名片，7-系统消息，10-编辑消息，11-引用消息，12-转发消息，13-撤回消息',
    `content` TEXT COMMENT '消息内容',
    `content_extra` JSON COMMENT '扩展内容（文件信息、位置信息等）',
    
    -- 特殊操作字段
    `original_message_id` BIGINT COMMENT '原始消息ID（编辑、引用、转发时使用）',
    `operation_type` VARCHAR(20) COMMENT '操作类型：edit-编辑，quote-引用，forward-转发，recall-撤回',
    
    -- 引用消息信息
    `quoted_message_id` BIGINT COMMENT '被引用的消息ID',
    `quoted_content` TEXT COMMENT '被引用的消息内容（截取）',
    `quoted_sender_id` BIGINT COMMENT '被引用消息的发送者ID',
    `quoted_content_type` TINYINT COMMENT '被引用消息的内容类型',
    
    -- 转发消息信息
    `original_conversation_id` BIGINT COMMENT '原始会话ID',
    `original_sender_id` BIGINT COMMENT '原始发送者ID',
    `forward_reason` VARCHAR(255) COMMENT '转发原因',
    
    -- 编辑消息信息
    `original_content` TEXT COMMENT '原始内容',
    `edit_reason` VARCHAR(255) COMMENT '编辑原因',
    
    -- 回复和转发（保留原有字段，向后兼容）
    `reply_to_id` BIGINT COMMENT '回复消息ID',
    `forward_from_id` BIGINT COMMENT '转发来源消息ID',
    
    -- 其他属性
    `mentions` JSON COMMENT '提及的用户ID列表',
    `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `pin_scope` TINYINT DEFAULT 0 COMMENT '置顶范围：0-仅我，1-所有人',
    `pinned_by` BIGINT COMMENT '置顶操作者ID',
    `pinned_at` DATETIME COMMENT '置顶时间',
    `is_edited` TINYINT DEFAULT 0 COMMENT '是否已编辑',
    `edit_count` INT DEFAULT 0 COMMENT '编辑次数',
    `last_edit_at` DATETIME COMMENT '最后编辑时间',
    `is_recalled` TINYINT DEFAULT 0 COMMENT '是否已撤回',
    `recall_reason` VARCHAR(255) COMMENT '撤回原因',
    `recalled_at` DATETIME COMMENT '撤回时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否已删除',
    `delete_scope` TINYINT DEFAULT 0 COMMENT '删除范围：0-仅我，1-所有人',
    `deleted_by` BIGINT COMMENT '删除操作者ID',
    `deleted_at` DATETIME COMMENT '删除时间',
    `delete_reason` VARCHAR(255) COMMENT '删除原因',
    
    -- 消息状态
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-已删除，1-正常，2-审核中，3-已拒绝',
    
    -- 时间信息
    `server_timestamp` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '服务器时间戳',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 多端同步字段
    `device_id` VARCHAR(64) COMMENT '设备ID（多端同步）',
    `source` VARCHAR(20) COMMENT '消息来源（web/mobile/desktop）',
    `version` VARCHAR(10) DEFAULT '1.0' COMMENT '消息版本',
    
    -- 索引
    UNIQUE KEY `uk_conv_seq` (`conversation_id`, `seq`),
    UNIQUE KEY `uk_conv_client_id` (`conversation_id`, `client_msg_id`),
    INDEX `idx_conversation_time` (`conversation_id`, `server_timestamp`),
    INDEX `idx_sender` (`sender_id`),
    INDEX `idx_receiver` (`receiver_id`),
    INDEX `idx_type` (`msg_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_pinned` (`conversation_id`, `is_pinned`),
    INDEX `idx_pin_scope` (`conversation_id`, `pin_scope`),
    INDEX `idx_deleted` (`conversation_id`, `is_deleted`),
    INDEX `idx_delete_scope` (`conversation_id`, `delete_scope`),
    INDEX `idx_device` (`device_id`),
    INDEX `idx_original_msg` (`original_message_id`),
    INDEX `idx_quoted_msg` (`quoted_message_id`),
    FULLTEXT KEY `ft_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表分片00（优化版）';

-- ================================
-- 批量创建分表的存储过程（保留，因为有用）
-- ================================
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
            `receiver_id` BIGINT COMMENT ''接收者ID（用户ID或群组ID）'',
            
            -- 消息类型和内容
            `msg_type` TINYINT NOT NULL COMMENT ''消息类型：0-文本，1-图片，2-文件，3-语音，4-视频，5-位置，6-名片，7-系统消息，10-编辑消息，11-引用消息，12-转发消息，13-撤回消息'',
            `content` TEXT COMMENT ''消息内容'',
            `content_extra` JSON COMMENT ''扩展内容（文件信息、位置信息等）'',
            
            -- 特殊操作字段
            `original_message_id` BIGINT COMMENT ''原始消息ID（编辑、引用、转发时使用）'',
            `operation_type` VARCHAR(20) COMMENT ''操作类型：edit-编辑，quote-引用，forward-转发，recall-撤回'',
            
            -- 引用消息信息
            `quoted_message_id` BIGINT COMMENT ''被引用的消息ID'',
            `quoted_content` TEXT COMMENT ''被引用的消息内容（截取）'',
            `quoted_sender_id` BIGINT COMMENT ''被引用消息的发送者ID'',
            `quoted_content_type` TINYINT COMMENT ''被引用消息的内容类型'',
            
            -- 转发消息信息
            `original_conversation_id` BIGINT COMMENT ''原始会话ID'',
            `original_sender_id` BIGINT COMMENT ''原始发送者ID'',
            `forward_reason` VARCHAR(255) COMMENT ''转发原因'',
            
            -- 编辑消息信息
            `original_content` TEXT COMMENT ''原始内容'',
            `edit_reason` VARCHAR(255) COMMENT ''编辑原因'',
            
            -- 回复和转发（保留原有字段，向后兼容）
            `reply_to_id` BIGINT COMMENT ''回复消息ID'',
            `forward_from_id` BIGINT COMMENT ''转发来源消息ID'',
            
            -- 其他属性
            `mentions` JSON COMMENT ''提及的用户ID列表'',
            `is_pinned` TINYINT DEFAULT 0 COMMENT ''是否置顶'',
            `pin_scope` TINYINT DEFAULT 0 COMMENT ''置顶范围：0-仅我，1-所有人'',
            `pinned_by` BIGINT COMMENT ''置顶操作者ID'',
            `pinned_at` DATETIME COMMENT ''置顶时间'',
            `is_edited` TINYINT DEFAULT 0 COMMENT ''是否已编辑'',
            `edit_count` INT DEFAULT 0 COMMENT ''编辑次数'',
            `last_edit_at` DATETIME COMMENT ''最后编辑时间'',
            `is_recalled` TINYINT DEFAULT 0 COMMENT ''是否已撤回'',
            `recall_reason` VARCHAR(255) COMMENT ''撤回原因'',
            `recalled_at` DATETIME COMMENT ''撤回时间'',
            `is_deleted` TINYINT DEFAULT 0 COMMENT ''是否已删除'',
            `delete_scope` TINYINT DEFAULT 0 COMMENT ''删除范围：0-仅我，1-所有人'',
            `deleted_by` BIGINT COMMENT ''删除操作者ID'',
            `deleted_at` DATETIME COMMENT ''删除时间'',
            `delete_reason` VARCHAR(255) COMMENT ''删除原因'',
            
            -- 消息状态
            `status` TINYINT DEFAULT 1 COMMENT ''状态：0-已删除，1-正常，2-审核中，3-已拒绝'',
            
            -- 时间信息
            `server_timestamp` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''服务器时间戳'',
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
            `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
            
            -- 多端同步字段
            `device_id` VARCHAR(64) COMMENT ''设备ID（多端同步）'',
            `source` VARCHAR(20) COMMENT ''消息来源（web/mobile/desktop）'',
            `version` VARCHAR(10) DEFAULT ''1.0'' COMMENT ''消息版本'',
            
            -- 索引
            UNIQUE KEY `uk_conv_seq` (`conversation_id`, `seq`),
            UNIQUE KEY `uk_conv_client_id` (`conversation_id`, `client_msg_id`),
            INDEX `idx_conversation_time` (`conversation_id`, `server_timestamp`),
            INDEX `idx_sender` (`sender_id`),
            INDEX `idx_receiver` (`receiver_id`),
            INDEX `idx_type` (`msg_type`),
            INDEX `idx_status` (`status`),
            INDEX `idx_pinned` (`conversation_id`, `is_pinned`),
            INDEX `idx_pin_scope` (`conversation_id`, `pin_scope`),
            INDEX `idx_deleted` (`conversation_id`, `is_deleted`),
            INDEX `idx_delete_scope` (`conversation_id`, `delete_scope`),
            INDEX `idx_device` (`device_id`),
            INDEX `idx_original_msg` (`original_message_id`),
            INDEX `idx_quoted_msg` (`quoted_message_id`),
            FULLTEXT KEY `ft_content` (`content`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''消息表分片', table_suffix, '（优化版）'';
        ');
        
        SET @sql = sql_stmt;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SET i = i + 1;
    END WHILE;
    
    SELECT '消息分表创建完成！共32张表（messages_00 ~ messages_31）' as message;
END$$

DELIMITER ;

-- ================================
-- 执行存储过程创建分表
-- ================================
CALL CreateMessageTables();

-- ================================
-- 删除存储过程（创建完成后不再需要）
-- ================================
DROP PROCEDURE CreateMessageTables();

-- ================================
-- 分表路由函数（可选，应用层也可以实现）
-- ================================
DELIMITER $$

-- 先删除已存在的函数（如果存在）
DROP FUNCTION IF EXISTS GetMessageTableName;

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
-- 分表管理视图（用于监控）
-- ================================
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
-- 消息类型说明视图
-- ================================
CREATE VIEW `message_types_info` AS
SELECT 
    0 as type_code, 'CHAT_TEXT' as type_name, '文本消息' as description
UNION ALL
SELECT 1, 'CHAT_IMAGE', '图片消息'
UNION ALL
SELECT 2, 'CHAT_FILE', '文件消息'
UNION ALL
SELECT 3, 'CHAT_VOICE', '语音消息'
UNION ALL
SELECT 4, 'CHAT_VIDEO', '视频消息'
UNION ALL
SELECT 5, 'CHAT_LOCATION', '位置消息'
UNION ALL
SELECT 6, 'CHAT_CARD', '名片消息'
UNION ALL
SELECT 7, 'CHAT_SYSTEM', '系统消息'
UNION ALL
SELECT 10, 'CHAT_EDIT', '编辑消息'
UNION ALL
SELECT 11, 'CHAT_QUOTE', '引用消息'
UNION ALL
SELECT 12, 'CHAT_FORWARD', '转发消息'
UNION ALL
SELECT 13, 'CHAT_RECALL', '撤回消息'
UNION ALL
SELECT 14, 'CHAT_DELETE', '删除消息';

-- ================================
-- 操作范围说明视图
-- ================================
CREATE VIEW `operation_scope_info` AS
SELECT 
    0 as scope_code, 'SELF_ONLY' as scope_name, '仅我' as description, '操作仅对当前用户生效' as detail
UNION ALL
SELECT 1, 'ALL_USERS', '所有人', '操作对所有用户生效';

SELECT '消息表分表设计完成！共32个分表，按会话ID取模路由' as result;
SELECT '优化版：支持新的消息格式，包含设备ID、消息来源、消息版本等字段' as note;
SELECT '新增字段：receiver_id, original_message_id, operation_type, quoted_*, original_*, device_id, source, version' as new_fields;
SELECT '新增操作权限：pin_scope（置顶范围）, delete_scope（删除范围）' as operation_permissions;
SELECT '支持操作：edit-编辑, quote-引用, forward-转发, recall-撤回, delete-删除, pin-置顶' as supported_operations; 