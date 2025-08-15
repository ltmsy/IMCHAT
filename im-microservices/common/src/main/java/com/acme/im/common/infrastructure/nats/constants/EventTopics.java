package com.acme.im.common.infrastructure.nats.constants;

/**
 * 事件主题常量定义
 * 采用层次化命名规范：{service}.{module}.{action}
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public final class EventTopics {

    private EventTopics() {
        // 工具类，禁止实例化
    }

    // ================================
    // 业务服务事件主题
    // ================================
    
    /**
     * 用户相关事件
     */
    public static final class Business {
        public static final class User {
            // 删除：用户注册事件 - 没有业务价值
            // public static final String REGISTERED = "business.user.registered";
            public static final String PROFILE_UPDATED = "business.user.profile.updated";
            public static final String STATUS_CHANGED = "business.user.status.changed";
            // 删除：用户删除事件 - 管理操作，不需要事件通知
            // public static final String DELETED = "business.user.deleted";
            public static final String LOGIN = "business.user.login";
            public static final String LOGOUT = "business.user.logout";
            public static final String AVATAR_UPDATED = "business.user.avatar.updated";
            public static final String ONLINE_STATUS_CHANGED = "business.user.online.status.changed";
            public static final String DEVICE_ADDED = "business.user.device.added";
            public static final String DEVICE_REMOVED = "business.user.device.removed";
            
            // 新增：设备信任状态变更事件
            public static final String DEVICE_TRUST_CHANGED = "business.user.device.trust.changed";
            
            // 新增：隐私设置更新事件
            public static final String PRIVACY_UPDATED = "business.user.privacy.updated";
            
            // 新增：黑名单管理事件
            public static final String BLACKLIST_ADDED = "business.user.blacklist.added";
            public static final String BLACKLIST_REMOVED = "business.user.blacklist.removed";
            
            // 通配符
            public static final String ALL = "business.user.*";
        }
        
        /**
         * 会话相关事件
         */
        public static final class Conversation {
            public static final String CREATED = "business.conversation.created";
            public static final String UPDATED = "business.conversation.updated";
            public static final String DELETED = "business.conversation.deleted";
            public static final String MEMBER_ADDED = "business.conversation.member.added";
            public static final String MEMBER_REMOVED = "business.conversation.member.removed";
            
            // 通配符
            public static final String ALL = "business.conversation.*";
        }
        
        /**
         * 群组相关事件
         */
        public static final class Group {
            public static final String CREATED = "business.group.created";
            public static final String UPDATED = "business.group.updated";
            public static final String DELETED = "business.group.deleted";
            public static final String MEMBER_JOINED = "business.group.member.joined";
            public static final String MEMBER_LEFT = "business.group.member.left";
            public static final String ROLE_CHANGED = "business.group.role.changed";
            
            // 通配符
            public static final String ALL = "business.group.*";
        }
        
        /**
         * 好友关系事件
         */
        public static final class Friend {
            public static final String REQUEST_SENT = "business.friend.request.sent";
            public static final String REQUEST_ACCEPTED = "business.friend.request.accepted";
            public static final String REQUEST_REJECTED = "business.friend.request.rejected";
            public static final String ADDED = "business.friend.added";
            public static final String REMOVED = "business.friend.removed";
            public static final String BLOCKED = "business.friend.blocked";
            public static final String UNBLOCKED = "business.friend.unblocked";
            
            // 通配符
            public static final String ALL = "business.friend.*";
        }
        
        // 业务服务所有事件
        public static final String ALL = "business.*";
    }

    // ================================
    // 安全相关事件主题
    // ================================

    /**
     * 安全相关事件
     */
    public static final class Security {
        public static final String TOKEN_VALIDATE = "security.token.validate";
        public static final String TOKEN_REFRESH = "security.token.refresh";
        public static final String TOKEN_REVOKE = "security.token.revoke";
        public static final String AUTH_FAILED = "security.auth.failed";
        public static final String PERMISSION_DENIED = "security.permission.denied";
        
        // 通配符
        public static final String ALL = "security.*";
    }

    // ================================
    // 通信服务事件主题
    // ================================
    
    /**
     * 通信服务事件
     */
    public static final class Communication {
        /**
         * 消息相关事件
         */
        public static final class Message {
            public static final String SENT = "communication.message.sent";
            public static final String RECEIVED = "communication.message.received";
            public static final String DELIVERED = "communication.message.delivered";
            public static final String READ = "communication.message.read";
            public static final String RECALLED = "communication.message.recalled";
            public static final String EDITED = "communication.message.edited";
            public static final String DELETED = "communication.message.deleted";
            public static final String PINNED = "communication.message.pinned";
            public static final String UNPINNED = "communication.message.unpinned";
            
            // 通配符
            public static final String ALL = "communication.message.*";
        }
        
        /**
         * 连接相关事件
         */
        public static final class Connection {
            public static final String ESTABLISHED = "communication.connection.established";
            public static final String CLOSED = "communication.connection.closed";
            public static final String HEARTBEAT = "communication.connection.heartbeat";
            public static final String TIMEOUT = "communication.connection.timeout";
            public static final String ERROR = "communication.connection.error";
            
            // 通配符
            public static final String ALL = "communication.connection.*";
        }
        
        /**
         * 推送相关事件
         */
        public static final class Push {
            public static final String SENT = "communication.push.sent";
            public static final String DELIVERED = "communication.push.delivered";
            public static final String FAILED = "communication.push.failed";
            public static final String CLICKED = "communication.push.clicked";
            
            // 通配符
            public static final String ALL = "communication.push.*";
        }
        
        /**
         * 多设备同步事件
         */
        public static final class Sync {
            public static final String USER_PROFILE = "communication.sync.user.profile";
            public static final String USER_STATUS = "communication.sync.user.status";
            public static final String MESSAGE_STATUS = "communication.sync.message.status";
            public static final String CONVERSATION = "communication.sync.conversation";
            public static final String FRIENDSHIP = "communication.sync.friendship";
            public static final String DEVICE_MANAGEMENT = "communication.sync.device.management";
            
            // 通配符
            public static final String ALL = "communication.sync.*";
        }
        
        // 通信服务所有事件
        public static final String ALL = "communication.*";
    }

    // ================================
    // 管理服务事件主题
    // ================================
    
    /**
     * 管理服务事件
     */
    public static final class Admin {
        /**
         * 系统相关事件
         */
        public static final class System {
            public static final String ALERT = "admin.system.alert";
            public static final String NOTIFICATION = "admin.system.notification";
            public static final String MAINTENANCE = "admin.system.maintenance";
            public static final String SHUTDOWN = "admin.system.shutdown";
            public static final String STARTUP = "admin.system.startup";
            
            // 通配符
            public static final String ALL = "admin.system.*";
        }
        
        /**
         * 监控相关事件
         */
        public static final class Monitoring {
            public static final String HEALTH_CHECK = "admin.monitoring.health.check";
            public static final String METRICS_REPORT = "admin.monitoring.metrics.report";
            public static final String PERFORMANCE_ALERT = "admin.monitoring.performance.alert";
            public static final String ERROR_REPORT = "admin.monitoring.error.report";
            
            // 通配符
            public static final String ALL = "admin.monitoring.*";
        }
        
        /**
         * 配置相关事件
         */
        public static final class Config {
            public static final String UPDATED = "admin.config.updated";
            public static final String RELOADED = "admin.config.reloaded";
            public static final String VALIDATED = "admin.config.validated";
            public static final String ERROR = "admin.config.error";
            
            // 通配符
            public static final String ALL = "admin.config.*";
        }
        
        // 管理服务所有事件
        public static final String ALL = "admin.*";
    }

    // ================================
    // 公共事件主题
    // ================================
    
    /**
     * 公共事件（跨服务）
     */
    public static final class Common {
        /**
         * 认证相关事件
         */
        public static final class Auth {
            public static final String VALIDATE = "common.auth.validate";
            public static final String RESULT = "common.auth.result";
            public static final String REFRESH = "common.auth.refresh";
            public static final String LOGOUT = "common.auth.logout";
            public static final String TOKEN_EXPIRED = "common.auth.token.expired";
            public static final String TOKEN_REFRESHED = "common.auth.token.refreshed";
            public static final String PERMISSION_DENIED = "common.auth.permission.denied";
            
            // 通配符
            public static final String ALL = "common.auth.*";
        }
        
        /**
         * 缓存相关事件
         */
        public static final class Cache {
            public static final String INVALIDATED = "common.cache.invalidated";
            public static final String REFRESHED = "common.cache.refreshed";
            public static final String WARMED_UP = "common.cache.warmed.up";
            
            // 通配符
            public static final String ALL = "common.cache.*";
        }
        
        /**
         * 消息相关事件
         */
        public static final class Message {
            public static final String CHAT = "common.message.chat";
            public static final String CONTROL = "common.message.control";
            public static final String RESULT = "common.message.result";
            public static final String BROADCAST = "common.message.broadcast";
            
            // 通配符
            public static final String ALL = "common.message.*";
        }
        
        /**
         * WebSocket连接状态事件
         */
        public static final class WebSocket {
            public static final String CONNECTED = "websocket.connected";
            public static final String DISCONNECTED = "websocket.disconnected";
            
            // 通配符
            public static final String ALL = "websocket.*";
        }
        
        // 公共事件所有
        public static final String ALL = "common.*";
    }
    
    // ================================
    // 事件类型常量
    // ================================
    
    /**
     * 事件类型
     */
    public static final class EventType {
        public static final String REQUEST = "REQUEST";
        public static final String RESPONSE = "RESPONSE";
        public static final String NOTIFICATION = "NOTIFICATION";
        public static final String BROADCAST = "BROADCAST";
    }
    
    /**
     * 事件状态
     */
    public static final class EventStatus {
        public static final String PENDING = "PENDING";
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";
        public static final String TIMEOUT = "TIMEOUT";
    }
    
    /**
     * 事件优先级
     */
    public static final class EventPriority {
        public static final String URGENT = "URGENT";
        public static final String HIGH = "HIGH";
        public static final String MEDIUM = "MEDIUM";
        public static final String LOW = "LOW";
    }
    
    // ================================
    // 全局通配符
    // ================================
    
    /**
     * 全局事件通配符
     */
    public static final String ALL_EVENTS = "*";
} 