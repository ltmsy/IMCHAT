package com.acme.im.common.infrastructure.nats.constants;

/**
 * 事件错误码常量类
 * 统一管理所有事件相关的错误码，便于错误处理和监控
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public final class EventErrorCodes {

    private EventErrorCodes() {
        // 工具类，禁止实例化
    }

    // ================================
    // 通用错误码 (1000-1999)
    // ================================
    
    /**
     * 数据验证错误
     */
    public static final String DATA_VALIDATION_ERROR = "E1000";
    public static final String DATA_NULL = "E1001";
    public static final String DATA_FORMAT_ERROR = "E1002";
    public static final String DATA_INCOMPLETE = "E1003";
    public static final String DATA_INVALID = "E1004";

    /**
     * 系统错误
     */
    public static final String SYSTEM_ERROR = "E1100";
    public static final String INTERNAL_ERROR = "E1101";
    public static final String TIMEOUT_ERROR = "E1102";
    public static final String RESOURCE_UNAVAILABLE = "E1103";
    public static final String SERVICE_UNAVAILABLE = "E1104";

    /**
     * 网络和通信错误
     */
    public static final String NETWORK_ERROR = "E1200";
    public static final String CONNECTION_ERROR = "E1201";
    public static final String PUBLISH_ERROR = "E1202";
    public static final String SUBSCRIBE_ERROR = "E1203";

    // ================================
    // 认证相关错误码 (2000-2999)
    // ================================
    
    /**
     * 认证错误
     */
    public static final String AUTH_ERROR = "E2000";
    public static final String AUTH_FAILED = "E2001";
    public static final String AUTH_PARAMS_INCOMPLETE = "E2002";
    public static final String AUTH_DATA_NULL = "E2003";
    public static final String AUTH_PROCESSING_ERROR = "E2004";
    public static final String TOKEN_EXPIRED = "E2005";
    public static final String TOKEN_INVALID = "E2006";
    public static final String TOKEN_EMPTY = "E2007";
    public static final String DEVICE_MISMATCH = "E2008";
    public static final String USER_NOT_FOUND = "E2009";
    public static final String USER_STATUS_ERROR = "E2010";
    public static final String PERMISSION_DENIED = "E2011";

    // ================================
    // 消息相关错误码 (3000-3999)
    // ================================
    
    /**
     * 消息错误
     */
    public static final String MESSAGE_ERROR = "E3000";
    public static final String MESSAGE_DATA_NULL = "E3001";
    public static final String MESSAGE_PARAMS_INCOMPLETE = "E3002";
    public static final String MESSAGE_PROCESSING_ERROR = "E3003";
    public static final String MESSAGE_NOT_FOUND = "E3004";
    public static final String MESSAGE_ALREADY_PROCESSED = "E3005";
    public static final String MESSAGE_CONTENT_INVALID = "E3006";
    public static final String MESSAGE_TYPE_UNSUPPORTED = "E3007";

    // ================================
    // 用户相关错误码 (4000-4999)
    // ================================
    
    /**
     * 用户错误
     */
    public static final String USER_ERROR = "E4000";
    public static final String USER_PROFILE_ERROR = "E4001";
    public static final String USER_SESSION_ERROR = "E4002";

    // ================================
    // 会话相关错误码 (5000-5999)
    // ================================
    
    /**
     * 会话错误
     */
    public static final String CONVERSATION_ERROR = "E5000";
    public static final String CONVERSATION_NOT_FOUND = "E5001";
    public static final String CONVERSATION_ACCESS_DENIED = "E5002";
    public static final String CONVERSATION_MEMBER_ERROR = "E5003";

    // ================================
    // 群组相关错误码 (6000-6999)
    // ================================
    
    /**
     * 群组错误
     */
    public static final String GROUP_ERROR = "E6000";
    public static final String GROUP_NOT_FOUND = "E6001";
    public static final String GROUP_ACCESS_DENIED = "E6002";
    public static final String GROUP_MEMBER_ERROR = "E6003";
    public static final String GROUP_ROLE_ERROR = "E6004";

    // ================================
    // 好友相关错误码 (7000-7999)
    // ================================
    
    /**
     * 好友关系错误
     */
    public static final String FRIEND_ERROR = "E7000";
    public static final String FRIEND_REQUEST_ERROR = "E7001";
    public static final String FRIEND_ALREADY_EXISTS = "E7002";
    public static final String FRIEND_NOT_FOUND = "E7003";

    // ================================
    // 缓存相关错误码 (8000-8999)
    // ================================
    
    /**
     * 缓存错误
     */
    public static final String CACHE_ERROR = "E8000";
    public static final String CACHE_GET_ERROR = "E8001";
    public static final String CACHE_SET_ERROR = "E8002";
    public static final String CACHE_DELETE_ERROR = "E8003";
    public static final String CACHE_INVALIDATION_ERROR = "E8004";

    // ================================
    // 插件相关错误码 (9000-9999)
    // ================================
    
    /**
     * 插件错误
     */
    public static final String PLUGIN_ERROR = "E9000";
    public static final String PLUGIN_NOT_FOUND = "E9001";
    public static final String PLUGIN_LOAD_ERROR = "E9002";
    public static final String PLUGIN_EXECUTION_ERROR = "E9003";
    public static final String PLUGIN_VERSION_MISMATCH = "E9004";

    // ================================
    // 工具方法
    // ================================
    
    /**
     * 获取错误描述
     */
    public static String getErrorDescription(String errorCode) {
        switch (errorCode) {
            case DATA_VALIDATION_ERROR:
                return "数据验证错误";
            case DATA_NULL:
                return "数据为空";
            case DATA_FORMAT_ERROR:
                return "数据格式错误";
            case DATA_INCOMPLETE:
                return "数据不完整";
            case DATA_INVALID:
                return "数据无效";
            case SYSTEM_ERROR:
                return "系统错误";
            case INTERNAL_ERROR:
                return "内部错误";
            case TIMEOUT_ERROR:
                return "超时错误";
            case RESOURCE_UNAVAILABLE:
                return "资源不可用";
            case SERVICE_UNAVAILABLE:
                return "服务不可用";
            case NETWORK_ERROR:
                return "网络错误";
            case CONNECTION_ERROR:
                return "连接错误";
            case PUBLISH_ERROR:
                return "发布错误";
            case SUBSCRIBE_ERROR:
                return "订阅错误";
            case AUTH_ERROR:
                return "认证错误";
            case AUTH_FAILED:
                return "认证失败";
            case AUTH_PARAMS_INCOMPLETE:
                return "认证参数不完整";
            case AUTH_DATA_NULL:
                return "认证数据为空";
            case AUTH_PROCESSING_ERROR:
                return "认证处理错误";
            case TOKEN_EXPIRED:
                return "令牌已过期";
            case TOKEN_INVALID:
                return "令牌无效";
            case TOKEN_EMPTY:
                return "令牌为空";
            case DEVICE_MISMATCH:
                return "设备ID不匹配";
            case USER_NOT_FOUND:
                return "用户不存在";
            case USER_STATUS_ERROR:
                return "用户状态错误";
            case PERMISSION_DENIED:
                return "权限被拒绝";
            case MESSAGE_ERROR:
                return "消息错误";
            case MESSAGE_DATA_NULL:
                return "消息数据为空";
            case MESSAGE_PARAMS_INCOMPLETE:
                return "消息参数不完整";
            case MESSAGE_PROCESSING_ERROR:
                return "消息处理错误";
            case MESSAGE_NOT_FOUND:
                return "消息不存在";
            case MESSAGE_ALREADY_PROCESSED:
                return "消息已处理";
            case MESSAGE_CONTENT_INVALID:
                return "消息内容无效";
            case MESSAGE_TYPE_UNSUPPORTED:
                return "不支持的消息类型";
            default:
                return "未知错误";
        }
    }

    /**
     * 检查是否为可重试错误
     */
    public static boolean isRetryable(String errorCode) {
        return TIMEOUT_ERROR.equals(errorCode) ||
               SYSTEM_ERROR.equals(errorCode) ||
               INTERNAL_ERROR.equals(errorCode) ||
               RESOURCE_UNAVAILABLE.equals(errorCode) ||
               SERVICE_UNAVAILABLE.equals(errorCode) ||
               NETWORK_ERROR.equals(errorCode) ||
               CONNECTION_ERROR.equals(errorCode) ||
               PUBLISH_ERROR.equals(errorCode) ||
               SUBSCRIBE_ERROR.equals(errorCode);
    }
} 