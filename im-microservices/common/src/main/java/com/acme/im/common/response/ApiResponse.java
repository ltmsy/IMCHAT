package com.acme.im.common.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一API响应组件
 * 提供标准化的响应格式
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
public class ApiResponse<T> {

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 请求ID（用于追踪）
     */
    private String requestId;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 构造函数
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 构造函数
     */
    public ApiResponse(int code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数
     */
    public ApiResponse(int code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage());
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(ResponseCode.ERROR.getCode(), ResponseCode.ERROR.getMessage());
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ResponseCode.ERROR.getCode(), message);
    }

    /**
     * 失败响应（自定义状态码和消息）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message);
    }

    /**
     * 失败响应（带数据）
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    /**
     * 参数错误响应
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(ResponseCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(ResponseCode.UNAUTHORIZED.getCode(), message);
    }

    /**
     * 禁止访问响应
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(ResponseCode.FORBIDDEN.getCode(), message);
    }

    /**
     * 资源不存在响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(ResponseCode.NOT_FOUND.getCode(), message);
    }

    /**
     * 服务器错误响应
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>(ResponseCode.SERVER_ERROR.getCode(), message);
    }

    /**
     * 设置请求ID
     */
    public ApiResponse<T> setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * 设置分页信息
     */
    public ApiResponse<T> setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
        return this;
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return this.code == ResponseCode.SUCCESS.getCode();
    }

    /**
     * 分页信息
     */
    @Data
    public static class PageInfo {
        private int pageNum;
        private int pageSize;
        private long total;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
} 