package com.acme.im.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户黑名单实体
 * 对应数据库表：user_blacklist
 */
@TableName("user_blacklist")
@Data
public class UserBlacklist {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("blocked_user_id")
    private Long blockedUserId;
    
    @TableField("block_type")
    private Integer blockType; // 1-临时屏蔽，2-永久拉黑
    
    @TableField("reason")
    private String reason;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
} 