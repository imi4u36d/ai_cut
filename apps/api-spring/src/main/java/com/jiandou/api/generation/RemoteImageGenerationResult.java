package com.jiandou.api.generation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 远程图像生成记录结构。
 * @param data data值
 * @param mimeType mime类型值
 * @param remoteSourceUrl 远程来源URL值
 * @param provider provider值
 * @param providerModel provider模型值
 * @param endpointHost endpointHost值
 * @param width width值
 * @param height height值
 * @param requestedSize requestedSize值
 * @param latencyMs latency毫秒值
 * @param providerRequest 实际Provider请求体
 * @param providerResponse 实际Provider响应体
 * @param httpStatus HTTP状态码
 */
public record RemoteImageGenerationResult(
    byte[] data,
    String mimeType,
    String remoteSourceUrl,
    String provider,
    String providerModel,
    String endpointHost,
    int width,
    int height,
    String requestedSize,
    int latencyMs,
    Map<String, Object> providerRequest,
    Map<String, Object> providerResponse,
    int httpStatus
) {
    public RemoteImageGenerationResult {
        providerRequest = providerRequest == null ? Map.of() : new LinkedHashMap<>(providerRequest);
        providerResponse = providerResponse == null ? Map.of() : new LinkedHashMap<>(providerResponse);
    }

    public RemoteImageGenerationResult(
        byte[] data,
        String mimeType,
        String remoteSourceUrl,
        String provider,
        String providerModel,
        String endpointHost,
        int width,
        int height,
        String requestedSize,
        int latencyMs
    ) {
        this(data, mimeType, remoteSourceUrl, provider, providerModel, endpointHost, width, height, requestedSize, latencyMs, Map.of(), Map.of(), 0);
    }
}
