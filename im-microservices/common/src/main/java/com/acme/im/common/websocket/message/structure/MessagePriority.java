package com.acme.im.common.websocket.message.structure;

/**
 * 消息优先级枚举
 * 定义消息的优先级级别
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public enum MessagePriority {

    /**
     * 最低优先级
     */
    LOWEST("lowest", "最低", 1),

    /**
     * 低优先级
     */
    LOW("low", "低", 2),

    /**
     * 普通优先级
     */
    NORMAL("normal", "普通", 3),

    /**
     * 高优先级
     */
    HIGH("high", "高", 4),

    /**
     * 最高优先级
     */
    HIGHEST("highest", "最高", 5),

    /**
     * 紧急优先级
     */
    URGENT("urgent", "紧急", 6),

    /**
     * 系统优先级
     */
    SYSTEM("system", "系统", 7);

    private final String code;
    private final String description;
    private final int level;

    MessagePriority(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 根据代码获取优先级
     */
    public static MessagePriority fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return NORMAL;
        }
        
        for (MessagePriority priority : values()) {
            if (priority.code.equals(code)) {
                return priority;
            }
        }
        return NORMAL;
    }

    /**
     * 根据级别获取优先级
     */
    public static MessagePriority fromLevel(int level) {
        for (MessagePriority priority : values()) {
            if (priority.level == level) {
                return priority;
            }
        }
        return NORMAL;
    }

    /**
     * 检查是否为高优先级
     */
    public boolean isHigh() {
        return this.level >= HIGH.level;
    }

    /**
     * 检查是否为低优先级
     */
    public boolean isLow() {
        return this.level <= LOW.level;
    }

    /**
     * 检查是否为系统优先级
     */
    public boolean isSystem() {
        return this == SYSTEM;
    }

    /**
     * 检查是否为紧急优先级
     */
    public boolean isUrgent() {
        return this == URGENT;
    }

    /**
     * 检查是否高于指定优先级
     */
    public boolean isHigherThan(MessagePriority other) {
        return this.level > other.level;
    }

    /**
     * 检查是否低于指定优先级
     */
    public boolean isLowerThan(MessagePriority other) {
        return this.level < other.level;
    }

    /**
     * 检查是否等于指定优先级
     */
    public boolean isEqualTo(MessagePriority other) {
        return this.level == other.level;
    }

    /**
     * 获取下一个更高优先级
     */
    public MessagePriority getNextHigher() {
        for (MessagePriority priority : values()) {
            if (priority.level > this.level) {
                return priority;
            }
        }
        return this;
    }

    /**
     * 获取下一个更低优先级
     */
    public MessagePriority getNextLower() {
        MessagePriority result = this;
        for (MessagePriority priority : values()) {
            if (priority.level < this.level && priority.level > result.level) {
                result = priority;
            }
        }
        return result;
    }

    /**
     * 获取最高优先级
     */
    public static MessagePriority getHighest() {
        return SYSTEM;
    }

    /**
     * 获取最低优先级
     */
    public static MessagePriority getLowest() {
        return LOWEST;
    }

    /**
     * 获取默认优先级
     */
    public static MessagePriority getDefault() {
        return NORMAL;
    }

    @Override
    public String toString() {
        return code;
    }
} 