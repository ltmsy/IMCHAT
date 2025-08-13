package com.acme.im.business.entity;

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
    private String deviceType;
    
    @TableField("platform")
    private String platform;
    
    @TableField("app_version")
    private String appVersion;
    
    @TableField("os_version")
    private String osVersion;
    
    @TableField("push_token")
    private String pushToken;
    
    @TableField("is_online")
    private Boolean isOnline;
    
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 