package com.jiandou.api.generation;

/**
 * 远程视频任务Submission记录结构。
 * @param provider provider值
 * @param requestedModel requested模型值
 * @param providerModel provider模型值
 * @param endpointHost endpointHost值
 * @param taskEndpointHost 任务EndpointHost值
 * @param taskId 任务标识
 * @param firstFrameUrl 首个FrameURL值
 * @param requestedLastFrameUrl requestedLastFrameURL值
 * @param returnLastFrame returnLastFrame值
 * @param generateAudio generateAudio值
 * @param actualPrompt actual提示词值
 * @param submitLatencyMs submitLatency毫秒值
 */
public record RemoteVideoTaskSubmission(
    String provider,
    String requestedModel,
    String providerModel,
    String endpointHost,
    String taskEndpointHost,
    String taskId,
    String firstFrameUrl,
    String requestedLastFrameUrl,
    boolean returnLastFrame,
    boolean generateAudio,
    String actualPrompt,
    int submitLatencyMs
) {
}
