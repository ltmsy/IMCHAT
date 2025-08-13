package com.acme.im.common.infrastructure.database;

/**
 * 数据源上下文持有者
 * 使用ThreadLocal管理当前线程的数据源选择
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceEnum> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置数据源
     */
    public static void setDataSource(DataSourceEnum dataSource) {
        CONTEXT_HOLDER.set(dataSource);
    }

    /**
     * 获取数据源
     */
    public static DataSourceEnum getDataSource() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除数据源
     */
    public static void clearDataSource() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 切换到主库
     */
    public static void switchToPrimary() {
        setDataSource(DataSourceEnum.PRIMARY);
    }

    /**
     * 切换到从库
     */
    public static void switchToSecondary() {
        setDataSource(DataSourceEnum.SECONDARY);
    }
} 