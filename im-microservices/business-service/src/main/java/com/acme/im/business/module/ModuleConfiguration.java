package com.acme.im.business.module;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 业务模块配置类
 * 扫描所有业务模块，确保Spring能够发现并管理所有模块组件
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = {
    "com.acme.im.business.module.user",
    "com.acme.im.business.module.social", 
    "com.acme.im.business.module.group",
    "com.acme.im.business.module.message",
    "com.acme.im.business.module.file",
    "com.acme.im.business.module.search",
    "com.acme.im.business.module.notification",
    "com.acme.im.business.module.common"
})
public class ModuleConfiguration {
    
    /**
     * 模块配置说明
     */
    public static final String[] MODULES = {
        "用户管理模块 (user)",
        "社交管理模块 (social)", 
        "群组管理模块 (group)",
        "消息管理模块 (message)",
        "文件管理模块 (file)",
        "搜索服务模块 (search)",
        "通知服务模块 (notification)",
        "公共组件模块 (common)"
    };
} 