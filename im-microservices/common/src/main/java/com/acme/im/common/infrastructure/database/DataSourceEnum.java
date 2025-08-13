package com.acme.im.common.infrastructure.database;

/**
 * 数据源枚举
 * 定义系统支持的数据源类型
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public enum DataSourceEnum {

    /**
     * 主数据源（写操作）
     */
    PRIMARY("primary", "主数据源"),

    /**
     * 从数据源（读操作）
     */
    SECONDARY("secondary", "从数据源");

    private final String value;
    private final String description;

    DataSourceEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据值获取枚举
     */
    public static DataSourceEnum fromValue(String value) {
        for (DataSourceEnum dataSource : values()) {
            if (dataSource.value.equals(value)) {
                return dataSource;
            }
        }
        throw new IllegalArgumentException("Unknown data source: " + value);
    }
} 