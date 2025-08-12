-- IM系统业务数据库优化版 - MySQL建表脚本
-- 基于分布式单体架构设计，包含高级性能优化
-- 维护方：im-business-service
-- 版本：v2.0.0
-- 字符集：utf8mb4，支持emoji和特殊字符

-- 创建数据库
CREATE DATABASE IF NOT EXISTS im_business 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE im_business;

-- ============================================
-- 1. 用户相关表（优化版）
-- ============================================

-- 用户基础信息表（分区表）
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL COMMENT '用户唯一ID',
    username VARCHAR(64) UNIQUE NOT NULL COMMENT '用户名',
    nickname VARCHAR(128) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    phone VARCHAR(32) UNIQUE COMMENT '手机号',
    email VARCHAR(128) UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(128) NOT NULL COMMENT '密码哈希',
    salt VARCHAR(64) NOT NULL COMMENT '密码盐值',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常 2-待激活',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    birthday DATE COMMENT '生日',
    signature VARCHAR(256) COMMENT '个性签名',
    region VARCHAR(64) COMMENT '地区',
    language VARCHAR(16) DEFAULT 'zh-CN' COMMENT '语言偏好',
    timezone VARCHAR(32) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) COMMENT '最后登录IP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext JSON COMMENT '扩展字段',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_region_language (region, language)
) ENGINE=InnoDB 
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
) COMMENT='用户基础信息表（分区表）';

-- 用户设备表（读写分离优化）
CREATE TABLE user_device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '设备唯一标识',
    device_type TINYINT NOT NULL COMMENT '设备类型：1-Web 2-iOS 3-Android 4-Windows 5-Mac',
    device_name VARCHAR(128) COMMENT '设备名称',
    device_model VARCHAR(128) COMMENT '设备型号',
    os_version VARCHAR(64) COMMENT '系统版本',
    app_version VARCHAR(32) COMMENT '应用版本',
    push_token VARCHAR(512) COMMENT '推送Token',
    is_active TINYINT DEFAULT 1 COMMENT '是否活跃：0-否 1-是',
    last_seen_at TIMESTAMP NULL COMMENT '最后活跃时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_device (user_id, device_id),
    INDEX idx_user_id (user_id),
    INDEX idx_device_id (device_id),
    INDEX idx_device_type (device_type),
    INDEX idx_is_active (is_active),
    INDEX idx_last_seen (last_seen_at),
    INDEX idx_user_active (user_id, is_active, last_seen_at)
) ENGINE=InnoDB COMMENT='用户设备表（读写分离优化）';

-- 用户关系表（好友关系，分区优化）
CREATE TABLE user_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_user_id BIGINT NOT NULL COMMENT '好友用户ID',
    relation_type TINYINT NOT NULL COMMENT '关系类型：1-好友 2-黑名单 3-关注',
    remark VARCHAR(128) COMMENT '备注名',
    group_name VARCHAR(64) COMMENT '分组名称',
    status TINYINT DEFAULT 1 COMMENT '状态：0-删除 1-正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_friend (user_id, friend_user_id, relation_type),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_user_id (friend_user_id),
    INDEX idx_relation_type (relation_type),
    INDEX idx_status (status),
    INDEX idx_user_status (user_id, status, relation_type)
) ENGINE=InnoDB 
PARTITION BY HASH(user_id) PARTITIONS 8 COMMENT='用户关系表（哈希分区）';

-- ============================================
-- 2. 会话相关表（分区优化）
-- ============================================

-- 会话表（分区表，按时间分区）
CREATE TABLE conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) UNIQUE NOT NULL COMMENT '会话唯一ID',
    conversation_type TINYINT NOT NULL COMMENT '会话类型：1-单聊 2-群聊 3-系统通知',
    name VARCHAR(128) COMMENT '会话名称（群名）',
    avatar_url VARCHAR(512) COMMENT '会话头像',
    description VARCHAR(512) COMMENT '会话描述',
    owner_user_id BIGINT COMMENT '创建者用户ID',
    member_count INT DEFAULT 0 COMMENT '成员数量',
    max_member_count INT DEFAULT 500 COMMENT '最大成员数量',
    status TINYINT DEFAULT 1 COMMENT '状态：0-解散 1-正常 2-禁用',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开：0-私有 1-公开',
    join_permission TINYINT DEFAULT 1 COMMENT '加入权限：0-禁止 1-自由加入 2-需要审批 3-邀请加入',
    message_permission TINYINT DEFAULT 1 COMMENT '发言权限：0-全员禁言 1-正常 2-仅管理员',
    invite_permission TINYINT DEFAULT 1 COMMENT '邀请权限：0-禁止 1-所有成员 2-仅管理员 3-仅群主',
    history_visible TINYINT DEFAULT 1 COMMENT '历史消息可见：0-不可见 1-部分可见 2-全部可见',
    history_visible_days INT DEFAULT 7 COMMENT '历史消息可见天数',
    last_message_seq BIGINT DEFAULT 0 COMMENT '最后消息序号',
    last_message_at TIMESTAMP NULL COMMENT '最后消息时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext JSON COMMENT '扩展字段',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_conversation_type (conversation_type),
    INDEX idx_owner_user_id (owner_user_id),
    INDEX idx_status (status),
    INDEX idx_is_public (is_public),
    INDEX idx_created_at (created_at),
    INDEX idx_last_message_at (last_message_at),
    INDEX idx_type_status (conversation_type, status, created_at)
) ENGINE=InnoDB 
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
) COMMENT='会话表（分区表）';

