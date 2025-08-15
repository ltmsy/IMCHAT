package com.acme.im.common.infrastructure.nats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友相关事件DTO
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class FriendEvents {

    /**
     * 好友申请结果事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendRequestResultEvent {
        private Long requestId;
        private Long fromUserId;
        private Long toUserId;
        private String status; // ACCEPTED, REJECTED, PENDING
        private String message;
        private LocalDateTime processTime;
    }

    /**
     * 好友关系建立事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendshipEstablishedEvent {
        private Long userId1;
        private Long userId2;
        private LocalDateTime establishTime;
    }

    /**
     * 好友关系解除事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendshipTerminatedEvent {
        private Long userId1;
        private Long userId2;
        private String reason;
        private LocalDateTime terminateTime;
    }

    /**
     * 好友状态变更事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendStatusChangedEvent {
        private Long userId;
        private Long friendId;
        private String oldStatus;
        private String newStatus;
        private String reason;
        private LocalDateTime changeTime;
    }
} 