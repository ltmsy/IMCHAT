package com.acme.im.business.module.user.repository;

import com.acme.im.business.module.user.entity.UserPrivacySettings;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户隐私设置数据访问接口
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Mapper
public interface UserPrivacySettingsRepository extends BaseMapper<UserPrivacySettings> {

    /**
     * 根据用户ID查找隐私设置
     */
    @Select("SELECT * FROM user_privacy_settings WHERE user_id = #{userId}")
    UserPrivacySettings findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查找好友申请模式
     */
    @Select("SELECT friend_request_mode FROM user_privacy_settings WHERE user_id = #{userId}")
    Integer findFriendRequestModeByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查找搜索可见性设置
     */
    @Select("SELECT allow_search_by_username, allow_search_by_phone, allow_search_by_email FROM user_privacy_settings WHERE user_id = #{userId}")
    UserPrivacySettings findSearchVisibilityByUserId(@Param("userId") Long userId);
} 