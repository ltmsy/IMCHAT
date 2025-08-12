-- IM系统业务数据库 - MySQL建表脚本
-- 基于分布式单体架构设计
-- 维护方：im-business-service
-- 版本：v1.0.0
-- 字符集：utf8mb4，支持emoji和特殊字符

-- 创建数据库
CREATE DATABASE IF NOT EXISTS im_business 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE im_business;

-- ============================================
-- 1. 用户相关表
-- ============================================

-- 用户基础信息表
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
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='用户基础信息表';

-- 用户设备表
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
    INDEX idx_last_seen (last_seen_at)
) ENGINE=InnoDB COMMENT='用户设备表';

-- 用户关系表（好友关系）
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
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='用户关系表';

-- 好友请求表
CREATE TABLE friend_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(64) UNIQUE NOT NULL COMMENT '请求唯一ID',
    requester_user_id BIGINT NOT NULL COMMENT '请求发起人用户ID',
    requested_user_id BIGINT NOT NULL COMMENT '被请求人用户ID',
    request_message VARCHAR(512) DEFAULT '' COMMENT '申请消息',
    request_source TINYINT DEFAULT 1 COMMENT '请求来源：1-搜索添加 2-群组添加 3-名片分享 4-扫码添加',
    status TINYINT DEFAULT 0 COMMENT '请求状态：0-待处理 1-已同意 2-已拒绝 3-已过期 4-已撤回',
    handle_message VARCHAR(512) COMMENT '处理消息',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    handled_at TIMESTAMP NULL COMMENT '处理时间',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_request_id (request_id),
    INDEX idx_requester_user_id (requester_user_id),
    INDEX idx_requested_user_id (requested_user_id),
    INDEX idx_status (status),
    INDEX idx_requested_at (requested_at),
    INDEX idx_expires_at (expires_at),
    UNIQUE KEY uk_requester_requested (requester_user_id, requested_user_id)
) ENGINE=InnoDB COMMENT='好友请求表';

-- 用户偏好设置表
CREATE TABLE user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL COMMENT '用户ID',
    language VARCHAR(16) DEFAULT 'zh-CN' COMMENT '语言偏好',
    theme VARCHAR(32) DEFAULT 'light' COMMENT '主题设置：light-亮色 dark-暗色 auto-跟随系统',
    theme_color VARCHAR(32) DEFAULT 'purple' COMMENT '主题颜色：purple-紫色 blue-蓝色 green-绿色',
    font_size VARCHAR(16) DEFAULT 'medium' COMMENT '字体大小：small-小 medium-中 large-大',
    font_density VARCHAR(16) DEFAULT 'normal' COMMENT '字体密度：compact-紧凑 normal-正常 comfortable-舒适',
    notification_enabled TINYINT DEFAULT 1 COMMENT '通知开关：0-关闭 1-开启',
    sound_enabled TINYINT DEFAULT 1 COMMENT '声音开关：0-关闭 1-开启',
    vibration_enabled TINYINT DEFAULT 1 COMMENT '震动开关：0-关闭 1-开启',
    notification_preview TINYINT DEFAULT 1 COMMENT '通知预览：0-不显示 1-显示',
    disturb_free_enabled TINYINT DEFAULT 0 COMMENT '免打扰模式：0-关闭 1-开启',
    disturb_free_start_time TIME COMMENT '免打扰开始时间',
    disturb_free_end_time TIME COMMENT '免打扰结束时间',
    auto_download_images TINYINT DEFAULT 1 COMMENT '自动下载图片：0-否 1-仅WiFi 2-始终',
    auto_download_videos TINYINT DEFAULT 0 COMMENT '自动下载视频：0-否 1-仅WiFi 2-始终',
    auto_download_files TINYINT DEFAULT 0 COMMENT '自动下载文件：0-否 1-仅WiFi 2-始终',
    auto_play_videos TINYINT DEFAULT 1 COMMENT '自动播放视频：0-否 1-仅WiFi 2-始终',
    compress_images TINYINT DEFAULT 1 COMMENT '压缩图片：0-否 1-是',
    save_to_gallery TINYINT DEFAULT 1 COMMENT '保存到相册：0-否 1-是',
    enter_send_message TINYINT DEFAULT 1 COMMENT 'Enter发送消息：0-否 1-是',
    double_click_avatar_action VARCHAR(32) DEFAULT 'profile' COMMENT '双击头像操作：profile-查看资料 call-发起通话',
    workspace_layout VARCHAR(32) DEFAULT 'three_column' COMMENT '工作区布局：two_column-双栏 three_column-三栏',
    sidebar_width INT DEFAULT 280 COMMENT '侧边栏宽度（像素）',
    message_list_density VARCHAR(16) DEFAULT 'normal' COMMENT '消息列表密度：compact-紧凑 normal-正常 comfortable-舒适',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_theme (theme),
    INDEX idx_language (language)
) ENGINE=InnoDB COMMENT='用户偏好设置表';

