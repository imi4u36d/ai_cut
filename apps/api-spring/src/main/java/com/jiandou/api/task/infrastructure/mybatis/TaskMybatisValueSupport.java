package com.jiandou.api.task.infrastructure.mybatis;

import java.time.OffsetDateTime;

/**
 * 任务MyBatis值支持。
 */
final class TaskMybatisValueSupport {

    /**
     * 创建新的任务MyBatis值支持。
     */
    private TaskMybatisValueSupport() {
    }

    /**
     * 处理string值。
     * @param value 待处理的值
     * @return 处理结果
     */
    static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 处理int值。
     * @param value 待处理的值
     * @param fallback 兜底值
     * @return 处理结果
     */
    static int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return fallback;
        }
        return Integer.parseInt(text);
    }

    /**
     * 处理默认Int。
     * @param value 待处理的值
     * @return 处理结果
     */
    static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 格式化format。
     * @param value 待处理的值
     * @return 处理结果
     */
    static String format(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * 处理默认Long。
     * @param value 待处理的值
     * @return 处理结果
     */
    static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 处理默认Double。
     * @param value 待处理的值
     * @return 处理结果
     */
    static double defaultDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    /**
     * 处理offset值。
     * @param value 待处理的值
     * @return 处理结果
     */
    static OffsetDateTime offsetValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return OffsetDateTime.parse(text);
    }

    /**
     * 检查是否布尔值。
     * @param value 待处理的值
     * @return 是否满足条件
     */
    static boolean boolValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    /**
     * 处理long值。
     * @param value 待处理的值
     * @param fallback 兜底值
     * @return 处理结果
     */
    static long longValue(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return fallback;
        }
        return Long.parseLong(text);
    }

    /**
     * 处理double值。
     * @param value 待处理的值
     * @param fallback 兜底值
     * @return 处理结果
     */
    static double doubleValue(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return fallback;
        }
        return Double.parseDouble(text);
    }
}
