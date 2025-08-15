package com.acme.im.business.module.common.repository;

import com.acme.im.common.infrastructure.nats.entity.EventRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 事件记录仓库接口
 * 使用MyBatis-Plus提供事件记录的数据库操作
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Mapper
public interface EventRepository extends BaseMapper<EventRecord> {

    /**
     * 根据事件ID查找事件记录
     */
    @Select("SELECT * FROM event_records WHERE event_id = #{eventId}")
    EventRecord findByEventId(@Param("eventId") String eventId);

    /**
     * 根据主题查找事件记录，按创建时间倒序
     */
    @Select("SELECT * FROM event_records WHERE subject = #{subject} ORDER BY created_at DESC LIMIT #{limit}")
    List<EventRecord> findBySubjectOrderByCreatedAtDesc(@Param("subject") String subject, @Param("limit") int limit);

    /**
     * 根据用户ID查找事件记录，按创建时间倒序
     */
    @Select("SELECT * FROM event_records WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<EventRecord> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, @Param("limit") int limit);

    /**
     * 根据状态查找事件记录，按创建时间倒序
     */
    @Select("SELECT * FROM event_records WHERE status = #{status} ORDER BY created_at DESC LIMIT #{limit}")
    List<EventRecord> findByStatusOrderByCreatedAtDesc(@Param("status") String status, @Param("limit") int limit);

    /**
     * 根据优先级列表查找事件记录，按创建时间倒序
     */
    @Select("SELECT * FROM event_records WHERE priority IN (${priorities}) ORDER BY created_at DESC LIMIT #{limit}")
    List<EventRecord> findByPriorityInOrderByCreatedAtDesc(@Param("priorities") String priorities, @Param("limit") int limit);

    /**
     * 根据过期时间删除事件记录
     */
    @Update("DELETE FROM event_records WHERE expires_at < #{beforeTime}")
    int deleteByExpiresAtBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计指定状态的事件数量
     */
    @Select("SELECT COUNT(*) FROM event_records WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    /**
     * 统计指定优先级列表的事件数量
     */
    @Select("SELECT COUNT(*) FROM event_records WHERE priority IN (${priorities})")
    long countByPriorityIn(@Param("priorities") String priorities);

    /**
     * 按主题分组统计事件数量
     */
    @Select("SELECT subject, COUNT(*) as count FROM event_records GROUP BY subject")
    List<Map<String, Object>> countBySubjectGroupBySubject();

    /**
     * 查找指定时间范围内的事件记录
     */
    @Select("SELECT * FROM event_records WHERE created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<EventRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查找包含指定错误代码的事件记录
     */
    @Select("SELECT * FROM event_records WHERE error_code = #{errorCode} ORDER BY created_at DESC LIMIT #{limit}")
    List<EventRecord> findByErrorCodeOrderByCreatedAtDesc(@Param("errorCode") String errorCode, @Param("limit") int limit);

    /**
     * 查找指定服务的事件记录
     */
    @Select("SELECT * FROM event_records WHERE source_service = #{serviceName} OR target_service = #{serviceName} ORDER BY created_at DESC LIMIT #{limit}")
    List<EventRecord> findByServiceOrderByCreatedAtDesc(@Param("serviceName") String serviceName, @Param("limit") int limit);
} 