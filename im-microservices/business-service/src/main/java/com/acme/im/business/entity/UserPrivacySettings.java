package com.acme.im.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户隐私设置实体
 * 对应数据库表：user_privacy_settings
 */
@TableName("user_privacy_settings")
@Data
public class UserPrivacySettings {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("profile_visibility")
    private Integer profileVisibility; // 1-公开，2-好友可见，3-仅自己可见
    
    @TableField("online_status_visibility")
    private Integer onlineStatusVisibility; // 1-公开，2-好友可见，3-仅自己可见
    
    @TableField("last_active_visibility")
    private Integer lastActiveVisibility; // 1-公开，2-好友可见，3-仅自己可见
    
    @TableField("read_receipts")
    private Boolean readReceipts; // 是否显示已读回执
    
    @TableField("typing_indicators")
    private Boolean typingIndicators; // 是否显示正在输入提示
    
    @TableField("friend_requests")
    private Boolean friendRequests; // 是否允许好友申请
    
    @TableField("group_invitations")
    private Boolean groupInvitations; // 是否允许群组邀请
    
    @TableField("search_by_phone")
    private Boolean searchByPhone; // 是否允许通过手机号搜索
    
    @TableField("search_by_email")
    private Boolean searchByEmail; // 是否允许通过邮箱搜索
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 