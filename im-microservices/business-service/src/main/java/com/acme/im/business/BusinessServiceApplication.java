package com.acme.im.business;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * 业务服务启动类
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.acme.im")
@MapperScan("com.acme.im.business.repository")
public class BusinessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
} 