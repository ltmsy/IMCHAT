package com.acme.im.common.infrastructure.database.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 数据源选择器
 * 根据服务类型自动选择合适的数据源
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class DataSourceSelector {

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Value("${app.datasource.strategy:auto}")
    private String dataSourceStrategy;

    /**
     * 初始化时设置数据源策略
     */
    @PostConstruct
    public void init() {
        // 根据服务名称自动设置数据源策略
        if ("communication-service".equals(serviceName)) {
            // 通信服务默认使用从库
            DataSourceRouter.useSecondary();
            log.info("通信服务初始化完成，默认使用从库数据源");
        } else if ("business-service".equals(serviceName)) {
            // 业务服务默认使用主库
            DataSourceRouter.usePrimary();
            log.info("业务服务初始化完成，默认使用主库数据源");
        } else {
            // 其他服务默认使用主库
            DataSourceRouter.usePrimary();
            log.info("服务 {} 初始化完成，默认使用主库数据源", serviceName);
        }
    }

    /**
     * 获取当前服务的数据源策略
     */
    public String getDataSourceStrategy() {
        return dataSourceStrategy;
    }

    /**
     * 获取当前服务名称
     */
    public String getServiceName() {
        return serviceName;
    }
} 