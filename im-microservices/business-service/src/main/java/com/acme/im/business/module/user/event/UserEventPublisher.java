package com.acme.im.business.module.user.event;

import com.acme.im.business.module.common.event.publisher.MultiDeviceSyncPublisher;
import com.acme.im.common.infrastructure.nats.dto.BaseEvent;
import com.acme.im.common.infrastructure.nats.publisher.AsyncEventPublisher;
import com.acme.im.common.infrastructure.nats.constants.EventTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户事件发布器
 * 负责发布所有用户相关的业务事件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final AsyncEventPublisher eventPublisher;
    private final MultiDeviceSyncPublisher multiDeviceSyncPublisher;

    // ================================
    // 用户生命周期事件
    // ================================

    /**
     * 发布用户登录事件
     */
    public void publishUserLogin(Long userId, String username, String deviceId) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId);
            UserLoginEvent event = new UserLoginEvent(userId.toString(), eventData);
            
            eventPublisher.publishEvent(EventTopics.Business.User.LOGIN, event);
            
            // 临时注释：多端同步事件（JetStream问题）
            // multiDeviceSyncPublisher.publishUserStatusSync(
            //     userId, "OFFLINE", "ONLINE", "用户登录", deviceId);
            
            log.info("发布用户登录事件: userId={}, username={}, deviceId={}", userId, username, deviceId);
            
        } catch (Exception e) {
            log.error("发布用户登录事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发布用户登出事件
     */
    public void publishUserLogout(Long userId, String username, String deviceId) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId);
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.LOGOUT, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.LOGOUT, event);
            
            // 临时注释：多端同步事件（JetStream问题）
            // multiDeviceSyncPublisher.publishUserStatusSync(
            //     userId, "ONLINE", "OFFLINE", "用户登出", deviceId);
            
            log.info("发布用户登出事件: userId={}, username={}, deviceId={}", userId, username, deviceId);
            
        } catch (Exception e) {
            log.error("发布用户登出事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 用户资料事件
    // ================================

    /**
     * 发布用户资料更新事件
     */
    public void publishUserProfileUpdated(Long userId, String username, String deviceId, 
                                        String field, String oldValue, String newValue) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("field", field)
                .withAdditionalData("oldValue", oldValue)
                .withAdditionalData("newValue", newValue);
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.PROFILE_UPDATED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.PROFILE_UPDATED, event);
            
            // 同时发布多端同步事件
            multiDeviceSyncPublisher.publishUserProfileSync(userId, field, oldValue, newValue, deviceId);
            
            log.info("发布用户资料更新事件: userId={}, field={}, oldValue={}, newValue={}", 
                    userId, field, oldValue, newValue);
            
        } catch (Exception e) {
            log.error("发布用户资料更新事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发布用户头像更新事件
     */
    public void publishUserAvatarUpdated(Long userId, String username, String deviceId, String newAvatarUrl) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("newAvatarUrl", newAvatarUrl);
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.AVATAR_UPDATED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.AVATAR_UPDATED, event);
            
            // 同时发布多端同步事件
            multiDeviceSyncPublisher.publishUserProfileSync(userId, "avatar_url", null, newAvatarUrl, deviceId);
            
            log.info("发布用户头像更新事件: userId={}, newAvatarUrl={}", userId, newAvatarUrl);
            
        } catch (Exception e) {
            log.error("发布用户头像更新事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 用户状态事件
    // ================================

    /**
     * 发布用户状态变更事件
     */
    public void publishUserStatusChanged(Long userId, String username, String deviceId, 
                                       Integer oldStatus, Integer newStatus, String reason) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("oldStatus", oldStatus)
                .withAdditionalData("newStatus", newStatus)
                .withAdditionalData("reason", reason);
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.STATUS_CHANGED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.STATUS_CHANGED, event);
            
            // 同时发布多端同步事件
            multiDeviceSyncPublisher.publishUserStatusSync(
                userId, String.valueOf(oldStatus), String.valueOf(newStatus), reason, deviceId);
            
            log.info("发布用户状态变更事件: userId={}, oldStatus={}, newStatus={}, reason={}", 
                    userId, oldStatus, newStatus, reason);
            
        } catch (Exception e) {
            log.error("发布用户状态变更事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发布用户在线状态变更事件
     */
    public void publishUserOnlineStatusChanged(Long userId, String username, String deviceId, 
                                             Integer oldOnlineStatus, Integer newOnlineStatus) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("oldOnlineStatus", oldOnlineStatus)
                .withAdditionalData("newOnlineStatus", newOnlineStatus);
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.ONLINE_STATUS_CHANGED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.ONLINE_STATUS_CHANGED, event);
            
            // 同时发布多端同步事件
            String oldStatus = getOnlineStatusString(oldOnlineStatus);
            String newStatus = getOnlineStatusString(newOnlineStatus);
            multiDeviceSyncPublisher.publishUserStatusSync(
                userId, oldStatus, newStatus, "在线状态变更", deviceId);
            
            log.info("发布用户在线状态变更事件: userId={}, oldStatus={}, newStatus={}", 
                    userId, oldStatus, newStatus);
            
        } catch (Exception e) {
            log.error("发布用户在线状态变更事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 用户设备事件
    // ================================

    /**
     * 发布用户设备添加事件
     */
    public void publishUserDeviceAdded(Long userId, String username, String deviceId, String deviceInfo) {
        UserEventData eventData = UserEventData.success(userId, username, deviceId)
            .withAdditionalData("deviceInfo", deviceInfo)
            .withAdditionalData("action", "device_added");
        
        BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.DEVICE_ADDED, eventData)
            .fromService("business-service", "default")
            .withUser(userId.toString(), deviceId, null);
        
        eventPublisher.publishEvent(EventTopics.Business.User.DEVICE_ADDED, event);
        
        // 同时发布多端同步事件
        multiDeviceSyncPublisher.publishDeviceManagementSync(userId, deviceId, "DEVICE_ADDED", deviceInfo, deviceId);
        
        log.info("发布用户设备添加事件: userId={}, deviceId={}", userId, deviceId);
    }
    
    /**
     * 发布用户设备移除事件
     */
    public void publishUserDeviceRemoved(Long userId, String username, String deviceId) {
        UserEventData eventData = UserEventData.success(userId, username, deviceId)
            .withAdditionalData("action", "device_removed");
        
        BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.DEVICE_REMOVED, eventData)
            .fromService("business-service", "default")
            .withUser(userId.toString(), deviceId, null);
        
        eventPublisher.publishEvent(EventTopics.Business.User.DEVICE_REMOVED, event);
        
        // 同时发布多端同步事件
        multiDeviceSyncPublisher.publishDeviceManagementSync(userId, deviceId, "DEVICE_REMOVED", null, deviceId);
        
        log.info("发布用户设备移除事件: userId={}, deviceId={}", userId, deviceId);
    }

    // ================================
    // 设备信任状态事件
    // ================================

    /**
     * 发布用户设备信任状态变更事件
     */
    public void publishUserDeviceTrustChanged(Long userId, String username, String deviceId, 
                                            boolean oldTrusted, boolean newTrusted) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("oldTrusted", oldTrusted)
                .withAdditionalData("newTrusted", newTrusted)
                .withAdditionalData("action", "device_trust_changed");
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.DEVICE_TRUST_CHANGED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.DEVICE_TRUST_CHANGED, event);
            
            // 同时发布多端同步事件
            multiDeviceSyncPublisher.publishDeviceManagementSync(userId, deviceId, "DEVICE_TRUST_CHANGED", 
                String.format("信任状态从%s变更为%s", oldTrusted ? "信任" : "不信任", newTrusted ? "信任" : "不信任"), deviceId);
            
            log.info("发布用户设备信任状态变更事件: userId={}, deviceId={}, oldTrusted={}, newTrusted={}", 
                    userId, deviceId, oldTrusted, newTrusted);
            
        } catch (Exception e) {
            log.error("发布用户设备信任状态变更事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 隐私设置事件
    // ================================

    /**
     * 发布用户隐私设置更新事件
     */
    public void publishUserPrivacyUpdated(Long userId, String username, String deviceId, 
                                        String field, Object oldValue, Object newValue) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("field", field)
                .withAdditionalData("oldValue", oldValue)
                .withAdditionalData("newValue", newValue)
                .withAdditionalData("action", "privacy_updated");
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.PRIVACY_UPDATED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.PRIVACY_UPDATED, event);
            
            // 同时发布多端同步事件
            multiDeviceSyncPublisher.publishUserProfileSync(userId, field, String.valueOf(oldValue), String.valueOf(newValue), deviceId);
            
            log.info("发布用户隐私设置更新事件: userId={}, field={}, oldValue={}, newValue={}", 
                    userId, field, oldValue, newValue);
            
        } catch (Exception e) {
            log.error("发布用户隐私设置更新事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 黑名单管理事件
    // ================================

    /**
     * 发布用户添加到黑名单事件
     */
    public void publishUserBlacklistAdded(Long userId, String username, String deviceId, 
                                        Long blockedUserId, String reason) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("blockedUserId", blockedUserId)
                .withAdditionalData("reason", reason)
                .withAdditionalData("action", "blacklist_added");
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.BLACKLIST_ADDED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.BLACKLIST_ADDED, event);
            
            // 同时发布多端同步事件
            Map<String, Object> blacklistData = Map.of("reason", reason, "blockedUserId", blockedUserId);
            multiDeviceSyncPublisher.publishFriendshipSync(userId, blockedUserId, "BLACKLIST_ADDED", deviceId, blacklistData);
            
            log.info("发布用户添加到黑名单事件: userId={}, blockedUserId={}, reason={}", 
                    userId, blockedUserId, reason);
            
        } catch (Exception e) {
            log.error("发布用户添加到黑名单事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发布用户从黑名单移除事件
     */
    public void publishUserBlacklistRemoved(Long userId, String username, String deviceId, 
                                          Long blockedUserId) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("blockedUserId", blockedUserId)
                .withAdditionalData("action", "blacklist_removed");
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.BLACKLIST_REMOVED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.BLACKLIST_REMOVED, event);
            
            // 同时发布多端同步事件
            Map<String, Object> blacklistData = Map.of("blockedUserId", blockedUserId);
            multiDeviceSyncPublisher.publishFriendshipSync(userId, blockedUserId, "BLACKLIST_REMOVED", deviceId, blacklistData);
            
            log.info("发布用户从黑名单移除事件: userId={}, blockedUserId={}", userId, blockedUserId);
            
        } catch (Exception e) {
            log.error("发布用户从黑名单移除事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 用户在线状态事件
    // ================================

    /**
     * 发布用户在线状态变更事件
     */
    public void publishUserOnlineStatusChanged(Long userId, String username, String deviceId, 
                                            boolean isOnline, String reason) {
        try {
            UserEventData eventData = UserEventData.success(userId, username, deviceId)
                .withAdditionalData("isOnline", isOnline)
                .withAdditionalData("reason", reason)
                .withAdditionalData("action", "online_status_changed");
            
            BaseEvent<UserEventData> event = BaseEvent.createNotification(EventTopics.Business.User.ONLINE_STATUS_CHANGED, eventData)
                .fromService("business-service", "default")
                .withUser(userId.toString(), deviceId, null);
            
            eventPublisher.publishEvent(EventTopics.Business.User.ONLINE_STATUS_CHANGED, event);
            
            // 发布多端同步事件
            String status = isOnline ? "ONLINE" : "OFFLINE";
            multiDeviceSyncPublisher.publishUserStatusSync(userId, 
                isOnline ? "OFFLINE" : "ONLINE", 
                status, 
                "用户在线状态变更: " + reason, deviceId);
            
            log.info("发布用户在线状态变更事件: userId={}, isOnline={}, reason={}", userId, isOnline, reason);
            
        } catch (Exception e) {
            log.error("发布用户在线状态变更事件失败: userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    // ================================
    // 辅助方法
    // ================================

    /**
     * 获取在线状态字符串
     */
    private String getOnlineStatusString(Integer onlineStatus) {
        if (onlineStatus == null) return "UNKNOWN";
        switch (onlineStatus) {
            case 0: return "OFFLINE";
            case 1: return "ONLINE";
            case 2: return "BUSY";
            case 3: return "INVISIBLE";
            default: return "UNKNOWN";
        }
    }
} 