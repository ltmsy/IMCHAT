package com.acme.im.business.module.social;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 社交管理模块
 * 包含好友关系、好友申请、黑名单管理等社交功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = "com.acme.im.business.module.social")
public class SocialModule {
    
    /**
     * 模块说明
     */
    public static final String MODULE_NAME = "社交管理模块";
    public static final String MODULE_DESCRIPTION = "负责好友关系管理、社交网络维护等";
    
    /**
     * 模块包含的功能
     */
    public static final String[] FEATURES = {
        "好友申请管理",
        "好友关系维护",
        "黑名单管理",
        "好友分组管理",
        "社交推荐"
    };
} 