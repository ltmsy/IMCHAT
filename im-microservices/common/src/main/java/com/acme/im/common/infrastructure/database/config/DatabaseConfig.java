package com.acme.im.common.infrastructure.database.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 数据库配置类
 * 支持主从双数据源配置
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@RequiredArgsConstructor
public class DatabaseConfig {

    private final DatabaseProperties databaseProperties;

    /**
     * 创建HikariCP配置
     */
    private HikariConfig createHikariConfig(DatabaseProperties.DataSourceConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        // 设置基本连接信息
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        
        // 设置驱动类名，如果为空则使用默认MySQL驱动
        String driverClassName = config.getDriverClassName();
        if (driverClassName != null && !driverClassName.trim().isEmpty()) {
            hikariConfig.setDriverClassName(driverClassName);
        } else {
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        
        // 设置HikariCP连接池配置
        DatabaseProperties.HikariConfig hikari = config.getHikari();
        if (hikari != null) {
            hikariConfig.setMaximumPoolSize(hikari.getMaximumPoolSize());
            hikariConfig.setMinimumIdle(hikari.getMinimumIdle());
            hikariConfig.setConnectionTimeout(hikari.getConnectionTimeout().toMillis());
            hikariConfig.setIdleTimeout(hikari.getIdleTimeout().toMillis());
            hikariConfig.setMaxLifetime(hikari.getMaxLifetime().toMillis());
            hikariConfig.setPoolName(hikari.getPoolName());
            hikariConfig.setConnectionTestQuery(hikari.getConnectionTestQuery());
            hikariConfig.setValidationTimeout(hikari.getValidationTimeout().toMillis());
        }
        
        // 设置默认值
        hikariConfig.setMaximumPoolSize(hikariConfig.getMaximumPoolSize() > 0 ? hikariConfig.getMaximumPoolSize() : 20);
        hikariConfig.setMinimumIdle(hikariConfig.getMinimumIdle() > 0 ? hikariConfig.getMinimumIdle() : 5);
        hikariConfig.setConnectionTimeout(hikariConfig.getConnectionTimeout() > 0 ? hikariConfig.getConnectionTimeout() : 30000);
        hikariConfig.setIdleTimeout(hikariConfig.getIdleTimeout() > 0 ? hikariConfig.getIdleTimeout() : 600000);
        hikariConfig.setMaxLifetime(hikariConfig.getMaxLifetime() > 0 ? hikariConfig.getMaxLifetime() : 1800000);
        
        log.info("创建HikariCP配置: url={}, username={}, poolName={}", 
                config.getUrl(), config.getUsername(), hikariConfig.getPoolName());
        
        return hikariConfig;
    }

    /**
     * 主数据源配置
     */
    @Bean
    @Primary
    public HikariConfig primaryHikariConfig() {
        log.info("配置主数据源: {}", databaseProperties.getPrimary().getUrl());
        return createHikariConfig(databaseProperties.getPrimary());
    }

    /**
     * 从数据源配置
     */
    @Bean
    public HikariConfig secondaryHikariConfig() {
        log.info("配置从数据源: {}", databaseProperties.getSecondary().getUrl());
        return createHikariConfig(databaseProperties.getSecondary());
    }

    /**
     * 主数据源
     */
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        HikariConfig config = primaryHikariConfig();
        return new HikariDataSource(config);
    }

    /**
     * 从数据源
     */
    @Bean
    public DataSource secondaryDataSource() {
        HikariConfig config = secondaryHikariConfig();
        return new HikariDataSource(config);
    }

    /**
     * 数据源路由（动态数据源）
     */
    @Bean
    public DataSource dataSource() {
        DataSourceRouter router = new DataSourceRouter();
        router.setPrimaryDataSource(primaryDataSource());
        router.setSecondaryDataSource(secondaryDataSource());
        return router;
    }

    /**
     * 主数据源事务管理器
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(primaryDataSource());
    }

    /**
     * 从数据源事务管理器
     */
    @Bean
    public PlatformTransactionManager secondaryTransactionManager() {
        return new DataSourceTransactionManager(secondaryDataSource());
    }

    /**
     * MyBatis-Plus分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
} 