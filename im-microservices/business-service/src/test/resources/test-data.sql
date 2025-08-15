-- 测试数据初始化脚本
-- 用于集成测试的数据准备

-- 创建用户表（如果不存在）
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(64) UNIQUE NOT NULL,
    `email` VARCHAR(128) UNIQUE,
    `phone` VARCHAR(20) UNIQUE,
    `nickname` VARCHAR(64),
    `avatar_url` VARCHAR(256),
    `signature` TEXT,
    `gender` TINYINT DEFAULT 0,
    `birthday` DATE,
    `region` VARCHAR(128),
    `status` TINYINT DEFAULT 1,
    `online_status` TINYINT DEFAULT 0,
    `last_login_at` DATETIME,
    `last_active_at` DATETIME,
    `password_hash` VARCHAR(255) NOT NULL,
    `salt` VARCHAR(64) NOT NULL,
    `two_factor_enabled` TINYINT DEFAULT 0,
    `two_factor_secret` VARCHAR(64),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建用户设备表（如果不存在）
CREATE TABLE IF NOT EXISTS `user_devices` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `device_id` VARCHAR(128) NOT NULL,
    `device_name` VARCHAR(128),
    `device_type` TINYINT NOT NULL,
    `device_info` JSON,
    `ip_address` VARCHAR(45),
    `location` VARCHAR(128),
    `is_online` TINYINT DEFAULT 0,
    `last_login_at` DATETIME,
    `last_active_at` DATETIME,
    `login_token` VARCHAR(255),
    `refresh_token` VARCHAR(255),
    `token_expires_at` DATETIME,
    `is_trusted` TINYINT DEFAULT 0,
    `disconnected_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建用户隐私设置表（如果不存在）
CREATE TABLE IF NOT EXISTS `user_privacy_settings` (
    `user_id` BIGINT PRIMARY KEY,
    `friend_request_mode` TINYINT DEFAULT 1,
    `allow_search_by_username` TINYINT DEFAULT 1,
    `allow_search_by_phone` TINYINT DEFAULT 0,
    `allow_search_by_email` TINYINT DEFAULT 0,
    `allow_group_invite` TINYINT DEFAULT 1,
    `allow_stranger_message` TINYINT DEFAULT 0,
    `message_read_receipt` TINYINT DEFAULT 1,
    `online_status_visible` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建用户黑名单表（如果不存在）
CREATE TABLE IF NOT EXISTS `user_blacklist` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `blocked_user_id` BIGINT NOT NULL,
    `block_type` TINYINT DEFAULT 1,
    `reason` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试用户数据
INSERT INTO `users` (`username`, `password_hash`, `salt`, `nickname`, `status`, `online_status`) VALUES
('testuser1', 'test_hash_1', 'test_salt_1', '测试用户1', 1, 0),
('testuser2', 'test_hash_2', 'test_salt_2', '测试用户2', 1, 0),
('testuser3', 'test_hash_3', 'test_salt_3', '测试用户3', 1, 0),
('admin', 'admin_hash', 'admin_salt', '管理员', 1, 0);

-- 插入测试设备数据
INSERT INTO `user_devices` (`user_id`, `device_id`, `device_name`, `device_type`, `ip_address`, `is_online`) VALUES
(1, 'device-001', '测试设备1', 1, '127.0.0.1', 1),
(1, 'device-002', '测试设备2', 2, '192.168.1.100', 0),
(2, 'device-003', '测试设备3', 1, '127.0.0.2', 1);

-- 插入测试隐私设置数据
INSERT INTO `user_privacy_settings` (`user_id`, `friend_request_mode`, `allow_search_by_username`, `allow_search_by_phone`, `allow_search_by_email`, `allow_group_invite`, `allow_stranger_message`, `message_read_receipt`, `online_status_visible`) VALUES
(1, 1, 1, 0, 0, 1, 0, 1, 1),
(2, 0, 1, 1, 0, 1, 1, 1, 1),
(3, 2, 0, 0, 0, 0, 0, 0, 0);

-- 插入测试黑名单数据
INSERT INTO `user_blacklist` (`user_id`, `blocked_user_id`, `block_type`, `reason`) VALUES
(1, 4, 1, '测试拉黑'),
(2, 3, 2, '测试拉黑2'); 