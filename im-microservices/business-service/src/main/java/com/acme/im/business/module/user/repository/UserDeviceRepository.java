package com.acme.im.business.module.user.repository;

import com.acme.im.business.module.user.entity.UserDevice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户设备数据访问接口
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Mapper
public interface UserDeviceRepository extends BaseMapper<UserDevice> {

    /**
     * 根据用户ID查找所有设备
     */
    @Select("SELECT * FROM user_devices WHERE user_id = #{userId} ORDER BY last_active_at DESC")
    List<UserDevice> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和设备ID查找设备
     */
    @Select("SELECT * FROM user_devices WHERE user_id = #{userId} AND device_id = #{deviceId}")
    UserDevice findByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    /**
     * 根据用户ID查找在线设备数量
     */
    @Select("SELECT COUNT(*) FROM user_devices WHERE user_id = #{userId} AND is_online = 1")
    int countOnlineDevicesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查找受信任设备数量
     */
    @Select("SELECT COUNT(*) FROM user_devices WHERE user_id = #{userId} AND is_trusted = 1")
    int countTrustedDevicesByUserId(@Param("userId") Long userId);

    /**
     * 根据设备ID查找设备
     */
    @Select("SELECT * FROM user_devices WHERE device_id = #{deviceId}")
    UserDevice findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据用户ID查找最近活跃的设备
     */
    @Select("SELECT * FROM user_devices WHERE user_id = #{userId} ORDER BY last_active_at DESC LIMIT 1")
    UserDevice findMostRecentDeviceByUserId(@Param("userId") Long userId);
} 