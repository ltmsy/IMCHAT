package com.acme.im.business.module.user.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户登录事件
 * 当用户成功登录时发布此事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLoginEvent extends UserEvent {
    
    /**
     * 构造函数
     */
    public UserLoginEvent(String userId, Object data) {
        super(userId, UserEventType.USER_LOGIN, data);
    }
    
    @Override
    public UserEventType getUserEventType() {
        return UserEventType.USER_LOGIN;
    }
} 