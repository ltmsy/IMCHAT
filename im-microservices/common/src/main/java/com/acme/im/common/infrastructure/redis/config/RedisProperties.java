package com.acme.im.common.infrastructure.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis配置属性类
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {
    
    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    private Duration timeout = Duration.ofSeconds(2);
    
    private Lettuce lettuce = new Lettuce();
    
    @Data
    public static class Lettuce {
        private Pool pool = new Pool();
    }
    
    @Data
    public static class Pool {
        private int maxActive = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
        private Duration maxWait = Duration.ofSeconds(1);
    }
} 