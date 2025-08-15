package com.acme.im.business.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系实体
 * 对应数据库表：friendships
 */
@TableName("friendships")
@Data
public class Friendship {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("friend_id")
    private Long friendId;
    
    @TableField
    private Integer status; // 1-正常，2-已删除
    
    @TableField
    private String remark;
    
    @TableField("group_name")
    private String groupName; // 分组名称
    
    @TableField("is_starred")
    private Boolean isStarred;
    
    @TableField("is_top")
    private Boolean isTop;
    
    @TableField("mute_notifications")
    private Boolean muteNotifications;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 