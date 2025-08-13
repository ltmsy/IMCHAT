package com.acme.im.business.service.impl;

import com.acme.im.business.entity.User;
import com.acme.im.business.entity.UserDevice;
import com.acme.im.business.entity.UserPrivacySettings;
import com.acme.im.business.entity.UserBlacklist;
import com.acme.im.business.repository.UserRepository;
import com.acme.im.business.service.UserService;
import com.acme.im.common.infrastructure.database.annotation.DataSource;
import com.acme.im.common.security.encryption.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户服务实现类
 * 使用MyBatis-Plus
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final EncryptionUtils encryptionUtils;
    
    // ================================
    // 用户基础信息管理
    // ================================
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public User registerUser(String username, String password, String deviceId) {
        log.info("开始用户注册: username={}, deviceId={}", username, deviceId);
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在: " + username);
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setNickname(username); // 默认昵称为用户名
        user.setStatus(1); // 正常状态
        user.setOnlineStatus(0); // 离线状态
        user.setGender(0); // 未知性别
        user.setTwoFactorEnabled(false);
        
        // 生成密码盐值和哈希
        String salt = encryptionUtils.generateSalt(16);
        String passwordWithSalt = password + salt;
        String passwordHash = encryptionUtils.hash(passwordWithSalt, "SHA-256");
        user.setSalt(salt);
        user.setPasswordHash(passwordHash);
        
        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setLastActiveAt(now);
        
        // 保存用户
        userRepository.insert(user);
        User savedUser = user;
        
        log.info("用户注册成功: userId={}, username={}, deviceId={}", savedUser.getId(), savedUser.getUsername(), deviceId);
        
        return savedUser;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public String loginUser(String username, String password, String deviceId) {
        log.info("开始用户登录: username={}, deviceId={}", username, deviceId);
        
        // 查找用户
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在: " + username);
        }
        
        User user = userOpt.get();
        
        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户状态异常，无法登录");
        }
        
        // 验证密码
        String inputPasswordWithSalt = password + user.getSalt();
        String inputPasswordHash = encryptionUtils.hash(inputPasswordWithSalt, "SHA-256");
        if (!user.getPasswordHash().equals(inputPasswordHash)) {
            throw new RuntimeException("密码错误");
        }
        
        // 更新用户状态
        user.setOnlineStatus(1); // 在线
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.updateById(user);
        
        // 生成登录令牌
        String token = generateLoginToken(user.getId(), deviceId);
        
        log.info("用户登录成功: userId={}, username={}, deviceId={}", user.getId(), user.getUsername(), deviceId);
        return token;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public void logoutUser(Long userId, String deviceId) {
        log.info("用户登出: userId={}, deviceId={}", userId, deviceId);
        
        User user = userRepository.selectById(userId);
        if (user != null) {
            user.setOnlineStatus(0); // 离线
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.updateById(user);
        }
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public Optional<User> findUserById(Long userId) {
        User user = userRepository.selectById(userId);
        return Optional.ofNullable(user);
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public Optional<User> findUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public User updateUserInfo(Long userId, User userInfo) {
        log.info("更新用户信息: userId={}", userId);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        
        // 更新允许修改的字段
        if (userInfo.getNickname() != null) {
            user.setNickname(userInfo.getNickname());
        }
        if (userInfo.getEmail() != null) {
            user.setEmail(userInfo.getEmail());
        }
        if (userInfo.getPhone() != null) {
            user.setPhone(userInfo.getPhone());
        }
        if (userInfo.getAvatarUrl() != null) {
            user.setAvatarUrl(userInfo.getAvatarUrl());
        }
        if (userInfo.getSignature() != null) {
            user.setSignature(userInfo.getSignature());
        }
        if (userInfo.getGender() != null) {
            user.setGender(userInfo.getGender());
        }
        if (userInfo.getBirthday() != null) {
            user.setBirthday(userInfo.getBirthday());
        }
        if (userInfo.getRegion() != null) {
            user.setRegion(userInfo.getRegion());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);
        
        log.info("用户信息更新成功: userId={}", userId);
        return user;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public boolean updateUserAvatar(Long userId, String avatarUrl) {
        log.info("更新用户头像: userId={}, avatarUrl={}", userId, avatarUrl);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        int result = userRepository.updateById(user);
        
        boolean success = result > 0;
        log.info("用户头像更新{}: userId={}", success ? "成功" : "失败", userId);
        return success;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public void updateUserOnlineStatus(Long userId, Integer onlineStatus) {
        log.info("更新用户在线状态: userId={}, onlineStatus={}", userId, onlineStatus);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        
        user.setOnlineStatus(onlineStatus);
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.updateById(user);
        
        log.info("用户在线状态更新成功: userId={}, onlineStatus={}", userId, onlineStatus);
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public void updateUserLastActiveTime(Long userId) {
        User user = userRepository.selectById(userId);
        if (user != null) {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.updateById(user);
        }
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改用户密码: userId={}", userId);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        
        // 验证旧密码
        String oldPasswordWithSalt = oldPassword + user.getSalt();
        String oldPasswordHash = encryptionUtils.hash(oldPasswordWithSalt, "SHA-256");
        if (!user.getPasswordHash().equals(oldPasswordHash)) {
            log.warn("旧密码错误: userId={}", userId);
            return false;
        }
        
        // 生成新的密码盐值和哈希
        String newSalt = encryptionUtils.generateSalt(16);
        String newPasswordWithSalt = newPassword + newSalt;
        String newPasswordHash = encryptionUtils.hash(newPasswordWithSalt, "SHA-256");
        
        user.setSalt(newSalt);
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(LocalDateTime.now());
        int result = userRepository.updateById(user);
        
        boolean success = result > 0;
        log.info("用户密码修改{}: userId={}", success ? "成功" : "失败", userId);
        return success;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public boolean resetPassword(String email) {
        log.info("密码重置功能待实现: email={}", email);
        // TODO: 实现密码重置功能
        return false;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public boolean updateUserStatus(Long userId, Integer status) {
        log.info("更新用户状态: userId={}, status={}", userId, status);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        int result = userRepository.updateById(user);
        
        boolean success = result > 0;
        log.info("用户状态更新{}: userId={}, status={}", success ? "成功" : "失败", userId, status);
        return success;
    }
    
    // ================================
    // 用户设备管理
    // ================================
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public List<UserDevice> getUserDevices(Long userId) {
        // TODO: 实现获取用户设备列表
        return List.of();
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public UserDevice addUserDevice(Long userId, UserDevice deviceInfo) {
        // TODO: 实现添加用户设备
        return null;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public boolean removeUserDevice(Long userId, String deviceId) {
        // TODO: 实现移除用户设备
        return false;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public void updateDeviceOnlineStatus(Long userId, String deviceId, boolean isOnline) {
        // TODO: 实现更新设备在线状态
    }
    
    // ================================
    // 用户隐私设置
    // ================================
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public Optional<UserPrivacySettings> getUserPrivacySettings(Long userId) {
        // TODO: 实现获取用户隐私设置
        return Optional.empty();
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public UserPrivacySettings updateUserPrivacySettings(Long userId, UserPrivacySettings privacySettings) {
        // TODO: 实现更新用户隐私设置
        return null;
    }
    
    // ================================
    // 用户黑名单管理
    // ================================
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public List<UserBlacklist> getUserBlacklist(Long userId) {
        // TODO: 实现获取用户黑名单
        return List.of();
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public UserBlacklist addUserToBlacklist(Long userId, Long blockedUserId, Integer blockType, String reason) {
        // TODO: 实现添加用户到黑名单
        return null;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.WRITE)
    public boolean removeUserFromBlacklist(Long userId, Long blockedUserId) {
        // TODO: 实现从黑名单移除用户
        return false;
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public boolean isUserInBlacklist(Long userId, Long targetUserId) {
        // TODO: 实现检查用户是否在黑名单中
        return false;
    }
    
    // ================================
    // 用户搜索和统计
    // ================================
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public List<User> searchUsers(String keyword, int limit) {
        // TODO: 实现用户搜索
        return List.of();
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public long getOnlineUserCount() {
        return userRepository.countOnlineUsers();
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public List<User> getRecentUsers(int limit) {
        return userRepository.findRecentUsers(limit);
    }
    
    @Override
    @DataSource(operation = DataSource.OperationType.READ)
    public UserStatistics getUserStatistics() {
        UserStatistics stats = new UserStatistics();
        stats.setTotalUsers(userRepository.selectCount(null));
        stats.setOnlineUsers(userRepository.countOnlineUsers());
        // TODO: 实现其他统计信息
        return stats;
    }
    
    // ================================
    // 辅助方法
    // ================================
    
    /**
     * 生成登录令牌
     */
    private String generateLoginToken(Long userId, String deviceId) {
        // 这里应该调用JWT服务生成令牌
        // 暂时返回一个简单的UUID作为令牌
        return UUID.randomUUID().toString();
    }
} 