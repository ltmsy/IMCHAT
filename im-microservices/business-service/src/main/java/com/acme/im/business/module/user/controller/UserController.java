package com.acme.im.business.module.user.controller;

import com.acme.im.business.module.user.dto.UserDTO;
import com.acme.im.business.module.user.dto.UserLoginRequest;
import com.acme.im.business.module.user.dto.UserRegisterRequest;
import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.service.UserService;
import com.acme.im.common.response.ApiResponse;
import com.acme.im.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.acme.im.business.module.user.entity.UserDevice;
import com.acme.im.business.module.user.entity.UserPrivacySettings;
import com.acme.im.business.module.user.entity.UserBlacklist;
import com.acme.im.business.module.user.dto.AddToBlacklistRequest;
import com.acme.im.business.module.user.dto.AdvancedSearchRequest;
import com.acme.im.common.response.ResponseCode;


/**
 * 用户管理控制器
 * 提供用户相关的REST API接口
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    
    // ================================
    // 用户注册和登录
    // ================================
    
    /**
     * 用户注册
     * POST /api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        log.info("收到用户注册请求: username={}", request.getUsername());
        
        try {
            User user = userService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getDeviceId()
            );
            
            UserDTO userDTO = convertToDTO(user);
            ApiResponse<UserDTO> response = ApiResponse.success("用户注册成功", userDTO);
            
            log.info("用户注册成功: userId={}, username={}, deviceId={}", user.getId(), user.getUsername(), request.getDeviceId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("用户注册失败: username={}, deviceId={}, error={}", request.getUsername(), request.getDeviceId(), e.getMessage());
            ApiResponse<UserDTO> response = ApiResponse.error("用户注册失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户登录
     * POST /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> loginUser(@Valid @RequestBody UserLoginRequest request) {
        log.info("收到用户登录请求: username={}", request.getUsername());
        
        try {
            // 登录需要用户名、密码和设备号
            String token = userService.loginUser(request.getUsername(), request.getPassword(), request.getDeviceId());
            
            ApiResponse<String> response = ApiResponse.success("用户登录成功", token);
            
            log.info("用户登录成功: username={}, deviceId={}", request.getUsername(), request.getDeviceId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户登录失败: username={}, deviceId={}, error={}", request.getUsername(), request.getDeviceId(), e.getMessage());
            ApiResponse<String> response = ApiResponse.error("用户登录失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户登出
     * POST /api/users/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logoutUser(
            @RequestHeader("Authorization") String token,
            @RequestParam String deviceId) {
        
        try {
            Long userId = extractUserIdFromToken(token);
            userService.logoutUser(userId, deviceId);
            
            ApiResponse<String> response = ApiResponse.success("用户登出成功", "登出成功");
            log.info("用户登出成功: userId={}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户登出失败: error={}", e.getMessage());
            ApiResponse<String> response = ApiResponse.error("用户登出失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ================================
    // 用户信息管理
    // ================================
    
    /**
     * 获取当前用户信息
     * GET /api/users/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUserProfile(
            @RequestHeader("Authorization") String token) {
        
        try {
            Long userId = extractUserIdFromToken(token);
            Optional<User> userOpt = userService.findUserById(userId);
            
            if (userOpt.isPresent()) {
                UserDTO userDTO = convertToDTO(userOpt.get());
                ApiResponse<UserDTO> response = ApiResponse.success("获取用户信息成功", userDTO);
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<UserDTO> response = ApiResponse.notFound("用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            log.error("获取用户信息失败: error={}", e.getMessage());
            ApiResponse<UserDTO> response = ApiResponse.error("获取用户信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新用户信息
     * PUT /api/users/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody User userInfo) {
        
        try {
            Long userId = extractUserIdFromToken(token);
            User updatedUser = userService.updateUserInfo(userId, userInfo);
            
            UserDTO userDTO = convertToDTO(updatedUser);
            ApiResponse<UserDTO> response = ApiResponse.success("用户信息更新成功", userDTO);
            
            log.info("用户信息更新成功: userId={}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新用户信息失败: error={}", e.getMessage());
            ApiResponse<UserDTO> response = ApiResponse.error("更新用户信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新用户头像
     * PUT /api/users/avatar
     */
    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> updateUserAvatar(
            @RequestHeader("Authorization") String token,
            @RequestParam String avatarUrl) {
        
        try {
            Long userId = extractUserIdFromToken(token);
            boolean success = userService.updateUserAvatar(userId, avatarUrl);
            
            if (success) {
                ApiResponse<String> response = ApiResponse.success("头像更新成功", "更新成功");
                log.info("用户头像更新成功: userId={}", userId);
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<String> response = ApiResponse.error("头像更新失败");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("更新用户头像失败: error={}", e.getMessage());
            ApiResponse<String> response = ApiResponse.error("更新用户头像失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 修改密码
     * PUT /api/users/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> passwordRequest) {
        
        String oldPassword = passwordRequest.get("oldPassword");
        String newPassword = passwordRequest.get("newPassword");
        
        try {
            Long userId = extractUserIdFromToken(token);
            boolean success = userService.changePassword(userId, oldPassword, newPassword);
            
            if (success) {
                ApiResponse<String> response = ApiResponse.success("密码修改成功", "修改成功");
                log.info("用户密码修改成功: userId={}", userId);
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<String> response = ApiResponse.error("密码修改失败");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("修改密码失败: error={}", e.getMessage());
            ApiResponse<String> response = ApiResponse.error("修改密码失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 重置密码
     * POST /api/users/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestParam String email) {
        log.info("收到密码重置请求: email={}", email);
        
        try {
            boolean success = userService.resetPassword(email);
            
            if (success) {
                ApiResponse<String> response = ApiResponse.success("密码重置成功，请查看邮箱", "重置成功");
                log.info("密码重置成功: email={}", email);
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<String> response = ApiResponse.error("密码重置失败，邮箱不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("密码重置失败: email={}, error={}", email, e.getMessage());
            ApiResponse<String> response = ApiResponse.error("密码重置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ================================
    // 设备管理接口
    // ================================

    /**
     * 获取用户设备列表
     */
    @GetMapping("/{userId}/devices")
    public ApiResponse<List<UserDevice>> getUserDevices(@PathVariable Long userId) {
        try {
            List<UserDevice> devices = userService.getUserDevices(userId);
            return ApiResponse.success(devices);
        } catch (Exception e) {
            log.error("获取用户设备列表失败: userId={}, error: {}", userId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "获取设备列表失败");
        }
    }

    /**
     * 添加用户设备
     */
    @PostMapping("/{userId}/devices")
    public ApiResponse<UserDevice> addUserDevice(@PathVariable Long userId, @RequestBody UserDevice deviceInfo) {
        try {
            UserDevice device = userService.addUserDevice(userId, deviceInfo);
            return ApiResponse.success(device);
        } catch (Exception e) {
            log.error("添加用户设备失败: userId={}, error: {}", userId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "添加设备失败");
        }
    }

    /**
     * 移除用户设备
     */
    @DeleteMapping("/{userId}/devices/{deviceId}")
    public ApiResponse<Boolean> removeUserDevice(@PathVariable Long userId, @PathVariable String deviceId) {
        try {
            boolean result = userService.removeUserDevice(userId, deviceId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("移除用户设备失败: userId={}, deviceId={}, error: {}", userId, deviceId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "移除设备失败");
        }
    }

    /**
     * 设置设备信任状态
     */
    @PutMapping("/{userId}/devices/{deviceId}/trust")
    public ApiResponse<Boolean> setDeviceTrusted(@PathVariable Long userId, @PathVariable String deviceId, 
                                               @RequestParam boolean isTrusted) {
        try {
            boolean result = userService.setDeviceTrusted(userId, deviceId, isTrusted);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("设置设备信任状态失败: userId={}, deviceId={}, error: {}", userId, deviceId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "设置设备信任状态失败");
        }
    }

    // ================================
    // 隐私设置接口
    // ================================

    /**
     * 获取用户隐私设置
     */
    @GetMapping("/{userId}/privacy")
    public ApiResponse<UserPrivacySettings> getUserPrivacySettings(@PathVariable Long userId) {
        try {
            Optional<UserPrivacySettings> settings = userService.getUserPrivacySettings(userId);
            return settings.map(ApiResponse::success)
                         .orElse(ApiResponse.error(ResponseCode.NOT_FOUND.getCode(), "隐私设置不存在"));
        } catch (Exception e) {
            log.error("获取用户隐私设置失败: userId={}, error: {}", userId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "获取隐私设置失败");
        }
    }

    /**
     * 更新用户隐私设置
     */
    @PutMapping("/{userId}/privacy")
    public ApiResponse<UserPrivacySettings> updateUserPrivacySettings(@PathVariable Long userId, 
                                                                   @RequestBody UserPrivacySettings privacySettings) {
        try {
            UserPrivacySettings updatedSettings = userService.updateUserPrivacySettings(userId, privacySettings);
            return ApiResponse.success(updatedSettings);
        } catch (Exception e) {
            log.error("更新用户隐私设置失败: userId={}, error: {}", userId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "更新隐私设置失败");
        }
    }

    // ================================
    // 黑名单管理接口
    // ================================

    /**
     * 获取用户黑名单
     */
    @GetMapping("/{userId}/blacklist")
    public ApiResponse<List<UserBlacklist>> getUserBlacklist(@PathVariable Long userId) {
        try {
            List<UserBlacklist> blacklist = userService.getUserBlacklist(userId);
            return ApiResponse.success(blacklist);
        } catch (Exception e) {
            log.error("获取用户黑名单失败: userId={}, error: {}", userId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "获取黑名单失败");
        }
    }

    /**
     * 添加用户到黑名单
     */
    @PostMapping("/{userId}/blacklist")
    public ApiResponse<UserBlacklist> addUserToBlacklist(@PathVariable Long userId, 
                                                       @RequestBody AddToBlacklistRequest request) {
        try {
            UserBlacklist blacklistRecord = userService.addUserToBlacklist(userId, request.getBlockedUserId(), 
                                                                         request.getBlockType(), request.getReason());
            return ApiResponse.success(blacklistRecord);
        } catch (Exception e) {
            log.error("添加用户到黑名单失败: userId={}, blockedUserId={}, error: {}", 
                     userId, request.getBlockedUserId(), e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "添加到黑名单失败");
        }
    }

    /**
     * 从黑名单移除用户
     */
    @DeleteMapping("/{userId}/blacklist/{blockedUserId}")
    public ApiResponse<Boolean> removeUserFromBlacklist(@PathVariable Long userId, @PathVariable Long blockedUserId) {
        try {
            boolean result = userService.removeUserFromBlacklist(userId, blockedUserId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("从黑名单移除用户失败: userId={}, blockedUserId={}, error: {}", 
                     userId, blockedUserId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "从黑名单移除失败");
        }
    }

    /**
     * 检查用户是否在黑名单中
     */
    @GetMapping("/{userId}/blacklist/check/{targetUserId}")
    public ApiResponse<Boolean> isUserInBlacklist(@PathVariable Long userId, @PathVariable Long targetUserId) {
        try {
            boolean result = userService.isUserInBlacklist(userId, targetUserId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("检查用户黑名单状态失败: userId={}, targetUserId={}, error: {}", 
                     userId, targetUserId, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "检查黑名单状态失败");
        }
    }

    // ================================
    // 搜索接口
    // ================================

    /**
     * 全文搜索用户
     */
    @GetMapping("/search")
    public ApiResponse<List<User>> searchUsers(@RequestParam String keyword, @RequestParam(defaultValue = "20") int limit) {
        try {
            List<User> users = userService.searchUsers(keyword, limit);
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("搜索用户失败: keyword={}, error: {}", keyword, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "搜索用户失败");
        }
    }

    /**
     * 根据用户名搜索用户
     */
    @GetMapping("/search/username")
    public ApiResponse<List<User>> searchUsersByUsername(@RequestParam String username, @RequestParam(defaultValue = "20") int limit) {
        try {
            List<User> users = userService.searchUsersByUsername(username, limit);
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("根据用户名搜索失败: username={}, error: {}", username, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "根据用户名搜索失败");
        }
    }

    /**
     * 根据昵称搜索用户
     */
    @GetMapping("/search/nickname")
    public ApiResponse<List<User>> searchUsersByNickname(@RequestParam String nickname, @RequestParam(defaultValue = "20") int limit) {
        try {
            List<User> users = userService.searchUsersByNickname(nickname, limit);
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("根据昵称搜索失败: nickname={}, error: {}", nickname, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "根据昵称搜索失败");
        }
    }

    /**
     * 高级搜索用户
     */
    @PostMapping("/search/advanced")
    public ApiResponse<List<User>> advancedSearchUsers(@RequestBody AdvancedSearchRequest request) {
        try {
            List<User> users = userService.advancedSearchUsers(
                request.getUsername(), request.getNickname(), request.getEmail(),
                request.getPhone(), request.getRegion(), request.getGender(), request.getLimit()
            );
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("高级搜索用户失败: request={}, error: {}", request, e.getMessage(), e);
            return ApiResponse.error(ResponseCode.ERROR.getCode(), "高级搜索失败");
        }
    }
    
    /**
     * 获取在线用户数量
     * GET /api/users/online-count
     */
    @GetMapping("/online-count")
    public ResponseEntity<ApiResponse<Long>> getOnlineUserCount() {
        try {
            long count = userService.getOnlineUserCount();
            ApiResponse<Long> response = ApiResponse.success("获取在线用户数量成功", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取在线用户数量失败: error={}", e.getMessage());
            ApiResponse<Long> response = ApiResponse.error("获取在线用户数量失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户统计信息
     * GET /api/users/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<UserService.UserStatistics>> getUserStatistics() {
        try {
            UserService.UserStatistics stats = userService.getUserStatistics();
            ApiResponse<UserService.UserStatistics> response = ApiResponse.success("获取用户统计信息成功", stats);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户统计信息失败: error={}", e.getMessage());
            ApiResponse<UserService.UserStatistics> response = ApiResponse.error("获取用户统计信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ================================
    // 私有辅助方法
    // ================================
    
    /**
     * 将User实体转换为UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setSignature(user.getSignature());
        dto.setGender(user.getGender());
        dto.setBirthday(user.getBirthday());
        dto.setRegion(user.getRegion());
        dto.setStatus(user.getStatus());
        dto.setOnlineStatus(user.getOnlineStatus());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setLastActiveAt(user.getLastActiveAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
    
    /**
     * 从JWT token中提取用户ID
     * 分布式环境友好的实现方式
     */
    private Long extractUserIdFromToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 直接使用JwtTokenProvider解析JWT，不依赖Security上下文
            // 这种方式在分布式环境下更可靠
            String username = jwtTokenProvider.getUsernameFromToken(token);
            log.debug("从JWT获取用户名: {}", username);
            
            // 根据用户名查找用户ID
            Optional<User> userOpt = userService.findUserByUsername(username);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            } else {
                log.warn("用户不存在: {}", username);
                throw new RuntimeException("用户不存在: " + username);
            }
        } catch (Exception e) {
            log.error("JWT token解析失败: {}", e.getMessage());
            throw new RuntimeException("获取用户信息失败: " + e.getMessage());
        }
    }

} 