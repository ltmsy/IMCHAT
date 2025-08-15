package com.acme.im.communication.config;

import com.acme.im.common.infrastructure.database.annotation.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 通信服务数据源配置
 * 确保数据源路由功能正常工作
 * 
 * 配置说明：
 * 1. 启用AOP代理，支持@DataSource注解
 * 2. 通信服务使用读写分离策略：
 *    - 读操作：使用从库(SECONDARY)
 *    - 写操作：使用主库(PRIMARY)
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Slf4j
public class CommunicationDataSourceConfig {

    /**
     * 配置说明：
     * 
     * 1. Repository层数据源配置：
     *    - MessageRepository: 读写分离，根据方法类型自动选择数据源
     *    - CustomMessageRepository: 只读，统一使用从库
     * 
     * 2. Service层数据源配置：
     *    - ReadOnlyMessageQueryService: 只读，使用从库
     *    - MessageService: 读写混合，通过Repository层的数据源配置自动路由
     * 
     * 3. 数据源路由策略：
     *    - 主库(PRIMARY): 用于消息的增删改操作
     *    - 从库(SECONDARY): 用于消息的查询操作
     * 
     * 4. 事务管理：
     *    - 写操作：使用主库事务管理器
     *    - 读操作：使用从库，支持只读事务优化
     */
    
    public CommunicationDataSourceConfig() {
        log.info("通信服务数据源配置初始化完成");
        log.info("数据源策略: 读写分离");
        log.info("  - 读操作: 从库(SECONDARY)");
        log.info("  - 写操作: 主库(PRIMARY)");
    }
} 