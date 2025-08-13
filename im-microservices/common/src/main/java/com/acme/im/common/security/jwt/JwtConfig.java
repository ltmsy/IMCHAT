package com.acme.im.common.security.jwt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 * 只保留实际使用的配置项
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class JwtConfig {

    /**
     * JWT属性配置
     */
    @Bean
    @ConfigurationProperties(prefix = "jwt")
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    /**
     * JWT属性配置类
     */
    @Data
    public static class JwtProperties {
        /**
         * JWT密钥（HMAC算法使用）
         */
        private String secret;

        /**
         * 访问令牌过期时间（分钟）
         */
        private long accessTokenExpiration = 60;

        /**
         * 刷新令牌过期时间（分钟）
         */
        private long refreshTokenExpiration = 10080;

        /**
         * 令牌签发者
         */
        private String issuer = "im-system";

        /**
         * 令牌受众
         */
        private String audience = "im-users";

        /**
         * 是否启用JWT
         */
        private boolean enabled = true;
    }
} 