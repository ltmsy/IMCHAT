package com.acme.im.business.module.group.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体
 * 对应数据库表：conversations
 */
@TableName("conversations")
@Data
public class Conversation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("conversation_type")
    private Integer conversationType; // 1-单聊，2-群聊
    
    @TableField("conversation_id")
    private String conversationId; // 会话唯一标识
    
    @TableField("name")
    private String name; // 会话名称
    
    @TableField("avatar_url")
    private String avatarUrl; // 会话头像
    
    @TableField("last_message")
    private String lastMessage; // 最后一条消息
    
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime; // 最后消息时间
    
    @TableField("unread_count")
    private Integer unreadCount; // 未读消息数
    
    @TableField("is_top")
    private Boolean isTop; // 是否置顶
    
    @TableField("is_muted")
    private Boolean isMuted; // 是否免打扰
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 