package com.acme.im.business.module.user;

import com.acme.im.business.module.user.controller.UserController;
import com.acme.im.business.module.user.service.UserService;
import com.acme.im.business.module.user.repository.UserRepository;
import com.acme.im.business.module.user.event.UserEventPublisher;
import com.acme.im.business.module.user.dto.UserRegisterRequest;
import com.acme.im.business.module.user.dto.UserLoginRequest;
import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.entity.UserDevice;
import com.acme.im.business.module.user.entity.UserPrivacySettings;
import com.acme.im.business.module.user.entity.UserBlacklist;
import com.acme.im.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户模块集成测试
 * 测试用户注册、登录、资料更新等完整流程
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserModuleIntegrationTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEventPublisher userEventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    // ================================
    // 用户注册测试
    // ================================

    @Test
    void testUserRegistration_Success() throws Exception {
        // 准备测试数据
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setPassword("testpass123");
        request.setDeviceId("test-device-001");

        // 执行注册请求
        String response = mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证响应
        ApiResponse<User> apiResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, User.class));
        
        assertTrue(apiResponse.isSuccess());
        assertNotNull(apiResponse.getData());
        assertEquals("testuser", apiResponse.getData().getUsername());

        // 验证数据库中的数据
        User savedUser = userRepository.findByUsername("testuser").orElse(null);
        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getUsername());
        assertEquals(1, savedUser.getStatus()); // 正常状态
        assertEquals(0, savedUser.getOnlineStatus()); // 离线状态
    }

    @Test
    void testUserRegistration_DuplicateUsername() throws Exception {
        // 先注册一个用户
        UserRegisterRequest firstRequest = new UserRegisterRequest();
        firstRequest.setUsername("duplicateuser");
        firstRequest.setPassword("testpass123");
        firstRequest.setDeviceId("test-device-001");

        userService.registerUser(firstRequest.getUsername(), firstRequest.getPassword(), firstRequest.getDeviceId());

        // 尝试注册相同用户名的用户
        UserRegisterRequest secondRequest = new UserRegisterRequest();
        secondRequest.setUsername("duplicateuser");
        secondRequest.setPassword("testpass456");
        secondRequest.setDeviceId("test-device-002");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在: duplicateuser"));
    }

    // ================================
    // 用户登录测试
    // ================================

    @Test
    void testUserLogin_Success() throws Exception {
        // 先注册一个用户
        String username = "logintestuser";
        String password = "testpass123";
        String deviceId = "test-device-003";

        User user = userService.registerUser(username, password, deviceId);

        // 执行登录请求
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        loginRequest.setDeviceId(deviceId);

        String response = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证响应
        ApiResponse<String> apiResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, String.class));
        
        assertTrue(apiResponse.isSuccess());
        assertNotNull(apiResponse.getData());
        assertFalse(apiResponse.getData().isEmpty());

        // 验证用户状态
        User updatedUser = userRepository.selectById(user.getId());
        assertEquals(1, updatedUser.getOnlineStatus()); // 在线状态
        assertNotNull(updatedUser.getLastLoginAt());
    }

    @Test
    void testUserLogin_InvalidCredentials() throws Exception {
        // 先注册一个用户
        String username = "invalidlogintestuser";
        String password = "testpass123";
        String deviceId = "test-device-004";

        userService.registerUser(username, password, deviceId);

        // 使用错误密码登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("wrongpassword");
        loginRequest.setDeviceId(deviceId);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }

    @Test
    void testUserLogin_UserNotExists() throws Exception {
        // 尝试登录不存在的用户
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("testpass123");
        loginRequest.setDeviceId("test-device-005");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户不存在: nonexistentuser"));
    }

    // ================================
    // 用户资料更新测试
    // ================================

    @Test
    void testUserProfileUpdate_Success() throws Exception {
        // 先注册并登录一个用户
        String username = "profiletestuser";
        String password = "testpass123";
        String deviceId = "test-device-006";

        User user = userService.registerUser(username, password, deviceId);
        String token = userService.loginUser(username, password, deviceId);

        // 准备更新数据
        User updateData = new User();
        updateData.setNickname("新昵称");
        updateData.setSignature("新的个性签名");
        updateData.setGender(1); // 男性

        // 执行更新请求
        String response = mockMvc.perform(put("/api/users/{userId}", user.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证响应
        ApiResponse<User> apiResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, User.class));
        
        assertTrue(apiResponse.isSuccess());
        assertNotNull(apiResponse.getData());
        assertEquals("新昵称", apiResponse.getData().getNickname());
        assertEquals("新的个性签名", apiResponse.getData().getSignature());
        assertEquals(1, apiResponse.getData().getGender());

        // 验证数据库中的数据
        User updatedUser = userRepository.selectById(user.getId());
        assertEquals("新昵称", updatedUser.getNickname());
        assertEquals("新的个性签名", updatedUser.getSignature());
        assertEquals(1, updatedUser.getGender());
    }

    // ================================
    // 用户头像更新测试
    // ================================

    @Test
    void testUserAvatarUpdate_Success() throws Exception {
        // 先注册并登录一个用户
        String username = "avatartestuser";
        String password = "testpass123";
        String deviceId = "test-device-007";

        User user = userService.registerUser(username, password, deviceId);
        String token = userService.loginUser(username, password, deviceId);

        // 准备头像URL
        String newAvatarUrl = "https://example.com/avatars/new-avatar.jpg";

        // 执行头像更新请求
        String response = mockMvc.perform(put("/api/users/{userId}/avatar", user.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newAvatarUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证响应
        ApiResponse<Boolean> apiResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, Boolean.class));
        
        assertTrue(apiResponse.isSuccess());
        assertTrue(apiResponse.getData());

        // 验证数据库中的数据
        User updatedUser = userRepository.selectById(user.getId());
        assertEquals(newAvatarUrl, updatedUser.getAvatarUrl());
    }

    // ================================
    // 用户登出测试
    // ================================

    @Test
    void testUserLogout_Success() throws Exception {
        // 先注册并登录一个用户
        String username = "logouttestuser";
        String password = "testpass123";
        String deviceId = "test-device-008";

        User user = userService.registerUser(username, password, deviceId);
        userService.loginUser(username, password, deviceId);

        // 验证用户在线状态
        User onlineUser = userRepository.selectById(user.getId());
        assertEquals(1, onlineUser.getOnlineStatus());

        // 执行登出
        userService.logoutUser(user.getId(), deviceId);

        // 验证用户离线状态
        User offlineUser = userRepository.selectById(user.getId());
        assertEquals(0, offlineUser.getOnlineStatus());
    }

    // ================================
    // 用户搜索测试
    // ================================

    @Test
    void testUserSearch_Success() throws Exception {
        // 先注册几个测试用户
        userService.registerUser("searchuser1", "pass123", "device-001");
        userService.registerUser("searchuser2", "pass123", "device-002");
        userService.registerUser("searchuser3", "pass123", "device-003");

        // 执行搜索请求
        String response = mockMvc.perform(get("/api/users/search")
                .param("keyword", "searchuser")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证响应
        ApiResponse<List<User>> apiResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)
            ));
        
        assertTrue(apiResponse.isSuccess());
        assertNotNull(apiResponse.getData());
        assertTrue(apiResponse.getData().size() >= 3);
    }

    // ================================
    // 用户统计测试
    // ================================

    @Test
    void testUserStatistics_Success() throws Exception {
        // 先注册几个测试用户
        userService.registerUser("statsuser1", "pass123", "device-004");
        userService.registerUser("statsuser2", "pass123", "device-005");

        // 执行统计请求
        String response = mockMvc.perform(get("/api/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证响应
        ApiResponse<UserService.UserStatistics> apiResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class, 
                UserService.UserStatistics.class
            ));
        
        assertTrue(apiResponse.isSuccess());
        assertNotNull(apiResponse.getData());
        assertTrue(apiResponse.getData().getTotalUsers() >= 2);
    }

    // ================================
    // 设备管理测试
    // ================================
    
    @Test
    void testGetUserDevices() {
        // 先添加一个设备
        UserDevice device = new UserDevice();
        device.setDeviceId("test-device-001");
        device.setDeviceName("测试设备");
        device.setDeviceType(1);
        device.setIpAddress("127.0.0.1");
        
        UserDevice addedDevice = userService.addUserDevice(1L, device);
        assertNotNull(addedDevice);
        assertEquals(1L, addedDevice.getUserId());
        assertEquals("test-device-001", addedDevice.getDeviceId());
        
        // 获取设备列表
        List<UserDevice> devices = userService.getUserDevices(1L);
        assertFalse(devices.isEmpty());
        assertTrue(devices.stream().anyMatch(d -> d.getDeviceId().equals("test-device-001")));
    }
    
    @Test
    void testAddUserDevice() {
        UserDevice device = new UserDevice();
        device.setDeviceId("test-device-002");
        device.setDeviceName("测试设备2");
        device.setDeviceType(2);
        device.setIpAddress("192.168.1.100");
        
        UserDevice addedDevice = userService.addUserDevice(1L, device);
        assertNotNull(addedDevice);
        assertEquals(1L, addedDevice.getUserId());
        assertEquals("test-device-002", addedDevice.getDeviceId());
        assertEquals(1, addedDevice.getIsOnline()); // 默认在线
        assertEquals(0, addedDevice.getIsTrusted()); // 默认不受信任
    }
    
    @Test
    void testRemoveUserDevice() {
        // 先添加设备
        UserDevice device = new UserDevice();
        device.setDeviceId("test-device-003");
        device.setDeviceName("测试设备3");
        device.setDeviceType(1);
        
        userService.addUserDevice(1L, device);
        
        // 移除设备
        boolean result = userService.removeUserDevice(1L, "test-device-003");
        assertTrue(result);
        
        // 验证设备已被移除
        List<UserDevice> devices = userService.getUserDevices(1L);
        assertFalse(devices.stream().anyMatch(d -> d.getDeviceId().equals("test-device-003")));
    }
    
    @Test
    void testSetDeviceTrusted() {
        // 先添加设备
        UserDevice device = new UserDevice();
        device.setDeviceId("test-device-004");
        device.setDeviceName("测试设备4");
        device.setDeviceType(1);
        
        userService.addUserDevice(1L, device);
        
        // 设置设备为受信任
        boolean result = userService.setDeviceTrusted(1L, "test-device-004", true);
        assertTrue(result);
        
        // 验证设备状态
        List<UserDevice> devices = userService.getUserDevices(1L);
        UserDevice trustedDevice = devices.stream()
                .filter(d -> d.getDeviceId().equals("test-device-004"))
                .findFirst()
                .orElse(null);
        assertNotNull(trustedDevice);
        assertEquals(1, trustedDevice.getIsTrusted());
    }
    
    // ================================
    // 隐私设置测试
    // ================================
    
    @Test
    void testGetUserPrivacySettings() {
        Optional<UserPrivacySettings> settings = userService.getUserPrivacySettings(1L);
        assertTrue(settings.isPresent());
        
        UserPrivacySettings privacySettings = settings.get();
        assertEquals(1L, privacySettings.getUserId());
        assertEquals(1, privacySettings.getFriendRequestMode()); // 需要验证
        assertEquals(1, privacySettings.getAllowSearchByUsername()); // 允许通过用户名搜索
        assertEquals(0, privacySettings.getAllowSearchByPhone()); // 不允许通过手机号搜索
    }
    
    @Test
    void testUpdateUserPrivacySettings() {
        UserPrivacySettings newSettings = new UserPrivacySettings();
        newSettings.setFriendRequestMode(0); // 自动通过
        newSettings.setAllowSearchByPhone(1); // 允许通过手机号搜索
        newSettings.setAllowStrangerMessage(1); // 允许陌生人消息
        
        UserPrivacySettings updatedSettings = userService.updateUserPrivacySettings(1L, newSettings);
        assertNotNull(updatedSettings);
        assertEquals(0, updatedSettings.getFriendRequestMode());
        assertEquals(1, updatedSettings.getAllowSearchByPhone());
        assertEquals(1, updatedSettings.getAllowStrangerMessage());
    }
    
    // ================================
    // 黑名单管理测试
    // ================================
    
    @Test
    void testAddUserToBlacklist() {
        UserBlacklist blacklistRecord = userService.addUserToBlacklist(1L, 2L, 1, "测试拉黑");
        assertNotNull(blacklistRecord);
        assertEquals(1L, blacklistRecord.getUserId());
        assertEquals(2L, blacklistRecord.getBlockedUserId());
        assertEquals(1, blacklistRecord.getBlockType());
        assertEquals("测试拉黑", blacklistRecord.getReason());
    }
    
    @Test
    void testGetUserBlacklist() {
        // 先添加黑名单记录
        userService.addUserToBlacklist(1L, 3L, 2, "测试拉黑2");
        
        List<UserBlacklist> blacklist = userService.getUserBlacklist(1L);
        assertFalse(blacklist.isEmpty());
        assertTrue(blacklist.stream().anyMatch(b -> b.getBlockedUserId().equals(3L)));
    }
    
    @Test
    void testRemoveUserFromBlacklist() {
        // 先添加黑名单记录
        userService.addUserToBlacklist(1L, 4L, 1, "测试拉黑3");
        
        // 从黑名单移除
        boolean result = userService.removeUserFromBlacklist(1L, 4L);
        assertTrue(result);
        
        // 验证已移除
        List<UserBlacklist> blacklist = userService.getUserBlacklist(1L);
        assertFalse(blacklist.stream().anyMatch(b -> b.getBlockedUserId().equals(4L)));
    }
    
    @Test
    void testIsUserInBlacklist() {
        // 先添加黑名单记录
        userService.addUserToBlacklist(1L, 5L, 1, "测试拉黑4");
        
        // 检查是否在黑名单中
        boolean inBlacklist = userService.isUserInBlacklist(1L, 5L);
        assertTrue(inBlacklist);
        
        // 检查不在黑名单中的用户
        boolean notInBlacklist = userService.isUserInBlacklist(1L, 999L);
        assertFalse(notInBlacklist);
    }
    
    // ================================
    // 搜索功能测试
    // ================================
    
    @Test
    void testSearchUsers() {
        List<User> users = userService.searchUsers("测试", 10);
        assertFalse(users.isEmpty());
        assertTrue(users.stream().anyMatch(u -> u.getUsername().contains("测试") || 
                                               (u.getNickname() != null && u.getNickname().contains("测试"))));
    }
    
    @Test
    void testSearchUsersByUsername() {
        List<User> users = userService.searchUsersByUsername("testuser", 10);
        assertFalse(users.isEmpty());
        assertTrue(users.stream().allMatch(u -> u.getUsername().contains("testuser")));
    }
    
    @Test
    void testSearchUsersByNickname() {
        List<User> users = userService.searchUsersByNickname("测试用户", 10);
        assertFalse(users.isEmpty());
        assertTrue(users.stream().allMatch(u -> u.getNickname() != null && u.getNickname().contains("测试用户")));
    }
    
    @Test
    void testAdvancedSearchUsers() {
        List<User> users = userService.advancedSearchUsers("testuser", "测试用户", null, null, null, null, 10);
        assertFalse(users.isEmpty());
    }
    
    @Test
    void testSearchUsersWithEmptyKeyword() {
        List<User> users = userService.searchUsers("", 10);
        assertTrue(users.isEmpty());
        
        users = userService.searchUsers(null, 10);
        assertTrue(users.isEmpty());
    }
} 