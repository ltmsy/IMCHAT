package com.acme.im.common.websocket.message.types;

/**
 * WebSocket消息类型枚举
 * 定义所有支持的消息类型
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public enum MessageType {

    // ==================== 系统消息类型 ====================
    
    /**
     * 连接建立
     */
    CONNECT("connect", "连接建立"),
    
    /**
     * 连接断开
     */
    DISCONNECT("disconnect", "连接断开"),
    
    /**
     * 心跳检测
     */
    HEARTBEAT("heartbeat", "心跳检测"),
    
    /**
     * 心跳响应
     */
    HEARTBEAT_ACK("heartbeat_ack", "心跳响应"),
    
    /**
     * 错误消息
     */
    ERROR("error", "错误消息"),
    
    /**
     * 状态更新
     */
    STATUS_UPDATE("status_update", "状态更新"),

    // ==================== 用户消息类型 ====================
    
    /**
     * 用户上线
     */
    USER_ONLINE("user_online", "用户上线"),
    
    /**
     * 用户下线
     */
    USER_OFFLINE("user_offline", "用户下线"),
    
    /**
     * 用户状态变更
     */
    USER_STATUS_CHANGE("user_status_change", "用户状态变更"),
    
    /**
     * 用户信息更新
     */
    USER_INFO_UPDATE("user_info_update", "用户信息更新"),

    // ==================== 消息相关类型 ====================
    
    /**
     * 发送消息
     */
    MESSAGE_SEND("message_send", "发送消息"),
    
    /**
     * 接收消息
     */
    MESSAGE_RECEIVE("message_receive", "接收消息"),
    
    /**
     * 消息已读
     */
    MESSAGE_READ("message_read", "消息已读"),
    
    /**
     * 消息撤回
     */
    MESSAGE_RECALL("message_recall", "消息撤回"),
    
    /**
     * 消息编辑
     */
    MESSAGE_EDIT("message_edit", "消息编辑"),
    
    /**
     * 消息删除
     */
    MESSAGE_DELETE("message_delete", "消息删除"),
    
    /**
     * 消息转发
     */
    MESSAGE_FORWARD("message_forward", "消息转发"),
    
    /**
     * 消息回复
     */
    MESSAGE_REPLY("message_reply", "消息回复"),

    // ==================== 会话相关类型 ====================
    
    /**
     * 会话创建
     */
    CONVERSATION_CREATE("conversation_create", "会话创建"),
    
    /**
     * 会话更新
     */
    CONVERSATION_UPDATE("conversation_update", "会话更新"),
    
    /**
     * 会话删除
     */
    CONVERSATION_DELETE("conversation_delete", "会话删除"),
    
    /**
     * 会话置顶
     */
    CONVERSATION_PIN("conversation_pin", "会话置顶"),
    
    /**
     * 会话免打扰
     */
    CONVERSATION_MUTE("conversation_mute", "会话免打扰"),
    
    /**
     * 会话已读
     */
    CONVERSATION_READ("conversation_read", "会话已读"),

    // ==================== 群组相关类型 ====================
    
    /**
     * 群组创建
     */
    GROUP_CREATE("group_create", "群组创建"),
    
    /**
     * 群组更新
     */
    GROUP_UPDATE("group_update", "群组更新"),
    
    /**
     * 群组删除
     */
    GROUP_DELETE("group_delete", "群组删除"),
    
    /**
     * 群成员加入
     */
    GROUP_MEMBER_JOIN("group_member_join", "群成员加入"),
    
    /**
     * 群成员离开
     */
    GROUP_MEMBER_LEAVE("group_member_leave", "群成员离开"),
    
    /**
     * 群成员被踢出
     */
    GROUP_MEMBER_KICK("group_member_kick", "群成员被踢出"),
    
    /**
     * 群成员角色变更
     */
    GROUP_MEMBER_ROLE_CHANGE("group_member_role_change", "群成员角色变更"),
    
    /**
     * 群公告更新
     */
    GROUP_ANNOUNCEMENT_UPDATE("group_announcement_update", "群公告更新"),

    // ==================== 好友相关类型 ====================
    
    /**
     * 好友申请
     */
    FRIEND_REQUEST("friend_request", "好友申请"),
    
    /**
     * 好友申请处理
     */
    FRIEND_REQUEST_HANDLE("friend_request_handle", "好友申请处理"),
    
    /**
     * 好友添加
     */
    FRIEND_ADD("friend_add", "好友添加"),
    
    /**
     * 好友删除
     */
    FRIEND_DELETE("friend_delete", "好友删除"),
    
    /**
     * 好友状态变更
     */
    FRIEND_STATUS_CHANGE("friend_status_change", "好友状态变更"),

    // ==================== 文件相关类型 ====================
    
    /**
     * 文件上传
     */
    FILE_UPLOAD("file_upload", "文件上传"),
    
    /**
     * 文件下载
     */
    FILE_DOWNLOAD("file_download", "文件下载"),
    
    /**
     * 文件删除
     */
    FILE_DELETE("file_delete", "文件删除"),
    
    /**
     * 文件分享
     */
    FILE_SHARE("file_share", "文件分享"),

    // ==================== 通知相关类型 ====================
    
    /**
     * 系统通知
     */
    SYSTEM_NOTIFICATION("system_notification", "系统通知"),
    
    /**
     * 业务通知
     */
    BUSINESS_NOTIFICATION("business_notification", "业务通知"),
    
    /**
     * 推送通知
     */
    PUSH_NOTIFICATION("push_notification", "推送通知"),

    // ==================== 搜索相关类型 ====================
    
    /**
     * 搜索请求
     */
    SEARCH_REQUEST("search_request", "搜索请求"),
    
    /**
     * 搜索响应
     */
    SEARCH_RESPONSE("search_response", "搜索响应"),

    // ==================== 其他类型 ====================
    
    /**
     * 未知类型
     */
    UNKNOWN("unknown", "未知类型");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取消息类型
     */
    public static MessageType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 检查是否为系统消息类型
     */
    public boolean isSystemMessage() {
        return this == CONNECT || this == DISCONNECT || this == HEARTBEAT || 
               this == HEARTBEAT_ACK || this == ERROR || this == STATUS_UPDATE;
    }

    /**
     * 检查是否为用户消息类型
     */
    public boolean isUserMessage() {
        return this == USER_ONLINE || this == USER_OFFLINE || 
               this == USER_STATUS_CHANGE || this == USER_INFO_UPDATE;
    }

    /**
     * 检查是否为消息相关类型
     */
    public boolean isMessageRelated() {
        return this == MESSAGE_SEND || this == MESSAGE_RECEIVE || 
               this == MESSAGE_READ || this == MESSAGE_RECALL || 
               this == MESSAGE_EDIT || this == MESSAGE_DELETE || 
               this == MESSAGE_FORWARD || this == MESSAGE_REPLY;
    }

    /**
     * 检查是否为会话相关类型
     */
    public boolean isConversationRelated() {
        return this == CONVERSATION_CREATE || this == CONVERSATION_UPDATE || 
               this == CONVERSATION_DELETE || this == CONVERSATION_PIN || 
               this == CONVERSATION_MUTE || this == CONVERSATION_READ;
    }

    /**
     * 检查是否为群组相关类型
     */
    public boolean isGroupRelated() {
        return this == GROUP_CREATE || this == GROUP_UPDATE || 
               this == GROUP_DELETE || this == GROUP_MEMBER_JOIN || 
               this == GROUP_MEMBER_LEAVE || this == GROUP_MEMBER_KICK || 
               this == GROUP_MEMBER_ROLE_CHANGE || this == GROUP_ANNOUNCEMENT_UPDATE;
    }

    /**
     * 检查是否为好友相关类型
     */
    public boolean isFriendRelated() {
        return this == FRIEND_REQUEST || this == FRIEND_REQUEST_HANDLE || 
               this == FRIEND_ADD || this == FRIEND_DELETE || 
               this == FRIEND_STATUS_CHANGE;
    }

    /**
     * 检查是否为文件相关类型
     */
    public boolean isFileRelated() {
        return this == FILE_UPLOAD || this == FILE_DOWNLOAD || 
               this == FILE_DELETE || this == FILE_SHARE;
    }

    /**
     * 检查是否为通知相关类型
     */
    public boolean isNotificationRelated() {
        return this == SYSTEM_NOTIFICATION || this == BUSINESS_NOTIFICATION || 
               this == PUSH_NOTIFICATION;
    }

    /**
     * 检查是否为搜索相关类型
     */
    public boolean isSearchRelated() {
        return this == SEARCH_REQUEST || this == SEARCH_RESPONSE;
    }

    @Override
    public String toString() {
        return code;
    }
} 