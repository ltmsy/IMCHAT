package com.acme.im.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.acme.im.common.security.jwt.JwtTokenProvider;
import com.acme.im.common.security.jwt.JwtAuthenticationFilter;

/**
 * 安全配置类（Spring MVC版本）
 * 提供密码编码器和Spring MVC安全配置
 * 
 * 职责分工：
 * - 此配置类负责路径匹配和权限控制（白名单管理）
 * - JwtAuthenticationFilter负责JWT令牌验证和认证上下文设置
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, 
                         JwtAuthenticationFilter jwtAuthenticationFilter,
                         CorsConfigurationSource corsConfigurationSource) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * 密码编码器
     * 使用BCrypt算法进行密码哈希
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring MVC安全配置
     * 配置HTTP安全策略
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 配置CORS跨域支持
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // 禁用CSRF保护（API服务通常不需要）
            .csrf(csrf -> csrf.disable())
            // 禁用HTTP Basic认证
            .httpBasic(httpBasic -> httpBasic.disable())
            // 禁用表单登录
            .formLogin(formLogin -> formLogin.disable())
            // 禁用登出
            .logout(logout -> logout.disable())
            // 设置会话管理为无状态
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置授权规则
            .authorizeHttpRequests(authz -> authz
                // 使用统一的白名单配置，决定哪些路径无需认证
                .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS.toArray(new String[0])).permitAll()
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 