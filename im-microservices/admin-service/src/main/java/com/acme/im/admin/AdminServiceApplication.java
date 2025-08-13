package com.acme.im.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin Service 启动类
 * 负责系统监控、运维管理、配置管理、安全审计等
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.acme.im.admin",
        "com.acme.im.common"
    }
)
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
} 