-- 会话成员表（读写分离优化）
CREATE TABLE conversation_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    member_role TINYINT DEFAULT 0 COMMENT '成员角色：0-普通成员 1-管理员 2-群主',
    nickname VARCHAR(128) COMMENT '群昵称',
    join_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    last_read_seq BIGINT DEFAULT 0 COMMENT '最后已读序号',
    last_read_at TIMESTAMP NULL COMMENT '最后已读时间',
    mute_until TIMESTAMP NULL COMMENT '禁言到期时间',
    is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    is_disturb_free TINYINT DEFAULT 0 COMMENT '是否免打扰：0-否 1-是',
    is_show_nickname TINYINT DEFAULT 1 COMMENT '是否显示群昵称：0-否 1-是',
    status TINYINT DEFAULT 1 COMMENT '状态：0-已退出 1-正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext JSON COMMENT '扩展字段',
    UNIQUE KEY uk_conversation_user (conversation_id, user_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_member_role (member_role),
    INDEX idx_join_at (join_at),
    INDEX idx_status (status),
    INDEX idx_last_read_seq (last_read_seq),
    INDEX idx_conv_user_status (conversation_id, user_id, status),
    INDEX idx_user_conv (user_id, conversation_id, status)
) ENGINE=InnoDB COMMENT='会话成员表（读写分离优化）';

-- 会话设置表
CREATE TABLE conversation_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    setting_key VARCHAR(64) NOT NULL COMMENT '设置键',
    setting_value TEXT COMMENT '设置值',
    setting_type TINYINT DEFAULT 1 COMMENT '设置类型：1-字符串 2-数字 3-布尔 4-JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_setting (conversation_id, setting_key),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_setting_key (setting_key)
) ENGINE=InnoDB COMMENT='会话设置表';

-- ============================================
-- 3. 群组相关表
-- ============================================

-- 群组基础信息表
CREATE TABLE group_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) UNIQUE NOT NULL COMMENT '群组唯一ID',
    conversation_id VARCHAR(64) UNIQUE NOT NULL COMMENT '关联的会话ID',
    group_name VARCHAR(128) NOT NULL COMMENT '群组名称',
    group_avatar VARCHAR(512) COMMENT '群组头像',
    group_description TEXT COMMENT '群组描述',
    group_announcement TEXT COMMENT '群组公告',
    group_type TINYINT DEFAULT 1 COMMENT '群组类型：1-普通群 2-企业群 3-官方群 4-临时群',
    owner_user_id BIGINT NOT NULL COMMENT '群主用户ID',
    member_count INT DEFAULT 0 COMMENT '当前成员数',
    max_member_count INT DEFAULT 500 COMMENT '最大成员数',
    status TINYINT DEFAULT 1 COMMENT '状态：0-解散 1-正常 2-禁用 3-待审核',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开群：0-私有 1-公开',
    join_approval TINYINT DEFAULT 1 COMMENT '加群审批：0-禁止加群 1-自由加入 2-需要审批 3-仅邀请',
    invite_approval TINYINT DEFAULT 0 COMMENT '邀请审批：0-无需审批 1-需要审批',
    member_invite TINYINT DEFAULT 1 COMMENT '成员邀请权限：0-禁止 1-允许 2-仅管理员 3-仅群主',
    at_all_permission TINYINT DEFAULT 2 COMMENT '@全体权限：0-禁用 1-所有人 2-仅管理员 3-仅群主',
    message_permission TINYINT DEFAULT 1 COMMENT '发言权限：0-全员禁言 1-正常发言 2-仅管理员',
    history_visible TINYINT DEFAULT 1 COMMENT '历史消息可见性：0-不可见 1-加入后可见 2-全部可见',
    history_visible_days INT DEFAULT 0 COMMENT '历史消息可见天数，0表示无限制',
    mute_all TINYINT DEFAULT 0 COMMENT '是否全员禁言：0-否 1-是',
    mute_all_except_admin TINYINT DEFAULT 0 COMMENT '禁言例外管理员：0-否 1-是',
    allow_member_modify_info TINYINT DEFAULT 0 COMMENT '允许成员修改群信息：0-否 1-是',
    allow_member_pin_message TINYINT DEFAULT 0 COMMENT '允许成员置顶消息：0-否 1-是',
    auto_approve_join TINYINT DEFAULT 0 COMMENT '自动审批加群：0-否 1-是',
    join_question TEXT COMMENT '入群验证问题',
    join_answer TEXT COMMENT '入群验证答案',
    group_tags JSON COMMENT '群组标签',
    location VARCHAR(128) COMMENT '群组位置',
    qr_code VARCHAR(512) COMMENT '群组二维码',
    invite_link VARCHAR(512) COMMENT '邀请链接',
    link_expires_at TIMESTAMP NULL COMMENT '链接过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext JSON COMMENT '扩展字段',
    INDEX idx_group_id (group_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_group_name (group_name),
    INDEX idx_group_type (group_type),
    INDEX idx_owner_user_id (owner_user_id),
    INDEX idx_status (status),
    INDEX idx_is_public (is_public),
    INDEX idx_join_approval (join_approval),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='群组基础信息表';

-- 群组公告表
CREATE TABLE group_announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    announcement_id VARCHAR(64) UNIQUE NOT NULL COMMENT '公告唯一ID',
    title VARCHAR(256) COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    publisher_user_id BIGINT NOT NULL COMMENT '发布者用户ID',
    is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    is_notify_all TINYINT DEFAULT 0 COMMENT '是否@全体：0-否 1-是',
    read_required TINYINT DEFAULT 0 COMMENT '是否必读：0-否 1-是',
    status TINYINT DEFAULT 1 COMMENT '状态：0-草稿 1-已发布 2-已撤回',
    published_at TIMESTAMP NULL COMMENT '发布时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_group_id (group_id),
    INDEX idx_announcement_id (announcement_id),
    INDEX idx_publisher_user_id (publisher_user_id),
    INDEX idx_is_pinned (is_pinned),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB COMMENT='群组公告表';

-- 群组公告阅读记录表
CREATE TABLE group_announcement_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    announcement_id VARCHAR(64) NOT NULL COMMENT '公告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    UNIQUE KEY uk_announcement_user (announcement_id, user_id),
    INDEX idx_announcement_id (announcement_id),
    INDEX idx_user_id (user_id),
    INDEX idx_read_at (read_at)
) ENGINE=InnoDB COMMENT='群组公告阅读记录表';

