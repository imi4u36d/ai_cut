package com.jiandou.api.generation;

import java.net.URI;

/**
 * 媒体Provider记录结构。
 * @param provider provider值
 * @param modelName 模型Name值
 * @param apiKey APIKey值
 * @param baseUrl 基础 URL
 * @param taskBaseUrl 任务BaseURL值
 * @param timeoutSeconds timeoutSeconds值
 * @param pollIntervalSeconds pollIntervalSeconds值
 * @param pollTimeoutSeconds pollTimeoutSeconds值
 * @param promptExtend 提示词Extend值
 * @param cameraFixed cameraFixed值
 * @param watermark watermark值
 * @param source 来源值
 */
public record MediaProviderProfile(
    String provider,
    String modelName,
    String apiKey,
    String baseUrl,
    String taskBaseUrl,
    int timeoutSeconds,
    int pollIntervalSeconds,
    int pollTimeoutSeconds,
    boolean promptExtend,
    boolean cameraFixed,
    boolean watermark,
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
        return hostOf(baseUrl);
    }

    /**
     * 处理任务EndpointHost。
     * @return 处理结果
     */
    public String taskEndpointHost() {
        return hostOf(taskBaseUrl);
    }

    /**
     * 处理host。
     * @param raw 原始值
     * @return 处理结果
     */
    private String hostOf(String raw) {
        try {
            return URI.create(raw).getHost();
        } catch (Exception ignored) {
            return "";
        }
    }
}
