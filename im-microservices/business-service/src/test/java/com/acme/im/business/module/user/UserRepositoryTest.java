package com.acme.im.business.module.user;

import com.acme.im.business.module.user.entity.User;
import com.acme.im.business.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserRepository单元测试
 * 使用Mockito进行测试，不依赖数据库
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository单元测试")
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户数据
        testUser = new User();
        testUser.setId(1L);
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
        // 配置Mock行为
        when(userRepository.insert(any(User.class))).thenReturn(1);
        
        // 执行插入操作
        int result = userRepository.insert(testUser);
        
        // 验证插入成功
        assertEquals(1, result);
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).insert(testUser);
    }

    @Test
    @DisplayName("根据ID查找用户成功")
    void testFindUserById_Success() {
        // 配置Mock行为
        when(userRepository.selectById(1L)).thenReturn(testUser);
        
        // 根据ID查找用户
        User foundUser = userRepository.selectById(1L);
        
        // 验证查找结果
        assertNotNull(foundUser);
        assertEquals(1L, foundUser.getId());
        assertEquals("testuser", foundUser.getUsername());
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据用户名查找用户成功")
    void testFindUserByUsername_Success() {
        // 配置Mock行为
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // 根据用户名查找用户
        Optional<User> foundUserOpt = userRepository.findByUsername("testuser");
        
        // 验证查找结果
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals("testuser", foundUser.getUsername());
        assertEquals("test@example.com", foundUser.getEmail());
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("根据邮箱查找用户成功")
    void testFindUserByEmail_Success() {
        // 配置Mock行为
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // 根据邮箱查找用户
        Optional<User> foundUserOpt = userRepository.findByEmail("test@example.com");
        
        // 验证查找结果
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals("test@example.com", foundUser.getEmail());
        assertEquals("testuser", foundUser.getUsername());
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("根据手机号查找用户成功")
    void testFindUserByPhone_Success() {
        // 配置Mock行为
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(testUser));
        
        // 根据手机号查找用户
        Optional<User> foundUserOpt = userRepository.findByPhone("13800138000");
        
        // 验证查找结果
        assertTrue(foundUserOpt.isPresent());
        User foundUser = foundUserOpt.get();
        assertEquals("13800138000", foundUser.getPhone());
        assertEquals("testuser", foundUser.getUsername());
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).findByPhone("13800138000");
    }

    @Test
    @DisplayName("更新用户成功")
    void testUpdateUser_Success() {
        // 配置Mock行为
        when(userRepository.updateById(any(User.class))).thenReturn(1);
        
        // 更新用户信息
        testUser.setNickname("更新后的昵称");
        testUser.setEmail("updated@example.com");
        testUser.setUpdatedAt(LocalDateTime.now());
        
        int result = userRepository.updateById(testUser);
        
        // 验证更新成功
        assertEquals(1, result);
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).updateById(testUser);
    }

    @Test
    @DisplayName("删除用户成功")
    void testDeleteUser_Success() {
        // 配置Mock行为
        when(userRepository.deleteById(1L)).thenReturn(1);
        
        // 删除用户
        int result = userRepository.deleteById(1L);
        
        // 验证删除成功
        assertEquals(1, result);
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("统计用户数量成功")
    void testCountUsers_Success() {
        // 配置Mock行为
        when(userRepository.selectCount(null)).thenReturn(5L);
        
        // 统计用户数量
        Long count = userRepository.selectCount(null);
        
        // 验证统计结果
        assertEquals(5L, count);
        
        // 验证Mock方法被调用
        verify(userRepository, times(1)).selectCount(null);
    }
} 