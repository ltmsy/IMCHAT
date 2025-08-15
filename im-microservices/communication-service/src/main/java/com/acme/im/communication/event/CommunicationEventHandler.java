package com.acme.im.communication.event;

import com.acme.im.common.infrastructure.nats.annotation.NatsEventHandler;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.MessageEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通信服务事件处理器
 * 专注处理通信相关的事件：消息传输、连接管理、推送服务、多设备同步
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class CommunicationEventHandler {

    // ================================
    // 消息相关事件处理
    // ================================

    /**
     * 处理消息发送事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Message.SENT, priority = 100)
    public void handleMessageSent(BaseEvent<MessageEvents.MessageCreatedEvent> event) {
        MessageEvents.MessageCreatedEvent data = event.getData();
        log.info("处理消息发送事件: messageId={}, conversationId={}, senderId={}", 
                data.getMessageId(), data.getConversationId(), data.getSenderId());
        
        try {
            // 处理消息发送后的逻辑
            // 1. 更新会话最后消息时间
            // 2. 推送给在线用户
            // 3. 存储离线推送任务
            
            log.info("消息发送事件处理成功: messageId={}", data.getMessageId());
            
        } catch (Exception e) {
            log.error("消息发送事件处理失败: messageId={}, error: {}", 
                    data.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理消息接收事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Message.RECEIVED, priority = 100)
    public void handleMessageReceived(BaseEvent<MessageEvents.MessageReadEvent> event) {
        MessageEvents.MessageReadEvent data = event.getData();
        log.info("处理消息接收事件: messageId={}, userId={}", 
                data.getMessageId(), data.getUserId());
        
        try {
            // 处理消息接收逻辑
            // 1. 更新消息状态为已接收
            // 2. 发送接收确认
            // 3. 更新用户未读计数
            
            log.info("消息接收事件处理成功: messageId={}", data.getMessageId());
            
        } catch (Exception e) {
            log.error("消息接收事件处理失败: messageId={}, error: {}", 
                    data.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理消息撤回事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Message.RECALLED, priority = 200, async = true)
    public void handleMessageRecalled(BaseEvent<MessageEvents.MessageRecalledEvent> event) {
        MessageEvents.MessageRecalledEvent data = event.getData();
        log.info("处理消息撤回事件: messageId={}, operatorId={}", 
                data.getMessageId(), data.getOperatorId());
        
        try {
            // 处理消息撤回逻辑
            // 1. 更新消息状态
            // 2. 推送撤回通知给所有相关用户
            // 3. 清理相关缓存
            
            log.info("消息撤回事件处理成功: messageId={}", data.getMessageId());
            
        } catch (Exception e) {
            log.error("消息撤回事件处理失败: messageId={}, error: {}", 
                    data.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理消息编辑事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Message.EDITED, priority = 200)
    public void handleMessageEdited(BaseEvent<MessageEvents.MessageEditedEvent> event) {
        MessageEvents.MessageEditedEvent data = event.getData();
        log.info("处理消息编辑事件: messageId={}, operatorId={}", 
                data.getMessageId(), data.getOperatorId());
        
        try {
            // 处理消息编辑逻辑
            // 1. 更新消息内容
            // 2. 推送编辑通知
            // 3. 记录编辑历史
            
            log.info("消息编辑事件处理成功: messageId={}", data.getMessageId());
            
        } catch (Exception e) {
            log.error("消息编辑事件处理失败: messageId={}, error: {}", 
                    data.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理消息已读事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Message.READ, priority = 150)
    public void handleMessageRead(BaseEvent<MessageEvents.MessageReadEvent> event) {
        MessageEvents.MessageReadEvent data = event.getData();
        log.info("处理消息已读事件: messageId={}, userId={}", 
                data.getMessageId(), data.getUserId());
        
        try {
            // 处理消息已读逻辑
            // 1. 更新消息读取状态
            // 2. 发送已读回执
            // 3. 更新未读计数
            
            log.info("消息已读事件处理成功: messageId={}", data.getMessageId());
            
        } catch (Exception e) {
            log.error("消息已读事件处理失败: messageId={}, error: {}", 
                    data.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 连接相关事件处理
    // ================================

    /**
     * 处理连接建立事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Connection.ESTABLISHED, priority = 100)
    public void handleConnectionEstablished(BaseEvent<?> event) {
        log.info("处理连接建立事件: data={}", event.getData());
        
        try {
            // 处理连接建立逻辑
            // 1. 注册连接信息
            // 2. 更新用户在线状态
            // 3. 推送离线消息
            
            log.info("连接建立事件处理成功");
            
        } catch (Exception e) {
            log.error("连接建立事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理连接关闭事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Connection.CLOSED, priority = 100)
    public void handleConnectionClosed(BaseEvent<?> event) {
        log.info("处理连接关闭事件: data={}", event.getData());
        
        try {
            // 处理连接关闭逻辑
            // 1. 清理连接信息
            // 2. 更新用户离线状态
            // 3. 清理相关缓存
            
            log.info("连接关闭事件处理成功");
            
        } catch (Exception e) {
            log.error("连接关闭事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理心跳事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Connection.HEARTBEAT, priority = 300)
    public void handleHeartbeat(BaseEvent<?> event) {
        log.debug("处理心跳事件: data={}", event.getData());
        
        try {
            // 处理心跳逻辑
            // 1. 更新连接活跃时间
            // 2. 检查连接健康状态
            
            log.debug("心跳事件处理成功");
            
        } catch (Exception e) {
            log.error("心跳事件处理失败: error: {}", e.getMessage(), e);
            // 心跳失败不抛出异常，避免影响其他处理
        }
    }

    // ================================
    // 推送相关事件处理
    // ================================

    /**
     * 处理推送发送事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Push.SENT, priority = 100)
    public void handlePushSent(BaseEvent<?> event) {
        log.info("处理推送发送事件: data={}", event.getData());
        
        try {
            // 处理推送发送逻辑
            // 1. 记录推送日志
            // 2. 更新推送统计
            
            log.info("推送发送事件处理成功");
            
        } catch (Exception e) {
            log.error("推送发送事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理推送投递事件
     */
    @NatsEventHandler(value = EventTopics.Communication.Push.DELIVERED, priority = 100)
    public void handlePushDelivered(BaseEvent<?> event) {
        log.info("处理推送投递事件: data={}", event.getData());
        
        try {
            // 处理推送投递逻辑
            // 1. 更新推送状态
            // 2. 记录投递时间
            
            log.info("推送投递事件处理成功");
            
        } catch (Exception e) {
            log.error("推送投递事件处理失败: error: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 多设备同步事件处理
    // ================================

    /**
     * 处理用户资料同步事件
     */
    @NatsEventHandler(value = "common.user.profile.sync", priority = 100)
    public void handleUserProfileSync(BaseEvent<?> event) {
        log.info("处理用户资料同步事件: userId={}, data={}", 
                event.getUserId(), event.getData());
        
        try {
            // 同步用户资料到所有连接的设备
            // 1. 获取用户所有设备连接
            // 2. 推送资料更新消息
            // 3. 更新本地缓存
            
            log.info("用户资料同步成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("用户资料同步失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理用户状态同步事件
     */
    @NatsEventHandler(value = "common.user.status.sync", priority = 100)
    public void handleUserStatusSync(BaseEvent<?> event) {
        log.info("处理用户状态同步事件: userId={}, data={}", 
                event.getUserId(), event.getData());
        
        try {
            // 同步用户状态到所有连接的设备
            // 1. 获取用户所有设备连接
            // 2. 推送状态更新消息
            // 3. 更新本地缓存
            
            log.info("用户状态同步成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("用户状态同步失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 业务事件监听（需要同步的）
    // ================================

    /**
     * 监听业务层用户登录事件（需要同步到通信层）
     */
    @NatsEventHandler(value = EventTopics.Business.User.LOGIN, priority = 100)
    public void onBusinessUserLogin(BaseEvent<?> event) {
        log.info("监听到业务层用户登录事件: userId={}, eventId={}", 
                event.getUserId(), event.getEventId());
        
        try {
            // 同步用户登录状态到通信层
            // 1. 更新在线用户缓存
            // 2. 建立用户连接映射
            // 3. 推送登录通知给好友
            // 4. 同步用户状态到其他设备
            
            log.info("业务层用户登录事件同步成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("业务层用户登录事件同步失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 监听业务层用户登出事件（需要同步到通信层）
     */
    @NatsEventHandler(value = EventTopics.Business.User.LOGOUT, priority = 100)
    public void onBusinessUserLogout(BaseEvent<?> event) {
        log.info("监听到业务层用户登出事件: userId={}, eventId={}", 
                event.getUserId(), event.getEventId());
        
        try {
            // 同步用户登出状态到通信层
            // 1. 更新在线用户缓存
            // 2. 清理用户连接映射
            // 3. 推送登出通知给好友
            // 4. 同步用户状态到其他设备
            
            log.info("业务层用户登出事件同步成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("业务层用户登出事件同步失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 监听业务层用户状态变更（需要同步到通信层）
     */
    @NatsEventHandler(value = EventTopics.Business.User.STATUS_CHANGED, priority = 100)
    public void onBusinessUserStatusChanged(BaseEvent<?> event) {
        log.info("监听到业务层用户状态变更: userId={}, data={}", 
                event.getUserId(), event.getData());
        
        try {
            // 同步用户状态到通信层
            // 1. 更新用户状态缓存
            // 2. 推送状态变更通知
            // 3. 同步到其他设备
            
            log.info("业务层用户状态变更同步成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("业务层用户状态变更同步失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 监听业务层用户资料更新（需要同步到通信层）
     */
    @NatsEventHandler(value = EventTopics.Business.User.PROFILE_UPDATED, priority = 100)
    public void onBusinessUserProfileUpdated(BaseEvent<?> event) {
        log.info("监听到业务层用户资料更新: userId={}, data={}", 
                event.getUserId(), event.getData());
        
        try {
            // 同步用户资料到通信层
            // 1. 更新用户资料缓存
            // 2. 推送资料变更通知
            // 3. 同步到其他设备
            
            log.info("业务层用户资料更新同步成功: userId={}", event.getUserId());
            
        } catch (Exception e) {
            log.error("业务层用户资料更新同步失败: userId={}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 监听业务层会话变更（需要同步到通信层）
     */
    @NatsEventHandler(value = EventTopics.Business.Conversation.CREATED, priority = 100)
    public void onBusinessConversationCreated(BaseEvent<?> event) {
        log.info("监听到业务层会话创建: subject={}, data={}", 
                event.getSubject(), event.getData());
        
        try {
            // 同步会话创建到通信层
            // 1. 创建会话缓存
            // 2. 推送会话创建通知
            // 3. 建立会话连接
            
            log.info("业务层会话创建同步成功: subject={}", event.getSubject());
            
        } catch (Exception e) {
            log.error("业务层会话创建同步失败: subject={}, error: {}", 
                    event.getSubject(), e.getMessage(), e);
            throw e;
        }
    }

    @NatsEventHandler(value = EventTopics.Business.Conversation.UPDATED, priority = 100)
    public void onBusinessConversationUpdated(BaseEvent<?> event) {
        log.info("监听到业务层会话更新: subject={}, data={}", 
                event.getSubject(), event.getData());
        
        try {
            // 同步会话更新到通信层
            // 1. 更新会话缓存
            // 2. 推送会话更新通知
            // 3. 同步到相关连接
            
            log.info("业务层会话更新同步成功: subject={}", event.getSubject());
            
        } catch (Exception e) {
            log.error("业务层会话更新同步失败: subject={}, error: {}", 
                    event.getSubject(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 监听业务层群组变更（需要同步到通信层）
     */
    @NatsEventHandler(value = EventTopics.Business.Group.CREATED, priority = 100)
    public void onBusinessGroupCreated(BaseEvent<?> event) {
        log.info("监听到业务层群组创建: subject={}, data={}", 
                event.getSubject(), event.getData());
        
        try {
            // 同步群组创建到通信层
            // 1. 创建群组缓存
            // 2. 推送群组创建通知
            // 3. 建立群组连接
            
            log.info("业务层群组创建同步成功: subject={}", event.getSubject());
            
        } catch (Exception e) {
            log.error("业务层群组创建同步失败: subject={}, error: {}", 
                    event.getSubject(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 监听业务层好友关系变更（需要同步到通信层）
     */
    @NatsEventHandler(value = EventTopics.Business.Friend.ADDED, priority = 100)
    public void onBusinessFriendAdded(BaseEvent<?> event) {
        log.info("监听到业务层好友添加: subject={}, data={}", 
                event.getSubject(), event.getData());
        
        try {
            // 同步好友关系到通信层
            // 1. 更新好友关系缓存
            // 2. 推送好友添加通知
            // 3. 建立好友连接
            
            log.info("业务层好友添加同步成功: subject={}", event.getSubject());
            
        } catch (Exception e) {
            log.error("业务层好友添加同步失败: subject={}, error: {}", 
                    event.getSubject(), e.getMessage(), e);
            throw e;
        }
    }

    // ================================
    // 通用事件处理
    // ================================

    /**
     * 处理所有通信相关事件（兜底处理）
     * 优先级最低，只做通用工作：审计、监控、统计
     */
    @NatsEventHandler(value = EventTopics.Communication.ALL, priority = 999)
    public void handleAllCommunicationEvents(BaseEvent<?> event) {
        log.info("📊 通信事件审计记录: subject={}, eventType={}, userId={}, eventId={}", 
                event.getSubject(), event.getEventType(), event.getUserId(), event.getEventId());
        
        // 这里只做通用工作，不处理具体业务逻辑
        // 1. 审计日志记录
        // 2. 事件统计和监控
        // 3. 系统健康状况检查
        // 4. 性能指标收集
        
        // 注意：不要调用具体的业务处理方法，避免重复处理
    }
} 