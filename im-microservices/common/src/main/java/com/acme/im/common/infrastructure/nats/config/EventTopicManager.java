package com.acme.im.common.infrastructure.nats.config;

import com.acme.im.common.infrastructure.nats.constants.EventTopics;

import java.util.*;

/**
 * 事件主题管理器
 * 负责管理各服务订阅的事件主题配置
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public final class EventTopicManager {

    private EventTopicManager() {
        // 工具类，禁止实例化
    }

    /**
     * 服务名称常量
     */
    public static final class ServiceNames {
        public static final String BUSINESS = "business";
        public static final String COMMUNICATION = "communication";
        public static final String ADMIN = "admin";
        public static final String COMMON = "common";
    }

    /**
     * 获取特定服务需要订阅的事件主题
     * 
     * @param serviceName 服务名称
     * @return 该服务需要订阅的主题列表
     */
    public static List<String> getServiceTopics(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case ServiceNames.BUSINESS:
                return getBusinessServiceTopics();
            case ServiceNames.COMMUNICATION:
                return getCommunicationServiceTopics();
            case ServiceNames.ADMIN:
                return getAdminServiceTopics();
            case ServiceNames.COMMON:
                return getCommonServiceTopics();
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 获取业务服务订阅的主题
     */
    private static List<String> getBusinessServiceTopics() {
        return Arrays.asList(
            // 用户相关事件（业务服务处理用户状态变更）
            EventTopics.Business.User.STATUS_CHANGED,
            EventTopics.Business.User.LOGIN,
            EventTopics.Business.User.LOGOUT,
            EventTopics.Business.User.PROFILE_UPDATED,
            EventTopics.Business.User.AVATAR_UPDATED,
            EventTopics.Business.User.ONLINE_STATUS_CHANGED,
            
            // 会话相关事件（业务服务管理会话生命周期）
            EventTopics.Business.Conversation.CREATED,
            EventTopics.Business.Conversation.UPDATED,
            EventTopics.Business.Conversation.DELETED,
            EventTopics.Business.Conversation.MEMBER_ADDED,
            EventTopics.Business.Conversation.MEMBER_REMOVED,
            
            // 群组相关事件（业务服务管理群组）
            EventTopics.Business.Group.CREATED,
            EventTopics.Business.Group.UPDATED,
            EventTopics.Business.Group.DELETED,
            EventTopics.Business.Group.MEMBER_JOINED,
            EventTopics.Business.Group.MEMBER_LEFT,
            EventTopics.Business.Group.ROLE_CHANGED,
            
            // 好友关系事件（业务服务管理社交关系）
            EventTopics.Business.Friend.REQUEST_SENT,
            EventTopics.Business.Friend.REQUEST_ACCEPTED,
            EventTopics.Business.Friend.REQUEST_REJECTED,
            EventTopics.Business.Friend.ADDED,
            EventTopics.Business.Friend.REMOVED,
            EventTopics.Business.Friend.BLOCKED,
            EventTopics.Business.Friend.UNBLOCKED,
            
            // 公共认证事件（需要处理认证状态变更）
            EventTopics.Common.Auth.TOKEN_EXPIRED,
            EventTopics.Common.Auth.PERMISSION_DENIED,
            
            // 公共缓存事件（需要处理缓存失效）
            EventTopics.Common.Cache.INVALIDATED
        );
    }

    /**
     * 获取通信服务订阅的主题
     */
    private static List<String> getCommunicationServiceTopics() {
        return Arrays.asList(
            // 消息相关事件（通信服务处理消息传输）
            EventTopics.Communication.Message.ALL,
            
            // 连接相关事件（通信服务管理连接）
            EventTopics.Communication.Connection.ALL,
            
            // 推送相关事件（通信服务处理推送）
            EventTopics.Communication.Push.ALL,
            
            // 多设备同步事件（通信服务处理设备同步）
            EventTopics.Communication.Sync.ALL,
            
            // 业务用户事件 - 具体主题（处理具体业务逻辑）
            EventTopics.Business.User.STATUS_CHANGED,
            EventTopics.Business.User.LOGIN,
            EventTopics.Business.User.LOGOUT,
            EventTopics.Business.User.PROFILE_UPDATED,
            EventTopics.Business.User.AVATAR_UPDATED,
            EventTopics.Business.User.ONLINE_STATUS_CHANGED,
            
            // 业务会话事件 - 具体主题
            EventTopics.Business.Conversation.CREATED,
            EventTopics.Business.Conversation.UPDATED,
            EventTopics.Business.Conversation.DELETED,
            EventTopics.Business.Conversation.MEMBER_ADDED,
            EventTopics.Business.Conversation.MEMBER_REMOVED,
            
            // 业务群组事件 - 具体主题
            EventTopics.Business.Group.CREATED,
            EventTopics.Business.Group.UPDATED,
            EventTopics.Business.Group.DELETED,
            EventTopics.Business.Group.MEMBER_JOINED,
            EventTopics.Business.Group.MEMBER_LEFT,
            EventTopics.Business.Group.ROLE_CHANGED,
            
            // 业务好友事件 - 具体主题
            EventTopics.Business.Friend.REQUEST_SENT,
            EventTopics.Business.Friend.REQUEST_ACCEPTED,
            EventTopics.Business.Friend.REQUEST_REJECTED,
            EventTopics.Business.Friend.ADDED,
            EventTopics.Business.Friend.REMOVED,
            EventTopics.Business.Friend.BLOCKED,
            EventTopics.Business.Friend.UNBLOCKED,
            
            // 公共认证事件（需要处理认证失败）
            EventTopics.Common.Auth.TOKEN_EXPIRED,
            EventTopics.Common.Auth.PERMISSION_DENIED,
            
            // 安全相关事件（需要处理token验证等）
            EventTopics.Security.TOKEN_VALIDATE,
            EventTopics.Security.TOKEN_REFRESH,
            EventTopics.Security.TOKEN_REVOKE,
            EventTopics.Security.AUTH_FAILED,
            EventTopics.Security.PERMISSION_DENIED
        );
    }

    /**
     * 获取管理服务订阅的主题
     */
    private static List<String> getAdminServiceTopics() {
        return Arrays.asList(
            // 系统相关事件（管理服务处理系统管理）
            EventTopics.Admin.System.ALL,
            
            // 监控相关事件（管理服务处理监控）
            EventTopics.Admin.Monitoring.ALL,
            
            // 配置相关事件（管理服务处理配置管理）
            EventTopics.Admin.Config.ALL,
            
            // 需要监控的业务事件
            // 删除：用户注册和删除事件 - 没有业务价值
            // EventTopics.Business.User.REGISTERED,
            // EventTopics.Business.User.DELETED,
            
            // 需要监控的通信事件
            EventTopics.Communication.Connection.ERROR,
            EventTopics.Communication.Push.FAILED
        );
    }

    /**
     * 获取公共模块订阅的主题
     */
    private static List<String> getCommonServiceTopics() {
        return Arrays.asList(
            // 公共认证事件
            EventTopics.Common.Auth.TOKEN_EXPIRED,
            EventTopics.Common.Auth.PERMISSION_DENIED,
            
            // 公共缓存事件
            EventTopics.Common.Cache.INVALIDATED,
            
            // 系统级监控事件
            EventTopics.Admin.Monitoring.HEALTH_CHECK,
            EventTopics.Admin.System.ALERT
        );
    }

    /**
     * 获取所有预定义的事件主题
     * 
     * @return 所有预定义主题列表
     */
    public static List<String> getAllPredefinedTopics() {
        List<String> allTopics = new ArrayList<>();
        
        // 添加各服务的主题
        allTopics.addAll(getBusinessServiceTopics());
        allTopics.addAll(getCommunicationServiceTopics());
        allTopics.addAll(getAdminServiceTopics());
        allTopics.addAll(getCommonServiceTopics());
        
        // 去重并排序
        return allTopics.stream()
                .distinct()
                .sorted()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 检查主题是否属于指定服务
     * 
     * @param topic 事件主题
     * @param serviceName 服务名称
     * @return 是否属于该服务
     */
    public static boolean isTopicBelongsToService(String topic, String serviceName) {
        if (topic == null || serviceName == null) {
            return false;
        }
        
        List<String> serviceTopics = getServiceTopics(serviceName);
        
        // 精确匹配
        if (serviceTopics.contains(topic)) {
            return true;
        }
        
        // 通配符匹配
        return serviceTopics.stream()
                .filter(serviceTopic -> serviceTopic.endsWith("*"))
                .anyMatch(pattern -> {
                    String prefix = pattern.substring(0, pattern.length() - 1);
                    return topic.startsWith(prefix);
                });
    }

    /**
     * 获取主题的服务归属
     * 
     * @param topic 事件主题
     * @return 归属的服务名称列表
     */
    public static List<String> getTopicOwners(String topic) {
        List<String> owners = new ArrayList<>();
        
        for (String service : Arrays.asList(ServiceNames.BUSINESS, ServiceNames.COMMUNICATION, 
                                          ServiceNames.ADMIN, ServiceNames.COMMON)) {
            if (isTopicBelongsToService(topic, service)) {
                owners.add(service);
            }
        }
        
        return owners;
    }

    /**
     * 验证主题名称格式
     * 
     * @param topic 事件主题
     * @return 是否符合命名规范
     */
    public static boolean isValidTopicFormat(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return false;
        }
        
        // 基本格式检查：service.module.action 或 service.module.*
        String[] parts = topic.split("\\.");
        
        // 至少需要两个部分：service.module
        if (parts.length < 2) {
            return false;
        }
        
        // 检查是否以已知服务名开头
        String serviceName = parts[0];
        return Arrays.asList(ServiceNames.BUSINESS, ServiceNames.COMMUNICATION, 
                           ServiceNames.ADMIN, ServiceNames.COMMON)
                     .contains(serviceName);
    }
} 