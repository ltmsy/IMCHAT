package com.acme.im.business.module.common.event.handler;

import com.acme.im.business.module.common.event.EventProcessorRegistry;
import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.UserEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务服务事件处理器
 * 专注处理业务逻辑相关的事件：用户管理、会话管理、群组管理、好友关系
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessEventHandler {

    private final EventProcessorRegistry eventProcessorRegistry;

    // ================================
    // 用户事件处理
    // ================================

    // 删除：用户注册事件处理 - 没有业务价值
    // /**
    //  * 处理用户注册事件
    //  */
    // @NatsEventHandler(value = EventTopics.Business.User.REGISTERED, priority = 100)
    // public void handleUserRegistered(BaseEvent<UserEvents.UserRegistrationCompletedEvent> event) {
    //     log.info("处理用户注册事件: userId={}, username={}", 
    //             event.getData().getUserId(), event.getData().getUsername());
    //     
    //     try {
    //         // 调用现有的业务处理逻辑
    //         BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
    //         eventProcessorRegistry.processEvent(adaptedEvent);
    //         
    //         log.info("用户注册事件处理成功: userId={}", event.getData().getUserId());
    //         
    //     } catch (Exception e) {
    //         log.error("用户注册事件处理失败: userId={}, error: {}", 
    //                 event.getData().getUserId(), e.getMessage(), e);
    //         throw e;
    //     }
    // }

    /**
     * 处理用户资料更新事件
     */
    @NatsEventHandler(value = EventTopics.Business.User.PROFILE_UPDATED, priority = 100)
    public void handleUserProfileUpdated(BaseEvent<?> event) {
        log.info("处理用户资料更新事件: userId={}, data={}", 
                event.getUserId(), event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("用户资料更新事件处理成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("用户资料更新事件处理失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理用户状态变更事件
     */
    @NatsEventHandler(value = EventTopics.Business.User.STATUS_CHANGED, priority = 100)
    public void handleUserStatusChanged(BaseEvent<?> event) {
        log.info("处理用户状态变更事件: userId={}, data={}", 
                event.getUserId(), event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("用户状态变更事件处理成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("用户状态变更事件处理失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理用户登录事件
     */
    @NatsEventHandler(value = EventTopics.Business.User.LOGIN, priority = 100)
    public void handleUserLogin(BaseEvent<?> event) {
        log.info("处理用户登录事件: userId={}, eventId={}", 
                event.getUserId(), event.getEventId());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("用户登录事件处理成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("用户登录事件处理失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理用户登出事件
     */
    @NatsEventHandler(value = EventTopics.Business.User.LOGOUT, priority = 100)
    public void handleUserLogout(BaseEvent<?> event) {
        log.info("处理用户登出事件: userId={}, eventId={}", 
                event.getUserId(), event.getEventId());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("用户登出事件处理成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("用户登出事件处理失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 会话相关事件处理
    // ================================

    /**
     * 处理会话创建事件
     */
    @NatsEventHandler(value = EventTopics.Business.Conversation.CREATED, priority = 100)
    public void handleConversationCreated(BaseEvent<?> event) {
        log.info("处理会话创建事件: conversationId={}", event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("会话创建事件处理成功");
            
        } catch (Exception e) {
            log.error("会话创建事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理会话更新事件
     */
    @NatsEventHandler(value = EventTopics.Business.Conversation.UPDATED, priority = 100)
    public void handleConversationUpdated(BaseEvent<?> event) {
        log.info("处理会话更新事件: conversationId={}", event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("会话更新事件处理成功");
            
        } catch (Exception e) {
            log.error("会话更新事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 群组相关事件处理
    // ================================

    /**
     * 处理群组创建事件
     */
    @NatsEventHandler(value = EventTopics.Business.Group.CREATED, priority = 100)
    public void handleGroupCreated(BaseEvent<?> event) {
        log.info("处理群组创建事件: groupId={}", event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("群组创建事件处理成功");
            
        } catch (Exception e) {
            log.error("群组创建事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理群组成员加入事件
     */
    @NatsEventHandler(value = EventTopics.Business.Group.MEMBER_JOINED, priority = 100)
    public void handleGroupMemberJoined(BaseEvent<?> event) {
        log.info("处理群组成员加入事件: data={}", event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("群组成员加入事件处理成功");
            
        } catch (Exception e) {
            log.error("群组成员加入事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 好友关系事件处理
    // ================================

    /**
     * 处理好友请求发送事件
     */
    @NatsEventHandler(value = EventTopics.Business.Friend.REQUEST_SENT, priority = 100)
    public void handleFriendRequestSent(BaseEvent<?> event) {
        log.info("处理好友请求发送事件: data={}", event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("好友请求发送事件处理成功");
            
        } catch (Exception e) {
            log.error("好友请求发送事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理好友添加事件
     */
    @NatsEventHandler(value = EventTopics.Business.Friend.ADDED, priority = 100)
    public void handleFriendAdded(BaseEvent<?> event) {
        log.info("处理好友添加事件: data={}", event.getData());
        
        try {
            BaseEvent<?> adaptedEvent = adaptToLegacyFormat(event);
            eventProcessorRegistry.processEvent(adaptedEvent);
            
            log.info("好友添加事件处理成功");
            
        } catch (Exception e) {
            log.error("好友添加事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 将新事件格式适配为旧事件格式
     * 为了兼容现有的EventProcessor机制
     */
    private BaseEvent<?> adaptToLegacyFormat(BaseEvent<?> newEvent) {
        // 这里可以添加事件格式转换逻辑
        // 暂时直接返回原事件
        return newEvent;
    }
} 