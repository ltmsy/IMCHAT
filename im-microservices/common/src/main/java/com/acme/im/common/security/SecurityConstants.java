package com.acme.im.common.security;

import java.util.Arrays;
import java.util.List;

/**
 * 安全常量类
 * 统一管理安全相关的常量，避免重复配置
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 公开接口路径（无需认证）
     */
    public static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/users/register",
        "/api/users/login",
        "/api/test/**",
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/ws/**",
        "/ws-native/**",
        "/error"
    );

    /**
     * 检查路径是否为公开接口
     * 
     * @param requestURI 请求URI
     * @return 是否为公开接口
     */
    public static boolean isPublicEndpoint(String requestURI) {
        if (requestURI == null) {
            return false;
        }
        
        return PUBLIC_ENDPOINTS.stream()
            .anyMatch(pattern -> {
                if (pattern.endsWith("/**")) {
                    return requestURI.startsWith(pattern.substring(0, pattern.length() - 2));
                }
                return requestURI.startsWith(pattern);
            });
    }

    /**
     * 检查路径是否为受保护接口（需要认证）
     * 
     * @param requestURI 请求URI
     * @return 是否为受保护接口
     */
    public static boolean isProtectedEndpoint(String requestURI) {
        return !isPublicEndpoint(requestURI);
    }

    /**
     * 认证相关常量
     */
    public static final class Auth {
        public static final String TOKEN_PREFIX = "Bearer ";
        public static final String TOKEN_HEADER = "Authorization";
        public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
        
        private Auth() {}
    }

    /**
     * 权限相关常量
     */
    public static final class Permission {
        public static final String ROLE_USER = "USER";
        public static final String ROLE_ADMIN = "ADMIN";
        public static final String ROLE_MODERATOR = "MODERATOR";
        
        private Permission() {}
    }
} 