-- 群组邀请记录表
CREATE TABLE group_invite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_id VARCHAR(64) UNIQUE NOT NULL COMMENT '邀请唯一ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    inviter_user_id BIGINT NOT NULL COMMENT '邀请人用户ID',
    invitee_user_id BIGINT COMMENT '被邀请人用户ID（可为空，如链接邀请）',
    invite_type TINYINT NOT NULL COMMENT '邀请类型：1-直接邀请 2-链接邀请 3-二维码邀请',
    invite_message VARCHAR(512) COMMENT '邀请消息',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理 1-已同意 2-已拒绝 3-已过期 4-已撤回',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    processed_at TIMESTAMP NULL COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invite_id (invite_id),
    INDEX idx_group_id (group_id),
    INDEX idx_inviter_user_id (inviter_user_id),
    INDEX idx_invitee_user_id (invitee_user_id),
    INDEX idx_invite_type (invite_type),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='群组邀请记录表';

-- 群组加入申请表
CREATE TABLE group_join_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(64) UNIQUE NOT NULL COMMENT '申请唯一ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '申请人用户ID',
    request_message VARCHAR(512) COMMENT '申请消息',
    verification_answer TEXT COMMENT '验证问题答案',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待审核 1-已同意 2-已拒绝 3-已撤回',
    approver_user_id BIGINT COMMENT '审批人用户ID',
    approve_message VARCHAR(512) COMMENT '审批消息',
    processed_at TIMESTAMP NULL COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_request_id (request_id),
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_approver_user_id (approver_user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='群组加入申请表';

-- 群组禁言记录表
CREATE TABLE group_mute_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mute_id VARCHAR(64) UNIQUE NOT NULL COMMENT '禁言记录ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '被禁言用户ID',
    operator_user_id BIGINT NOT NULL COMMENT '操作人用户ID',
    mute_type TINYINT NOT NULL COMMENT '禁言类型：1-临时禁言 2-永久禁言',
    mute_reason VARCHAR(512) COMMENT '禁言原因',
    mute_duration INT COMMENT '禁言时长（分钟），0表示永久',
    muted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '禁言时间',
    unmute_at TIMESTAMP NULL COMMENT '解禁时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0-已解除 1-生效中',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mute_id (mute_id),
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    INDEX idx_operator_user_id (operator_user_id),
    INDEX idx_mute_type (mute_type),
    INDEX idx_status (status),
    INDEX idx_muted_at (muted_at),
    INDEX idx_unmute_at (unmute_at)
) ENGINE=InnoDB COMMENT='群组禁言记录表';

-- 群组文件中心表
CREATE TABLE group_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) UNIQUE NOT NULL COMMENT '文件唯一ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    uploader_user_id BIGINT NOT NULL COMMENT '上传者用户ID',
    file_name VARCHAR(256) NOT NULL COMMENT '文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    file_type VARCHAR(64) NOT NULL COMMENT '文件类型',
    file_path VARCHAR(512) NOT NULL COMMENT '文件路径',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    status TINYINT DEFAULT 1 COMMENT '状态：0-已删除 1-正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id),
    INDEX idx_group_id (group_id),
    INDEX idx_uploader_user_id (uploader_user_id),
    INDEX idx_file_type (file_type),
    INDEX idx_is_pinned (is_pinned),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='群组文件中心表';

-- ============================================
-- 4. 权限相关表
-- ============================================

-- 权限定义表
CREATE TABLE permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(64) UNIQUE NOT NULL COMMENT '权限代码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    permission_desc VARCHAR(256) COMMENT '权限描述',
    permission_type TINYINT NOT NULL COMMENT '权限类型：1-系统权限 2-会话权限 3-功能权限',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_permission_code (permission_code),
    INDEX idx_permission_type (permission_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB COMMENT='权限定义表';

-- 角色表
CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) UNIQUE NOT NULL COMMENT '角色代码',
    role_name VARCHAR(128) NOT NULL COMMENT '角色名称',
    role_desc VARCHAR(256) COMMENT '角色描述',
    role_type TINYINT NOT NULL COMMENT '角色类型：1-系统角色 2-会话角色',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_code (role_code),
    INDEX idx_role_type (role_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB COMMENT='角色表';

-- 角色权限关联表
CREATE TABLE role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- 用户角色表
CREATE TABLE user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    scope_type TINYINT DEFAULT 1 COMMENT '作用域类型：1-全局 2-会话',
    scope_id VARCHAR(64) COMMENT '作用域ID（会话ID等）',
    granted_by BIGINT COMMENT '授权人用户ID',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    is_active TINYINT DEFAULT 1 COMMENT '是否活跃：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role_scope (user_id, role_id, scope_type, scope_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    INDEX idx_scope_type_id (scope_type, scope_id),
    INDEX idx_granted_by (granted_by),
    INDEX idx_is_active (is_active),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB COMMENT='用户角色表';

-- ============================================
-- 5. 媒体相关表
-- ============================================

-- 媒体文件表
CREATE TABLE media_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    media_id VARCHAR(64) UNIQUE NOT NULL COMMENT '媒体文件唯一ID',
    file_name VARCHAR(256) NOT NULL COMMENT '文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(64) NOT NULL COMMENT '文件类型（MIME）',
    file_extension VARCHAR(16) COMMENT '文件扩展名',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件哈希值（MD5/SHA256）',
    storage_type TINYINT DEFAULT 1 COMMENT '存储类型：1-本地 2-MinIO 3-云存储',
    storage_path VARCHAR(512) NOT NULL COMMENT '存储路径',
    storage_bucket VARCHAR(128) COMMENT '存储桶名称',
    url VARCHAR(512) COMMENT '访问URL',
    thumbnail_url VARCHAR(512) COMMENT '缩略图URL',
    width INT COMMENT '宽度（图片/视频）',
    height INT COMMENT '高度（图片/视频）',
    duration INT COMMENT '时长（音频/视频，秒）',
    uploader_user_id BIGINT NOT NULL COMMENT '上传者用户ID',
    upload_ip VARCHAR(64) COMMENT '上传IP',
    upload_device_id VARCHAR(128) COMMENT '上传设备ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0-上传中 1-正常 2-处理中 3-处理失败 4-已删除',
    scan_status TINYINT DEFAULT 0 COMMENT '扫描状态：0-未扫描 1-安全 2-可疑 3-违规',
    scan_result JSON COMMENT '扫描结果详情',
    reference_count INT DEFAULT 0 COMMENT '引用次数',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext JSON COMMENT '扩展字段',
    INDEX idx_media_id (media_id),
    INDEX idx_file_hash (file_hash),
    INDEX idx_file_type (file_type),
    INDEX idx_uploader_user_id (uploader_user_id),
    INDEX idx_status (status),
    INDEX idx_scan_status (scan_status),
    INDEX idx_created_at (created_at),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB COMMENT='媒体文件表';

-- 媒体处理任务表
CREATE TABLE media_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) UNIQUE NOT NULL COMMENT '任务唯一ID',
    media_id VARCHAR(64) NOT NULL COMMENT '媒体文件ID',
    task_type TINYINT NOT NULL COMMENT '任务类型：1-缩略图生成 2-格式转换 3-内容扫描 4-水印添加',
    task_status TINYINT DEFAULT 0 COMMENT '任务状态：0-待处理 1-处理中 2-已完成 3-失败',
    task_params JSON COMMENT '任务参数',
    task_result JSON COMMENT '任务结果',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT DEFAULT 3 COMMENT '最大重试次数',
    error_message TEXT COMMENT '错误信息',
    started_at TIMESTAMP NULL COMMENT '开始处理时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_media_id (media_id),
    INDEX idx_task_type (task_type),
    INDEX idx_task_status (task_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='媒体处理任务表';

-- ============================================
-- 6. 配置相关表
-- ============================================

-- 系统配置表
CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(128) UNIQUE NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type TINYINT DEFAULT 1 COMMENT '配置类型：1-字符串 2-数字 3-布尔 4-JSON',
    config_desc VARCHAR(256) COMMENT '配置描述',
    is_encrypted TINYINT DEFAULT 0 COMMENT '是否加密：0-否 1-是',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key),
    INDEX idx_config_type (config_type),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB COMMENT='系统配置表';

