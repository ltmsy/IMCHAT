package com.acme.im.business.module.common.event.publisher;

import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.dto.MultiDeviceEvents;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 多端设备同步事件发布器
 * 专门处理需要多端同步的业务场景
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MultiDeviceSyncPublisher {

    private final AsyncEventPublisher eventPublisher;

    // ================================
    // 用户信息同步
    // ================================

    /**
     * 发布用户信息同步事件
     * 当用户在一个设备上修改信息时，通知其他设备同步
     */
    public void publishUserProfileSync(Long userId, String field, String oldValue, String newValue, String sourceDeviceId) {
        try {
            MultiDeviceEvents.UserProfileSyncEvent eventData = MultiDeviceEvents.UserProfileSyncEvent.builder()
                    .userId(userId)
                    .field(field)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .sourceDeviceId(sourceDeviceId)
                    .updateTime(LocalDateTime.now())
                    .build();

            BaseEvent<MultiDeviceEvents.UserProfileSyncEvent> baseEvent = BaseEvent.createNotification(
                    "multi.device.user.profile.sync", eventData)
                    .fromService("business-service", "default")
                    .withUser(userId.toString(), sourceDeviceId, null);

            eventPublisher.publishEvent("multi.device.user.profile.sync", baseEvent);
            
            log.info("发布用户信息同步事件: userId={}, field={}, sourceDeviceId={}", 
                    userId, field, sourceDeviceId);
            
        } catch (Exception e) {
            log.error("发布用户信息同步事件失败: userId={}, field={}, error: {}", 
                    userId, field, e.getMessage(), e);
        }
    }

    /**
     * 发布用户状态同步事件
     */
    public void publishUserStatusSync(Long userId, String oldStatus, String newStatus, String reason, String sourceDeviceId) {
        try {
            MultiDeviceEvents.UserStatusSyncEvent eventData = MultiDeviceEvents.UserStatusSyncEvent.builder()
                    .userId(userId)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .reason(reason)
                    .sourceDeviceId(sourceDeviceId)
                    .changeTime(LocalDateTime.now())
                    .build();

            BaseEvent<MultiDeviceEvents.UserStatusSyncEvent> baseEvent = BaseEvent.createNotification(
                    "multi.device.user.status.sync", eventData)
                    .fromService("business-service", "default")
                    .withUser(userId.toString(), sourceDeviceId, null);

            eventPublisher.publishEvent("multi.device.user.status.sync", baseEvent);
            
            log.info("发布用户状态同步事件: userId={}, oldStatus={}, newStatus={}, sourceDeviceId={}", 
                    userId, oldStatus, newStatus, sourceDeviceId);
            
        } catch (Exception e) {
            log.error("发布用户状态同步事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 消息状态同步
    // ================================

    /**
     * 发布消息状态同步事件
     * 当消息状态在一个设备上变更时，通知其他设备同步
     */
    public void publishMessageStatusSync(Long messageId, Long conversationId, String status, 
                                       String oldStatus, Long operatorId, String sourceDeviceId) {
        try {
            MultiDeviceEvents.MessageStatusSyncEvent eventData = MultiDeviceEvents.MessageStatusSyncEvent.builder()
                    .messageId(messageId)
                    .conversationId(conversationId)
                    .status(status)
                    .oldStatus(oldStatus)
                    .operatorId(operatorId)
                    .sourceDeviceId(sourceDeviceId)
                    .updateTime(LocalDateTime.now())
                    .metadata(new HashMap<>())
                    .build();

            BaseEvent<MultiDeviceEvents.MessageStatusSyncEvent> baseEvent = BaseEvent.createNotification(
                    "multi.device.message.status.sync", eventData)
                    .fromService("business-service", "default")
                    .withUser(operatorId.toString(), sourceDeviceId, null);

            eventPublisher.publishEvent("multi.device.message.status.sync", baseEvent);
            
            log.info("发布消息状态同步事件: messageId={}, conversationId={}, status={}, sourceDeviceId={}", 
                    messageId, conversationId, status, sourceDeviceId);
            
        } catch (Exception e) {
            log.error("发布消息状态同步事件失败: messageId={}, error: {}", messageId, e.getMessage(), e);
        }
    }

    /**
     * 发布消息撤回同步事件
     */
    public void publishMessageRecallSync(Long messageId, Long conversationId, Long operatorId, String sourceDeviceId) {
        publishMessageStatusSync(messageId, conversationId, "RECALLED", "SENT", operatorId, sourceDeviceId);
    }

    /**
     * 发布消息编辑同步事件
     */
    public void publishMessageEditSync(Long messageId, Long conversationId, Long operatorId, String sourceDeviceId) {
        publishMessageStatusSync(messageId, conversationId, "EDITED", "SENT", operatorId, sourceDeviceId);
    }

    /**
     * 发布消息删除同步事件
     */
    public void publishMessageDeleteSync(Long messageId, Long conversationId, Long operatorId, String sourceDeviceId) {
        publishMessageStatusSync(messageId, conversationId, "DELETED", "SENT", operatorId, sourceDeviceId);
    }

    // ================================
    // 会话状态同步
    // ================================

    /**
     * 发布会话状态同步事件
     */
    public void publishConversationSync(Long conversationId, String eventType, Long operatorId, 
                                      String sourceDeviceId, Map<String, Object> data) {
        try {
            MultiDeviceEvents.ConversationSyncEvent eventData = MultiDeviceEvents.ConversationSyncEvent.builder()
                    .conversationId(conversationId)
                    .eventType(eventType)
                    .operatorId(operatorId)
                    .sourceDeviceId(sourceDeviceId)
                    .data(data != null ? data : new HashMap<>())
                    .eventTime(LocalDateTime.now())
                    .build();

            BaseEvent<MultiDeviceEvents.ConversationSyncEvent> baseEvent = BaseEvent.createNotification(
                    "multi.device.conversation.sync", eventData)
                    .fromService("business-service", "default")
                    .withUser(operatorId.toString(), sourceDeviceId, null);

            eventPublisher.publishToJetStream("multi.device.conversation.sync", baseEvent);
            
            log.info("发布会话状态同步事件: conversationId={}, eventType={}, operatorId={}, sourceDeviceId={}", 
                    conversationId, eventType, operatorId, sourceDeviceId);
            
        } catch (Exception e) {
            log.error("发布会话状态同步事件失败: conversationId={}, error: {}", 
                    conversationId, e.getMessage(), e);
        }
    }

    // ================================
    // 好友关系同步
    // ================================

    /**
     * 发布好友关系同步事件
     */
    public void publishFriendshipSync(Long userId1, Long userId2, String eventType, 
                                    String sourceDeviceId, Map<String, Object> data) {
        try {
            MultiDeviceEvents.FriendshipSyncEvent eventData = MultiDeviceEvents.FriendshipSyncEvent.builder()
                    .userId1(userId1)
                    .userId2(userId2)
                    .eventType(eventType)
                    .sourceDeviceId(sourceDeviceId)
                    .data(data != null ? data : new HashMap<>())
                    .eventTime(LocalDateTime.now())
                    .build();

            BaseEvent<MultiDeviceEvents.FriendshipSyncEvent> baseEvent = BaseEvent.createNotification(
                    "multi.device.friendship.sync", eventData)
                    .fromService("business-service", "default")
                    .withUser(userId1.toString(), sourceDeviceId, null);

            eventPublisher.publishEvent("multi.device.friendship.sync", baseEvent);
            
            log.info("发布好友关系同步事件: userId1={}, userId2={}, eventType={}, sourceDeviceId={}", 
                    userId1, userId2, eventType, sourceDeviceId);
            
        } catch (Exception e) {
            log.error("发布好友关系同步事件失败: userId1={}, userId2={}, error: {}", 
                    userId1, userId2, e.getMessage(), e);
        }
    }

    // ================================
    // 系统通知同步
    // ================================

    /**
     * 发布系统通知同步事件
     */
    public void publishSystemNotificationSync(String notificationType, Long targetUserId, 
                                            String title, String content, String priority,
                                            Map<String, Object> data) {
        try {
            MultiDeviceEvents.SystemNotificationSyncEvent eventData = MultiDeviceEvents.SystemNotificationSyncEvent.builder()
                    .notificationType(notificationType)
                    .targetUserId(targetUserId)
                    .title(title)
                    .content(content)
                    .priority(priority)
                    .data(data != null ? data : new HashMap<>())
                    .createTime(LocalDateTime.now())
                    .expireTime(LocalDateTime.now().plusDays(7)) // 默认7天过期
                    .build();

            BaseEvent<MultiDeviceEvents.SystemNotificationSyncEvent> baseEvent = BaseEvent.createNotification(
                    "multi.device.system.notification.sync", eventData)
                    .fromService("business-service", "default")
                    .withUser(targetUserId.toString(), null, null);

            eventPublisher.publishToJetStream("multi.device.system.notification.sync", baseEvent);
            
            log.info("发布系统通知同步事件: notificationType={}, targetUserId={}, title={}", 
                    notificationType, targetUserId, title);
            
        } catch (Exception e) {
            log.error("发布系统通知同步事件失败: notificationType={}, targetUserId={}, error: {}", 
                    notificationType, targetUserId, e.getMessage(), e);
        }
    }

    // ================================
    // 设备管理同步
    // ================================

    /**
     * 发布设备管理同步事件
     */
    public void publishDeviceManagementSync(Long userId, String deviceId, String eventType, 
                                          String deviceInfo, String sourceDeviceId) {
        try {
            MultiDeviceEvents.DeviceManagementSyncEvent eventData = MultiDeviceEvents.DeviceManagementSyncEvent.builder()
                    .userId(userId)
                    .deviceId(deviceId)
                    .eventType(eventType)
                    .deviceInfo(deviceInfo)
                    .sourceDeviceId(sourceDeviceId)
                    .eventTime(LocalDateTime.now())
                    .build();

            BaseEvent<MultiDeviceEvents.DeviceManagementSyncEvent> baseEvent = BaseEvent.createNotification(
                    "multi.device.device.management.sync", eventData)
                    .fromService("business-service", "default")
                    .withUser(userId.toString(), sourceDeviceId, null);

            eventPublisher.publishEvent("multi.device.device.management.sync", baseEvent);
            
            log.info("发布设备管理同步事件: userId={}, deviceId={}, eventType={}, sourceDeviceId={}", 
                    userId, deviceId, eventType, sourceDeviceId);
            
        } catch (Exception e) {
            log.error("发布设备管理同步事件失败: userId={}, deviceId={}, error: {}", 
                    userId, deviceId, e.getMessage(), e);
        }
    }
} 