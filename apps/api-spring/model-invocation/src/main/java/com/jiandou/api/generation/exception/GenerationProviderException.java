package com.jiandou.api.generation.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生成Provider异常。
 */
public final class GenerationProviderException extends RuntimeException {

    private final Map<String, Object> providerRequest;
    private final Object providerResponse;
    private final int httpStatus;

    /**
     * 创建新的生成Provider异常。
     * @param message 消息文本
     */
    public GenerationProviderException(String message) {
        this(message, Map.of(), null, 0);
    }

    public GenerationProviderException(String message, Map<String, Object> providerRequest, Object providerResponse, int httpStatus) {
        super(message);
        this.providerRequest = providerRequest == null ? Map.of() : new LinkedHashMap<>(providerRequest);
        this.providerResponse = providerResponse;
        this.httpStatus = Math.max(0, httpStatus);
    }

    public Map<String, Object> providerRequest() {
        return providerRequest;
    }

    public Object providerResponse() {
        return providerResponse;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