-- 用户配置表
CREATE TABLE user_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type TINYINT DEFAULT 1 COMMENT '配置类型：1-字符串 2-数字 3-布尔 4-JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_config (user_id, config_key),
    INDEX idx_user_id (user_id),
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB COMMENT='用户配置表';

-- ============================================
-- 7. 审计和日志表
-- ============================================

-- 操作日志表
CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id VARCHAR(64) UNIQUE NOT NULL COMMENT '日志唯一ID',
    user_id BIGINT COMMENT '操作用户ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(256) COMMENT '操作描述',
    resource_type VARCHAR(64) COMMENT '资源类型',
    resource_id VARCHAR(64) COMMENT '资源ID',
    request_id VARCHAR(64) COMMENT '请求ID',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '用户代理',
    device_id VARCHAR(128) COMMENT '设备ID',
    operation_result TINYINT DEFAULT 1 COMMENT '操作结果：0-失败 1-成功',
    error_code VARCHAR(32) COMMENT '错误码',
    error_message TEXT COMMENT '错误信息',
    operation_data JSON COMMENT '操作数据',
    execution_time INT COMMENT '执行时间（毫秒）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_id (log_id),
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_resource_type_id (resource_type, resource_id),
    INDEX idx_request_id (request_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_created_at (created_at),
    INDEX idx_operation_result (operation_result)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 登录日志表
CREATE TABLE login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    login_type TINYINT NOT NULL COMMENT '登录类型：1-密码 2-短信 3-第三方',
    device_id VARCHAR(128) COMMENT '设备ID',
    device_type TINYINT COMMENT '设备类型：1-Web 2-iOS 3-Android 4-Windows 5-Mac',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    location VARCHAR(128) COMMENT '登录地点',
    user_agent VARCHAR(512) COMMENT '用户代理',
    login_result TINYINT DEFAULT 1 COMMENT '登录结果：0-失败 1-成功',
    failure_reason VARCHAR(128) COMMENT '失败原因',
    session_id VARCHAR(128) COMMENT '会话ID',
    logout_at TIMESTAMP NULL COMMENT '登出时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_login_type (login_type),
    INDEX idx_device_id (device_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_login_result (login_result),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='登录日志表';

-- ============================================
-- 8. 收藏与标记相关表
-- ============================================

-- 收藏夹表
CREATE TABLE user_collection_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    folder_id VARCHAR(64) UNIQUE NOT NULL COMMENT '收藏夹唯一ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    folder_name VARCHAR(128) NOT NULL COMMENT '收藏夹名称',
    folder_type TINYINT DEFAULT 1 COMMENT '收藏夹类型：1-默认 2-自定义',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    is_exportable TINYINT DEFAULT 1 COMMENT '是否允许导出：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_folder_type (folder_type),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB COMMENT='收藏夹表';

-- 收藏内容表
CREATE TABLE user_collection_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    collection_id VARCHAR(64) UNIQUE NOT NULL COMMENT '收藏唯一ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    folder_id VARCHAR(64) COMMENT '收藏夹ID',
    item_type TINYINT NOT NULL COMMENT '收藏类型：1-消息 2-图片 3-文件 4-链接 5-代码片段',
    source_type TINYINT NOT NULL COMMENT '来源类型：1-单聊 2-群聊 3-系统',
    source_id VARCHAR(64) NOT NULL COMMENT '来源ID（会话ID或消息ID）',
    content TEXT COMMENT '收藏内容',
    media_ref JSON COMMENT '媒体引用信息',
    tags JSON COMMENT '标签数组',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_collection_id (collection_id),
    INDEX idx_user_id (user_id),
    INDEX idx_folder_id (folder_id),
    INDEX idx_item_type (item_type),
    INDEX idx_source_type_id (source_type, source_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='收藏内容表';

-- 消息标记表
CREATE TABLE message_mark (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mark_id VARCHAR(64) UNIQUE NOT NULL COMMENT '标记唯一ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    mark_type TINYINT NOT NULL COMMENT '标记类型：1-重点 2-待办 3-自定义',
    mark_content VARCHAR(256) COMMENT '标记内容',
    is_synced_to_task TINYINT DEFAULT 0 COMMENT '是否同步到任务中心：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mark_id (mark_id),
    INDEX idx_user_id (user_id),
    INDEX idx_message_id (message_id),
    INDEX idx_mark_type (mark_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='消息标记表';

-- ============================================
-- 9. 搜索与检索相关表
-- ============================================

-- 搜索历史表
CREATE TABLE user_search_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    history_id VARCHAR(64) UNIQUE NOT NULL COMMENT '搜索历史唯一ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    search_type TINYINT NOT NULL COMMENT '搜索类型：1-全局搜索 2-会话内搜索 3-收藏搜索',
    search_keyword VARCHAR(256) NOT NULL COMMENT '搜索关键词',
    search_filters JSON COMMENT '搜索过滤条件',
    result_count INT DEFAULT 0 COMMENT '搜索结果数量',
    search_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    INDEX idx_history_id (history_id),
    INDEX idx_user_id (user_id),
    INDEX idx_search_type (search_type),
    INDEX idx_search_keyword (search_keyword),
    INDEX idx_search_time (search_time)
) ENGINE=InnoDB COMMENT='搜索历史表';

-- 搜索索引配置表
CREATE TABLE search_index_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_id VARCHAR(64) UNIQUE NOT NULL COMMENT '配置唯一ID',
    index_type VARCHAR(64) NOT NULL COMMENT '索引类型：message/user/group/file',
    index_strategy TINYINT DEFAULT 1 COMMENT '索引策略：1-云端 2-本地 3-混合',
    local_capacity BIGINT DEFAULT 1073741824 COMMENT '本地索引容量（字节）',
    cleanup_policy VARCHAR(64) DEFAULT '7d' COMMENT '清理策略：7d/30d/90d',
    offline_search_range VARCHAR(64) DEFAULT '30d' COMMENT '离线搜索范围',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_id (config_id),
    INDEX idx_index_type (index_type),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB COMMENT='搜索索引配置表';

-- ============================================
-- 10. 通知与状态相关表
-- ============================================

-- 通知配置表
CREATE TABLE user_notification_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL COMMENT '用户ID',
    notification_type TINYINT NOT NULL COMMENT '通知类型：1-普通 2-仅@我 3-免打扰',
    work_time_start TIME DEFAULT '09:00:00' COMMENT '工作时间开始',
    work_time_end TIME DEFAULT '18:00:00' COMMENT '工作时间结束',
    work_time_enabled TINYINT DEFAULT 1 COMMENT '工作时间策略：0-关闭 1-开启',
    frontend_focus_suppress TINYINT DEFAULT 1 COMMENT '前台聚焦抑制：0-关闭 1-开启',
    badge_count_enabled TINYINT DEFAULT 1 COMMENT '徽标计数：0-关闭 1-开启',
    read_receipt_enabled TINYINT DEFAULT 1 COMMENT '已读回执：0-关闭 1-开启',
    show_read_count TINYINT DEFAULT 1 COMMENT '显示已读人数：0-关闭 1-开启',
    online_status_source TINYINT DEFAULT 1 COMMENT '在线状态来源：1-手动 2-系统日历 3-第三方',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type)
) ENGINE=InnoDB COMMENT='通知配置表';

-- 通知记录表
CREATE TABLE user_notification_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id VARCHAR(64) UNIQUE NOT NULL COMMENT '通知唯一ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    notification_type TINYINT NOT NULL COMMENT '通知类型：1-消息 2-好友申请 3-群邀请 4-系统通知',
    title VARCHAR(256) COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    source_type VARCHAR(64) COMMENT '来源类型',
    source_id VARCHAR(64) COMMENT '来源ID',
    priority TINYINT DEFAULT 2 COMMENT '优先级：1-高 2-中 3-低',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0-否 1-是',
    read_at TIMESTAMP NULL COMMENT '已读时间',
    is_sent TINYINT DEFAULT 0 COMMENT '是否已发送：0-否 1-是',
    sent_at TIMESTAMP NULL COMMENT '发送时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_id (notification_id),
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_is_read (is_read),
    INDEX idx_priority (priority),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='通知记录表';

-- ============================================
-- 11. 安全与合规相关表
-- ============================================

-- 敏感词配置表
CREATE TABLE sensitive_word_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_id VARCHAR(64) UNIQUE NOT NULL COMMENT '配置唯一ID',
    word_pattern VARCHAR(256) NOT NULL COMMENT '敏感词模式',
    word_type TINYINT NOT NULL COMMENT '敏感词类型：1-政治 2-色情 3-暴力 4-其他',
    action_type TINYINT DEFAULT 1 COMMENT '处理动作：1-拦截 2-提醒 3-替换',
    replacement_word VARCHAR(256) COMMENT '替换词',
    risk_level TINYINT DEFAULT 2 COMMENT '风险等级：1-低 2-中 3-高',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_id (config_id),
    INDEX idx_word_pattern (word_pattern),
    INDEX idx_word_type (word_type),
    INDEX idx_action_type (action_type),
    INDEX idx_risk_level (risk_level),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB COMMENT='敏感词配置表';

-- 内容安全扫描记录表
CREATE TABLE content_security_scan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scan_id VARCHAR(64) UNIQUE NOT NULL COMMENT '扫描唯一ID',
    content_type TINYINT NOT NULL COMMENT '内容类型：1-文本 2-图片 3-视频 4-文件',
    content_id VARCHAR(64) NOT NULL COMMENT '内容ID',
    scan_result TINYINT NOT NULL COMMENT '扫描结果：1-安全 2-可疑 3-违规',
    risk_score DECIMAL(3,2) DEFAULT 0.00 COMMENT '风险评分：0.00-1.00',
    detected_issues JSON COMMENT '检测到的问题',
    scan_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '扫描时间',
    INDEX idx_scan_id (scan_id),
    INDEX idx_content_type (content_type),
    INDEX idx_content_id (content_id),
    INDEX idx_scan_result (scan_result),
    INDEX idx_risk_score (risk_score),
    INDEX idx_scan_time (scan_time)
) ENGINE=InnoDB COMMENT='内容安全扫描记录表';

-- 审计日志表
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    audit_id VARCHAR(64) UNIQUE NOT NULL COMMENT '审计唯一ID',
    user_id BIGINT COMMENT '操作用户ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(512) COMMENT '操作描述',
    resource_type VARCHAR(64) COMMENT '资源类型',
    resource_id VARCHAR(64) COMMENT '资源ID',
    old_value JSON COMMENT '操作前值',
    new_value JSON COMMENT '操作后值',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '用户代理',
    device_id VARCHAR(128) COMMENT '设备ID',
    trace_id VARCHAR(128) COMMENT '链路追踪ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_id (audit_id),
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_resource_type_id (resource_type, resource_id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='审计日志表';

-- ============================================
-- 12. 诊断与性能相关表
-- ============================================

-- 系统诊断记录表
CREATE TABLE system_diagnostic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    diagnostic_id VARCHAR(64) UNIQUE NOT NULL COMMENT '诊断唯一ID',
    node_id VARCHAR(64) NOT NULL COMMENT '节点ID',
    diagnostic_type VARCHAR(64) NOT NULL COMMENT '诊断类型：connection/performance/error',
    connection_status TINYINT DEFAULT 1 COMMENT '连接状态：0-异常 1-正常',
    reconnect_count INT DEFAULT 0 COMMENT '重连次数',
    message_queue_length INT DEFAULT 0 COMMENT '消息队列长度',
    service_addresses JSON COMMENT '服务地址列表',
    performance_metrics JSON COMMENT '性能指标',
    error_logs JSON COMMENT '错误日志',
    diagnostic_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '诊断时间',
    INDEX idx_diagnostic_id (diagnostic_id),
    INDEX idx_node_id (node_id),
    INDEX idx_diagnostic_type (diagnostic_type),
    INDEX idx_connection_status (connection_status),
    INDEX idx_diagnostic_time (diagnostic_time)
) ENGINE=InnoDB COMMENT='系统诊断记录表';

-- 性能监控记录表
CREATE TABLE performance_monitor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    monitor_id VARCHAR(64) UNIQUE NOT NULL COMMENT '监控唯一ID',
    metric_name VARCHAR(128) NOT NULL COMMENT '指标名称',
    metric_value DECIMAL(10,4) NOT NULL COMMENT '指标值',
    metric_unit VARCHAR(32) COMMENT '指标单位',
    node_id VARCHAR(64) COMMENT '节点ID',
    service_name VARCHAR(64) COMMENT '服务名称',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    INDEX idx_monitor_id (monitor_id),
    INDEX idx_metric_name (metric_name),
    INDEX idx_node_id (node_id),
    INDEX idx_service_name (service_name),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB COMMENT='性能监控记录表';

-- ============================================
-- 3. 在线状态持久化表（新增）
-- ============================================

-- 在线状态历史表（分区表）
CREATE TABLE presence_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '设备ID',
    node_id VARCHAR(64) NOT NULL COMMENT '节点ID',
    presence_type TINYINT NOT NULL COMMENT '状态类型：1-上线 2-下线 3-状态变更',
    old_status VARCHAR(32) COMMENT '原状态',
    new_status VARCHAR(32) NOT NULL COMMENT '新状态',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '用户代理',
    location VARCHAR(128) COMMENT '地理位置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_device (user_id, device_id),
    INDEX idx_node_id (node_id),
    INDEX idx_presence_type (presence_type),
    INDEX idx_created_at (created_at),
    INDEX idx_user_time (user_id, created_at)
) ENGINE=InnoDB 
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
) COMMENT='在线状态历史表（分区表）';