-- 用户隐私设置表
CREATE TABLE user_privacy_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL COMMENT '用户ID',
    profile_visible_scope TINYINT DEFAULT 1 COMMENT '资料可见范围：0-仅自己 1-好友 2-公开',
    allow_search_by_phone TINYINT DEFAULT 1 COMMENT '允许通过手机号搜索：0-否 1-是',
    allow_search_by_email TINYINT DEFAULT 1 COMMENT '允许通过邮箱搜索：0-否 1-是',
    allow_search_by_username TINYINT DEFAULT 1 COMMENT '允许通过用户名搜索：0-否 1-是',
    show_online_status TINYINT DEFAULT 1 COMMENT '显示在线状态：0-否 1-是',
    show_last_seen TINYINT DEFAULT 1 COMMENT '显示最后在线时间：0-否 1-是',
    allow_friend_request TINYINT DEFAULT 1 COMMENT '好友申请方式：0-禁止 1-允许 2-需要验证 3-仅通过链接',
    friend_verify_question VARCHAR(256) COMMENT '好友验证问题',
    allow_group_invite TINYINT DEFAULT 1 COMMENT '群组邀请：0-禁止 1-允许 2-需要验证',
    allow_group_member_add_friend TINYINT DEFAULT 1 COMMENT '群成员添加好友：0-禁止 1-允许 2-需要验证',
    read_receipt_enabled TINYINT DEFAULT 1 COMMENT '已读回执：0-关闭 1-开启',
    typing_indicator_enabled TINYINT DEFAULT 1 COMMENT '输入状态指示：0-关闭 1-开启',
    allow_message_forward TINYINT DEFAULT 1 COMMENT '允许消息被转发：0-否 1-是',
    forward_notification_enabled TINYINT DEFAULT 1 COMMENT '转发通知：0-关闭 1-开启',
    allow_screenshot TINYINT DEFAULT 1 COMMENT '允许截屏：0-否 1-是',
    screenshot_notification_enabled TINYINT DEFAULT 0 COMMENT '截屏通知：0-关闭 1-开启',
    allow_message_recall TINYINT DEFAULT 1 COMMENT '允许撤回消息：0-否 1-是',
    recall_time_limit INT DEFAULT 120 COMMENT '撤回时间限制（秒）',
    block_stranger_messages TINYINT DEFAULT 0 COMMENT '拦截陌生人消息：0-否 1-是',
    auto_accept_group_invite TINYINT DEFAULT 0 COMMENT '自动接受群邀请：0-否 1-是',
    data_sync_enabled TINYINT DEFAULT 1 COMMENT '数据同步：0-关闭 1-开启',
    message_cloud_sync TINYINT DEFAULT 1 COMMENT '消息云同步：0-关闭 1-开启',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_search_settings (allow_search_by_phone, allow_search_by_email, allow_search_by_username),
    INDEX idx_friend_settings (allow_friend_request, allow_group_invite)
) ENGINE=InnoDB COMMENT='用户隐私设置表';

-- ============================================
-- 2. 会话相关表
-- ============================================

-- 会话表
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
    INDEX idx_last_message_at (last_message_at)
) ENGINE=InnoDB COMMENT='会话表';

