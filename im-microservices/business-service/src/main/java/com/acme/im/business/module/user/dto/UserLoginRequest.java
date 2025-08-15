package com.acme.im.business.module.user.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * 用户登录请求DTO
 * 包含：用户名、密码、设备号
 */
@Data
public class UserLoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "用户名只能包含字母")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "密码只能包含字母和数字")
    private String password;
    
    @NotBlank(message = "设备号不能为空")
    @Size(max = 128, message = "设备号长度不能超过128个字符")
    private String deviceId;
} 