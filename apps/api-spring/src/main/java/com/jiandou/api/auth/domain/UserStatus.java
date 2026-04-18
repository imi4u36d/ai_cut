package com.jiandou.api.auth.domain;

/**
 * 用户状态。
 */
public enum UserStatus {
    ACTIVE,
    DISABLED;

    /**
     * 返回状态值。
     * @return 处理结果
     */
    public String value() {
        return name();
    }

    /**
     * 解析状态。
     * @param raw 原始值
     * @return 处理结果
     */
    public static UserStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("状态不能为空");
        }
        return UserStatus.valueOf(raw.trim().toUpperCase());
    }
}
