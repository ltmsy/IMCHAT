package com.acme.im.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 通信服务启动类
 * 负责实时通信、消息路由、推送等功能
 * 
 * 技术栈：
 * - Spring WebFlux：响应式Web框架
 * - NATS：事件总线和消息队列
 * - Redis：连接管理和消息缓存
 * - MySQL：消息存储（分表）
 * 
 * 核心功能：
 * - WebSocket连接管理
 * - 实时消息推送
 * - 消息路由分发
 * - 离线消息处理
 * - 消息序列管理
 * - 幂等性处理
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {
        "com.acme.im.communication",
        "com.acme.im.common"
})
public class CommunicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationServiceApplication.class, args);
    }
} 