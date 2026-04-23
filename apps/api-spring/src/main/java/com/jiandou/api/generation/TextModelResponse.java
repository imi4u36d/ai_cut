package com.jiandou.api.generation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文本模型响应体。
 * @param text 文本值
 * @param endpoint endpoint值
 * @param endpointHost endpointHost值
 * @param latencyMs latency毫秒值
 * @param responsesApi responsesAPI值
 * @param responseId 响应标识值
 * @param providerRequest 实际Provider请求体
 * @param providerResponse 实际Provider响应体
 * @param httpStatus HTTP状态码
 */
public record TextModelResponse(
    String text,
    String endpoint,
    String endpointHost,
    int latencyMs,
    boolean responsesApi,
    String responseId,
    Map<String, Object> providerRequest,
    Map<String, Object> providerResponse,
    int httpStatus
) {
    public TextModelResponse {
        providerRequest = providerRequest == null ? Map.of() : new LinkedHashMap<>(providerRequest);
        providerResponse = providerResponse == null ? Map.of() : new LinkedHashMap<>(providerResponse);
    }

    public TextModelResponse(
        String text,
        String endpoint,
        String endpointHost,
        int latencyMs,
        boolean responsesApi,
        String responseId
    ) {
        this(text, endpoint, endpointHost, latencyMs, responsesApi, responseId, Map.of(), Map.of(), 0);
    }
}
