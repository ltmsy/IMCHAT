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
 * 支持新的消息格式和特殊操作
 * 
 * @author IM开发团队
 * @since 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    // ================================
    // 基础字段
    // ================================
    
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
     * 接收者ID（用户ID或群组ID）
     */
    private Long receiverId;
    
    // ================================
    // 消息类型和内容
    // ================================
    
    /**
     * 消息类型
     * 0-文本，1-图片，2-文件，3-语音，4-视频，5-位置，6-名片，7-系统消息
     * 10-编辑消息，11-引用消息，12-转发消息，13-撤回消息，14-删除消息
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

    // ================================
    // 特殊操作字段
    // ================================
    
    /**
     * 原始消息ID（编辑、引用、转发时使用）
     */
    private Long originalMessageId;
    
    /**
     * 操作类型：edit-编辑，quote-引用，forward-转发，recall-撤回，delete-删除
     */
    private String operationType;

    // ================================
    // 引用消息信息
    // ================================
    
    /**
     * 被引用的消息ID
     */
    private Long quotedMessageId;
    
    /**
     * 被引用的消息内容（截取）
     */
    private String quotedContent;
    
    /**
     * 被引用消息的发送者ID
     */
    private Long quotedSenderId;
    
    /**
     * 被引用消息的内容类型
     */
    private Integer quotedContentType;

    // ================================
    // 转发消息信息
    // ================================
    
    /**
     * 原始会话ID
     */
    private Long originalConversationId;
    
    /**
     * 原始发送者ID
     */
    private Long originalSenderId;
    
    /**
     * 转发原因
     */
    private String forwardReason;

    // ================================
    // 编辑消息信息
    // ================================
    
    /**
     * 原始内容
     */
    private String originalContent;
    
    /**
     * 编辑原因
     */
    private String editReason;

    // ================================
    // 回复和转发（保留原有字段，向后兼容）
    // ================================
    
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

    // ================================
    // 置顶相关字段
    // ================================
    
    /**
     * 是否置顶
     */
    private Integer isPinned;
    
    /**
     * 置顶范围：0-仅我，1-所有人
     */
    private Integer pinScope;
    
    /**
     * 置顶操作者ID
     */
    private Long pinnedBy;
    
    /**
     * 置顶时间
     */
    private LocalDateTime pinnedAt;

    // ================================
    // 编辑相关字段
    // ================================
    
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

    // ================================
    // 撤回相关字段
    // ================================
    
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

    // ================================
    // 删除相关字段
    // ================================
    
    /**
     * 是否已删除
     */
    private Integer isDeleted;
    
    /**
     * 删除范围：0-仅我，1-所有人
     */
    private Integer deleteScope;
    
    /**
     * 删除操作者ID
     */
    private Long deletedBy;
    
    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;
    
    /**
     * 删除原因
     */
    private String deleteReason;

    // ================================
    // 状态和时间字段
    // ================================
    
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
    // 多端同步字段
    // ================================
    
    /**
     * 设备ID（多端同步）
     */
    private String deviceId;
    
    /**
     * 消息来源（web/mobile/desktop）
     */
    private String source;
    
    /**
     * 消息版本
     */
    private String version;

    // ================================
    // 业务方法
    // ================================

    /**
     * 判断是否为文本消息
     */
    public boolean isTextMessage() {
        return msgType != null && msgType == 0;
    }
    
    /**
     * 判断是否为图片消息
     */
    public boolean isImageMessage() {
        return msgType != null && msgType == 1;
    }
    
    /**
     * 判断是否为文件消息
     */
    public boolean isFileMessage() {
        return msgType != null && msgType == 2;
    }
    
    /**
     * 判断是否为语音消息
     */
    public boolean isVoiceMessage() {
        return msgType != null && msgType == 3;
    }
    
    /**
     * 判断是否为视频消息
     */
    public boolean isVideoMessage() {
        return msgType != null && msgType == 4;
    }
    
    /**
     * 判断是否为位置消息
     */
    public boolean isLocationMessage() {
        return msgType != null && msgType == 5;
    }
    
    /**
     * 判断是否为名片消息
     */
    public boolean isCardMessage() {
        return msgType != null && msgType == 6;
    }
    
    /**
     * 判断是否为系统消息
     */
    public boolean isSystemMessage() {
        return msgType != null && msgType == 7;
    }

    /**
     * 判断是否为编辑消息
     */
    public boolean isEditMessage() {
        return msgType != null && msgType == 10;
    }
    
    /**
     * 判断是否为引用消息
     */
    public boolean isQuoteMessage() {
        return msgType != null && msgType == 11;
    }
    
    /**
     * 判断是否为转发消息
     */
    public boolean isForwardMessage() {
        return msgType != null && msgType == 12;
    }
    
    /**
     * 判断是否为撤回消息
     */
    public boolean isRecallMessage() {
        return msgType != null && msgType == 13;
    }
    
    /**
     * 判断是否为删除消息
     */
    public boolean isDeleteMessage() {
        return msgType != null && msgType == 14;
    }

    /**
     * 判断是否为特殊操作消息
     */
    public boolean isSpecialOperationMessage() {
        return isEditMessage() || isQuoteMessage() || isForwardMessage() || 
               isRecallMessage() || isDeleteMessage();
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
     * 判断是否为全局置顶（所有人可见）
     */
    public boolean isGlobalPinned() {
        return isPinned() && pinScope != null && pinScope == 1;
    }
    
    /**
     * 判断是否为个人置顶（仅我可见）
     */
    public boolean isPersonalPinned() {
        return isPinned() && pinScope != null && pinScope == 0;
    }
    
    /**
     * 判断是否已删除
     */
    public boolean isDeleted() {
        return isDeleted != null && isDeleted == 1;
    }
    
    /**
     * 判断是否为全局删除（所有人不可见）
     */
    public boolean isGlobalDeleted() {
        return isDeleted() && deleteScope != null && deleteScope == 1;
    }
    
    /**
     * 判断是否为个人删除（仅我不可见）
     */
    public boolean isPersonalDeleted() {
        return isDeleted() && deleteScope != null && deleteScope == 0;
    }
    
    /**
     * 判断是否正常状态
     */
    public boolean isNormal() {
        return status != null && status == 1;
    }

    // ================================
    // 消息类型枚举
    // ================================
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        TEXT(0, "文本消息"),
        IMAGE(1, "图片消息"),
        FILE(2, "文件消息"),
        VOICE(3, "语音消息"),
        VIDEO(4, "视频消息"),
        LOCATION(5, "位置消息"),
        CARD(6, "名片消息"),
        SYSTEM(7, "系统消息"),
        EDIT(10, "编辑消息"),
        QUOTE(11, "引用消息"),
        FORWARD(12, "转发消息"),
        RECALL(13, "撤回消息"),
        DELETE(14, "删除消息");

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
                    return null;
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
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        EDIT("edit", "编辑"),
        QUOTE("quote", "引用"),
        FORWARD("forward", "转发"),
        RECALL("recall", "撤回"),
        DELETE("delete", "删除"),
        PIN("pin", "置顶"),
        UNPIN("unpin", "取消置顶");

        private final String code;
        private final String description;

        OperationType(String code, String description) {
            this.code = code;
            this.description = description;
        }
    
        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static OperationType fromCode(String code) {
            for (OperationType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的操作类型: " + code);
        }
    }
    
    /**
     * 操作范围枚举
     */
    public enum OperationScope {
        SELF_ONLY(0, "仅我"),
        ALL_USERS(1, "所有人");

        private final int code;
        private final String description;

        OperationScope(int code, String description) {
            this.code = code;
            this.description = description;
        }
    
        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static OperationScope fromCode(int code) {
            for (OperationScope scope : values()) {
                if (scope.code == code) {
                    return scope;
                }
            }
            throw new IllegalArgumentException("未知的操作范围: " + code);
        }
    }

    // ================================
    // 业务方法
    // ================================

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
        try {
            return MessageType.fromCode(msgType).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知类型(" + msgType + ")";
        }
    }

    /**
     * 检查消息是否可编辑
     */
    public boolean isEditable() {
        if (isRecalled() || isDeleted()) {
            return false; // 已撤回或删除的消息不可编辑
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
        if (isRecalled() || isDeleted()) {
            return false; // 已撤回或删除的消息不可再次撤回
        }
        if (serverTimestamp == null) {
            return false;
        }
        // 24小时内的消息可以撤回
        return serverTimestamp.isAfter(LocalDateTime.now().minusHours(24));
    }
    
    /**
     * 检查消息是否可删除
     */
    public boolean isDeletable() {
        if (isDeleted()) {
            return false; // 已删除的消息不可再次删除
        }
        return true;
    }
    
    /**
     * 检查消息是否可置顶
     */
    public boolean isPinnable() {
        if (isDeleted()) {
            return false; // 已删除的消息不可置顶
        }
        return true;
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
               msgType == MessageType.FILE.getCode() ||
               msgType == MessageType.VOICE.getCode();
    }
    
} 