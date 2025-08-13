package com.acme.im.common.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 通用验证工具类
 * 提供常用的数据验证方法
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class ValidationUtils {

    // 常用正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^1[3-9]\\d{9}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$");

    /**
     * 验证字符串是否为空或null
     */
    public static boolean isEmpty(String str) {
        return !StringUtils.hasText(str);
    }

    /**
     * 验证字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return StringUtils.hasText(str);
    }

    /**
     * 验证集合是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 验证集合是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * 验证数组是否为空
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 验证数组是否不为空
     */
    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    /**
     * 验证对象是否为null
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 验证对象是否不为null
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 验证字符串长度是否在指定范围内
     */
    public static boolean isLengthInRange(String str, int minLength, int maxLength) {
        if (isEmpty(str)) {
            return false;
        }
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 验证字符串长度是否等于指定值
     */
    public static boolean isLengthEqual(String str, int length) {
        if (isEmpty(str)) {
            return false;
        }
        return str.length() == length;
    }

    /**
     * 验证数字是否在指定范围内
     */
    public static boolean isNumberInRange(Number number, Number min, Number max) {
        if (number == null || min == null || max == null) {
            return false;
        }
        double value = number.doubleValue();
        double minValue = min.doubleValue();
        double maxValue = max.doubleValue();
        return value >= minValue && value <= maxValue;
    }

    /**
     * 验证是否为有效的邮箱地址
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证是否为有效的手机号码
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 验证是否为有效的用户名
     */
    public static boolean isValidUsername(String username) {
        if (isEmpty(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 验证是否为有效的密码
     */
    public static boolean isValidPassword(String password) {
        if (isEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 验证是否为有效的URL
     */
    public static boolean isValidUrl(String url) {
        if (isEmpty(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * 验证是否为有效的身份证号码
     */
    public static boolean isValidIdCard(String idCard) {
        if (isEmpty(idCard)) {
            return false;
        }
        // 简单的身份证验证（18位）
        if (idCard.length() != 18) {
            return false;
        }
        // 检查前17位是否为数字
        for (int i = 0; i < 17; i++) {
            if (!Character.isDigit(idCard.charAt(i))) {
                return false;
            }
        }
        // 检查最后一位是否为数字或X
        char lastChar = idCard.charAt(17);
        return Character.isDigit(lastChar) || lastChar == 'X' || lastChar == 'x';
    }

    /**
     * 验证是否为有效的IPv4地址
     */
    public static boolean isValidIPv4(String ip) {
        if (isEmpty(ip)) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证是否为有效的端口号
     */
    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    /**
     * 验证字符串是否只包含数字
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证字符串是否只包含字母
     */
    public static boolean isAlpha(String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证字符串是否只包含字母和数字
     */
    public static boolean isAlphanumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证字符串是否包含敏感词
     */
    public static boolean containsSensitiveWords(String content, Collection<String> sensitiveWords) {
        if (isEmpty(content) || isEmpty(sensitiveWords)) {
            return false;
        }
        String lowerContent = content.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerContent.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证文件大小是否在限制范围内
     */
    public static boolean isFileSizeValid(long fileSize, long maxSize) {
        return fileSize > 0 && fileSize <= maxSize;
    }

    /**
     * 验证文件扩展名是否允许
     */
    public static boolean isFileExtensionAllowed(String fileName, Collection<String> allowedExtensions) {
        if (isEmpty(fileName) || isEmpty(allowedExtensions)) {
            return false;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return false;
        }
        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        return allowedExtensions.contains(extension);
    }
} 