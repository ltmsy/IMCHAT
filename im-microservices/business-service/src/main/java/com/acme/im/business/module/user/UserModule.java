package com.acme.im.business.module.user;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 用户管理模块
 * 包含用户注册、登录、个人资料、设备管理等所有用户相关功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = "com.acme.im.business.module.user")
public class UserModule {
    
    /**
     * 模块说明
     */
    public static final String MODULE_NAME = "用户管理模块";
    public static final String MODULE_DESCRIPTION = "负责用户生命周期管理、认证授权、个人资料维护等";
    
    /**
     * 模块包含的功能
     */
    public static final String[] FEATURES = {
        "用户注册与登录",
        "个人资料管理", 
        "设备管理",
        "隐私设置",
        "在线状态管理",
        "用户搜索"
    };
} 