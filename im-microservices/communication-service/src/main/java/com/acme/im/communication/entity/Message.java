package com.acme.im.communication.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息实体类
 * 对应分表 messages_00 ~ messages_31
 * 按会话ID取模分表存储
 * 
 * 表结构严格按照 messages_sharding_design.sql 定义
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    /**
     * 消息ID - 主键自增
     */
    private Long id;
    
    /**
     * 会话ID - 分表路由字段
     */
    private Long conversationId;
    
    /**
     * 消息序号 - 会话内递增
     */
    private Long seq;

    /**
     * 客户端消息ID - 幂等性标识
     */
    private String clientMsgId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 消息类型
     * 1-文本，2-图片，3-文件，4-语音，5-视频，6-位置，7-名片，8-系统消息
     */
    private Integer msgType;

    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 扩展内容（文件信息、位置信息等）
     * JSON格式存储
     */
    private String contentExtra;

    /**
     * 回复消息ID
     */
    private Long replyToId;
    
    /**
     * 转发来源消息ID
     */
    private Long forwardFromId;
    
    /**
     * 提及的用户ID列表
     * JSON格式存储
     */
    private String mentions;

    /**
     * 是否置顶
     */
    private Integer isPinned;

    /**
     * 是否已编辑
     */
    private Integer isEdited;

    /**
     * 编辑次数
     */
    private Integer editCount;
    
    /**
     * 最后编辑时间
     */
    private LocalDateTime lastEditAt;
    
    /**
     * 是否已撤回
     */
    private Integer isRecalled;
    
    /**
     * 撤回原因
     */
    private String recallReason;
    
    /**
     * 撤回时间
     */
    private LocalDateTime recalledAt;
    
    /**
     * 状态：0-已删除，1-正常，2-审核中，3-已拒绝
     */
    private Integer status;

    /**
     * 服务器时间戳（毫秒精度）
     */
    private LocalDateTime serverTimestamp;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // ================================
    // 业务方法
    // ================================

    /**
     * 判断是否为文本消息
     */
    public boolean isTextMessage() {
        return msgType != null && msgType == 1;
    }
    
    /**
     * 判断是否为图片消息
     */
    public boolean isImageMessage() {
        return msgType != null && msgType == 2;
    }
    
    /**
     * 判断是否为文件消息
     */
    public boolean isFileMessage() {
        return msgType != null && msgType == 3;
    }
    
    /**
     * 判断是否为系统消息
     */
    public boolean isSystemMessage() {
        return msgType != null && msgType == 8;
    }

    /**
     * 判断是否已撤回
     */
    public boolean isRecalled() {
        return isRecalled != null && isRecalled == 1;
    }

    /**
     * 判断是否已编辑
     */
    public boolean isEdited() {
        return isEdited != null && isEdited == 1;
    }
    
    /**
     * 判断是否置顶
     */
    public boolean isPinned() {
        return isPinned != null && isPinned == 1;
    }
    
    /**
     * 判断是否正常状态
     */
    public boolean isNormal() {
        return status != null && status == 1;
    }
    
    /**
     * 判断是否已删除
     */
    public boolean isDeleted() {
        return status != null && status == 0;
    }

    // ================================
    // 消息类型枚举
    // ================================
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        TEXT(1, "文本消息"),
        IMAGE(2, "图片消息"),
        FILE(3, "文件消息"),
        VOICE(4, "语音消息"),
        VIDEO(5, "视频消息"),
        LOCATION(6, "位置消息"),
        CARD(7, "名片消息"),
        SYSTEM(8, "系统消息");

        private final int code;
        private final String description;

        MessageType(int code, String description) {
            this.code = code;
            this.description = description;
    }
    
        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static MessageType fromCode(int code) {
            for (MessageType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的消息类型: " + code);
        }
    }
    
    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        DELETED(0, "已删除"),
        NORMAL(1, "正常"),
        AUDITING(2, "审核中"),
        REJECTED(3, "已拒绝");

        private final int code;
        private final String description;

        MessageStatus(int code, String description) {
            this.code = code;
            this.description = description;
    }
    
        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static MessageStatus fromCode(int code) {
            for (MessageStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("未知的消息状态: " + code);
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 获取分表名称
     */
    public String getTableName() {
        if (conversationId == null) {
            return "messages_00";
        }
        int shardIndex = (int) (conversationId % 32);
        return String.format("messages_%02d", shardIndex);
    }

    /**
     * 获取分表索引
     */
    public int getShardIndex() {
        if (conversationId == null) {
            return 0;
        }
        return (int) (conversationId % 32);
    }

    /**
     * 检查分表索引是否有效
     */
    public boolean isValidShardIndex() {
        return conversationId != null && conversationId > 0;
    }

    /**
     * 获取消息类型描述
     */
    public String getMessageTypeDescription() {
        if (msgType == null) {
            return "未知";
        }
        return MessageType.fromCode(msgType).getDescription();
    }

    /**
     * 检查消息是否可编辑
     */
    public boolean isEditable() {
        if (isRecalled != null && isRecalled == 1) {
            return false; // 已撤回的消息不可编辑
        }
        if (msgType == null) {
            return false;
        }
        // 只有文本消息可以编辑
        return msgType == MessageType.TEXT.getCode();
    }

    /**
     * 检查消息是否可撤回
     */
    public boolean isRecallable() {
        if (isRecalled != null && isRecalled == 1) {
            return false; // 已撤回的消息不可再次撤回
        }
        if (serverTimestamp == null) {
            return false;
        }
        // 24小时内的消息可以撤回
        return serverTimestamp.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * 检查是否为媒体消息
     */
    public boolean isMediaMessage() {
        if (msgType == null) {
            return false;
        }
        return msgType == MessageType.IMAGE.getCode() || 
               msgType == MessageType.VIDEO.getCode() || 
               msgType == MessageType.FILE.getCode();
    }
} 