package com.jiandou.api.generation.text;

import java.util.Map;

/**
 * Prepared文本模型请求体。
 * @param endpoint endpoint值
 * @param body body值
 * @param responsesApi responsesAPI值
 */
public record PreparedTextModelRequest(
    String endpoint,
    Map<String, Object> body,
    boolean responsesApi
) {}
