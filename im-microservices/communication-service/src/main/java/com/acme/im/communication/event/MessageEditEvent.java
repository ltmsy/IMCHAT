package com.acme.im.communication.event;

import com.acme.im.communication.entity.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 消息编辑事件
 * 当消息被编辑时发布
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Getter
public class MessageEditEvent extends ApplicationEvent {
    
    private final Message originalMessage;
    private final Message editedMessage;
    
    public MessageEditEvent(Message originalMessage, Message editedMessage) {
        super(editedMessage);
        this.originalMessage = originalMessage;
        this.editedMessage = editedMessage;
    }
} 