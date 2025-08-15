package com.acme.im.business.module.common.config;

import com.acme.im.common.infrastructure.database.config.DataSourceRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 业务服务数据源配置
 * 支持读写分离：写操作使用主库，读操作使用从库
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class BusinessDataSourceConfig {

    @Value("${app.datasource.strategy:auto}")
    private String dataSourceStrategy;

    @Value("${app.datasource.read-write-split:true}")
    private boolean readWriteSplit;

    /**
     * 初始化数据源策略
     */
    @PostConstruct
    public void init() {
        if ("auto".equals(dataSourceStrategy)) {
            if (readWriteSplit) {
                log.info("业务服务启用读写分离：写操作使用主库，读操作使用从库");
            } else {
                log.info("业务服务使用主库数据源");
            }
        } else if ("primary".equals(dataSourceStrategy)) {
            DataSourceRouter.usePrimary();
            log.info("业务服务强制使用主库数据源");
        } else if ("secondary".equals(dataSourceStrategy)) {
            DataSourceRouter.useSecondary();
            log.info("业务服务强制使用从库数据源");
        }
    }

    /**
     * 切换到主库（写操作）
     */
    public void usePrimaryForWrite() {
        DataSourceRouter.usePrimary();
        log.debug("业务服务切换到主库（写操作）");
    }

    /**
     * 切换到从库（读操作）
     */
    public void useSecondaryForRead() {
        DataSourceRouter.useSecondary();
        log.debug("业务服务切换到从库（读操作）");
    }

    /**
     * 获取当前数据源策略
     */
    public String getDataSourceStrategy() {
        return dataSourceStrategy;
    }

    /**
     * 是否启用读写分离
     */
    public boolean isReadWriteSplit() {
        return readWriteSplit;
    }
} 