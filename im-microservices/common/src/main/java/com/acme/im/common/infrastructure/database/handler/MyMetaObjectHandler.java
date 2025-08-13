package com.acme.im.common.infrastructure.database.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus字段填充处理器
 * 自动填充创建时间和更新时间
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始执行插入填充");
        
        // 设置创建时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        // 设置更新时间
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        
        log.debug("插入填充完成");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始执行更新填充");
        
        // 设置更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        
        log.debug("更新填充完成");
    }
} 