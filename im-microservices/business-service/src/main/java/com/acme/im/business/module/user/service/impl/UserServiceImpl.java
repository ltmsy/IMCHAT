package com.acme.im.business.module.user.service.impl;

import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.entity.UserDevice;
import com.acme.im.business.module.user.entity.UserPrivacySettings;
import com.acme.im.business.module.user.entity.UserBlacklist;
import com.acme.im.business.module.user.service.UserService;
import com.acme.im.business.module.user.repository.UserRepository;
import com.acme.im.business.module.user.event.UserEventPublisher;
import com.acme.im.common.infrastructure.database.annotation.DataSource;
import com.acme.im.common.security.encryption.EncryptionUtils;
import com.acme.im.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.acme.im.business.module.common.event.publisher.MultiDeviceSyncPublisher;
import com.acme.im.business.module.user.repository.UserDeviceRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.acme.im.business.module.user.repository.UserPrivacySettingsRepository;
import com.acme.im.business.module.user.repository.UserBlacklistRepository;
import java.util.Objects;

/**
 * 用户服务实现类
 * 使用MyBatis-Plus，集成JWT认证
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final EncryptionUtils encryptionUtils;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventPublisher userEventPublisher;
    private final UserDeviceRepository userDeviceRepository;
    private final ObjectMapper objectMapper;
    private final UserPrivacySettingsRepository userPrivacySettingsRepository;
    private final UserBlacklistRepository userBlacklistRepository;

    @Autowired
    private MultiDeviceSyncPublisher multiDeviceSyncPublisher;
    
    // ================================
    // 用户基础信息管理
    // ================================
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
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
        
        // 删除：发布用户注册事件 - 没有业务价值
        // try {
        //     userEventPublisher.publishUserRegistered(savedUser.getId(), savedUser.getUsername(), deviceId);
        // } catch (Exception e) {
        //     log.warn("发布用户注册事件失败: userId={}, error: {}", savedUser.getId(), e.getMessage());
        // }
        
        return savedUser;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
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
        Integer oldOnlineStatus = user.getOnlineStatus();
        user.setOnlineStatus(1); // 在线
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.updateById(user);
        
        // 生成JWT登录令牌
        String token = generateLoginToken(user.getUsername(), deviceId);
        
        log.info("用户登录成功: userId={}, username={}, deviceId={}", user.getId(), user.getUsername(), deviceId);
        
        // 发布用户登录事件
        try {
            userEventPublisher.publishUserLogin(user.getId(), user.getUsername(), deviceId);
            // 发布在线状态变更事件
            if (!oldOnlineStatus.equals(user.getOnlineStatus())) {
                userEventPublisher.publishUserOnlineStatusChanged(user.getId(), user.getUsername(), deviceId, oldOnlineStatus, user.getOnlineStatus());
            }
        } catch (Exception e) {
            log.warn("发布用户登录事件失败: userId={}, error: {}", user.getId(), e.getMessage());
        }
        
        return token;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public void logoutUser(Long userId, String deviceId) {
        log.info("用户登出: userId={}, deviceId={}", userId, deviceId);
        
        User user = userRepository.selectById(userId);
        if (user != null) {
            Integer oldOnlineStatus = user.getOnlineStatus();
            user.setOnlineStatus(0); // 离线
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.updateById(user);
            
            // 发布用户登出事件
            try {
                userEventPublisher.publishUserLogout(userId, user.getUsername(), deviceId);
                // 发布在线状态变更事件
                if (!oldOnlineStatus.equals(user.getOnlineStatus())) {
                    userEventPublisher.publishUserOnlineStatusChanged(userId, user.getUsername(), deviceId, oldOnlineStatus, user.getOnlineStatus());
                }
            } catch (Exception e) {
                log.warn("发布用户登出事件失败: userId={}, error: {}", userId, e.getMessage());
            }
        }
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public Optional<User> findUserById(Long userId) {
        User user = userRepository.selectById(userId);
        return Optional.ofNullable(user);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public Optional<User> findUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public User updateUserInfo(Long userId, User userInfo) {
        log.info("更新用户信息: userId={}", userId);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        
        // 记录旧值用于事件发布
        String oldNickname = user.getNickname();
        String oldEmail = user.getEmail();
        String oldPhone = user.getPhone();
        String oldAvatarUrl = user.getAvatarUrl();
        String oldSignature = user.getSignature();
        Integer oldGender = user.getGender();
        String oldRegion = user.getRegion();
        
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
        
        // 发布用户资料更新事件
        try {
            if (userInfo.getNickname() != null && !userInfo.getNickname().equals(oldNickname)) {
                userEventPublisher.publishUserProfileUpdated(userId, user.getUsername(), null, "nickname", oldNickname, userInfo.getNickname());
            }
            if (userInfo.getEmail() != null && !userInfo.getEmail().equals(oldEmail)) {
                userEventPublisher.publishUserProfileUpdated(userId, user.getUsername(), null, "email", oldEmail, userInfo.getEmail());
            }
            if (userInfo.getPhone() != null && !userInfo.getPhone().equals(oldPhone)) {
                userEventPublisher.publishUserProfileUpdated(userId, user.getUsername(), null, "phone", oldPhone, userInfo.getPhone());
            }
            if (userInfo.getSignature() != null && !userInfo.getSignature().equals(oldSignature)) {
                userEventPublisher.publishUserProfileUpdated(userId, user.getUsername(), null, "signature", oldSignature, userInfo.getSignature());
            }
            if (userInfo.getGender() != null && !userInfo.getGender().equals(oldGender)) {
                userEventPublisher.publishUserProfileUpdated(userId, user.getUsername(), null, "gender", String.valueOf(oldGender), String.valueOf(userInfo.getGender()));
            }
            if (userInfo.getRegion() != null && !userInfo.getRegion().equals(oldRegion)) {
                userEventPublisher.publishUserProfileUpdated(userId, user.getUsername(), null, "region", oldRegion, userInfo.getRegion());
            }
        } catch (Exception e) {
            log.warn("发布用户资料更新事件失败: userId={}, error: {}", userId, e.getMessage());
        }
        
        return user;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public boolean updateUserAvatar(Long userId, String avatarUrl) {
        log.info("更新用户头像: userId={}, avatarUrl={}", userId, avatarUrl);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        
        String oldAvatarUrl = user.getAvatarUrl();
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        int result = userRepository.updateById(user);
        
        boolean success = result > 0;
        log.info("用户头像更新{}: userId={}", success ? "成功" : "失败", userId);
        
        // 发布用户头像更新事件
        if (success) {
            try {
                userEventPublisher.publishUserAvatarUpdated(userId, user.getUsername(), null, avatarUrl);
            } catch (Exception e) {
                log.warn("发布用户头像更新事件失败: userId={}, error: {}", userId, e.getMessage());
            }
        }
        
        return success;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public void updateUserOnlineStatus(Long userId, Integer onlineStatus) {
        log.info("更新用户在线状态: userId={}, onlineStatus={}", userId, onlineStatus);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        
        Integer oldOnlineStatus = user.getOnlineStatus();
        user.setOnlineStatus(onlineStatus);
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.updateById(user);
        
        log.info("用户在线状态更新成功: userId={}, onlineStatus={}", userId, onlineStatus);
        
        // 发布用户在线状态变更事件
        if (!oldOnlineStatus.equals(onlineStatus)) {
            try {
                userEventPublisher.publishUserOnlineStatusChanged(userId, user.getUsername(), null, oldOnlineStatus, onlineStatus);
            } catch (Exception e) {
                log.warn("发布用户在线状态变更事件失败: userId={}, error: {}", userId, e.getMessage());
            }
        }
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public void updateUserLastActiveTime(Long userId) {
        User user = userRepository.selectById(userId);
        if (user != null) {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.updateById(user);
        }
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
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
        
        // 发布密码修改事件
        if (success) {
            try {
                userEventPublisher.publishUserProfileUpdated(userId, user.getUsername(), null, "password", "***", "***");
            } catch (Exception e) {
                log.warn("发布密码修改事件失败: userId={}, error: {}", userId, e.getMessage());
            }
        }
        
        return success;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public boolean resetPassword(String email) {
        log.info("密码重置功能待实现: email={}", email);
        // TODO: 实现密码重置功能
        return false;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public boolean updateUserStatus(Long userId, Integer status) {
        log.info("更新用户状态: userId={}, status={}", userId, status);
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        
        Integer oldStatus = user.getStatus();
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        int result = userRepository.updateById(user);
        
        boolean success = result > 0;
        log.info("用户状态更新{}: userId={}", success ? "成功" : "失败", userId);
        
        // 发布用户状态变更事件
        if (success && !oldStatus.equals(status)) {
            try {
                String reason = status == 0 ? "用户被禁用" : status == 1 ? "用户被启用" : "用户被冻结";
                userEventPublisher.publishUserStatusChanged(userId, user.getUsername(), null, oldStatus, status, reason);
            } catch (Exception e) {
                log.warn("发布用户状态变更事件失败: userId={}, error: {}", userId, e.getMessage());
            }
        }
        
        return success;
    }

    /**
     * 更新用户信息（带多端同步）
     */



    
    // ================================
    // 用户设备管理
    // ================================
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public List<UserDevice> getUserDevices(Long userId) {
        log.info("获取用户设备列表: userId={}", userId);
        
        // 使用MyBatis-Plus查询用户设备
        QueryWrapper<UserDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .orderByDesc("last_active_at");
        
        List<UserDevice> devices = userDeviceRepository.selectList(queryWrapper);
        
        log.info("获取用户设备列表成功: userId={}, deviceCount={}", userId, devices.size());
        return devices;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public UserDevice addUserDevice(Long userId, UserDevice deviceInfo) {
        log.info("添加用户设备: userId={}, deviceId={}", userId, deviceInfo.getDeviceId());
        
        // 检查设备是否已存在
        QueryWrapper<UserDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("device_id", deviceInfo.getDeviceId());
        
        UserDevice existingDevice = userDeviceRepository.selectOne(queryWrapper);
        if (existingDevice != null) {
            log.warn("设备已存在: userId={}, deviceId={}", userId, deviceInfo.getDeviceId());
            return existingDevice;
        }
        
        // 设置设备信息
        deviceInfo.setUserId(userId);
        deviceInfo.setIsOnline(1); // 默认在线
        deviceInfo.setIsTrusted(0); // 默认不受信任
        deviceInfo.setLastLoginAt(LocalDateTime.now());
        deviceInfo.setLastActiveAt(LocalDateTime.now());
        deviceInfo.setCreatedAt(LocalDateTime.now());
        deviceInfo.setUpdatedAt(LocalDateTime.now());
        
        // 保存设备
        userDeviceRepository.insert(deviceInfo);
        
        log.info("用户设备添加成功: userId={}, deviceId={}", userId, deviceInfo.getDeviceId());
        
        // 发布设备添加事件
        try {
            String deviceInfoJson = objectMapper.writeValueAsString(deviceInfo);
            userEventPublisher.publishUserDeviceAdded(userId, getUserUsername(userId), deviceInfo.getDeviceId(), deviceInfoJson);
        } catch (Exception e) {
            log.warn("发布设备添加事件失败: userId={}, deviceId={}, error: {}", userId, deviceInfo.getDeviceId(), e.getMessage());
        }
        
        return deviceInfo;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public boolean removeUserDevice(Long userId, String deviceId) {
        log.info("移除用户设备: userId={}, deviceId={}", userId, deviceId);
        
        // 查找设备
        QueryWrapper<UserDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("device_id", deviceId);
        
        UserDevice device = userDeviceRepository.selectOne(queryWrapper);
        if (device == null) {
            log.warn("设备不存在: userId={}, deviceId={}", userId, deviceId);
            return false;
        }
        
        // 删除设备
        int result = userDeviceRepository.delete(queryWrapper);
        boolean success = result > 0;
        
        log.info("用户设备移除{}: userId={}, deviceId={}", success ? "成功" : "失败", userId, deviceId);
        
        // 发布设备移除事件
        if (success) {
            try {
                userEventPublisher.publishUserDeviceRemoved(userId, getUserUsername(userId), deviceId);
            } catch (Exception e) {
                log.warn("发布设备移除事件失败: userId={}, deviceId={}, error: {}", userId, deviceId, e.getMessage());
            }
        }
        
        return success;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public void updateDeviceOnlineStatus(Long userId, String deviceId, boolean isOnline) {
        log.info("更新设备在线状态: userId={}, deviceId={}, isOnline={}", userId, deviceId, isOnline);
        
        // 查找设备
        QueryWrapper<UserDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("device_id", deviceId);
        
        UserDevice device = userDeviceRepository.selectOne(queryWrapper);
        if (device == null) {
            log.warn("设备不存在: userId={}, deviceId={}", userId, deviceId);
            return;
        }
        
        // 更新在线状态
        Integer oldOnlineStatus = device.getIsOnline();
        device.setIsOnline(isOnline ? 1 : 0);
        device.setLastActiveAt(LocalDateTime.now());
        if (!isOnline) {
            device.setDisconnectedAt(LocalDateTime.now());
        }
        device.setUpdatedAt(LocalDateTime.now());
        
        userDeviceRepository.updateById(device);
        
        log.info("设备在线状态更新成功: userId={}, deviceId={}, isOnline={}", userId, deviceId, isOnline);
        
        // 发布设备状态变更事件
        if (!oldOnlineStatus.equals(device.getIsOnline())) {
            try {
                userEventPublisher.publishUserOnlineStatusChanged(userId, getUserUsername(userId), deviceId, oldOnlineStatus, device.getIsOnline());
            } catch (Exception e) {
                log.warn("发布设备状态变更事件失败: userId={}, deviceId={}, error: {}", userId, deviceId, e.getMessage());
            }
        }
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public boolean setDeviceTrusted(Long userId, String deviceId, boolean isTrusted) {
        log.info("设置设备信任状态: userId={}, deviceId={}, isTrusted={}", userId, deviceId, isTrusted);
        
        // 查找设备
        QueryWrapper<UserDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("device_id", deviceId);
        
        UserDevice device = userDeviceRepository.selectOne(queryWrapper);
        if (device == null) {
            log.warn("设备不存在: userId={}, deviceId={}", userId, deviceId);
            return false;
        }
        
        // 保存旧的信任状态
        boolean oldTrusted = device.getIsTrusted() == 1;
        
        // 更新信任状态
        device.setIsTrusted(isTrusted ? 1 : 0);
        device.setUpdatedAt(LocalDateTime.now());
        
        int result = userDeviceRepository.updateById(device);
        boolean success = result > 0;
        
        log.info("设备信任状态设置{}: userId={}, deviceId={}, isTrusted={}", success ? "成功" : "失败", userId, deviceId, isTrusted);
        
        // 发布设备信任状态变更事件
        if (success && oldTrusted != isTrusted) {
            try {
                userEventPublisher.publishUserDeviceTrustChanged(userId, getUserUsername(userId), deviceId, oldTrusted, isTrusted);
            } catch (Exception e) {
                log.warn("发布设备信任状态变更事件失败: userId={}, deviceId={}, error: {}", userId, deviceId, e.getMessage());
            }
        }
        
        return success;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public void updateDeviceLastActive(Long userId, String deviceId) {
        // 查找设备
        QueryWrapper<UserDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("device_id", deviceId);
        
        UserDevice device = userDeviceRepository.selectOne(queryWrapper);
        if (device != null) {
            device.setLastActiveAt(LocalDateTime.now());
            device.setUpdatedAt(LocalDateTime.now());
            userDeviceRepository.updateById(device);
        }
    }
    
    // ================================
    // 用户隐私设置
    // ================================
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public Optional<UserPrivacySettings> getUserPrivacySettings(Long userId) {
        log.info("获取用户隐私设置: userId={}", userId);
        
        UserPrivacySettings settings = userPrivacySettingsRepository.findByUserId(userId);
        
        if (settings == null) {
            // 如果没有隐私设置，返回默认设置
            settings = createDefaultPrivacySettings(userId);
        }
        
        log.info("获取用户隐私设置成功: userId={}", userId);
        return Optional.of(settings);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public UserPrivacySettings updateUserPrivacySettings(Long userId, UserPrivacySettings privacySettings) {
        log.info("更新用户隐私设置: userId={}", userId);
        
        // 设置用户ID
        privacySettings.setUserId(userId);
        privacySettings.setUpdatedAt(LocalDateTime.now());
        
        // 检查是否已存在隐私设置
        UserPrivacySettings existingSettings = userPrivacySettingsRepository.findByUserId(userId);
        
        if (existingSettings == null) {
            // 创建新的隐私设置
            privacySettings.setCreatedAt(LocalDateTime.now());
            userPrivacySettingsRepository.insert(privacySettings);
            log.info("创建用户隐私设置成功: userId={}", userId);
            
            // 发布隐私设置创建事件
            try {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, "privacy_created", null, "created");
            } catch (Exception e) {
                log.warn("发布隐私设置创建事件失败: userId={}, error: {}", userId, e.getMessage());
            }
        } else {
            // 更新现有隐私设置
            privacySettings.setId(existingSettings.getId());
            privacySettings.setCreatedAt(existingSettings.getCreatedAt());
            
            // 比较变更的字段并发布事件
            publishPrivacySettingsChangeEvents(userId, existingSettings, privacySettings);
            
            userPrivacySettingsRepository.updateById(privacySettings);
            log.info("更新用户隐私设置成功: userId={}", userId);
        }
        
        return privacySettings;
    }
    
    /**
     * 创建默认隐私设置
     */
    private UserPrivacySettings createDefaultPrivacySettings(Long userId) {
        UserPrivacySettings settings = new UserPrivacySettings();
        settings.setUserId(userId);
        settings.setFriendRequestMode(1); // 需要验证
        settings.setAllowSearchByUsername(1); // 允许通过用户名搜索
        settings.setAllowSearchByPhone(0); // 不允许通过手机号搜索
        settings.setAllowSearchByEmail(0); // 不允许通过邮箱搜索
        settings.setAllowGroupInvite(1); // 允许群组邀请
        settings.setAllowStrangerMessage(0); // 不允许陌生人消息
        settings.setMessageReadReceipt(1); // 消息已读回执
        settings.setOnlineStatusVisible(1); // 在线状态可见
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        return settings;
    }
    
    /**
     * 发布隐私设置变更事件
     */
    private void publishPrivacySettingsChangeEvents(Long userId, UserPrivacySettings oldSettings, UserPrivacySettings newSettings) {
        try {
            // 检查各个字段的变更
            if (!Objects.equals(oldSettings.getFriendRequestMode(), newSettings.getFriendRequestMode())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "friendRequestMode", oldSettings.getFriendRequestMode(), newSettings.getFriendRequestMode());
            }
            
            if (!Objects.equals(oldSettings.getAllowSearchByUsername(), newSettings.getAllowSearchByUsername())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "allowSearchByUsername", oldSettings.getAllowSearchByUsername(), newSettings.getAllowSearchByUsername());
            }
            
            if (!Objects.equals(oldSettings.getAllowSearchByPhone(), newSettings.getAllowSearchByPhone())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "allowSearchByPhone", oldSettings.getAllowSearchByPhone(), newSettings.getAllowSearchByPhone());
            }
            
            if (!Objects.equals(oldSettings.getAllowSearchByEmail(), newSettings.getAllowSearchByEmail())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "allowSearchByEmail", oldSettings.getAllowSearchByEmail(), newSettings.getAllowSearchByEmail());
            }
            
            if (!Objects.equals(oldSettings.getAllowGroupInvite(), newSettings.getAllowGroupInvite())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "allowGroupInvite", oldSettings.getAllowGroupInvite(), newSettings.getAllowGroupInvite());
            }
            
            if (!Objects.equals(oldSettings.getAllowStrangerMessage(), newSettings.getAllowStrangerMessage())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "allowStrangerMessage", oldSettings.getAllowStrangerMessage(), newSettings.getAllowStrangerMessage());
            }
            
            if (!Objects.equals(oldSettings.getMessageReadReceipt(), newSettings.getMessageReadReceipt())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "messageReadReceipt", oldSettings.getMessageReadReceipt(), newSettings.getMessageReadReceipt());
            }
            
            if (!Objects.equals(oldSettings.getOnlineStatusVisible(), newSettings.getOnlineStatusVisible())) {
                userEventPublisher.publishUserPrivacyUpdated(userId, getUserUsername(userId), null, 
                    "onlineStatusVisible", oldSettings.getOnlineStatusVisible(), newSettings.getOnlineStatusVisible());
            }
            
        } catch (Exception e) {
            log.warn("发布隐私设置变更事件失败: userId={}, error: {}", userId, e.getMessage());
        }
    }
    
    // ================================
    // 用户黑名单管理
    // ================================
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public List<UserBlacklist> getUserBlacklist(Long userId) {
        log.info("获取用户黑名单: userId={}", userId);
        
        List<UserBlacklist> blacklist = userBlacklistRepository.findByUserId(userId);
        
        log.info("获取用户黑名单成功: userId={}, blacklistCount={}", userId, blacklist.size());
        return blacklist;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public UserBlacklist addUserToBlacklist(Long userId, Long blockedUserId, Integer blockType, String reason) {
        log.info("添加用户到黑名单: userId={}, blockedUserId={}, blockType={}", userId, blockedUserId, blockType);
        
        // 检查是否已经在黑名单中
        UserBlacklist existingRecord = userBlacklistRepository.findByUserIdAndBlockedUserId(userId, blockedUserId);
        if (existingRecord != null) {
            log.warn("用户已在黑名单中: userId={}, blockedUserId={}", userId, blockedUserId);
            return existingRecord;
        }
        
        // 创建黑名单记录
        UserBlacklist blacklistRecord = new UserBlacklist();
        blacklistRecord.setUserId(userId);
        blacklistRecord.setBlockedUserId(blockedUserId);
        blacklistRecord.setBlockType(blockType);
        blacklistRecord.setReason(reason);
        blacklistRecord.setCreatedAt(LocalDateTime.now());
        
        // 保存黑名单记录
        userBlacklistRepository.insert(blacklistRecord);
        
        log.info("用户添加到黑名单成功: userId={}, blockedUserId={}", userId, blockedUserId);
        
        // 发布黑名单添加事件
        try {
            userEventPublisher.publishUserBlacklistAdded(userId, getUserUsername(userId), null, blockedUserId, reason);
        } catch (Exception e) {
            log.warn("发布黑名单添加事件失败: userId={}, blockedUserId={}, error: {}", userId, blockedUserId, e.getMessage());
        }
        
        return blacklistRecord;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.PRIMARY)
    public boolean removeUserFromBlacklist(Long userId, Long blockedUserId) {
        log.info("从黑名单移除用户: userId={}, blockedUserId={}", userId, blockedUserId);
        
        // 查找黑名单记录
        UserBlacklist blacklistRecord = userBlacklistRepository.findByUserIdAndBlockedUserId(userId, blockedUserId);
        if (blacklistRecord == null) {
            log.warn("黑名单记录不存在: userId={}, blockedUserId={}", userId, blockedUserId);
            return false;
        }
        
        // 删除黑名单记录
        int result = userBlacklistRepository.deleteById(blacklistRecord.getId());
        boolean success = result > 0;
        
        log.info("从黑名单移除用户{}: userId={}, blockedUserId={}", success ? "成功" : "失败", userId, blockedUserId);
        
        // 发布黑名单移除事件
        if (success) {
            try {
                userEventPublisher.publishUserBlacklistRemoved(userId, getUserUsername(userId), null, blockedUserId);
            } catch (Exception e) {
                log.warn("发布黑名单移除事件失败: userId={}, blockedUserId={}, error: {}", userId, blockedUserId, e.getMessage());
            }
        }
        
        return success;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public boolean isUserInBlacklist(Long userId, Long targetUserId) {
        int count = userBlacklistRepository.countByUserIdAndBlockedUserId(userId, targetUserId);
        return count > 0;
    }
    
    // ================================
    // 用户搜索和统计
    // ================================
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public List<User> searchUsers(String keyword, int limit) {
        log.info("搜索用户: keyword={}, limit={}", keyword, limit);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("搜索关键词为空");
            return List.of();
        }
        
        // 去除首尾空格
        keyword = keyword.trim();
        
        // 使用全文搜索
        List<User> users = userRepository.fullTextSearch(keyword, limit);
        
        log.info("搜索用户完成: keyword={}, resultCount={}", keyword, users.size());
        return users;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public List<User> searchUsersByUsername(String username, int limit) {
        log.info("根据用户名搜索用户: username={}, limit={}", username, username, limit);
        
        if (username == null || username.trim().isEmpty()) {
            return List.of();
        }
        
        return userRepository.searchByUsername(username.trim(), limit);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public List<User> searchUsersByNickname(String nickname, int limit) {
        log.info("根据昵称搜索用户: nickname={}, limit={}", nickname, limit);
        
        if (nickname == null || nickname.trim().isEmpty()) {
            return List.of();
        }
        
        return userRepository.searchByNickname(nickname.trim(), limit);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public List<User> advancedSearchUsers(String username, String nickname, String email, 
                                        String phone, String region, Integer gender, int limit) {
        log.info("高级搜索用户: username={}, nickname={}, email={}, phone={}, region={}, gender={}, limit={}", 
                username, nickname, email, phone, region, gender, limit);
        
        // 检查是否至少有一个搜索条件
        if ((username == null || username.trim().isEmpty()) &&
            (nickname == null || nickname.trim().isEmpty()) &&
            (email == null || email.trim().isEmpty()) &&
            (phone == null || phone.trim().isEmpty()) &&
            (region == null || region.trim().isEmpty()) &&
            gender == null) {
            log.warn("高级搜索条件为空");
            return List.of();
        }
        
        // 去除字符串字段的首尾空格
        username = username != null ? username.trim() : null;
        nickname = nickname != null ? nickname.trim() : null;
        email = email != null ? email.trim() : null;
        phone = phone != null ? phone.trim() : null;
        region = region != null ? region.trim() : null;
        
        List<User> users = userRepository.advancedSearch(username, nickname, email, phone, region, gender, limit);
        
        log.info("高级搜索用户完成: resultCount={}", users.size());
        return users;
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public long getOnlineUserCount() {
        return userRepository.countOnlineUsers();
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
    public List<User> getRecentUsers(int limit) {
        return userRepository.findRecentUsers(limit);
    }
    
    @Override
    @DataSource(type = DataSource.DataSourceType.SECONDARY)
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
     * 使用JWT服务生成访问令牌
     */
    private String generateLoginToken(String username, String deviceId) {
        try {
            // 调用JWT服务生成访问令牌
            String accessToken = jwtTokenProvider.generateAccessToken(username, deviceId);
            log.debug("JWT访问令牌生成成功: username={}, deviceId={}", username, deviceId);
            return accessToken;
        } catch (Exception e) {
            log.error("JWT访问令牌生成失败: username={}, deviceId={}", username, deviceId, e);
            // 如果JWT生成失败，返回错误信息
            throw new RuntimeException("令牌生成失败，请稍后重试");
        }
    }

    private String getUserUsername(Long userId) {
        User user = userRepository.selectById(userId);
        return user != null ? user.getUsername() : "unknown_user";
    }
} 