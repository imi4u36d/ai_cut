package com.jiandou.api.generation;

/**
 * 文本模型响应体。
 * @param text 文本值
 * @param endpoint endpoint值
 * @param endpointHost endpointHost值
 * @param latencyMs latency毫秒值
 * @param responsesApi responsesAPI值
 * @param responseId 响应标识值
 */
public record TextModelResponse(
    String text,
    String endpoint,
    String endpointHost,
    int latencyMs,
    boolean responsesApi,
    String responseId
) {
}
