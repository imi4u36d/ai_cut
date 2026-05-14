package com.jiandou.api.credit.domain;

import java.util.Locale;

/**
 * 积分功能编码。
 */
public final class CreditFeatureCode {

    public static final String IMAGE_GENERATION = "IMAGE_GENERATION";
    public static final String VIDEO_GENERATION = "VIDEO_GENERATION";

    private CreditFeatureCode() {
    }

    /**
     * 规范化功能编码。
     * @param value 原始值
     * @return 处理结果
     */
    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
