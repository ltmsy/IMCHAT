package com.acme.im.common.infrastructure.database.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源路由配置
 * 支持主从数据源动态切换
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
public class DataSourceRouter extends AbstractRoutingDataSource {

    /**
     * 主数据源
     */
    private DataSource primaryDataSource;

    /**
     * 从数据源
     */
    private DataSource secondaryDataSource;

    /**
     * 数据源上下文
     */
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    /**
     * 设置主数据源
     */
    public void setPrimaryDataSource(DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    /**
     * 设置从数据源
     */
    public void setSecondaryDataSource(DataSource secondaryDataSource) {
        this.secondaryDataSource = secondaryDataSource;
    }

    /**
     * 设置当前数据源
     */
    public static void setDataSource(String dataSource) {
        contextHolder.set(dataSource);
        log.debug("切换到数据源: {}", dataSource);
    }

    /**
     * 获取当前数据源
     */
    public static String getDataSource() {
        return contextHolder.get();
    }

    /**
     * 清除数据源上下文
     */
    public static void clearDataSource() {
        contextHolder.remove();
        log.debug("清除数据源上下文");
    }

    /**
     * 切换到主库
     */
    public static void usePrimary() {
        setDataSource("primary");
    }

    /**
     * 切换到从库
     */
    public static void useSecondary() {
        setDataSource("secondary");
    }

    /**
     * 初始化数据源映射
     */
    @Override
    public void afterPropertiesSet() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("primary", primaryDataSource);
        targetDataSources.put("secondary", secondaryDataSource);
        
        setTargetDataSources(targetDataSources);
        setDefaultTargetDataSource(primaryDataSource);
        
        super.afterPropertiesSet();
        log.info("数据源路由初始化完成: primary={}, secondary={}", 
                primaryDataSource, secondaryDataSource);
    }

    /**
     * 确定当前线程应该使用的数据源
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSource = contextHolder.get();
        if (dataSource == null) {
            dataSource = "primary"; // 默认使用主库
        }
        log.debug("当前使用数据源: {}", dataSource);
        return dataSource;
    }
} 