-- 在线状态快照表（读写分离）
CREATE TABLE presence_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '设备ID',
    node_id VARCHAR(64) NOT NULL COMMENT '节点ID',
    status VARCHAR(32) NOT NULL COMMENT '当前状态',
    last_seen_at TIMESTAMP NOT NULL COMMENT '最后活跃时间',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '用户代理',
    location VARCHAR(128) COMMENT '地理位置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_device (user_id, device_id),
    INDEX idx_user_id (user_id),
    INDEX idx_device_id (device_id),
    INDEX idx_node_id (node_id),
    INDEX idx_status (status),
    INDEX idx_last_seen (last_seen_at),
    INDEX idx_user_status (user_id, status, last_seen_at)
) ENGINE=InnoDB COMMENT='在线状态快照表（读写分离）';

-- ============================================
-- 4. 性能监控表（新增）
-- ============================================

-- 客户端性能监控表
CREATE TABLE client_performance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '设备ID',
    metric_name VARCHAR(64) NOT NULL COMMENT '指标名称',
    metric_value DECIMAL(10,4) NOT NULL COMMENT '指标值',
    metric_unit VARCHAR(32) COMMENT '指标单位',
    client_version VARCHAR(32) COMMENT '客户端版本',
    os_version VARCHAR(64) COMMENT '操作系统版本',
    network_type VARCHAR(32) COMMENT '网络类型',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_device (user_id, device_id),
    INDEX idx_metric_name (metric_name),
    INDEX idx_timestamp (timestamp),
    INDEX idx_user_metric_time (user_id, metric_name, timestamp)
) ENGINE=InnoDB 
PARTITION BY RANGE (YEAR(timestamp)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
) COMMENT='客户端性能监控表（分区表）';

