package com.acme.im.business.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户基础信息实体
 * 对应数据库表：users
 * 使用MyBatis-Plus注解
 */
@TableName("users")
@Data
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String email;
    
    private String phone;
    
    private String nickname;
    
    @TableField("avatar_url")
    private String avatarUrl;
    
    private String signature;
    
    private Integer gender; // 0-未知，1-男，2-女
    
    private LocalDate birthday;
    
    private String region;
    
    private Integer status; // 0-禁用，1-正常，2-冻结
    
    @TableField("online_status")
    private Integer onlineStatus; // 0-离线，1-在线，2-忙碌，3-隐身
    
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
    
    @TableField("password_hash")
    private String passwordHash;
    
    private String salt;
    
    @TableField("two_factor_enabled")
    private Boolean twoFactorEnabled;
    
    @TableField("two_factor_secret")
    private String twoFactorSecret;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 