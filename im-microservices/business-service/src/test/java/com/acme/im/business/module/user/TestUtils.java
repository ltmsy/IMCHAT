package com.acme.im.business.module.user;

import com.acme.im.business.module.user.dto.UserLoginRequest;
import com.acme.im.business.module.user.dto.UserRegisterRequest;
import com.acme.im.business.module.user.dto.UserDTO;
import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.entity.UserDevice;
import com.acme.im.business.module.user.entity.UserPrivacySettings;
import com.acme.im.business.module.user.entity.UserBlacklist;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 用户模块测试工具类
 * 提供测试数据构建和通用测试方法
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class TestUtils {

    // ================================
    // 测试用户数据构建
    // ================================
    
    /**
     * 创建测试用户
     */
    public static User createTestUser() {
        return createTestUser(1L, "testuser", "test@example.com", "13800138000");
    }
    
    /**
     * 创建指定参数的测试用户
     */
    public static User createTestUser(Long id, String username, String email, String phone) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setNickname("测试用户");
        user.setAvatarUrl("https://example.com/avatar.jpg");
        user.setSignature("这是一个测试用户");
        user.setGender(1);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setRegion("北京市");
        user.setStatus(1);
        user.setOnlineStatus(0);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        user.setPasswordHash("test_password_hash");
        user.setSalt("test_salt");
        user.setTwoFactorEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
    
    /**
     * 创建测试用户列表
     */
    public static List<User> createTestUserList() {
        return Arrays.asList(
            createTestUser(1L, "user1", "user1@example.com", "13800138001"),
            createTestUser(2L, "user2", "user2@example.com", "13800138002"),
            createTestUser(3L, "user3", "user3@example.com", "13800138003")
        );
    }
    
    /**
     * 创建测试用户设备
     */
    public static UserDevice createTestUserDevice() {
        return createTestUserDevice(1L, 1L, "test-device-001");
    }
    
    /**
     * 创建指定参数的测试用户设备
     */
    public static UserDevice createTestUserDevice(Long id, Long userId, String deviceId) {
        UserDevice device = new UserDevice();
        device.setId(id);
        device.setUserId(userId);
        device.setDeviceId(deviceId);
        device.setDeviceName("测试设备");
        device.setDeviceType(1);
        device.setDeviceInfo("{\"os\":\"Android\",\"version\":\"10.0\"}");
        device.setIpAddress("127.0.0.1");
        device.setLocation("北京市");
        device.setIsOnline(1);
        device.setLastLoginAt(LocalDateTime.now());
        device.setLastActiveAt(LocalDateTime.now());
        device.setLoginToken("test_login_token");
        device.setRefreshToken("test_refresh_token");
        device.setTokenExpiresAt(LocalDateTime.now().plusDays(7));
        device.setIsTrusted(1);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        return device;
    }
    
    /**
     * 创建测试用户隐私设置
     */
    public static UserPrivacySettings createTestUserPrivacySettings() {
        return createTestUserPrivacySettings(1L);
    }
    
    /**
     * 创建指定用户ID的测试隐私设置
     */
    public static UserPrivacySettings createTestUserPrivacySettings(Long userId) {
        UserPrivacySettings settings = new UserPrivacySettings();
        settings.setUserId(userId);
        settings.setFriendRequestMode(1);
        settings.setAllowSearchByUsername(1);
        settings.setAllowSearchByPhone(0);
        settings.setAllowSearchByEmail(0);
        settings.setAllowGroupInvite(1);
        settings.setAllowStrangerMessage(0);
        settings.setMessageReadReceipt(1);
        settings.setOnlineStatusVisible(1);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        return settings;
    }
    
    /**
     * 创建测试用户黑名单
     */
    public static UserBlacklist createTestUserBlacklist() {
        return createTestUserBlacklist(1L, 1L, 2L);
    }
    
    /**
     * 创建指定参数的测试黑名单
     */
    public static UserBlacklist createTestUserBlacklist(Long id, Long userId, Long blockedUserId) {
        UserBlacklist blacklist = new UserBlacklist();
        blacklist.setId(id);
        blacklist.setUserId(userId);
        blacklist.setBlockedUserId(blockedUserId);
        blacklist.setBlockType(1);
        blacklist.setReason("测试拉黑");
        blacklist.setCreatedAt(LocalDateTime.now());
        return blacklist;
    }
    
    // ================================
    // 测试DTO数据构建
    // ================================
    
    /**
     * 创建用户注册请求
     */
    public static UserRegisterRequest createUserRegisterRequest() {
        return createUserRegisterRequest("testuser", "testpass123", "test-device-001");
    }
    
    /**
     * 创建指定参数的注册请求
     */
    public static UserRegisterRequest createUserRegisterRequest(String username, String password, String deviceId) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setDeviceId(deviceId);
        return request;
    }
    
    /**
     * 创建用户登录请求
     */
    public static UserLoginRequest createUserLoginRequest() {
        return createUserLoginRequest("testuser", "testpass123", "test-device-001");
    }
    
    /**
     * 创建指定参数的登录请求
     */
    public static UserLoginRequest createUserLoginRequest(String username, String password, String deviceId) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setDeviceId(deviceId);
        return request;
    }
    
    /**
     * 创建用户DTO
     */
    public static UserDTO createUserDTO() {
        return createUserDTO(1L, "testuser");
    }
    
    /**
     * 创建指定参数的用户DTO
     */
    public static UserDTO createUserDTO(Long id, String username) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setUsername(username);
        dto.setNickname("测试用户");
        dto.setAvatarUrl("https://example.com/avatar.jpg");
        dto.setStatus(1);
        dto.setOnlineStatus(0);
        return dto;
    }
    
    // ================================
    // 测试验证方法
    // ================================
    
    /**
     * 验证用户基本信息
     */
    public static void assertUserBasicInfo(User user, String expectedUsername, String expectedEmail) {
        assert user != null : "用户不能为空";
        assert expectedUsername.equals(user.getUsername()) : "用户名不匹配";
        assert expectedEmail.equals(user.getEmail()) : "邮箱不匹配";
        assert user.getStatus() == 1 : "用户状态应该为激活";
        assert user.getCreatedAt() != null : "创建时间不能为空";
        assert user.getUpdatedAt() != null : "更新时间不能为空";
    }
    
    /**
     * 验证用户设备信息
     */
    public static void assertUserDeviceInfo(UserDevice device, Long expectedUserId, String expectedDeviceId) {
        assert device != null : "设备不能为空";
        assert expectedUserId.equals(device.getUserId()) : "用户ID不匹配";
        assert expectedDeviceId.equals(device.getDeviceId()) : "设备ID不匹配";
        assert device.getIsOnline() == 1 : "设备应该在线";
        assert device.getLoginToken() != null : "登录令牌不能为空";
    }
    
    /**
     * 验证用户隐私设置
     */
    public static void assertUserPrivacySettings(UserPrivacySettings settings, Long expectedUserId) {
        assert settings != null : "隐私设置不能为空";
        assert expectedUserId.equals(settings.getUserId()) : "用户ID不匹配";
        assert settings.getFriendRequestMode() == 1 : "好友申请模式应该为允许";
        assert settings.getAllowSearchByUsername() == 1 : "应该允许通过用户名搜索";
    }
    
    /**
     * 验证用户黑名单
     */
    public static void assertUserBlacklist(UserBlacklist blacklist, Long expectedUserId, Long expectedBlockedUserId) {
        assert blacklist != null : "黑名单不能为空";
        assert expectedUserId.equals(blacklist.getUserId()) : "用户ID不匹配";
        assert expectedBlockedUserId.equals(blacklist.getBlockedUserId()) : "被拉黑用户ID不匹配";
        assert blacklist.getCreatedAt() != null : "创建时间不能为空";
    }
} 