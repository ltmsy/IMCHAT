package com.acme.im.business.controller;

import com.acme.im.business.dto.UserDTO;
import com.acme.im.business.dto.UserLoginRequest;
import com.acme.im.business.dto.UserRegisterRequest;
import com.acme.im.business.entity.User;
import com.acme.im.business.entity.UserDevice;
import com.acme.im.business.service.UserService;
import com.acme.im.common.response.ApiResponse;
import com.acme.im.common.security.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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
    // 用户搜索
    // ================================
    
    /**
     * 搜索用户
     * GET /api/users/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserDTO>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("搜索用户: keyword={}, limit={}", keyword, limit);
        
        try {
            List<User> users = userService.searchUsers(keyword, limit);
            List<UserDTO> userDTOs = users.stream()
                    .map(this::convertToDTO)
                    .toList();
            
            ApiResponse<List<UserDTO>> response = ApiResponse.success("搜索用户成功", userDTOs);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("搜索用户失败: keyword={}, error={}", keyword, e.getMessage());
            ApiResponse<List<UserDTO>> response = ApiResponse.error("搜索用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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