package com.acme.im.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 用户详情服务实现
 * 为Spring Security提供用户认证信息
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * 用户加载器接口
     * 业务服务可以实现此接口来提供真实的用户信息
     */
    public interface UserLoader {
        /**
         * 根据用户名加载用户信息
         * 
         * @param username 用户名
         * @return 用户信息，如果不存在返回null
         */
        UserInfo loadUserByUsername(String username);
        
        /**
         * 用户信息
         */
        class UserInfo {
            private final String id;
            private final String username;
            private final String password;
            private final List<String> roles;
            private final boolean enabled;
            private final boolean accountNonExpired;
            private final boolean accountNonLocked;
            private final boolean credentialsNonExpired;
            
            public UserInfo(String id, String username, String password, List<String> roles, 
                          boolean enabled, boolean accountNonExpired, boolean accountNonLocked, 
                          boolean credentialsNonExpired) {
                this.id = id;
                this.username = username;
                this.password = password;
                this.roles = roles;
                this.enabled = enabled;
                this.accountNonExpired = accountNonExpired;
                this.accountNonLocked = accountNonLocked;
                this.credentialsNonExpired = credentialsNonExpired;
            }
            
            // Getters
            public String getId() { return id; }
            public String getUsername() { return username; }
            public String getPassword() { return password; }
            public List<String> getRoles() { return roles; }
            public boolean isEnabled() { return enabled; }
            public boolean isAccountNonExpired() { return accountNonExpired; }
            public boolean isAccountNonLocked() { return accountNonLocked; }
            public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
        }
    }

    // 用户加载器，业务服务可以注入实现
    private UserLoader userLoader;

    /**
     * 设置用户加载器
     * 业务服务调用此方法注入真实的用户加载实现
     */
    public void setUserLoader(UserLoader userLoader) {
        this.userLoader = userLoader;
        log.info("用户加载器已设置: {}", userLoader.getClass().getSimpleName());
    }

    /**
     * 根据用户名加载用户详情
     * 优先使用注入的用户加载器，否则使用默认实现
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户详情: {}", username);
        
        // 优先使用注入的用户加载器
        if (userLoader != null) {
            try {
                UserLoader.UserInfo userInfo = userLoader.loadUserByUsername(username);
                if (userInfo != null) {
                    return createUserDetails(userInfo);
                }
            } catch (Exception e) {
                log.warn("用户加载器加载用户失败: username={}, error={}", username, e.getMessage());
            }
        }
        
        // 使用默认实现（开发环境或测试环境）
        log.warn("使用默认用户加载器，生产环境请注入真实的用户加载器");
        return createDefaultUserDetails(username);
    }

    /**
     * 创建用户详情对象
     */
    private UserDetails createUserDetails(UserLoader.UserInfo userInfo) {
        List<SimpleGrantedAuthority> authorities = userInfo.getRoles().stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
        
        return User.builder()
                .username(userInfo.getUsername())
                .password(userInfo.getPassword())
                .authorities(authorities)
                .accountExpired(!userInfo.isAccountNonExpired())
                .accountLocked(!userInfo.isAccountNonLocked())
                .credentialsExpired(!userInfo.isCredentialsNonExpired())
                .disabled(!userInfo.isEnabled())
                .build();
    }

    /**
     * 创建默认用户详情（仅用于开发/测试）
     */
    private UserDetails createDefaultUserDetails(String username) {
        return User.builder()
                .username(username)
                .password("") // 密码为空，因为JWT认证不需要密码
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(SecurityConstants.Permission.ROLE_USER)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
} 