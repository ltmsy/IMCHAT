package com.acme.im.business.module.user.entity;

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
    
    @TableField("friend_request_mode")
    private Integer friendRequestMode; // 0-自动通过，1-需要验证，2-拒绝所有
    
    @TableField("allow_search_by_username")
    private Integer allowSearchByUsername; // 0-不允许，1-允许
    
    @TableField("allow_search_by_phone")
    private Integer allowSearchByPhone; // 0-不允许，1-允许
    
    @TableField("allow_search_by_email")
    private Integer allowSearchByEmail; // 0-不允许，1-允许
    
    @TableField("allow_group_invite")
    private Integer allowGroupInvite; // 0-禁止，1-需要验证，2-直接同意
    
    @TableField("allow_stranger_message")
    private Integer allowStrangerMessage; // 0-不允许，1-允许
    
    @TableField("message_read_receipt")
    private Integer messageReadReceipt; // 0-不显示，1-显示
    
    @TableField("online_status_visible")
    private Integer onlineStatusVisible; // 0-不可见，1-可见
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 