package com.acme.im.common.security.permission;

import com.acme.im.common.infrastructure.nats.publisher.EventPublisher;
import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 权限管理器
 * 支持细粒度权限管理、角色权限映射、权限验证等功能
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class PermissionManager {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private Gson gson;

    // 权限缓存
    private final Map<String, Permission> permissionCache = new ConcurrentHashMap<>();
    private final Map<String, Role> roleCache = new ConcurrentHashMap<>();
    private final Map<String, User> userCache = new ConcurrentHashMap<>();
    
    // 权限关系缓存
    private final Map<String, Set<String>> userRoleCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> rolePermissionCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> permissionResourceCache = new ConcurrentHashMap<>();

    /**
     * 权限实体
     */
    @Data
    public static class Permission {
        private String id;
        private String name;
        private String code;
        private String description;
        private String resource;
        private String action;
        private String[] tags;
        private int priority;
        private boolean enabled;
        private long createTime;
        private long updateTime;
        private String createBy;
        private String updateBy;
        private Map<String, Object> attributes;
    }

    /**
     * 角色实体
     */
    @Data
    public static class Role {
        private String id;
        private String name;
        private String code;
        private String description;
        private String[] tags;
        private int priority;
        private boolean enabled;
        private long createTime;
        private long updateTime;
        private String createBy;
        private String updateBy;
        private Map<String, Object> attributes;
    }

    /**
     * 用户实体
     */
    @Data
    public static class User {
        private String id;
        private String username;
        private String email;
        private String phone;
        private String status;
        private String[] tags;
        private boolean enabled;
        private long createTime;
        private long updateTime;
        private String createBy;
        private String updateBy;
        private Map<String, Object> attributes;
    }

    /**
     * 权限检查结果
     */
    @Data
    public static class PermissionCheckResult {
        private boolean hasPermission;
        private String permissionCode;
        private String resource;
        private String action;
        private String reason;
        private long checkTime;
        private String userId;
        private String[] userRoles;
        private String[] userPermissions;
        private Map<String, Object> context;
    }

    /**
     * 权限变更事件
     */
    @Data
    public static class PermissionChangeEvent {
        private String eventType;
        private String targetType;
        private String targetId;
        private String userId;
        private String sessionId;
        private long timestamp;
        private Map<String, Object> metadata;
    }

    /**
     * 初始化权限管理器
     */
    public void initialize() {
        try {
            // 加载默认权限
            loadDefaultPermissions();
            
            // 加载默认角色
            loadDefaultRoles();
            
            log.info("权限管理器初始化成功");
        } catch (Exception e) {
            log.error("权限管理器初始化失败", e);
        }
    }

    /**
     * 检查用户是否有指定权限
     * 
     * @param userId 用户ID
     * @param resource 资源
     * @param action 操作
     * @return 权限检查结果
     */
    public PermissionCheckResult checkPermission(String userId, String resource, String action) {
        PermissionCheckResult result = new PermissionCheckResult();
        result.setUserId(userId);
        result.setResource(resource);
        result.setAction(action);
        result.setCheckTime(System.currentTimeMillis());
        
        try {
            // 获取用户角色
            Set<String> userRoles = userRoleCache.get(userId);
            if (userRoles == null || userRoles.isEmpty()) {
                result.setHasPermission(false);
                result.setReason("用户没有分配角色");
                return result;
            }
            
            result.setUserRoles(userRoles.toArray(new String[0]));
            
            // 检查角色权限
            Set<String> userPermissions = new HashSet<>();
            for (String roleCode : userRoles) {
                Set<String> rolePermissions = rolePermissionCache.get(roleCode);
                if (rolePermissions != null) {
                    userPermissions.addAll(rolePermissions);
                }
            }
            
            result.setUserPermissions(userPermissions.toArray(new String[0]));
            
            // 构建权限代码
            String permissionCode = resource + ":" + action;
            result.setPermissionCode(permissionCode);
            
            // 检查是否有权限
            boolean hasPermission = userPermissions.contains(permissionCode);
            result.setHasPermission(hasPermission);
            
            if (!hasPermission) {
                result.setReason("用户没有该资源的操作权限");
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("权限检查失败: userId={}, resource={}, action={}", userId, resource, action, e);
            result.setHasPermission(false);
            result.setReason("权限检查异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 添加权限
     * 
     * @param permission 权限对象
     * @return 是否添加成功
     */
    public boolean addPermission(Permission permission) {
        try {
            if (permission == null || permission.getCode() == null) {
                log.warn("权限信息无效");
                return false;
            }
            
            permissionCache.put(permission.getCode(), permission);
            
            // 发布权限变更事件
            publishPermissionChangeEvent("ADD", "PERMISSION", permission.getId(), permission.getCreateBy());
            
            log.info("权限添加成功: code={}, name={}", permission.getCode(), permission.getName());
            return true;
            
        } catch (Exception e) {
            log.error("权限添加失败: code={}", permission != null ? permission.getCode() : "null", e);
            return false;
        }
    }

    /**
     * 添加角色
     * 
     * @param role 角色对象
     * @return 是否添加成功
     */
    public boolean addRole(Role role) {
        try {
            if (role == null || role.getCode() == null) {
                log.warn("角色信息无效");
                return false;
            }
            
            roleCache.put(role.getCode(), role);
            
            // 发布权限变更事件
            publishPermissionChangeEvent("ADD", "ROLE", role.getId(), role.getCreateBy());
            
            log.info("角色添加成功: code={}, name={}", role.getCode(), role.getName());
            return true;
            
        } catch (Exception e) {
            log.error("角色添加失败: code={}", role != null ? role.getCode() : "null", e);
            return false;
        }
    }

    /**
     * 分配用户角色
     * 
     * @param userId 用户ID
     * @param roleCode 角色代码
     * @return 是否分配成功
     */
    public boolean assignUserRole(String userId, String roleCode) {
        try {
            if (userId == null || roleCode == null) {
                log.warn("用户ID或角色代码为空");
                return false;
            }
            
            userRoleCache.computeIfAbsent(userId, k -> new HashSet<>()).add(roleCode);
            
            // 发布权限变更事件
            publishPermissionChangeEvent("ASSIGN", "USER_ROLE", userId, "system");
            
            log.info("用户角色分配成功: userId={}, roleCode={}", userId, roleCode);
            return true;
            
        } catch (Exception e) {
            log.error("用户角色分配失败: userId={}, roleCode={}", userId, roleCode, e);
            return false;
        }
    }

    /**
     * 分配角色权限
     * 
     * @param roleCode 角色代码
     * @param permissionCode 权限代码
     * @return 是否分配成功
     */
    public boolean assignRolePermission(String roleCode, String permissionCode) {
        try {
            if (roleCode == null || permissionCode == null) {
                log.warn("角色代码或权限代码为空");
                return false;
            }
            
            rolePermissionCache.computeIfAbsent(roleCode, k -> new HashSet<>()).add(permissionCode);
            
            // 发布权限变更事件
            publishPermissionChangeEvent("ASSIGN", "ROLE_PERMISSION", roleCode, "system");
            
            log.info("角色权限分配成功: roleCode={}, permissionCode={}", roleCode, permissionCode);
            return true;
            
        } catch (Exception e) {
            log.error("角色权限分配失败: roleCode={}, permissionCode={}", roleCode, permissionCode, e);
            return false;
        }
    }

    /**
     * 发布权限变更事件
     */
    private void publishPermissionChangeEvent(String eventType, String targetType, String targetId, String userId) {
        try {
            PermissionChangeEvent event = new PermissionChangeEvent();
            event.setEventType(eventType);
            event.setTargetType(targetType);
            event.setTargetId(targetId);
            event.setUserId(userId);
            event.setTimestamp(System.currentTimeMillis());
            event.setMetadata(new HashMap<>());
            
            eventPublisher.publishEvent("im.permission.change", event);
            
        } catch (Exception e) {
            log.error("发布权限变更事件失败", e);
        }
    }

    /**
     * 加载默认权限
     */
    private void loadDefaultPermissions() {
        // 这里可以加载系统默认权限
        log.debug("加载默认权限");
    }

    /**
     * 加载默认角色
     */
    private void loadDefaultRoles() {
        // 这里可以加载系统默认角色
        log.debug("加载默认角色");
    }

    /**
     * 获取权限统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPermissions", permissionCache.size());
        stats.put("totalRoles", roleCache.size());
        stats.put("totalUsers", userCache.size());
        stats.put("userRoleMappings", userRoleCache.size());
        stats.put("rolePermissionMappings", rolePermissionCache.size());
        return stats;
    }
} 