package com.acme.im.common.response;

/**
 * 响应状态码枚举
 * 定义系统使用的所有状态码
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public enum ResponseCode {

    // 成功状态码
    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),
    ACCEPTED(202, "请求已接受"),

    // 客户端错误状态码
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    UNPROCESSABLE_ENTITY(422, "请求无法处理"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 服务器错误状态码
    ERROR(500, "服务器内部错误"),
    SERVER_ERROR(500, "服务器内部错误"),
    NOT_IMPLEMENTED(501, "功能未实现"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    // 业务状态码 (1000-9999)
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    INVALID_CREDENTIALS(1003, "用户名或密码错误"),
    ACCOUNT_LOCKED(1004, "账户已锁定"),
    ACCOUNT_EXPIRED(1005, "账户已过期"),
    INSUFFICIENT_PERMISSIONS(1006, "权限不足"),
    TOKEN_EXPIRED(1007, "令牌已过期"),
    TOKEN_INVALID(1008, "令牌无效"),
    SESSION_EXPIRED(1009, "会话已过期"),

    // 消息相关状态码 (2000-2999)
    MESSAGE_SEND_FAILED(2001, "消息发送失败"),
    MESSAGE_NOT_FOUND(2002, "消息不存在"),
    MESSAGE_ALREADY_READ(2003, "消息已读"),
    MESSAGE_DELETE_FAILED(2004, "消息删除失败"),
    CONVERSATION_NOT_FOUND(2005, "会话不存在"),
    GROUP_NOT_FOUND(2006, "群组不存在"),
    USER_NOT_IN_GROUP(2007, "用户不在群组中"),

    // 文件相关状态码 (3000-3999)
    FILE_UPLOAD_FAILED(3001, "文件上传失败"),
    FILE_NOT_FOUND(3002, "文件不存在"),
    FILE_TOO_LARGE(3003, "文件过大"),
    INVALID_FILE_TYPE(3004, "无效的文件类型"),
    FILE_DOWNLOAD_FAILED(3005, "文件下载失败"),

    // 系统相关状态码 (9000-9999)
    SYSTEM_MAINTENANCE(9001, "系统维护中"),
    DATABASE_ERROR(9002, "数据库错误"),
    CACHE_ERROR(9003, "缓存错误"),
    EXTERNAL_SERVICE_ERROR(9004, "外部服务错误"),
    NETWORK_ERROR(9005, "网络错误"),
    TIMEOUT_ERROR(9006, "请求超时"),
    UNKNOWN_ERROR(9999, "未知错误");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据状态码获取枚举
     */
    public static ResponseCode fromCode(int code) {
        for (ResponseCode responseCode : values()) {
            if (responseCode.code == code) {
                return responseCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * 检查是否为成功状态码
     */
    public boolean isSuccess() {
        return this.code >= 200 && this.code < 300;
    }

    /**
     * 检查是否为客户端错误状态码
     */
    public boolean isClientError() {
        return this.code >= 400 && this.code < 500;
    }

    /**
     * 检查是否为服务器错误状态码
     */
    public boolean isServerError() {
        return this.code >= 500 && this.code < 600;
    }

    /**
     * 检查是否为业务状态码
     */
    public boolean isBusinessError() {
        return this.code >= 1000 && this.code < 10000;
    }
} 