-- 会话成员表
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
    INDEX idx_last_read_seq (last_read_seq)
) ENGINE=InnoDB COMMENT='会话成员表';

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
-- 13. 初始化数据
-- ============================================

-- 插入系统默认权限
INSERT INTO permission (permission_code, permission_name, permission_desc, permission_type) VALUES
('message.send', '发送消息', '发送文本和媒体消息的权限', 2),
('message.recall', '撤回消息', '撤回已发送消息的权限', 2),
('message.delete', '删除消息', '删除消息的权限', 2),
('message.pin', '置顶消息', '置顶消息的权限', 2),
('member.invite', '邀请成员', '邀请新成员加入会话的权限', 2),
('member.remove', '移除成员', '移除会话成员的权限', 2),
('member.mute', '禁言成员', '禁言会话成员的权限', 2),
('conversation.manage', '管理会话', '管理会话设置的权限', 2),
('conversation.dissolve', '解散会话', '解散会话的权限', 2),
('media.upload', '上传媒体', '上传图片、视频等媒体文件的权限', 3),
('admin.system', '系统管理', '系统管理权限', 1);

-- 插入系统默认角色
INSERT INTO role (role_code, role_name, role_desc, role_type) VALUES
('system.admin', '系统管理员', '系统最高权限管理员', 1),
('conversation.owner', '群主', '会话创建者，拥有最高管理权限', 2),
('conversation.admin', '管理员', '会话管理员，拥有部分管理权限', 2),
('conversation.member', '普通成员', '会话普通成员', 2);

-- 配置角色权限关联
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p 
WHERE r.role_code = 'system.admin';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p 
WHERE r.role_code = 'conversation.owner' 
AND p.permission_type IN (2, 3);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p 
WHERE r.role_code = 'conversation.admin' 
AND p.permission_code IN ('message.send', 'message.delete', 'message.pin', 'member.invite', 'member.mute', 'media.upload');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p 
WHERE r.role_code = 'conversation.member' 
AND p.permission_code IN ('message.send', 'message.recall', 'media.upload');

-- 初始化默认收藏夹
INSERT INTO user_collection_folder (folder_id, user_id, folder_name, folder_type, sort_order) VALUES
('default_folder', 0, '默认收藏夹', 1, 0);

-- 初始化默认搜索索引配置
INSERT INTO search_index_config (config_id, index_type, index_strategy, local_capacity, cleanup_policy, offline_search_range) VALUES
('idx_message', 'message', 3, 1073741824, '30d', '30d'),
('idx_user', 'user', 1, 268435456, '90d', '7d'),
('idx_group', 'group', 1, 268435456, '90d', '7d'),
('idx_file', 'file', 2, 536870912, '30d', '30d');

-- 初始化默认敏感词配置
INSERT INTO sensitive_word_config (config_id, word_pattern, word_type, action_type, risk_level) VALUES
('sw_politics_001', '政治敏感词示例', 1, 1, 3),
('sw_porn_001', '色情敏感词示例', 2, 1, 3),
('sw_violence_001', '暴力敏感词示例', 3, 1, 2);

