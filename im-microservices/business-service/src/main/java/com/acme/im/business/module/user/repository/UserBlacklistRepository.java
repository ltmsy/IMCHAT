package com.acme.im.business.module.user.repository;

import com.acme.im.business.module.user.entity.UserBlacklist;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户黑名单数据访问接口
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Mapper
public interface UserBlacklistRepository extends BaseMapper<UserBlacklist> {

    /**
     * 根据用户ID查找黑名单列表
     */
    @Select("SELECT * FROM user_blacklist WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserBlacklist> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和被拉黑用户ID查找黑名单记录
     */
    @Select("SELECT * FROM user_blacklist WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    UserBlacklist findByUserIdAndBlockedUserId(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    /**
     * 检查用户是否在黑名单中
     */
    @Select("SELECT COUNT(*) FROM user_blacklist WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    int countByUserIdAndBlockedUserId(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    /**
     * 根据用户ID统计黑名单数量
     */
    @Select("SELECT COUNT(*) FROM user_blacklist WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 根据被拉黑用户ID查找拉黑记录
     */
    @Select("SELECT * FROM user_blacklist WHERE blocked_user_id = #{blockedUserId}")
    List<UserBlacklist> findByBlockedUserId(@Param("blockedUserId") Long blockedUserId);
} 