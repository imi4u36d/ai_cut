package com.jiandou.api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JianDou 运行时应用配置。
 */
@ConfigurationProperties(prefix = "jiandou.app")
public class JiandouAppProperties {

    private static final String DEFAULT_EXECUTION_MODE = "queue";

    private String env = "dev";
    private String executionMode = DEFAULT_EXECUTION_MODE;
    private String webOrigin = "http://127.0.0.1:5173,http://localhost:5173,http://127.0.0.1:5174,http://localhost:5174";
    private boolean cookieSecure = false;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env == null ? "dev" : env.trim();
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = normalizeExecutionMode(executionMode);
    }

    public String getWebOrigin() {
        return webOrigin;
    }

    public void setWebOrigin(String webOrigin) {
        this.webOrigin = webOrigin == null ? "" : webOrigin.trim();
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String resolveCookieSameSite() {
        return cookieSecure ? "None" : "Lax";
    }

    public List<String> resolveWebOrigins() {
        String[] rawValues = webOrigin.split(",");
        List<String> values = new ArrayList<>();
        for (String rawValue : rawValues) {
            String value = rawValue == null ? "" : rawValue.trim();
            if (!value.isEmpty() && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private String normalizeExecutionMode(String rawValue) {
        return rawValue == null ? DEFAULT_EXECUTION_MODE : rawValue.trim().toLowerCase(Locale.ROOT);
    }
}
