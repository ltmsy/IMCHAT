package com.acme.im.business.module.user.event;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户事件数据
 * 定义用户事件携带的数据结构
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventData {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;
    
    /**
     * 事件相关数据
     */
    private Map<String, Object> additionalData;
    
    /**
     * 事件描述
     */
    private String description;
    
    /**
     * 事件结果（成功/失败）
     */
    private Boolean success;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 创建成功事件数据
     */
    public static UserEventData success(Long userId, String username, String deviceId) {
        return UserEventData.builder()
                .userId(userId)
                .username(username)
                .deviceId(deviceId)
                .eventTime(LocalDateTime.now())
                .success(true)
                .build();
    }
    
    /**
     * 创建失败事件数据
     */
    public static UserEventData failure(Long userId, String username, String deviceId, String errorMessage) {
        return UserEventData.builder()
                .userId(userId)
                .username(username)
                .deviceId(deviceId)
                .eventTime(LocalDateTime.now())
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 添加额外数据
     */
    public UserEventData withAdditionalData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        additionalData.put(key, value);
        return this;
    }
} 