-- 本地索引配置表（新增）
CREATE TABLE local_index_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(128) NOT NULL COMMENT '设备ID',
    index_type VARCHAR(64) NOT NULL COMMENT '索引类型：message/user/group/file',
    index_strategy TINYINT DEFAULT 1 COMMENT '索引策略：1-云端 2-本地 3-混合',
    local_capacity BIGINT DEFAULT 1073741824 COMMENT '本地索引容量（字节）',
    current_size BIGINT DEFAULT 0 COMMENT '当前索引大小（字节）',
    cleanup_policy VARCHAR(64) DEFAULT '7d' COMMENT '清理策略：7d/30d/90d',
    offline_search_range VARCHAR(64) DEFAULT '30d' COMMENT '离线搜索范围',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-否 1-是',
    last_sync_at TIMESTAMP NULL COMMENT '最后同步时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_device_type (user_id, device_id, index_type),
    INDEX idx_user_id (user_id),
    INDEX idx_device_id (device_id),
    INDEX idx_index_type (index_type),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB COMMENT='本地索引配置表';

-- ============================================
-- 5. 数据安全增强表（新增）
-- ============================================

-- 数据分类表
CREATE TABLE data_classification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(64) NOT NULL COMMENT '表名',
    column_name VARCHAR(64) NOT NULL COMMENT '字段名',
    data_type VARCHAR(32) NOT NULL COMMENT '数据类型',
    sensitivity_level TINYINT NOT NULL COMMENT '敏感级别：1-公开 2-内部 3-机密 4-绝密',
    classification_reason VARCHAR(256) COMMENT '分类原因',
    retention_policy VARCHAR(64) COMMENT '保留策略',
    encryption_required TINYINT DEFAULT 0 COMMENT '是否需要加密：0-否 1-是',
    masking_required TINYINT DEFAULT 0 COMMENT '是否需要脱敏：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_table_column (table_name, column_name),
    INDEX idx_sensitivity_level (sensitivity_level),
    INDEX idx_encryption_required (encryption_required),
    INDEX idx_masking_required (masking_required)
) ENGINE=InnoDB COMMENT='数据分类表';

