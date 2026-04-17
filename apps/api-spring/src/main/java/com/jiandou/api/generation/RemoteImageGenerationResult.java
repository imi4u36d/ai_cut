package com.jiandou.api.generation;

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
    int latencyMs
) {
}
