package com.acme.im.business.module.group;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 群组管理模块
 * 包含群组创建、成员管理、权限控制、群组设置等群组相关功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = "com.acme.im.business.module.group")
public class GroupModule {
    
    /**
     * 模块说明
     */
    public static final String MODULE_NAME = "群组管理模块";
    public static final String MODULE_DESCRIPTION = "负责群组生命周期管理、成员管理、权限控制等";
    
    /**
     * 模块包含的功能
     */
    public static final String[] FEATURES = {
        "群组创建与解散",
        "成员邀请与管理",
        "群组权限控制",
        "群组设置管理",
        "群组公告管理",
        "群组搜索"
    };
} 