-- 数据脱敏配置表
CREATE TABLE data_masking_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(64) NOT NULL COMMENT '表名',
    column_name VARCHAR(64) NOT NULL COMMENT '字段名',
    masking_type VARCHAR(32) NOT NULL COMMENT '脱敏类型：hash/partial/random/null',
    masking_rule TEXT COMMENT '脱敏规则（JSON格式）',
    masking_params JSON COMMENT '脱敏参数',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_table_column (table_name, column_name),
    INDEX idx_masking_type (masking_type),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB COMMENT='数据脱敏配置表';

-- 数据访问审计表（增强版）
CREATE TABLE data_access_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    audit_id VARCHAR(64) UNIQUE NOT NULL COMMENT '审计唯一ID',
    user_id BIGINT COMMENT '访问用户ID',
    access_type VARCHAR(32) NOT NULL COMMENT '访问类型：read/write/export/delete',
    table_name VARCHAR(64) COMMENT '访问表名',
    column_name VARCHAR(64) COMMENT '访问字段名',
    record_id VARCHAR(64) COMMENT '记录ID',
    access_result TINYINT DEFAULT 1 COMMENT '访问结果：0-拒绝 1-允许 2-部分允许',
    access_reason VARCHAR(256) COMMENT '访问原因',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '用户代理',
    device_id VARCHAR(128) COMMENT '设备ID',
    trace_id VARCHAR(128) COMMENT '链路追踪ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_id (audit_id),
    INDEX idx_user_id (user_id),
    INDEX idx_access_type (access_type),
    INDEX idx_table_column (table_name, column_name),
    INDEX idx_trace_id (trace_id),
    INDEX idx_created_at (created_at),
    INDEX idx_user_access_time (user_id, access_type, created_at)
) ENGINE=InnoDB 
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
) COMMENT='数据访问审计表（分区表）';

-- ============================================
-- 6. 读写分离视图（新增）
-- ============================================

-- 活跃用户视图（读优化）
CREATE VIEW v_active_users AS
SELECT 
    u.user_id,
    u.username,
    u.nickname,
    u.avatar_url,
    u.status,
    u.last_login_at,
    ps.status as presence_status,
    ps.last_seen_at
FROM user u
LEFT JOIN presence_snapshot ps ON u.user_id = ps.user_id
WHERE u.status = 1
AND (ps.last_seen_at > DATE_SUB(NOW(), INTERVAL 7 DAY) OR ps.last_seen_at IS NULL);

-- 会话统计视图（读优化）
CREATE VIEW v_conversation_stats AS
SELECT 
    c.conversation_id,
    c.conversation_type,
    c.name,
    c.member_count,
    c.last_message_at,
    COUNT(cm.user_id) as online_members,
    MAX(cm.last_read_at) as last_activity
FROM conversation c
LEFT JOIN conversation_member cm ON c.conversation_id = cm.conversation_id
LEFT JOIN presence_snapshot ps ON cm.user_id = ps.user_id
WHERE c.status = 1
GROUP BY c.conversation_id, c.conversation_type, c.name, c.member_count, c.last_message_at;

-- 性能指标汇总视图（读优化）
CREATE VIEW v_performance_summary AS
SELECT 
    DATE(timestamp) as date,
    metric_name,
    AVG(metric_value) as avg_value,
    MAX(metric_value) as max_value,
    MIN(metric_value) as min_value,
    COUNT(*) as sample_count
FROM client_performance
WHERE timestamp > DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(timestamp), metric_name;

-- ============================================
-- 7. 存储过程优化（新增）
-- ============================================

DELIMITER $$

-- 数据清理存储过程（增强版）
CREATE PROCEDURE CleanExpiredData()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE table_name VARCHAR(64);
    DECLARE partition_name VARCHAR(64);
    DECLARE cur CURSOR FOR 
        SELECT TABLE_NAME FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = 'im_business' 
        AND TABLE_NAME LIKE '%_p2023';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO table_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 删除过期分区
        SET @sql = CONCAT('ALTER TABLE ', table_name, ' DROP PARTITION p2023');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
    END LOOP;
    CLOSE cur;
END$$

-- 性能数据聚合存储过程
CREATE PROCEDURE AggregatePerformanceData()
BEGIN
    -- 清理30天前的详细数据
    DELETE FROM client_performance 
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    -- 清理90天前的在线状态历史
    DELETE FROM presence_history 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
    
    -- 清理365天前的审计日志
    DELETE FROM data_access_audit 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 365 DAY);
END$$

