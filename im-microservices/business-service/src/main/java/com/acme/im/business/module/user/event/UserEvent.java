package com.acme.im.business.module.user.event;

import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户事件基础类
 * 定义所有用户相关事件的基础结构
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class UserEvent extends BaseEvent<Object> {
    
    /**
     * 用户事件类型枚举
     */
    public enum UserEventType {
        // 删除：用户注册和删除事件 - 没有业务价值
        // USER_REGISTERED("用户注册"),
        USER_LOGIN("用户登录"),
        USER_LOGOUT("用户登出"),
        USER_PROFILE_UPDATED("用户资料更新"),
        USER_AVATAR_UPDATED("用户头像更新"),
        USER_STATUS_CHANGED("用户状态变更"),
        USER_ONLINE_STATUS_CHANGED("用户在线状态变更"),
        USER_DEVICE_ADDED("用户设备添加"),
        USER_DEVICE_REMOVED("用户设备移除");
        // 删除：用户删除事件 - 管理操作，不需要事件通知
        // USER_DELETED("用户删除");
        
        private final String description;
        
        UserEventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 构造函数
     */
    public UserEvent() {
        super();
    }
    
    /**
     * 构造函数
     */
    public UserEvent(String userId, UserEventType eventType, Object data) {
        super();
        this.setEventId(UUID.randomUUID().toString());
        this.setUserId(userId);
        this.setSubject("im.user." + eventType.name().toLowerCase());
        this.setData(data);
        this.setCreatedAt(LocalDateTime.now());
        this.setEventType("NOTIFICATION");
        this.setStatus("SUCCESS");
        this.setPriority("MEDIUM");
    }
    
    /**
     * 获取用户事件类型
     */
    public abstract UserEventType getUserEventType();
    
    /**
     * 获取事件描述
     */
    public String getEventDescription() {
        return getUserEventType().getDescription();
    }
} 