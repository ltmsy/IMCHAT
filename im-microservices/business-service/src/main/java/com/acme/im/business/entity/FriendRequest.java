package com.acme.im.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请实体
 * 对应数据库表：friend_requests
 */
@TableName("friend_requests")
@Data
public class FriendRequest {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("from_user_id")
    private Long fromUserId;
    
    @TableField("to_user_id")
    private Long toUserId;
    
    @TableField("message")
    private String message;
    
    @TableField("status")
    private Integer status; // 0-待处理，1-已同意，2-已拒绝，3-已过期
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 