package com.jiandou.api.generation.runtime;

/**
 * 文本 provider 能力描述。
 * @param supportsSeed 是否支持 seed
 * @param supportsResponsesApi 是否支持 Responses API
 */
public record TextProviderCapabilities(
    boolean supportsSeed,
    boolean supportsResponsesApi
) {
}
