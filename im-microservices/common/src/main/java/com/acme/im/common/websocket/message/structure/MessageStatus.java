package com.acme.im.common.websocket.message.structure;

/**
 * 消息状态枚举
 * 定义消息的生命周期状态
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public enum MessageStatus {

    // ==================== 初始状态 ====================
    
    /**
     * 待处理
     */
    PENDING("pending", "待处理", 0),

    // ==================== 发送状态 ====================
    
    /**
     * 发送中
     */
    SENDING("sending", "发送中", 1),
    
    /**
     * 已发送
     */
    SENT("sent", "已发送", 2),
    
    /**
     * 发送失败
     */
    SEND_FAILED("send_failed", "发送失败", -1),

    // ==================== 传输状态 ====================
    
    /**
     * 传输中
     */
    TRANSMITTING("transmitting", "传输中", 3),
    
    /**
     * 传输失败
     */
    TRANSMIT_FAILED("transmit_failed", "传输失败", -2),

    // ==================== 接收状态 ====================
    
    /**
     * 已送达
     */
    DELIVERED("delivered", "已送达", 4),
    
    /**
     * 送达失败
     */
    DELIVERY_FAILED("delivery_failed", "送达失败", -3),
    
    /**
     * 已接收
     */
    RECEIVED("received", "已接收", 5),
    
    /**
     * 接收失败
     */
    RECEIVE_FAILED("receive_failed", "接收失败", -4),

    // ==================== 处理状态 ====================
    
    /**
     * 处理中
     */
    PROCESSING("processing", "处理中", 6),
    
    /**
     * 处理成功
     */
    SUCCESS("success", "处理成功", 7),
    
    /**
     * 处理失败
     */
    FAILED("failed", "处理失败", -5),

    // ==================== 确认状态 ====================
    
    /**
     * 已确认
     */
    ACKNOWLEDGED("acknowledged", "已确认", 8),
    
    /**
     * 已读
     */
    READ("read", "已读", 9),
    
    /**
     * 已回复
     */
    REPLIED("replied", "已回复", 10),
    
    /**
     * 已转发
     */
    FORWARDED("forwarded", "已转发", 11),

    // ==================== 特殊状态 ====================
    
    /**
     * 已撤回
     */
    RECALLED("recalled", "已撤回", -10),
    
    /**
     * 已删除
     */
    DELETED("deleted", "已删除", -11),
    
    /**
     * 已过期
     */
    EXPIRED("expired", "已过期", -12),
    
    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消", -13);

    private final String code;
    private final String description;
    private final int priority;

    MessageStatus(String code, String description, int priority) {
        this.code = code;
        this.description = description;
        this.priority = priority;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 根据代码获取状态
     */
    public static MessageStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return PENDING;
        }
        
        for (MessageStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }

    /**
     * 检查是否为初始状态
     */
    public boolean isInitial() {
        return this == PENDING;
    }

    /**
     * 检查是否为发送状态
     */
    public boolean isSending() {
        return this == SENDING || this == SENT || this == SEND_FAILED;
    }

    /**
     * 检查是否为传输状态
     */
    public boolean isTransmitting() {
        return this == TRANSMITTING || this == TRANSMIT_FAILED;
    }

    /**
     * 检查是否为接收状态
     */
    public boolean isReceiving() {
        return this == DELIVERED || this == DELIVERY_FAILED || 
               this == RECEIVED || this == RECEIVE_FAILED;
    }

    /**
     * 检查是否为处理状态
     */
    public boolean isProcessing() {
        return this == PROCESSING || this == SUCCESS || this == FAILED;
    }

    /**
     * 检查是否为确认状态
     */
    public boolean isAcknowledged() {
        return this == ACKNOWLEDGED || this == READ || 
               this == REPLIED || this == FORWARDED;
    }

    /**
     * 检查是否为特殊状态
     */
    public boolean isSpecial() {
        return this == RECALLED || this == DELETED || 
               this == EXPIRED || this == CANCELLED;
    }

    /**
     * 检查是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS || this == SENT || this == DELIVERED || 
               this == RECEIVED || this == ACKNOWLEDGED || this == READ || 
               this == REPLIED || this == FORWARDED;
    }

    /**
     * 检查是否为失败状态
     */
    public boolean isFailed() {
        return this == SEND_FAILED || this == TRANSMIT_FAILED || 
               this == DELIVERY_FAILED || this == RECEIVE_FAILED || 
               this == FAILED;
    }

    /**
     * 检查是否为最终状态
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == READ || 
               this == RECALLED || this == DELETED || this == EXPIRED || 
               this == CANCELLED;
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return this == SEND_FAILED || this == TRANSMIT_FAILED || 
               this == DELIVERY_FAILED || this == RECEIVE_FAILED || 
               this == FAILED;
    }

    /**
     * 检查是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == SENDING || this == TRANSMITTING || 
               this == PROCESSING;
    }

    /**
     * 检查是否可以撤回
     */
    public boolean canRecall() {
        return this == SENT || this == DELIVERED || this == RECEIVED || 
               this == ACKNOWLEDGED || this == READ;
    }

    /**
     * 检查是否为高优先级状态
     */
    public boolean isHighPriority() {
        return this.priority >= 5;
    }

    /**
     * 检查是否为低优先级状态
     */
    public boolean isLowPriority() {
        return this.priority < 0;
    }

    /**
     * 获取下一个状态
     */
    public MessageStatus getNextStatus() {
        switch (this) {
            case PENDING:
                return SENDING;
            case SENDING:
                return SENT;
            case SENT:
                return TRANSMITTING;
            case TRANSMITTING:
                return DELIVERED;
            case DELIVERED:
                return RECEIVED;
            case RECEIVED:
                return PROCESSING;
            case PROCESSING:
                return SUCCESS;
            case SUCCESS:
                return ACKNOWLEDGED;
            case ACKNOWLEDGED:
                return READ;
            default:
                return this;
        }
    }

    /**
     * 获取失败状态
     */
    public MessageStatus getFailureStatus() {
        switch (this) {
            case SENDING:
                return SEND_FAILED;
            case TRANSMITTING:
                return TRANSMIT_FAILED;
            case DELIVERED:
                return DELIVERY_FAILED;
            case RECEIVED:
                return RECEIVE_FAILED;
            case PROCESSING:
                return FAILED;
            default:
                return FAILED;
        }
    }

    @Override
    public String toString() {
        return code;
    }
} 