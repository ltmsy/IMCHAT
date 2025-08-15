package com.acme.im.business.module.user;

import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基本用户功能测试
 * 测试用户的核心CRUD操作
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("基本用户功能测试")
class BasicUserTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户数据
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setPasswordHash("password123_hash");
        testUser.setSalt("password123_salt");
        testUser.setNickname("测试用户");
        testUser.setGender(1);
        testUser.setStatus(1);
        testUser.setOnlineStatus(0);
        testUser.setTwoFactorEnabled(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建用户成功")
    void testCreateUser_Success() {
        // 执行插入操作
        int result = userRepository.insert(testUser);
        
        // 验证插入成功
        assertEquals(1, result);
        assertNotNull(testUser.getId());
        
        // 验证用户信息
        assertEquals("testuser", testUser.getUsername());
        assertEquals("test@example.com", testUser.getEmail());
        assertEquals("13800138000", testUser.getPhone());
    }

    @Test
    @DisplayName("根据ID查找用户成功")
    void testFindUserById_Success() {
        // 先插入用户
        userRepository.insert(testUser);
        Long userId = testUser.getId();
        
        // 根据ID查找用户
        User foundUser = userRepository.selectById(userId);
        
        // 验证查找结果
        assertNotNull(foundUser);
        assertEquals(userId, foundUser.getId());
        assertEquals("testuser", foundUser.getUsername());
    }

    @Test
    @DisplayName("根据用户名查找用户成功")
    void testFindUserByUsername_Success() {
        // 先插入用户
        userRepository.insert(testUser);
        
        // 根据用户名查找用户
        Optional<User> foundUserOpt = userRepository.findByUsername("testuser");
        
        // 验证查找结果
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals("testuser", foundUser.getUsername());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    @DisplayName("根据邮箱查找用户成功")
    void testFindUserByEmail_Success() {
        // 先插入用户
        userRepository.insert(testUser);
        
        // 根据邮箱查找用户
        Optional<User> foundUserOpt = userRepository.findByEmail("test@example.com");
        
        // 验证查找结果
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals("test@example.com", foundUser.getEmail());
        assertEquals("testuser", foundUser.getUsername());
    }

    @Test
    @DisplayName("根据手机号查找用户成功")
    void testFindUserByPhone_Success() {
        // 先插入用户
        userRepository.insert(testUser);
        
        // 根据手机号查找用户
        Optional<User> foundUserOpt = userRepository.findByPhone("13800138000");
        
        // 验证查找结果
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals("13800138000", foundUser.getPhone());
        assertEquals("testuser", foundUser.getUsername());
    }

    @Test
    @DisplayName("更新用户成功")
    void testUpdateUser_Success() {
        // 先插入用户
        userRepository.insert(testUser);
        Long userId = testUser.getId();
        
        // 更新用户信息
        testUser.setNickname("更新后的昵称");
        testUser.setEmail("updated@example.com");
        testUser.setUpdatedAt(LocalDateTime.now());
        
        int result = userRepository.updateById(testUser);
        
        // 验证更新成功
        assertEquals(1, result);
        
        // 验证更新后的信息
        User updatedUser = userRepository.selectById(userId);
        assertEquals("更新后的昵称", updatedUser.getNickname());
        assertEquals("updated@example.com", updatedUser.getEmail());
    }

    @Test
    @DisplayName("删除用户成功")
    void testDeleteUser_Success() {
        // 先插入用户
        userRepository.insert(testUser);
        Long userId = testUser.getId();
        
        // 删除用户
        int result = userRepository.deleteById(userId);
        
        // 验证删除成功
        assertEquals(1, result);
        
        // 验证用户已被删除
        User deletedUser = userRepository.selectById(userId);
        assertNull(deletedUser);
    }

    @Test
    @DisplayName("统计用户数量成功")
    void testCountUsers_Success() {
        // 先插入用户
        userRepository.insert(testUser);
        
        // 统计用户数量
        Long count = userRepository.selectCount(null);
        
        // 验证统计结果
        assertTrue(count > 0);
    }
} 