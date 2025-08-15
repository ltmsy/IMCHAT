package com.acme.im.communication.event;

import com.acme.im.communication.entity.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 新消息事件
 * 当新消息创建时发布
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Getter
public class NewMessageEvent extends ApplicationEvent {
    
    private final Message message;
    
    public NewMessageEvent(Message message) {
        super(message);
        this.message = message;
    }
} 