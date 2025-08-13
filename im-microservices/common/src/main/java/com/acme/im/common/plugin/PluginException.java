package com.acme.im.common.plugin;

/**
 * 插件异常
 * 插件操作过程中的异常封装
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public class PluginException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginException(Throwable cause) {
        super(cause);
    }
} 