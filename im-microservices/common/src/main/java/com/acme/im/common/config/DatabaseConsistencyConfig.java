package com.acme.im.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据库配置一致性说明
 * 
 * 注意：当前项目同时使用了JPA和MyBatis-Plus，需要注意以下配置一致性：
 * 
 * 1. 事务管理：
 *    - 使用@EnableTransactionManagement启用Spring事务管理
 *    - 事务边界应该明确，避免跨框架的事务嵌套
 * 
 * 2. 实体映射：
 *    - JPA实体：使用@Entity、@Table等注解
 *    - MyBatis实体：使用@TableName等注解
 *    - 同一张表不要同时用两种方式映射
 * 
 * 3. 数据源配置：
 *    - 当前使用单一数据源，事务管理器为DataSourceTransactionManager
 *    - 如需要读写分离，请使用DynamicRoutingDataSource
 * 
 * 4. 建议的架构模式：
 *    - 业务服务：主要使用JPA进行CRUD操作
 *    - 通信服务：主要使用MyBatis-Plus进行消息查询
 *    - 避免在同一服务中混用两种ORM框架
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConsistencyConfig {

    /**
     * 数据库配置一致性检查
     * 在应用启动时验证配置是否正确
     */
    public void validateConfiguration() {
        // TODO: 实现配置一致性检查逻辑
        // 1. 检查数据源配置
        // 2. 检查事务管理器配置
        // 3. 检查ORM框架配置
        // 4. 检查包扫描配置
    }
} 