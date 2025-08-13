package com.acme.im.business.repository;

import com.acme.im.business.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 对应数据库表：users
 * 使用MyBatis-Plus
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
    
    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查找用户
     * 
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    Optional<User> findByEmail(@Param("email") String email);
    
    /**
     * 根据手机号查找用户
     * 
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE phone = #{phone}")
    Optional<User> findByPhone(@Param("phone") String phone);
    
    /**
     * 根据状态查找用户列表
     * 
     * @param status 用户状态
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE status = #{status}")
    List<User> findByStatus(@Param("status") Integer status);
    
    /**
     * 根据在线状态查找用户列表
     * 
     * @param onlineStatus 在线状态
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE online_status = #{onlineStatus}")
    List<User> findByOnlineStatus(@Param("onlineStatus") Integer onlineStatus);
    
    /**
     * 根据最后活跃时间查找用户列表
     * 
     * @param lastActiveAt 最后活跃时间
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE last_active_at < #{lastActiveAt}")
    List<User> findByLastActiveAtBefore(@Param("lastActiveAt") LocalDateTime lastActiveAt);
    
    /**
     * 根据地区查找用户列表
     * 
     * @param region 地区
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE region = #{region}")
    List<User> findByRegion(@Param("region") String region);
    
    /**
     * 根据用户名模糊查询
     * 
     * @param username 用户名（模糊匹配）
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE username LIKE CONCAT('%', #{username}, '%') AND status = 1")
    List<User> findByUsernameContaining(@Param("username") String username);
    
    /**
     * 根据昵称模糊查询
     * 
     * @param nickname 昵称（模糊匹配）
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE nickname LIKE CONCAT('%', #{nickname}, '%') AND status = 1")
    List<User> findByNicknameContaining(@Param("nickname") String nickname);
    
    /**
     * 查找在线用户数量
     * 
     * @return 在线用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE online_status > 0")
    long countOnlineUsers();
    
    /**
     * 获取最近注册的用户
     * 
     * @param limit 限制数量
     * @return 用户列表
     */
    @Select("SELECT * FROM users ORDER BY created_at DESC LIMIT #{limit}")
    List<User> findRecentUsers(@Param("limit") int limit);
    
    /**
     * 根据最后活跃时间统计用户数量
     * 
     * @param lastActiveAt 最后活跃时间
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE last_active_at > #{lastActiveAt}")
    long countByLastActiveAtAfter(@Param("lastActiveAt") LocalDateTime lastActiveAt);
    
    /**
     * 根据创建时间统计用户数量
     * 
     * @param createdAt 创建时间
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE created_at > #{createdAt}")
    long countByCreatedAtAfter(@Param("createdAt") LocalDateTime createdAt);
    
    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    long countByUsername(@Param("username") String username);
    
    /**
     * 检查邮箱是否存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    long countByEmail(@Param("email") String email);
    
    /**
     * 检查手机号是否存在
     * 
     * @param phone 手机号
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE phone = #{phone}")
    long countByPhone(@Param("phone") String phone);
    
    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    default boolean existsByUsername(String username) {
        return countByUsername(username) > 0;
    }
    
    /**
     * 检查邮箱是否存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    default boolean existsByEmail(String email) {
        return countByEmail(email) > 0;
    }
    
    /**
     * 检查手机号是否存在
     * 
     * @param phone 手机号
     * @return 是否存在
     */
    default boolean existsByPhone(String phone) {
        return countByPhone(phone) > 0;
    }
} 