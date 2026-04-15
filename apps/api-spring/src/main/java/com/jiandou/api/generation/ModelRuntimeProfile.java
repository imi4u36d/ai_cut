package com.jiandou.api.generation;

import java.net.URI;

/**
 * 模型运行时记录结构。
 * @param provider provider值
 * @param modelName 模型Name值
 * @param fallbackModel 兜底模型值
 * @param apiKey APIKey值
 * @param baseUrl 基础 URL
 * @param timeoutSeconds timeoutSeconds值
 * @param temperature temperature值
 * @param maxTokens 最大Tokens值
 * @param source 来源值
 */
public record ModelRuntimeProfile(
    String provider,
    String modelName,
    String fallbackModel,
    String apiKey,
    String baseUrl,
    int timeoutSeconds,
    double temperature,
    int maxTokens,
    String source
) {

    /**
     * 检查是否ready。
     * @return 是否满足条件
     */
    public boolean ready() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }

    /**
     * 处理endpointHost。
     * @return 处理结果
     */
    public String endpointHost() {
        try {
            return URI.create(baseUrl).getHost();
        } catch (Exception ignored) {
            return "";
        }
    }
}
