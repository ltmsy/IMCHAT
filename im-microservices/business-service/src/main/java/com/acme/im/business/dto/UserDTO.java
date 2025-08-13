package com.acme.im.business.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 */
@Data
public class UserDTO {
    
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Integer gender;
    private LocalDate birthday;
    private String region;
    private Integer status;
    private Integer onlineStatus;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 不包含敏感信息：passwordHash, salt, twoFactorSecret
} 