package com.acme.im.common.infrastructure.database.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 数据库配置属性类
 * 使用@ConfigurationProperties自动绑定配置
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class DatabaseProperties {
    
    private DataSourceConfig primary = new DataSourceConfig();
    private DataSourceConfig secondary = new DataSourceConfig();
    
    @Data
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private HikariConfig hikari = new HikariConfig();
    }
    
    @Data
    public static class HikariConfig {
        private int maximumPoolSize = 20;
        private int minimumIdle = 5;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private String poolName = "DefaultPool";
        private String connectionTestQuery = "SELECT 1";
        private Duration validationTimeout = Duration.ofSeconds(5);
    }
} 