-- 插入系统默认配置
INSERT INTO system_config (config_key, config_value, config_type, config_desc, is_public) VALUES
('system.name', 'IM即时通讯系统', 1, '系统名称', 1),
('system.version', '1.0.0', 1, '系统版本', 1),
('message.max_length', '10000', 2, '消息最大长度', 1),
('media.max_size', '104857600', 2, '媒体文件最大大小（100MB）', 1),
('conversation.max_members', '500', 2, '会话最大成员数', 1),
('user.max_conversations', '1000', 2, '用户最大会话数', 1),
('rate_limit.message_per_minute', '60', 2, '每分钟发送消息限制', 0),
('rate_limit.media_per_minute', '10', 2, '每分钟上传媒体限制', 0),
('friend_request.expire_days', '7', 2, '好友请求过期天数', 1),
('friend_request.max_pending', '50', 2, '最大待处理好友请求数', 1),
('group_invite.expire_hours', '72', 2, '群组邀请过期小时数', 1),
('message.recall_time_limit', '120', 2, '消息撤回时间限制（秒）', 1),
('notification.batch_interval', '5', 2, '通知批量发送间隔（秒）', 0),
('privacy.default_allow_search', '1', 2, '默认允许搜索', 1),
('collection.max_items_per_folder', '10000', 2, '每个收藏夹最大条目数', 1),
('collection.max_folders_per_user', '50', 2, '每个用户最大收藏夹数', 1),
('search.history_retention_days', '90', 2, '搜索历史保留天数', 1),
('search.max_history_per_user', '1000', 2, '每用户最大搜索历史数', 1),
('search.local_index_max_size', '1073741824', 2, '本地索引最大大小（字节）', 0),
('notification.max_unread_count', '9999', 2, '最大未读通知数显示', 1),
('notification.retention_days', '30', 2, '通知记录保留天数', 0),
('security.content_scan_enabled', '1', 3, '是否启用内容安全扫描', 1),
('security.sensitive_word_enabled', '1', 3, '是否启用敏感词过滤', 1),
('security.risk_score_threshold', '0.8', 1, '风险评分阈值', 0),
('audit.log_retention_days', '365', 2, '审计日志保留天数', 0),
('diagnostic.check_interval_minutes', '5', 2, '诊断检查间隔（分钟）', 0),
('performance.metric_retention_days', '30', 2, '性能指标保留天数', 0);

-- 创建索引优化查询性能
-- 复合索引用于常见查询场景
CREATE INDEX idx_conversation_member_role_status ON conversation_member (conversation_id, member_role, status);
CREATE INDEX idx_user_relation_type_status ON user_relation (user_id, relation_type, status);
CREATE INDEX idx_media_file_uploader_status ON media_file (uploader_user_id, status, created_at);
CREATE INDEX idx_operation_log_user_type_time ON operation_log (user_id, operation_type, created_at);

-- 群组相关复合索引
CREATE INDEX idx_group_info_type_status ON group_info (group_type, status, created_at);
CREATE INDEX idx_group_info_public_join ON group_info (is_public, join_approval, status);
CREATE INDEX idx_group_announcement_group_status ON group_announcement (group_id, status, published_at);
CREATE INDEX idx_group_invite_group_status ON group_invite (group_id, status, created_at);
CREATE INDEX idx_group_join_request_group_status ON group_join_request (group_id, status, created_at);
CREATE INDEX idx_group_mute_group_status ON group_mute_record (group_id, status, muted_at);
CREATE INDEX idx_group_file_group_status ON group_file (group_id, status, created_at);

-- 好友和用户设置相关复合索引
CREATE INDEX idx_friend_request_requester_status ON friend_request (requester_user_id, status, requested_at);
CREATE INDEX idx_friend_request_requested_status ON friend_request (requested_user_id, status, requested_at);
CREATE INDEX idx_user_preferences_theme_lang ON user_preferences (theme, language);
CREATE INDEX idx_user_privacy_search_friend ON user_privacy_settings (allow_search_by_phone, allow_search_by_email, allow_friend_request);

-- 收藏与标记相关复合索引
CREATE INDEX idx_collection_folder_user_type ON user_collection_folder (user_id, folder_type, sort_order);
CREATE INDEX idx_collection_item_user_folder ON user_collection_item (user_id, folder_id, created_at);
CREATE INDEX idx_collection_item_source ON user_collection_item (source_type, source_id, item_type);
CREATE INDEX idx_message_mark_user_type ON message_mark (user_id, mark_type, created_at);

-- 搜索与检索相关复合索引
CREATE INDEX idx_search_history_user_type ON user_search_history (user_id, search_type, search_time);
CREATE INDEX idx_search_index_type_enabled ON search_index_config (index_type, is_enabled);

-- 通知与状态相关复合索引
CREATE INDEX idx_notification_config_user_type ON user_notification_config (user_id, notification_type);
CREATE INDEX idx_notification_record_user_read ON user_notification_record (user_id, is_read, created_at);
CREATE INDEX idx_notification_record_type_priority ON user_notification_record (notification_type, priority, created_at);

