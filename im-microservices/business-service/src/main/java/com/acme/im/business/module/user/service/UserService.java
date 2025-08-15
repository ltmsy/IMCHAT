package com.acme.im.business.module.user.service;

import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.entity.UserDevice;
import com.acme.im.business.module.user.entity.UserPrivacySettings;
import com.acme.im.business.module.user.entity.UserBlacklist;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 提供用户管理相关的业务逻辑
 */
public interface UserService {
    
    // ================================
    // 用户基础信息管理
    // ================================
    
    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param deviceId 设备号
     * @return 注册成功的用户信息
     */
    User registerUser(String username, String password, String deviceId);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @param deviceId 设备号
     * @return 登录令牌
     */
    String loginUser(String username, String password, String deviceId);
    
    /**
     * 用户登出
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    void logoutUser(Long userId, String deviceId);
    
    /**
     * 根据ID查找用户
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    Optional<User> findUserById(Long userId);
    
    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findUserByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     * 
     * @param email 邮箱
     * @return 用户信息
     */
    Optional<User> findUserByEmail(String email);
    
    /**
     * 根据手机号查找用户
     * 
     * @param phone 手机号
     * @return 用户信息
     */
    Optional<User> findUserByPhone(String phone);
    
    /**
     * 更新用户信息
     * 
     * @param userId 用户ID
     * @param userInfo 用户信息
     * @return 更新后的用户信息
     */
    User updateUserInfo(Long userId, User userInfo);
    
    /**
     * 更新用户头像
     * 
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     * @return 更新结果
     */
    boolean updateUserAvatar(Long userId, String avatarUrl);
    
    /**
     * 更新用户在线状态
     * 
     * @param userId 用户ID
     * @param onlineStatus 在线状态
     */
    void updateUserOnlineStatus(Long userId, Integer onlineStatus);
    
    /**
     * 更新用户最后活跃时间
     * 
     * @param userId 用户ID
     */
    void updateUserLastActiveTime(Long userId);
    
    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 重置密码
     * 
     * @param email 邮箱
     * @return 重置结果
     */
    boolean resetPassword(String email);
    
    /**
     * 启用/禁用用户
     * 
     * @param userId 用户ID
     * @param status 状态
     * @return 操作结果
     */
    boolean updateUserStatus(Long userId, Integer status);
    
    // ================================
    // 用户设备管理
    // ================================
    
    /**
     * 获取用户设备列表
     * 
     * @param userId 用户ID
     * @return 设备列表
     */
    List<UserDevice> getUserDevices(Long userId);
    
    /**
     * 添加用户设备
     * 
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     * @return 设备信息
     */
    UserDevice addUserDevice(Long userId, UserDevice deviceInfo);
    
    /**
     * 移除用户设备
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return 移除结果
     */
    boolean removeUserDevice(Long userId, String deviceId);
    
    /**
     * 更新设备在线状态
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param isOnline 是否在线
     */
    void updateDeviceOnlineStatus(Long userId, String deviceId, boolean isOnline);
    
    /**
     * 设置设备信任状态
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param isTrusted 是否受信任
     * @return 设置结果
     */
    boolean setDeviceTrusted(Long userId, String deviceId, boolean isTrusted);
    
    /**
     * 更新设备最后活跃时间
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    void updateDeviceLastActive(Long userId, String deviceId);
    
    // ================================
    // 用户隐私设置
    // ================================
    
    /**
     * 获取用户隐私设置
     * 
     * @param userId 用户ID
     * @return 隐私设置
     */
    Optional<UserPrivacySettings> getUserPrivacySettings(Long userId);
    
    /**
     * 更新用户隐私设置
     * 
     * @param userId 用户ID
     * @param privacySettings 隐私设置
     * @return 更新后的隐私设置
     */
    UserPrivacySettings updateUserPrivacySettings(Long userId, UserPrivacySettings privacySettings);
    
    // ================================
    // 用户黑名单管理
    // ================================
    
    /**
     * 获取用户黑名单
     * 
     * @param userId 用户ID
     * @return 黑名单列表
     */
    List<UserBlacklist> getUserBlacklist(Long userId);
    
    /**
     * 添加用户到黑名单
     * 
     * @param userId 用户ID
     * @param blockedUserId 被拉黑用户ID
     * @param blockType 拉黑类型
     * @param reason 拉黑原因
     * @return 黑名单记录
     */
    UserBlacklist addUserToBlacklist(Long userId, Long blockedUserId, Integer blockType, String reason);
    
    /**
     * 从黑名单移除用户
     * 
     * @param userId 用户ID
     * @param blockedUserId 被拉黑用户ID
     * @return 移除结果
     */
    boolean removeUserFromBlacklist(Long userId, Long blockedUserId);
    
    /**
     * 检查用户是否在黑名单中
     * 
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @return 是否在黑名单中
     */
    boolean isUserInBlacklist(Long userId, Long targetUserId);
    
    // ================================
    // 用户搜索和统计
    // ================================
    
    /**
     * 搜索用户
     * 
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> searchUsers(String keyword, int limit);
    
    /**
     * 根据用户名搜索用户
     * 
     * @param username 用户名关键词
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> searchUsersByUsername(String username, int limit);
    
    /**
     * 根据昵称搜索用户
     * 
     * @param nickname 昵称关键词
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> searchUsersByNickname(String nickname, int limit);
    
    /**
     * 高级搜索用户
     * 
     * @param username 用户名
     * @param nickname 昵称
     * @param email 邮箱
     * @param phone 手机号
     * @param region 地区
     * @param gender 性别
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> advancedSearchUsers(String username, String nickname, String email, 
                                  String phone, String region, Integer gender, int limit);
    
    /**
     * 获取在线用户数量
     * 
     * @return 在线用户数量
     */
    long getOnlineUserCount();
    
    /**
     * 获取最近注册的用户
     * 
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> getRecentUsers(int limit);
    
    /**
     * 获取用户统计信息
     * 
     * @return 统计信息
     */
    UserStatistics getUserStatistics();
    
    /**
     * 用户统计信息
     */
    class UserStatistics {
        private long totalUsers;
        private long onlineUsers;
        private long activeUsers;
        private long newUsersToday;
        
        // getters and setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        
        public long getOnlineUsers() { return onlineUsers; }
        public void setOnlineUsers(long onlineUsers) { this.onlineUsers = onlineUsers; }
        
        public long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
        
        public long getNewUsersToday() { return newUsersToday; }
        public void setNewUsersToday(long newUsersToday) { this.newUsersToday = newUsersToday; }
    }
} 