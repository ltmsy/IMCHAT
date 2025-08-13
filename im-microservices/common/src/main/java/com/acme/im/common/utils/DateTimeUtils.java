package com.acme.im.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 日期时间工具类
 * 提供常用的日期时间处理方法
 * 
 * @author IM开发团队
 * @since 1.0.0
 */
@Component
@Slf4j
public class DateTimeUtils {

    // 常用日期时间格式
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_MS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATE_SLASH_FORMAT = "yyyy/MM/dd";
    public static final String TIME_COLON_FORMAT = "HH:mm:ss.SSS";

    // 常用格式化器
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
    public static final DateTimeFormatter DATETIME_MS_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_MS_FORMAT);

    /**
     * 获取当前日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间
     */
    public static LocalTime currentTime() {
        return LocalTime.now();
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     */
    public static long currentTimestampSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 格式化日期时间
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return dateTime.format(formatter);
        } catch (Exception e) {
            log.error("格式化日期时间失败: dateTime={}, pattern={}, error={}", dateTime, pattern, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 格式化日期时间（使用默认格式）
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DATETIME_FORMAT);
    }

    /**
     * 格式化日期
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * 格式化时间
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }

    /**
     * 解析日期时间字符串
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.error("解析日期时间失败: dateTimeStr={}, pattern={}, error={}", dateTimeStr, pattern, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析日期时间字符串（使用默认格式）
     */
    public static LocalDateTime parse(String dateTimeStr) {
        return parse(dateTimeStr, DATETIME_FORMAT);
    }

    /**
     * 解析日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.error("解析日期失败: dateStr={}, error={}", dateStr, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析时间字符串
     */
    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception e) {
            log.error("解析时间失败: timeStr={}, error={}", timeStr, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Date转换为LocalDateTime
     */
    public static LocalDateTime fromDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * LocalDateTime转换为Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 时间戳转换为LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime转换为时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 获取两个日期时间之间的差值（毫秒）
     */
    public static long diffMillis(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toMillis();
    }

    /**
     * 获取两个日期时间之间的差值（秒）
     */
    public static long diffSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).getSeconds();
    }

    /**
     * 获取两个日期时间之间的差值（分钟）
     */
    public static long diffMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toMinutes();
    }

    /**
     * 获取两个日期时间之间的差值（小时）
     */
    public static long diffHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toHours();
    }

    /**
     * 获取两个日期之间的差值（天）
     */
    public static long diffDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 添加时间
     */
    public static LocalDateTime add(LocalDateTime dateTime, long amount, ChronoUnit unit) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plus(amount, unit);
    }

    /**
     * 添加天数
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        return add(dateTime, days, ChronoUnit.DAYS);
    }

    /**
     * 添加小时
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        return add(dateTime, hours, ChronoUnit.HOURS);
    }

    /**
     * 添加分钟
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return add(dateTime, minutes, ChronoUnit.MINUTES);
    }

    /**
     * 添加秒数
     */
    public static LocalDateTime addSeconds(LocalDateTime dateTime, long seconds) {
        return add(dateTime, seconds, ChronoUnit.SECONDS);
    }

    /**
     * 减去时间
     */
    public static LocalDateTime subtract(LocalDateTime dateTime, long amount, ChronoUnit unit) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.minus(amount, unit);
    }

    /**
     * 减去天数
     */
    public static LocalDateTime subtractDays(LocalDateTime dateTime, long days) {
        return subtract(dateTime, days, ChronoUnit.DAYS);
    }

    /**
     * 减去小时
     */
    public static LocalDateTime subtractHours(LocalDateTime dateTime, long hours) {
        return subtract(dateTime, hours, ChronoUnit.HOURS);
    }

    /**
     * 减去分钟
     */
    public static LocalDateTime subtractMinutes(LocalDateTime dateTime, long minutes) {
        return subtract(dateTime, minutes, ChronoUnit.MINUTES);
    }

    /**
     * 减去秒数
     */
    public static LocalDateTime subtractSeconds(LocalDateTime dateTime, long seconds) {
        return subtract(dateTime, seconds, ChronoUnit.SECONDS);
    }

    /**
     * 获取一天的开始时间
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * 获取一天的结束时间
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 获取一周的开始时间
     */
    public static LocalDateTime startOfWeek(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
    }

    /**
     * 获取一周的结束时间
     */
    public static LocalDateTime endOfWeek(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
    }

    /**
     * 获取一个月的开始时间
     */
    public static LocalDateTime startOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }

    /**
     * 获取一个月的结束时间
     */
    public static LocalDateTime endOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
    }

    /**
     * 获取一年的开始时间
     */
    public static LocalDateTime startOfYear(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
    }

    /**
     * 获取一年的结束时间
     */
    public static LocalDateTime endOfYear(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
    }

    /**
     * 判断是否为同一天
     */
    public static boolean isSameDay(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.toLocalDate().equals(dateTime2.toLocalDate());
    }

    /**
     * 判断是否为今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * 判断是否为昨天
     */
    public static boolean isYesterday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now().minusDays(1));
    }

    /**
     * 判断是否为明天
     */
    public static boolean isTomorrow(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now().plusDays(1));
    }

    /**
     * 获取相对时间描述（如：刚刚、5分钟前、1小时前等）
     */
    public static String getRelativeTimeDescription(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知时间";
        }

        LocalDateTime now = LocalDateTime.now();
        long diffSeconds = Math.abs(Duration.between(dateTime, now).getSeconds());

        if (diffSeconds < 60) {
            return "刚刚";
        } else if (diffSeconds < 3600) {
            long minutes = diffSeconds / 60;
            return minutes + "分钟前";
        } else if (diffSeconds < 86400) {
            long hours = diffSeconds / 3600;
            return hours + "小时前";
        } else if (diffSeconds < 2592000) {
            long days = diffSeconds / 86400;
            return days + "天前";
        } else if (diffSeconds < 31536000) {
            long months = diffSeconds / 2592000;
            return months + "个月前";
        } else {
            long years = diffSeconds / 31536000;
            return years + "年前";
        }
    }
} 