-- 安全与合规相关复合索引
CREATE INDEX idx_sensitive_word_type_enabled ON sensitive_word_config (word_type, is_enabled, risk_level);
CREATE INDEX idx_content_scan_type_result ON content_security_scan (content_type, scan_result, scan_time);
CREATE INDEX idx_audit_log_user_type_time ON audit_log (user_id, operation_type, created_at);

-- 诊断与性能相关复合索引
CREATE INDEX idx_diagnostic_node_type_time ON system_diagnostic (node_id, diagnostic_type, diagnostic_time);
CREATE INDEX idx_performance_service_metric ON performance_monitor (service_name, metric_name, timestamp);

-- 添加外键约束（可选，根据实际需求决定是否启用）
-- ALTER TABLE user_device ADD CONSTRAINT fk_user_device_user FOREIGN KEY (user_id) REFERENCES user(user_id);
-- ALTER TABLE user_relation ADD CONSTRAINT fk_user_relation_user FOREIGN KEY (user_id) REFERENCES user(user_id);
-- ALTER TABLE conversation_member ADD CONSTRAINT fk_conversation_member_user FOREIGN KEY (user_id) REFERENCES user(user_id);
-- 注意：在高并发环境下，外键约束可能影响性能，建议在应用层保证数据一致性

-- 设置表的存储引擎参数优化
-- ALTER TABLE conversation ENGINE=InnoDB ROW_FORMAT=DYNAMIC;
-- ALTER TABLE conversation_member ENGINE=InnoDB ROW_FORMAT=DYNAMIC;
-- ALTER TABLE media_file ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

-- 创建数据清理存储过程
DELIMITER $$

-- 清理过期的好友请求
CREATE PROCEDURE CleanExpiredFriendRequests()
BEGIN
    UPDATE friend_request 
    SET status = 3 
    WHERE status = 0 
    AND expires_at < NOW();
END$$

-- 清理过期的群组邀请
CREATE PROCEDURE CleanExpiredGroupInvites()
BEGIN
    UPDATE group_invite 
    SET status = 3 
    WHERE status = 0 
    AND expires_at < NOW();
END$$

-- 清理过期的搜索历史
CREATE PROCEDURE CleanOldSearchHistory()
BEGIN
    DELETE FROM user_search_history 
    WHERE search_time < DATE_SUB(NOW(), INTERVAL 90 DAY);
END$$

-- 清理过期的通知记录
CREATE PROCEDURE CleanOldNotifications()
BEGIN
    DELETE FROM user_notification_record 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
END$$

-- 清理过期的审计日志
CREATE PROCEDURE CleanOldAuditLogs()
BEGIN
    DELETE FROM audit_log 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 365 DAY);
END$$

-- 清理过期的性能监控数据
CREATE PROCEDURE CleanOldPerformanceData()
BEGIN
    DELETE FROM performance_monitor 
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);
END$$

DELIMITER ;

-- 创建数据统计视图
CREATE VIEW v_user_statistics AS
SELECT 
    DATE(created_at) as date,
    COUNT(*) as new_users,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active_users
FROM user 
GROUP BY DATE(created_at);

CREATE VIEW v_conversation_statistics AS
SELECT 
    conversation_type,
    COUNT(*) as total_conversations,
    AVG(member_count) as avg_members,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active_conversations
FROM conversation 
GROUP BY conversation_type;

CREATE VIEW v_group_statistics AS
SELECT 
    group_type,
    COUNT(*) as total_groups,
    AVG(member_count) as avg_members,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active_groups
FROM group_info 
GROUP BY group_type;

CREATE VIEW v_media_statistics AS
SELECT 
    DATE(created_at) as date,
    file_type,
    COUNT(*) as file_count,
    SUM(file_size) as total_size,
    AVG(file_size) as avg_size
FROM media_file 
WHERE status = 1
GROUP BY DATE(created_at), file_type;

-- 脚本执行完成
SELECT 'MySQL业务数据库初始化完成！包含用户、会话、群组、权限、媒体、配置、收藏、搜索、通知、安全、审计、诊断等完整功能模块。' as result;
