package com.acme.im.business.module.user.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 添加到黑名单请求DTO
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToBlacklistRequest {
    
    /**
     * 被拉黑用户ID
     */
    private Long blockedUserId;
    
    /**
     * 拉黑类型：1-消息，2-好友申请，4-查看资料（可位运算组合）
     */
    private Integer blockType;
    
    /**
     * 拉黑原因
     */
    private String reason;
} 