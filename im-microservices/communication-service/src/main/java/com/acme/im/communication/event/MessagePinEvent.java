package com.acme.im.communication.event;

import com.acme.im.communication.entity.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 消息置顶事件
 * 当消息被置顶时发布
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Getter
public class MessagePinEvent extends ApplicationEvent {
    
    private final Message message;
    private final Integer pinScope;
    
    public MessagePinEvent(Message message, Integer pinScope) {
        super(message);
        this.message = message;
        this.pinScope = pinScope;
    }
} 