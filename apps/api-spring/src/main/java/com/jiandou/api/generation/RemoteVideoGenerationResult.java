package com.jiandou.api.generation;

/**
 * 远程视频生成记录结构。
 * @param data data值
 * @param mimeType mime类型值
 * @param remoteSourceUrl 远程来源URL值
 * @param provider provider值
 * @param providerModel provider模型值
 * @param modelName 模型Name值
 * @param endpointHost endpointHost值
 * @param taskEndpointHost 任务EndpointHost值
 * @param taskId 任务标识
 * @param width width值
 * @param height height值
 * @param durationSeconds 时长Seconds值
 * @param hasAudio hasAudio值
 * @param firstFrameUrl 首个FrameURL值
 * @param requestedLastFrameUrl requestedLastFrameURL值
 * @param lastFrameUrl lastFrameURL值
 * @param returnLastFrame returnLastFrame值
 * @param generateAudio generateAudio值
 * @param actualPrompt actual提示词值
 * @param submitLatencyMs submitLatency毫秒值
 */
public record RemoteVideoGenerationResult(
    byte[] data,
    String mimeType,
    String remoteSourceUrl,
    String provider,
    String providerModel,
    String modelName,
    String endpointHost,
    String taskEndpointHost,
    String taskId,
    int width,
    int height,
    int durationSeconds,
    boolean hasAudio,
    String firstFrameUrl,
    String requestedLastFrameUrl,
    String lastFrameUrl,
    boolean returnLastFrame,
    boolean generateAudio,
    String actualPrompt,
    int submitLatencyMs
) {
}
