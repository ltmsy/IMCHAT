package com.acme.im.business.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设备实体
 * 对应数据库表：user_devices
 */
@TableName("user_devices")
@Data
public class UserDevice {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("device_id")
    private String deviceId;
    
    @TableField("device_name")
    private String deviceName;
    
    @TableField("device_type")
    private Integer deviceType;
    
    @TableField("device_info")
    private String deviceInfo;
    
    @TableField("ip_address")
    private String ipAddress;
    
    @TableField("location")
    private String location;
    
    @TableField("is_online")
    private Integer isOnline;
    
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
    
    @TableField("login_token")
    private String loginToken;
    
    @TableField("refresh_token")
    private String refreshToken;
    
    @TableField("token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    @TableField("is_trusted")
    private Integer isTrusted;
    
    @TableField("disconnected_at")
    private LocalDateTime disconnectedAt;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 