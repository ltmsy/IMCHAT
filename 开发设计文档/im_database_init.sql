-- IM系统数据库初始化脚本
-- 版本: 1.0
-- 创建时间: 2024-01-01
-- 说明: 支持用户管理、社交关系、消息通信、内容管理、系统配置等核心功能

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `im_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `im_system`;

-- ================================
-- 用户管理相关表
-- ================================

-- 用户基础信息表
CREATE TABLE `users` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) UNIQUE NOT NULL COMMENT '用户名',
    `email` VARCHAR(128) UNIQUE COMMENT '邮箱',
    `phone` VARCHAR(20) UNIQUE COMMENT '手机号',
    `nickname` VARCHAR(64) COMMENT '昵称',
    `avatar_url` VARCHAR(256) COMMENT '头像URL',
    `signature` TEXT COMMENT '个性签名',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `birthday` DATE COMMENT '生日',
    `region` VARCHAR(128) COMMENT '地区',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常，2-冻结',
    `online_status` TINYINT DEFAULT 0 COMMENT '在线状态：0-离线，1-在线，2-忙碌，3-隐身',
    `last_login_at` DATETIME COMMENT '最后登录时间',
    `last_active_at` DATETIME COMMENT '最后活跃时间',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `salt` VARCHAR(64) NOT NULL COMMENT '密码盐值',
    `two_factor_enabled` TINYINT DEFAULT 0 COMMENT '是否启用二步验证',
    `two_factor_secret` VARCHAR(64) COMMENT '二步验证密钥',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`),
    INDEX `idx_online_status` (`online_status`),
    INDEX `idx_last_active` (`last_active_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础信息表';

-- 用户设备管理表
CREATE TABLE `user_devices` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `device_id` VARCHAR(128) NOT NULL COMMENT '设备标识',
    `device_name` VARCHAR(128) COMMENT '设备名称',
    `device_type` TINYINT NOT NULL COMMENT '设备类型：1-PC网页，2-PC桌面，3-移动APP',
    `device_info` JSON COMMENT '设备信息（浏览器、操作系统等）',
    `ip_address` VARCHAR(45) COMMENT 'IP地址',
    `location` VARCHAR(128) COMMENT '登录地点',
    `is_online` TINYINT DEFAULT 0 COMMENT '是否在线',
    `last_login_at` DATETIME COMMENT '最后登录时间',
    `last_active_at` DATETIME COMMENT '最后活跃时间',
    `login_token` VARCHAR(255) COMMENT '登录令牌',
    `refresh_token` VARCHAR(255) COMMENT '刷新令牌',
    `token_expires_at` DATETIME COMMENT '令牌过期时间',
    `is_trusted` TINYINT DEFAULT 0 COMMENT '是否为受信任设备',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_device` (`device_id`),
    INDEX `idx_online` (`is_online`),
    INDEX `idx_last_active` (`last_active_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设备管理表';

-- 用户隐私设置表
CREATE TABLE `user_privacy_settings` (
    `user_id` BIGINT PRIMARY KEY COMMENT '用户ID',
    `friend_request_mode` TINYINT DEFAULT 1 COMMENT '好友申请模式：0-自动通过，1-需要验证，2-拒绝所有',
    `allow_search_by_username` TINYINT DEFAULT 1 COMMENT '允许通过用户名搜索',
    `allow_search_by_phone` TINYINT DEFAULT 0 COMMENT '允许通过手机号搜索',
    `allow_search_by_email` TINYINT DEFAULT 0 COMMENT '允许通过邮箱搜索',
    `allow_group_invite` TINYINT DEFAULT 1 COMMENT '允许群组邀请：0-禁止，1-需要验证，2-直接同意',
    `allow_stranger_message` TINYINT DEFAULT 0 COMMENT '允许陌生人消息',
    `message_read_receipt` TINYINT DEFAULT 1 COMMENT '消息已读回执',
    `online_status_visible` TINYINT DEFAULT 1 COMMENT '在线状态可见',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户隐私设置表';

-- 用户黑名单表
CREATE TABLE `user_blacklist` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '黑名单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `blocked_user_id` BIGINT NOT NULL COMMENT '被拉黑用户ID',
    `block_type` TINYINT DEFAULT 1 COMMENT '拉黑类型：1-消息，2-好友申请，4-查看资料（可位运算组合）',
    `reason` VARCHAR(255) COMMENT '拉黑原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`blocked_user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_user_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_blocked_user` (`blocked_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户黑名单表';

-- ================================
-- 社交关系相关表
-- ================================

-- 好友关系表
CREATE TABLE `friendships` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友ID',
    `status` TINYINT DEFAULT 1 COMMENT '关系状态：1-正常，2-已删除',
    `remark` VARCHAR(64) COMMENT '好友备注',
    `group_name` VARCHAR(64) DEFAULT '我的好友' COMMENT '分组名称',
    `is_starred` TINYINT DEFAULT 0 COMMENT '是否星标好友',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `mute_notifications` TINYINT DEFAULT 0 COMMENT '是否免打扰',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`friend_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_friendship` (`user_id`, `friend_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_friend` (`friend_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_group` (`user_id`, `group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 好友申请表
CREATE TABLE `friend_requests` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发起用户ID',
    `to_user_id` BIGINT NOT NULL COMMENT '目标用户ID',
    `message` TEXT COMMENT '申请消息',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已同意，2-已拒绝，3-已过期',
    `source` TINYINT DEFAULT 1 COMMENT '申请来源：1-搜索，2-群组，3-名片分享，4-扫码',
    `source_info` VARCHAR(255) COMMENT '来源信息（群组ID等）',
    `processed_at` DATETIME COMMENT '处理时间',
    `expires_at` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`from_user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`to_user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_from_user` (`from_user_id`),
    INDEX `idx_to_user` (`to_user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- 会话表
CREATE TABLE `conversations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    `type` TINYINT NOT NULL COMMENT '会话类型：1-单聊，2-群聊，3-服务号，4-系统消息',
    `name` VARCHAR(128) COMMENT '会话名称（群名称）',
    `avatar_url` VARCHAR(256) COMMENT '会话头像',
    `description` TEXT COMMENT '会话描述',
    `owner_id` BIGINT COMMENT '创建者ID',
    `max_members` INT DEFAULT 500 COMMENT '最大成员数',
    `member_count` INT DEFAULT 0 COMMENT '当前成员数',
    `settings` JSON COMMENT '会话设置',
    `last_message_id` BIGINT COMMENT '最后一条消息ID',
    `last_message_at` DATETIME COMMENT '最后消息时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-已解散，1-正常，2-已归档',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`owner_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_type` (`type`),
    INDEX `idx_owner` (`owner_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_last_message` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 会话成员表
CREATE TABLE `conversation_members` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员关系ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` TINYINT DEFAULT 0 COMMENT '角色：0-普通成员，1-管理员，2-群主',
    `nickname` VARCHAR(64) COMMENT '群昵称',
    `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `is_muted` TINYINT DEFAULT 0 COMMENT '是否免打扰',
    `mute_until` DATETIME COMMENT '禁言到期时间',
    `last_read_seq` BIGINT DEFAULT 0 COMMENT '最后已读消息序号',
    `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
    `join_source` TINYINT DEFAULT 1 COMMENT '加入方式：1-邀请，2-搜索，3-扫码，4-链接',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_conv_user` (`conversation_id`, `user_id`),
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_role` (`conversation_id`, `role`),
    INDEX `idx_unread` (`user_id`, `unread_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话成员表';

-- 群组设置表
CREATE TABLE `group_settings` (
    `conversation_id` BIGINT PRIMARY KEY COMMENT '群组ID',
    `join_approval` TINYINT DEFAULT 1 COMMENT '入群审批：0-自由加入，1-需要审批',
    `invite_permission` TINYINT DEFAULT 1 COMMENT '邀请权限：0-所有人，1-管理员，2-群主',
    `message_permission` TINYINT DEFAULT 0 COMMENT '发言权限：0-所有人，1-管理员，2-群主',
    `at_all_permission` TINYINT DEFAULT 1 COMMENT '@全体权限：0-禁用，1-管理员，2-群主',
    `history_visible` TINYINT DEFAULT 1 COMMENT '历史消息可见：0-不可见，1-最近7天，2-最近30天，3-全部',
    `allow_member_invite` TINYINT DEFAULT 1 COMMENT '允许成员邀请',
    `allow_member_modify_info` TINYINT DEFAULT 1 COMMENT '允许成员修改群信息',
    `allow_temp_session` TINYINT DEFAULT 1 COMMENT '允许临时会话',
    `mute_all` TINYINT DEFAULT 0 COMMENT '全员禁言',
    `invite_link` VARCHAR(128) COMMENT '邀请链接',
    `invite_code` VARCHAR(32) COMMENT '邀请码',
    `link_expires_at` DATETIME COMMENT '链接过期时间',
    `max_link_uses` INT DEFAULT 0 COMMENT '链接最大使用次数，0为无限制',
    `link_use_count` INT DEFAULT 0 COMMENT '链接已使用次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组设置表';

-- 群组申请表
CREATE TABLE `group_join_requests` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    `conversation_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
    `inviter_id` BIGINT COMMENT '邀请人ID',
    `message` TEXT COMMENT '申请消息',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已同意，2-已拒绝，3-已过期',
    `source` TINYINT DEFAULT 1 COMMENT '申请来源：1-搜索，2-邀请，3-扫码，4-链接',
    `processed_by` BIGINT COMMENT '处理人ID',
    `processed_at` DATETIME COMMENT '处理时间',
    `expires_at` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`inviter_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`processed_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组申请表';

-- ================================
-- 消息相关表
-- ================================

-- 消息表（已废弃，使用分表 messages_00 ~ messages_31）
-- 此表结构仅作为参考，实际使用分表存储
-- CREATE TABLE `messages` (...) -- 已移除，使用分表方案

-- 消息序列号表
CREATE TABLE `conversation_sequences` (
    `conversation_id` BIGINT PRIMARY KEY COMMENT '会话ID',
    `current_seq` BIGINT DEFAULT 0 COMMENT '当前序列号',
    `last_message_at` DATETIME COMMENT '最后消息时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话序列号表';

-- 消息已读状态表
CREATE TABLE `message_read_status` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `last_read_seq` BIGINT NOT NULL COMMENT '最后已读消息序号',
    `last_read_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后已读时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_conv_user` (`conversation_id`, `user_id`),
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读状态表';

-- 消息幂等性表
CREATE TABLE `message_idempotency` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `client_msg_id` VARCHAR(64) NOT NULL COMMENT '客户端消息ID',
    `server_msg_id` BIGINT NOT NULL COMMENT '服务器消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY `uk_conv_client_id` (`conversation_id`, `client_msg_id`),
    INDEX `idx_server_msg` (`server_msg_id`),
    INDEX `idx_sender` (`sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息幂等性表';

-- ================================
-- 内容管理相关表
-- ================================

-- 文件信息表
CREATE TABLE `files` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    `filename` VARCHAR(256) NOT NULL COMMENT '文件名',
    `original_name` VARCHAR(256) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(512) NOT NULL COMMENT '文件路径',
    `bucket` VARCHAR(64) NOT NULL COMMENT '存储桶',
    `object_key` VARCHAR(512) NOT NULL COMMENT '对象键',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `mime_type` VARCHAR(128) COMMENT '文件类型',
    `file_hash` CHAR(64) COMMENT '文件哈希值',
    `thumbnail_url` VARCHAR(512) COMMENT '缩略图URL',
    `preview_url` VARCHAR(512) COMMENT '预览URL',
    `download_url` VARCHAR(512) COMMENT '下载URL',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者ID',
    `conversation_id` BIGINT COMMENT '所属会话ID',
    `message_id` BIGINT COMMENT '关联消息ID',
    `upload_status` TINYINT DEFAULT 1 COMMENT '上传状态：0-失败，1-成功，2-处理中',
    `access_level` TINYINT DEFAULT 1 COMMENT '访问级别：1-私有，2-群组内，3-公开',
    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    `expires_at` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`uploader_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`conversation_id`) REFERENCES `conversations`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`message_id`) REFERENCES `messages`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uk_file_hash` (`file_hash`),
    INDEX `idx_uploader` (`uploader_id`),
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_message` (`message_id`),
    INDEX `idx_created` (`created_at`),
    INDEX `idx_mime_type` (`mime_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- 收藏表
CREATE TABLE `favorites` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(32) NOT NULL COMMENT '收藏类型：message-消息，file-文件，link-链接',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `title` VARCHAR(255) COMMENT '收藏标题',
    `content` TEXT COMMENT '收藏内容',
    `thumbnail_url` VARCHAR(512) COMMENT '缩略图URL',
    `source_info` JSON COMMENT '来源信息',
    `tags` JSON COMMENT '标签',
    `folder_id` BIGINT COMMENT '收藏夹ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_user` (`user_id`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_folder` (`folder_id`),
    INDEX `idx_created` (`created_at`),
    FULLTEXT KEY `ft_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 收藏夹表
CREATE TABLE `favorite_folders` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏夹ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(128) NOT NULL COMMENT '收藏夹名称',
    `description` TEXT COMMENT '描述',
    `icon` VARCHAR(64) COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `item_count` INT DEFAULT 0 COMMENT '收藏数量',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认收藏夹',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_user` (`user_id`),
    INDEX `idx_sort` (`user_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏夹表';

-- ================================
-- 连接管理相关表
-- ================================

-- 连接信息表
CREATE TABLE `connections` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '连接ID',
    `connection_id` VARCHAR(128) UNIQUE NOT NULL COMMENT '连接标识',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `device_id` VARCHAR(128) NOT NULL COMMENT '设备ID',
    `service_instance` VARCHAR(128) NOT NULL COMMENT '服务实例',
    `connection_type` TINYINT NOT NULL COMMENT '连接类型：1-WebSocket，2-长轮询',
    `ip_address` VARCHAR(45) COMMENT 'IP地址',
    `user_agent` TEXT COMMENT '用户代理',
    `platform` VARCHAR(64) COMMENT '平台信息',
    `app_version` VARCHAR(32) COMMENT '应用版本',
    `protocol_version` VARCHAR(16) COMMENT '协议版本',
    `connected_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '连接时间',
    `last_heartbeat_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后心跳时间',
    `last_activity_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-已断开，1-已连接，2-异常',
    `disconnect_reason` VARCHAR(255) COMMENT '断开原因',
    `disconnected_at` DATETIME COMMENT '断开时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_user` (`user_id`),
    INDEX `idx_device` (`device_id`),
    INDEX `idx_instance` (`service_instance`),
    INDEX `idx_status` (`status`),
    INDEX `idx_heartbeat` (`last_heartbeat_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连接信息表';

-- ================================
-- 系统配置相关表
-- ================================

-- 系统配置分类表
CREATE TABLE `config_categories` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(128) NOT NULL COMMENT '分类名称',
    `key_name` VARCHAR(128) UNIQUE NOT NULL COMMENT '分类键名',
    `description` TEXT COMMENT '分类描述',
    `parent_id` BIGINT COMMENT '父分类ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `is_system` TINYINT DEFAULT 0 COMMENT '是否系统分类',
    `icon` VARCHAR(64) COMMENT '图标',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`parent_id`) REFERENCES `config_categories`(`id`) ON DELETE SET NULL,
    INDEX `idx_parent` (`parent_id`),
    INDEX `idx_sort` (`sort_order`),
    INDEX `idx_system` (`is_system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置分类表';

-- 系统配置项表
CREATE TABLE `config_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `key_name` VARCHAR(128) NOT NULL COMMENT '配置键',
    `display_name` VARCHAR(128) NOT NULL COMMENT '显示名称',
    `value` TEXT COMMENT '配置值',
    `default_value` TEXT COMMENT '默认值',
    `value_type` VARCHAR(32) NOT NULL COMMENT '值类型：string,int,bool,json,text',
    `description` TEXT COMMENT '配置描述',
    `validation_rule` JSON COMMENT '验证规则',
    `is_required` TINYINT DEFAULT 0 COMMENT '是否必需',
    `is_sensitive` TINYINT DEFAULT 0 COMMENT '是否敏感信息',
    `is_readonly` TINYINT DEFAULT 0 COMMENT '是否只读',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`category_id`) REFERENCES `config_categories`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_key_name` (`key_name`),
    INDEX `idx_category` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_sort` (`category_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置项表';

-- 功能开关表
CREATE TABLE `feature_flags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '开关ID',
    `name` VARCHAR(128) UNIQUE NOT NULL COMMENT '功能名称',
    `key_name` VARCHAR(128) UNIQUE NOT NULL COMMENT '功能键名',
    `description` TEXT COMMENT '功能描述',
    `is_enabled` TINYINT DEFAULT 0 COMMENT '是否启用',
    `target_type` TINYINT DEFAULT 1 COMMENT '目标类型：1-全局，2-用户，3-群组',
    `target_rules` JSON COMMENT '目标规则',
    `rollout_percentage` INT DEFAULT 0 COMMENT '推出百分比（0-100）',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `created_by` BIGINT COMMENT '创建者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_enabled` (`is_enabled`),
    INDEX `idx_target_type` (`target_type`),
    INDEX `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能开关表';

-- ================================
-- 监控审计相关表
-- ================================

-- 敏感词表
CREATE TABLE `sensitive_words` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '词汇ID',
    `word` VARCHAR(128) NOT NULL COMMENT '敏感词',
    `category` VARCHAR(64) COMMENT '分类',
    `level` TINYINT DEFAULT 1 COMMENT '级别：1-提醒，2-替换，3-拦截',
    `replacement` VARCHAR(128) COMMENT '替换词',
    `is_regex` TINYINT DEFAULT 0 COMMENT '是否正则表达式',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `hit_count` INT DEFAULT 0 COMMENT '命中次数',
    `created_by` BIGINT COMMENT '创建者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    UNIQUE KEY `uk_word` (`word`),
    INDEX `idx_category` (`category`),
    INDEX `idx_level` (`level`),
    INDEX `idx_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';

-- 内容审核记录表
CREATE TABLE `content_audit_logs` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审核ID',
    `content_type` VARCHAR(32) NOT NULL COMMENT '内容类型：message,file,image',
    `content_id` BIGINT NOT NULL COMMENT '内容ID',
    `user_id` BIGINT COMMENT '用户ID',
    `audit_type` VARCHAR(32) NOT NULL COMMENT '审核类型：sensitive_word,spam,manual',
    `audit_result` TINYINT NOT NULL COMMENT '审核结果：0-拒绝，1-通过，2-待审核',
    `risk_level` TINYINT COMMENT '风险等级：1-低，2-中，3-高',
    `hit_rules` JSON COMMENT '命中规则',
    `audit_content` TEXT COMMENT '审核内容',
    `auditor_id` BIGINT COMMENT '审核员ID',
    `audit_reason` TEXT COMMENT '审核原因',
    `auto_audit` TINYINT DEFAULT 1 COMMENT '是否自动审核',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`auditor_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_content` (`content_type`, `content_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_result` (`audit_result`),
    INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核记录表';

-- ================================
-- 数据初始化
-- ================================

-- 插入默认配置分类
INSERT INTO `config_categories` (`name`, `key_name`, `description`, `is_system`, `sort_order`) VALUES
('系统设置', 'system', '系统基础配置', 1, 1),
('用户设置', 'user', '用户相关配置', 1, 2),
('消息设置', 'message', '消息相关配置', 1, 3),
('文件设置', 'file', '文件相关配置', 1, 4),
('安全设置', 'security', '安全相关配置', 1, 5),
('通知设置', 'notification', '通知相关配置', 1, 6);

-- 插入默认配置项
INSERT INTO `config_items` (`category_id`, `key_name`, `display_name`, `value`, `default_value`, `value_type`, `description`) VALUES
(1, 'system.name', '系统名称', 'IM聊天系统', 'IM聊天系统', 'string', '系统显示名称'),
(1, 'system.version', '系统版本', '1.0.0', '1.0.0', 'string', '当前系统版本'),
(1, 'system.max_online_users', '最大在线用户数', '10000', '10000', 'int', '系统支持的最大在线用户数'),
(2, 'user.max_friends', '最大好友数量', '1000', '1000', 'int', '单个用户最大好友数量'),
(2, 'user.max_groups', '最大群组数量', '100', '100', 'int', '单个用户最大群组数量'),
(3, 'message.max_length', '消息最大长度', '5000', '5000', 'int', '单条消息最大字符数'),
(3, 'message.recall_time_limit', '消息撤回时限', '120', '120', 'int', '消息撤回时间限制（秒）'),
(4, 'file.max_size', '文件最大大小', '104857600', '104857600', 'int', '单个文件最大大小（字节）'),
(4, 'file.allowed_types', '允许的文件类型', '["jpg","jpeg","png","gif","pdf","doc","docx","xls","xlsx"]', '["jpg","jpeg","png","gif","pdf","doc","docx","xls","xlsx"]', 'json', '允许上传的文件类型'),
(5, 'security.password_min_length', '密码最小长度', '8', '8', 'int', '用户密码最小长度'),
(5, 'security.login_attempt_limit', '登录尝试次数限制', '5', '5', 'int', '登录失败次数限制'),
(6, 'notification.push_enabled', '推送通知开关', '1', '1', 'bool', '是否启用推送通知');

-- 插入默认功能开关
INSERT INTO `feature_flags` (`name`, `key_name`, `description`, `is_enabled`) VALUES
('消息撤回', 'message_recall', '允许用户撤回已发送的消息', 1),
('消息编辑', 'message_edit', '允许用户编辑已发送的消息', 1),
('文件上传', 'file_upload', '允许用户上传文件', 1),
('群组创建', 'group_create', '允许用户创建群组', 1),
('好友申请', 'friend_request', '允许用户发送好友申请', 1),
('消息转发', 'message_forward', '允许用户转发消息', 1),
('消息收藏', 'message_favorite', '允许用户收藏消息', 1),
('在线状态', 'online_status', '显示用户在线状态', 1);

-- 插入默认敏感词
INSERT INTO `sensitive_words` (`word`, `category`, `level`, `replacement`) VALUES
('测试敏感词', '测试', 2, '***'),
('垃圾信息', '垃圾', 3, NULL),
('广告', '广告', 2, '[广告]');

SET FOREIGN_KEY_CHECKS = 1;

-- ================================
-- 索引优化建议
-- ================================

-- 为高频查询添加复合索引
-- ALTER TABLE `messages` ADD INDEX `idx_conv_sender_time` (`conversation_id`, `sender_id`, `server_timestamp`);
-- ALTER TABLE `conversation_members` ADD INDEX `idx_user_pinned` (`user_id`, `is_pinned`);
-- ALTER TABLE `friendships` ADD INDEX `idx_user_status_group` (`user_id`, `status`, `group_name`);

-- ================================
-- 分区表建议（适用于大数据量场景）
-- ================================

-- 消息表按时间分区（月度分区）
-- ALTER TABLE `messages` PARTITION BY RANGE (TO_DAYS(`created_at`)) (
--     PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
--     PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
--     -- 继续添加更多分区...
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- 注意：操作日志表已移除，不需要分区

-- ================================
-- 性能优化建议
-- ================================

-- 1. 启用查询缓存（适用于读多写少的场景）
-- SET GLOBAL query_cache_type = ON;
-- SET GLOBAL query_cache_size = 268435456; -- 256MB

-- 2. 优化InnoDB配置
-- SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB，根据实际内存调整
-- SET GLOBAL innodb_log_file_size = 268435456; -- 256MB
-- SET GLOBAL innodb_flush_log_at_trx_commit = 2; -- 提高写性能，但可能丢失1秒数据

-- 3. 启用慢查询日志
-- SET GLOBAL slow_query_log = ON;
-- SET GLOBAL long_query_time = 2; -- 记录超过2秒的查询

-- ================================
-- 数据库维护建议
-- ================================

-- 1. 定期清理过期数据
--    - 清理已撤回超过7天的消息（使用分表清理存储过程）
--    - 清理已过期的好友申请和群组申请
--    - 清理过期的连接记录和文件信息

-- 2. 定期优化表
--    - OPTIMIZE TABLE messages_xx; (分表优化)
--    - OPTIMIZE TABLE conversation_members;

-- 3. 定期备份数据库
--    - 全量备份：每日凌晨
--    - 增量备份：每小时
--    - binlog备份：实时

-- 4. 监控数据库性能
--    - 监控连接数、QPS、TPS
--    - 监控慢查询、锁等待
--    - 监控磁盘空间、内存使用
--    - 监控各消息分表的数据分布和性能

-- ================================
-- 结束
-- ================================

-- 数据库初始化完成
SELECT 'IM系统数据库初始化完成！' as message;
SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'im_system'; 