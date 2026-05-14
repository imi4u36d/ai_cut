package com.jiandou.api;

import com.jiandou.api.config.MybatisLoggingInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * JianDouSpringAPI应用入口。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class JianDouSpringApiApplication {

    /**
     * 启动应用。
     * @param args 启动参数
     */
    public static void main(String[] args) {
        MybatisLoggingInitializer.initialize();
        SpringApplication.run(JianDouSpringApiApplication.class, args);
    }
}