-- 数据脱敏处理存储过程
CREATE PROCEDURE ApplyDataMasking(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64)
)
BEGIN
    DECLARE masking_type VARCHAR(32);
    DECLARE masking_rule TEXT;
    
    -- 获取脱敏配置
    SELECT masking_type, masking_rule 
    INTO masking_type, masking_rule
    FROM data_masking_config 
    WHERE table_name = p_table_name 
    AND column_name = p_column_name 
    AND is_enabled = 1;
    
    IF masking_type IS NOT NULL THEN
        -- 根据脱敏类型应用不同规则
        CASE masking_type
            WHEN 'hash' THEN
                SET @sql = CONCAT('UPDATE ', p_table_name, ' SET ', p_column_name, ' = MD5(', p_column_name, ')');
            WHEN 'partial' THEN
                SET @sql = CONCAT('UPDATE ', p_table_name, ' SET ', p_column_name, ' = CONCAT(LEFT(', p_column_name, ', 3), "***", RIGHT(', p_column_name, ', 2))');
            WHEN 'null' THEN
                SET @sql = CONCAT('UPDATE ', p_table_name, ' SET ', p_column_name, ' = NULL');
            ELSE
                SET @sql = CONCAT('UPDATE ', p_table_name, ' SET ', p_column_name, ' = "***"');
        END CASE;
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- ============================================
-- 8. 事件调度器（新增）
-- ============================================

-- 启用事件调度器
SET GLOBAL event_scheduler = ON;

-- 创建定期清理事件
CREATE EVENT IF NOT EXISTS e_cleanup_expired_data
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    CALL CleanExpiredData();
    CALL AggregatePerformanceData();
END;

-- 创建性能数据聚合事件
CREATE EVENT IF NOT EXISTS e_aggregate_performance
ON SCHEDULE EVERY 1 HOUR
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    -- 聚合小时级性能数据
    INSERT INTO performance_monitor (metric_name, metric_value, metric_unit, service_name, timestamp)
    SELECT 
        metric_name,
        AVG(metric_value) as avg_value,
        metric_unit,
        'client' as service_name,
        DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00') as hour_timestamp
    FROM client_performance
    WHERE timestamp > DATE_SUB(NOW(), INTERVAL 1 HOUR)
    GROUP BY metric_name, metric_unit, DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00');
END;

-- ============================================
-- 9. 初始化数据
-- ============================================

-- 插入数据分类配置
INSERT INTO data_classification (table_name, column_name, data_type, sensitivity_level, classification_reason, retention_policy) VALUES
('user', 'password_hash', 'hash', 4, '用户密码哈希，绝密信息', '永久保留'),
('user', 'phone', 'string', 3, '手机号码，机密信息', '7年'),
('user', 'email', 'string', 3, '邮箱地址，机密信息', '7年'),
('user', 'id_card', 'string', 4, '身份证号，绝密信息', '永久保留'),
('conversation_member', 'last_read_seq', 'number', 2, '阅读状态，内部信息', '1年'),
('media_file', 'file_path', 'string', 2, '文件路径，内部信息', '3年');

-- 插入数据脱敏配置
INSERT INTO data_masking_config (table_name, column_name, masking_type, masking_rule, masking_params) VALUES
('user', 'phone', 'partial', '{"pattern": "\\d{3}\\*{4}\\d{4}", "visible": [0,1,2,7,8,9,10,11]}', '{"prefix": 3, "suffix": 4}'),
('user', 'email', 'partial', '{"pattern": "\\w{1,3}\\*{1,10}@\\w{1,3}\\*{1,10}\\.\\w{2,4}", "visible": [0,1,2,3,4,5,6,7,8,9,10,11]}', '{"prefix": 3, "suffix": 10}'),
('user', 'id_card', 'partial', '{"pattern": "\\d{6}\\*{8}\\d{4}", "visible": [0,1,2,3,4,5,14,15,16,17]}', '{"prefix": 6, "suffix": 4}');

-- 插入本地索引默认配置
INSERT INTO local_index_config (user_id, device_id, index_type, index_strategy, local_capacity, cleanup_policy, offline_search_range) VALUES
(0, 'default', 'message', 3, 1073741824, '30d', '30d'),
(0, 'default', 'user', 1, 268435456, '90d', '7d'),
(0, 'default', 'group', 1, 268435456, '90d', '7d'),
(0, 'default', 'file', 2, 536870912, '30d', '30d');

-- ============================================
-- 10. 性能优化配置
-- ============================================

-- 设置InnoDB缓冲池大小（根据服务器内存调整）
-- SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB

-- 设置查询缓存大小
-- SET GLOBAL query_cache_size = 134217728; -- 128MB

-- 设置临时表大小
-- SET GLOBAL tmp_table_size = 67108864; -- 64MB
-- SET GLOBAL max_heap_table_size = 67108864; -- 64MB

-- 设置连接数
-- SET GLOBAL max_connections = 1000;

-- 设置慢查询日志
-- SET GLOBAL slow_query_log = 1;
-- SET GLOBAL long_query_time = 2;

-- ============================================
-- 11. 读写分离配置建议
-- ============================================

/*
读写分离配置建议：

1. 主库（写操作）：
   - 用户注册、登录、资料更新
   - 会话创建、成员管理
   - 消息发送、状态更新
   - 权限变更、角色分配

2. 从库（读操作）：
   - 用户信息查询
   - 会话列表查询
   - 历史消息查询
   - 统计报表查询
   - 搜索功能

3. 配置示例：
   - 主库：192.168.1.10:3306
   - 从库1：192.168.1.11:3306
   - 从库2：192.168.1.12:3306

4. 应用层配置：
   - 写操作：使用主库连接
   - 读操作：使用从库连接池
   - 事务操作：强制使用主库
*/

-- ============================================
-- 12. 分区表维护建议
-- ============================================

/*
分区表维护建议：

1. 自动分区管理：
   - 每年自动创建新分区
   - 自动删除过期分区
   - 监控分区使用情况

2. 分区策略：
   - 按时间分区：适合时间序列数据
   - 按哈希分区：适合均匀分布数据
   - 按范围分区：适合有序数据

3. 分区数量：
   - 建议分区数量不超过100个
   - 单个分区大小控制在10GB以内
   - 定期检查分区分布均衡性
*/

-- 脚本执行完成
SELECT 'MySQL业务数据库优化版初始化完成！包含分区表、读写分离、性能监控、数据安全等高级功能。' as result; 