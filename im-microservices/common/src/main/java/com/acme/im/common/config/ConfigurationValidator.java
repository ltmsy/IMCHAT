package com.acme.im.common.config;

/**
 * 配置验证器接口
 * 定义配置验证的标准
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
public interface ConfigurationValidator {

    /**
     * 验证配置是否有效
     * 
     * @return 验证结果
     */
    ValidationResult validate();

    /**
     * 验证结果
     */
    class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "配置验证通过");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
} 