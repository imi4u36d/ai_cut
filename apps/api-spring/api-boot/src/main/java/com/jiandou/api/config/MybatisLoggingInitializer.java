package com.jiandou.api.config;

import org.apache.ibatis.logging.LogFactory;

/**
 * Initializes MyBatis logging before MyBatis-Plus static configuration is loaded.
 */
public final class MybatisLoggingInitializer {

    private MybatisLoggingInitializer() {
    }

    public static void initialize() {
        LogFactory.useSlf4jLogging();
    }
}
