package com.acme.im.common.infrastructure.database;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 动态数据源路由
 * 根据当前线程上下文自动选择主从数据源
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSource();
    }

    /**
     * 设置数据源映射
     */
    public void setDataSourceMap(Map<Object, Object> dataSourceMap) {
        super.setTargetDataSources(dataSourceMap);
    }

    /**
     * 设置默认数据源
     */
    public void setDefaultDataSource(DataSource defaultDataSource) {
        super.setDefaultTargetDataSource(defaultDataSource);
    }
} 