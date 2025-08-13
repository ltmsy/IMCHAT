package com.acme.im.common.websocket.constants;

/**
 * WebSocket常量类
 * 定义错误码、状态码、消息类型等常量
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public final class WebSocketConstants {

    private WebSocketConstants() {
        // 私有构造函数，防止实例化
    }

    /**
     * WebSocket连接状态
     */
    public static final class ConnectionStatus {
        /** 连接中 */
        public static final String CONNECTING = "CONNECTING";
        /** 已连接 */
        public static final String CONNECTED = "CONNECTED";
        /** 连接断开 */
        public static final String DISCONNECTED = "DISCONNECTED";
        /** 连接失败 */
        public static final String FAILED = "FAILED";
        /** 重连中 */
        public static final String RECONNECTING = "RECONNECTING";
        /** 已关闭 */
        public static final String CLOSED = "CLOSED";
    }

    /**
     * WebSocket消息状态
     */
    public static final class MessageStatus {
        /** 待发送 */
        public static final String PENDING = "PENDING";
        /** 发送中 */
        public static final String SENDING = "SENDING";
        /** 已发送 */
        public static final String SENT = "SENT";
        /** 发送失败 */
        public static final String FAILED = "FAILED";
        /** 已送达 */
        public static final String DELIVERED = "DELIVERED";
        /** 已读 */
        public static final String READ = "READ";
        /** 已撤回 */
        public static final String RECALLED = "RECALLED";
    }

    /**
     * WebSocket错误码
     */
    public static final class ErrorCode {
        /** 成功 */
        public static final int SUCCESS = 0;
        /** 通用错误 */
        public static final int GENERAL_ERROR = 1000;
        /** 参数错误 */
        public static final int PARAMETER_ERROR = 1001;
        /** 认证失败 */
        public static final int AUTHENTICATION_FAILED = 1002;
        /** 权限不足 */
        public static final int PERMISSION_DENIED = 1003;
        /** 资源不存在 */
        public static final int RESOURCE_NOT_FOUND = 1004;
        /** 资源已存在 */
        public static final int RESOURCE_ALREADY_EXISTS = 1005;
        /** 操作失败 */
        public static final int OPERATION_FAILED = 1006;
        /** 系统错误 */
        public static final int SYSTEM_ERROR = 1007;
        /** 网络错误 */
        public static final int NETWORK_ERROR = 1008;
        /** 超时错误 */
        public static final int TIMEOUT_ERROR = 1009;
        /** 限流错误 */
        public static final int RATE_LIMIT_ERROR = 1010;
        /** 熔断错误 */
        public static final int CIRCUIT_BREAKER_ERROR = 1011;
        /** 服务不可用 */
        public static final int SERVICE_UNAVAILABLE = 1012;
        /** 消息格式错误 */
        public static final int MESSAGE_FORMAT_ERROR = 2000;
        /** 消息类型不支持 */
        public static final int MESSAGE_TYPE_NOT_SUPPORTED = 2001;
        /** 消息大小超限 */
        public static final int MESSAGE_SIZE_EXCEEDED = 2002;
        /** 消息频率超限 */
        public static final int MESSAGE_FREQUENCY_EXCEEDED = 2003;
        /** 连接已存在 */
        public static final int CONNECTION_ALREADY_EXISTS = 3000;
        /** 连接不存在 */
        public static final int CONNECTION_NOT_FOUND = 3001;
        /** 连接已满 */
        public static final int CONNECTION_FULL = 3002;
        /** 连接超时 */
        public static final int CONNECTION_TIMEOUT = 3003;
        /** 心跳超时 */
        public static final int HEARTBEAT_TIMEOUT = 3004;
        /** 会话过期 */
        public static final int SESSION_EXPIRED = 4000;
        /** 会话无效 */
        public static final int SESSION_INVALID = 4001;
        /** 用户离线 */
        public static final int USER_OFFLINE = 5000;
        /** 用户忙碌 */
        public static final int USER_BUSY = 5001;
        /** 用户不可用 */
        public static final int USER_UNAVAILABLE = 5002;
    }

    /**
     * WebSocket状态码
     */
    public static final class StatusCode {
        /** 正常关闭 */
        public static final int NORMAL_CLOSURE = 1000;
        /** 端点离开 */
        public static final int GOING_AWAY = 1001;
        /** 协议错误 */
        public static final int PROTOCOL_ERROR = 1002;
        /** 不支持的数据类型 */
        public static final int UNSUPPORTED_DATA = 1003;
        /** 保留 */
        public static final int RESERVED = 1004;
        /** 无状态码 */
        public static final int NO_STATUS_RECEIVED = 1005;
        /** 异常关闭 */
        public static final int ABNORMAL_CLOSURE = 1006;
        /** 无效帧负载数据 */
        public static final int INVALID_FRAME_PAYLOAD_DATA = 1007;
        /** 策略违规 */
        public static final int POLICY_VIOLATION = 1008;
        /** 消息太大 */
        public static final int MESSAGE_TOO_BIG = 1009;
        /** 内部错误 */
        public static final int INTERNAL_ERROR = 1011;
        /** 服务重启 */
        public static final int SERVICE_RESTART = 1012;
        /** 临时重定向 */
        public static final int TRY_AGAIN_LATER = 1013;
        /** 错误网关 */
        public static final int BAD_GATEWAY = 1014;
        /** TLS握手失败 */
        public static final int TLS_HANDSHAKE = 1015;
    }

    /**
     * WebSocket消息类型
     */
    public static final class MessageType {
        /** 文本消息 */
        public static final String TEXT = "TEXT";
        /** 二进制消息 */
        public static final String BINARY = "BINARY";
        /** 控制消息 */
        public static final String CONTROL = "CONTROL";
        /** 心跳消息 */
        public static final String HEARTBEAT = "HEARTBEAT";
        /** 认证消息 */
        public static final String AUTHENTICATION = "AUTHENTICATION";
        /** 授权消息 */
        public static final String AUTHORIZATION = "AUTHORIZATION";
        /** 连接消息 */
        public static final String CONNECTION = "CONNECTION";
        /** 断开消息 */
        public static final String DISCONNECTION = "DISCONNECTION";
        /** 订阅消息 */
        public static final String SUBSCRIPTION = "SUBSCRIPTION";
        /** 取消订阅消息 */
        public static final String UNSUBSCRIPTION = "UNSUBSCRIPTION";
        /** 广播消息 */
        public static final String BROADCAST = "BROADCAST";
        /** 点对点消息 */
        public static final String P2P = "P2P";
        /** 群组消息 */
        public static final String GROUP = "GROUP";
        /** 通知消息 */
        public static final String NOTIFICATION = "NOTIFICATION";
        /** 状态更新消息 */
        public static final String STATUS_UPDATE = "STATUS_UPDATE";
        /** 配置更新消息 */
        public static final String CONFIG_UPDATE = "CONFIG_UPDATE";
        /** 错误消息 */
        public static final String ERROR = "ERROR";
        /** 警告消息 */
        public static final String WARNING = "WARNING";
        /** 信息消息 */
        public static final String INFO = "INFO";
        /** 调试消息 */
        public static final String DEBUG = "DEBUG";
    }

    /**
     * WebSocket操作类型
     */
    public static final class OperationType {
        /** 创建 */
        public static final String CREATE = "CREATE";
        /** 读取 */
        public static final String READ = "READ";
        /** 更新 */
        public static final String UPDATE = "UPDATE";
        /** 删除 */
        public static final String DELETE = "DELETE";
        /** 查询 */
        public static final String QUERY = "QUERY";
        /** 搜索 */
        public static final String SEARCH = "SEARCH";
        /** 过滤 */
        public static final String FILTER = "FILTER";
        /** 排序 */
        public static final String SORT = "SORT";
        /** 分页 */
        public static final String PAGINATION = "PAGINATION";
        /** 导出 */
        public static final String EXPORT = "EXPORT";
        /** 导入 */
        public static final String IMPORT = "IMPORT";
        /** 备份 */
        public static final String BACKUP = "BACKUP";
        /** 恢复 */
        public static final String RESTORE = "RESTORE";
        /** 同步 */
        public static final String SYNC = "SYNC";
        /** 异步 */
        public static final String ASYNC = "ASYNC";
        /** 批量操作 */
        public static final String BATCH = "BATCH";
        /** 事务操作 */
        public static final String TRANSACTION = "TRANSACTION";
    }

    /**
     * WebSocket资源类型
     */
    public static final class ResourceType {
        /** 用户 */
        public static final String USER = "USER";
        /** 消息 */
        public static final String MESSAGE = "MESSAGE";
        /** 会话 */
        public static final String SESSION = "SESSION";
        /** 群组 */
        public static final String GROUP = "GROUP";
        /** 文件 */
        public static final String FILE = "FILE";
        /** 配置 */
        public static final String CONFIG = "CONFIG";
        /** 日志 */
        public static final String LOG = "LOG";
        /** 监控 */
        public static final String MONITOR = "MONITOR";
        /** 审计 */
        public static final String AUDIT = "AUDIT";
        /** 权限 */
        public static final String PERMISSION = "PERMISSION";
        /** 角色 */
        public static final String ROLE = "ROLE";
        /** 系统 */
        public static final String SYSTEM = "SYSTEM";
        /** 网络 */
        public static final String NETWORK = "NETWORK";
        /** 存储 */
        public static final String STORAGE = "STORAGE";
        /** 缓存 */
        public static final String CACHE = "CACHE";
        /** 队列 */
        public static final String QUEUE = "QUEUE";
        /** 数据库 */
        public static final String DATABASE = "DATABASE";
        /** 服务 */
        public static final String SERVICE = "SERVICE";
        /** 接口 */
        public static final String API = "API";
        /** 事件 */
        public static final String EVENT = "EVENT";
        /** 任务 */
        public static final String TASK = "TASK";
        /** 作业 */
        public static final String JOB = "JOB";
    }

    /**
     * WebSocket优先级
     */
    public static final class Priority {
        /** 低优先级 */
        public static final int LOW = 1;
        /** 普通优先级 */
        public static final int NORMAL = 5;
        /** 高优先级 */
        public static final int HIGH = 8;
        /** 紧急优先级 */
        public static final int URGENT = 10;
    }

    /**
     * WebSocket超时时间（毫秒）
     */
    public static final class Timeout {
        /** 连接超时 */
        public static final long CONNECTION = 30000;
        /** 认证超时 */
        public static final long AUTHENTICATION = 10000;
        /** 心跳超时 */
        public static final long HEARTBEAT = 60000;
        /** 消息超时 */
        public static final long MESSAGE = 30000;
        /** 操作超时 */
        public static final long OPERATION = 60000;
        /** 会话超时 */
        public static final long SESSION = 1800000; // 30分钟
        /** 令牌超时 */
        public static final long TOKEN = 3600000; // 1小时
    }

    /**
     * WebSocket限制
     */
    public static final class Limit {
        /** 最大连接数 */
        public static final int MAX_CONNECTIONS = 10000;
        /** 最大消息大小（字节） */
        public static final int MAX_MESSAGE_SIZE = 1048576; // 1MB
        /** 最大消息频率（每秒） */
        public static final int MAX_MESSAGE_FREQUENCY = 100;
        /** 最大重连次数 */
        public static final int MAX_RECONNECT_ATTEMPTS = 5;
        /** 最大心跳间隔（毫秒） */
        public static final long MAX_HEARTBEAT_INTERVAL = 120000; // 2分钟
        /** 最大会话数 */
        public static final int MAX_SESSIONS = 1000;
        /** 最大群组数 */
        public static final int MAX_GROUPS = 100;
        /** 最大群组成员数 */
        public static final int MAX_GROUP_MEMBERS = 500;
    }

    /**
     * WebSocket配置键
     */
    public static final class ConfigKey {
        /** 启用WebSocket */
        public static final String ENABLE_WEBSOCKET = "websocket.enabled";
        /** WebSocket端口 */
        public static final String WEBSOCKET_PORT = "websocket.port";
        /** WebSocket路径 */
        public static final String WEBSOCKET_PATH = "websocket.path";
        /** 最大连接数 */
        public static final String MAX_CONNECTIONS = "websocket.max-connections";
        /** 最大消息大小 */
        public static final String MAX_MESSAGE_SIZE = "websocket.max-message-size";
        /** 心跳间隔 */
        public static final String HEARTBEAT_INTERVAL = "websocket.heartbeat-interval";
        /** 认证超时 */
        public static final String AUTH_TIMEOUT = "websocket.auth-timeout";
        /** 会话超时 */
        public static final String SESSION_TIMEOUT = "websocket.session-timeout";
        /** 启用压缩 */
        public static final String ENABLE_COMPRESSION = "websocket.enable-compression";
        /** 启用加密 */
        public static final String ENABLE_ENCRYPTION = "websocket.enable-encryption";
        /** 启用SSL */
        public static final String ENABLE_SSL = "websocket.enable-ssl";
        /** SSL密钥库 */
        public static final String SSL_KEYSTORE = "websocket.ssl-keystore";
        /** SSL密钥库密码 */
        public static final String SSL_KEYSTORE_PASSWORD = "websocket.ssl-keystore-password";
        /** SSL信任库 */
        public static final String SSL_TRUSTSTORE = "websocket.ssl-truststore";
        /** SSL信任库密码 */
        public static final String SSL_TRUSTSTORE_PASSWORD = "websocket.ssl-truststore-password";
    }

    /**
     * WebSocket事件类型
     */
    public static final class EventType {
        /** 连接建立 */
        public static final String CONNECTION_ESTABLISHED = "CONNECTION_ESTABLISHED";
        /** 连接断开 */
        public static final String CONNECTION_CLOSED = "CONNECTION_CLOSED";
        /** 消息接收 */
        public static final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
        /** 消息发送 */
        public static final String MESSAGE_SENT = "MESSAGE_SENT";
        /** 错误发生 */
        public static final String ERROR_OCCURRED = "ERROR_OCCURRED";
        /** 状态变化 */
        public static final String STATUS_CHANGED = "STATUS_CHANGED";
        /** 配置更新 */
        public static final String CONFIG_UPDATED = "CONFIG_UPDATED";
        /** 用户上线 */
        public static final String USER_ONLINE = "USER_ONLINE";
        /** 用户离线 */
        public static final String USER_OFFLINE = "USER_OFFLINE";
        /** 会话创建 */
        public static final String SESSION_CREATED = "SESSION_CREATED";
        /** 会话销毁 */
        public static final String SESSION_DESTROYED = "SESSION_DESTROYED";
        /** 群组创建 */
        public static final String GROUP_CREATED = "GROUP_CREATED";
        /** 群组解散 */
        public static final String GROUP_DISSOLVED = "GROUP_DISSOLVED";
        /** 成员加入 */
        public static final String MEMBER_JOINED = "MEMBER_JOINED";
        /** 成员离开 */
        public static final String MEMBER_LEFT = "MEMBER_LEFT";
    }

    /**
     * WebSocket标签
     */
    public static final class Tag {
        /** 系统 */
        public static final String SYSTEM = "system";
        /** 用户 */
        public static final String USER = "user";
        /** 消息 */
        public static final String MESSAGE = "message";
        /** 会话 */
        public static final String SESSION = "session";
        /** 群组 */
        public static final String GROUP = "group";
        /** 文件 */
        public static final String FILE = "file";
        /** 安全 */
        public static final String SECURITY = "security";
        /** 监控 */
        public static final String MONITOR = "monitor";
        /** 审计 */
        public static final String AUDIT = "audit";
        /** 配置 */
        public static final String CONFIG = "config";
        /** 网络 */
        public static final String NETWORK = "network";
        /** 存储 */
        public static final String STORAGE = "storage";
        /** 缓存 */
        public static final String CACHE = "cache";
        /** 队列 */
        public static final String QUEUE = "queue";
        /** 数据库 */
        public static final String DATABASE = "database";
        /** 服务 */
        public static final String SERVICE = "service";
        /** 接口 */
        public static final String API = "api";
        /** 事件 */
        public static final String EVENT = "event";
        /** 任务 */
        public static final String TASK = "task";
        /** 作业 */
        public static final String JOB = "job";
    }
} 