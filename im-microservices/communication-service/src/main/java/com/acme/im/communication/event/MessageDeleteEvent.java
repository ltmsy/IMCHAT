package com.acme.im.communication.event;

import com.acme.im.communication.entity.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 消息删除事件
 * 当消息被删除时发布
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Getter
public class MessageDeleteEvent extends ApplicationEvent {
    
    private final Message message;
    private final String deleteReason;
    private final Integer deleteScope;
    
    public MessageDeleteEvent(Message message, String deleteReason, Integer deleteScope) {
        super(message);
        this.message = message;
        this.deleteReason = deleteReason;
        this.deleteScope = deleteScope;
    }
} 