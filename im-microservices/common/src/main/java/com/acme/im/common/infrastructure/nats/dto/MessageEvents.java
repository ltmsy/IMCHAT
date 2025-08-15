package com.acme.im.common.infrastructure.nats.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息相关事件DTO类
 * 统一管理所有消息相关的事件数据结构
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class MessageEvents {

    /**
     * 消息创建事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageCreatedEvent {
        private Long messageId;
        private Long conversationId;
        private Long senderId;
        private Long seq;
        private Integer msgType;
        private String content;
        private String contentExtra;
        private Long replyToId;
        private Long forwardFromId;
        private String mentions;
        private LocalDateTime serverTimestamp;
        private LocalDateTime timestamp;
    }

    /**
     * 消息撤回事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageRecalledEvent {
        private Long conversationId;
        private Long messageId;
        private Long operatorId;
        private String reason;
        private LocalDateTime timestamp;
    }

    /**
     * 消息编辑事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageEditedEvent {
        private Long conversationId;
        private Long messageId;
        private Long operatorId;
        private String newContent;
        private String oldContent;
        private Integer editCount;
        private LocalDateTime timestamp;
    }

    /**
     * 消息置顶事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessagePinnedEvent {
        private Long conversationId;
        private Long messageId;
        private Long operatorId;
        private boolean pinned;
        private LocalDateTime timestamp;
    }

    /**
     * 消息删除事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDeletedEvent {
        private Long conversationId;
        private Long messageId;
        private Long operatorId;
        private String reason;
        private LocalDateTime timestamp;
    }

    /**
     * 消息已读事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageReadEvent {
        private Long conversationId;
        private Long messageId;
        private Long userId;
        private Long deviceId;
        private LocalDateTime readTime;
        private LocalDateTime timestamp;
    }

    /**
     * 消息转发事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageForwardEvent {
        private Long sourceConversationId;
        private Long targetConversationId;
        private Long messageId;
        private Long forwarderId;
        private String forwardReason;
        private LocalDateTime timestamp;
    }

    /**
     * 消息回复事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageReplyEvent {
        private Long conversationId;
        private Long messageId;
        private Long replyToId;
        private Long senderId;
        private String content;
        private LocalDateTime timestamp;
    }

    /**
     * 消息提及事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageMentionEvent {
        private Long conversationId;
        private Long messageId;
        private Long senderId;
        private List<Long> mentionedUserIds;
        private String content;
        private LocalDateTime timestamp;
    }
} 