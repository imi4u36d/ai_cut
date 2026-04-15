package com.jiandou.api.generation.text;

import com.jiandou.api.generation.ModelRuntimeProfile;

/**
 * 文本模型传输Policy。
 */
public final class TextModelTransportPolicy {

    /**
     * 创建新的文本模型传输Policy。
     */
    private TextModelTransportPolicy() {}

    /**
     * 检查是否supportsResponsesAPI。
     * @param profile profile值
     * @return 是否满足条件
     */
    public static boolean supportsResponsesApi(ModelRuntimeProfile profile) {
        String provider = normalized(profile.provider());
        String baseUrl = normalized(profile.baseUrl());
        return "openai".equals(provider)
            || "qwen".equals(provider)
            || provider.contains("ark")
            || provider.contains("volc")
            || baseUrl.contains("openai.com")
            || baseUrl.contains("dashscope.aliyuncs.com")
            || baseUrl.contains("volces.com/api/v3");
    }

    /**
     * 检查是否prefersChatCompletionsFor视觉。
     * @param profile profile值
     * @return 是否满足条件
     */
    public static boolean prefersChatCompletionsForVision(ModelRuntimeProfile profile) {
        String provider = normalized(profile.provider());
        String modelName = normalized(profile.modelName());
        String baseUrl = normalized(profile.baseUrl());
        return "qwen".equals(provider)
            || modelName.contains("-vl-")
            || baseUrl.contains("dashscope.aliyuncs.com");
    }

    /**
     * 处理解析Endpoint。
     * @param baseUrl 基础 URL
     * @param responsesApi responsesAPI值
     * @return 处理结果
     */
    public static String resolveEndpoint(String baseUrl, boolean responsesApi) {
        String normalized = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        if (responsesApi) {
            return normalized.endsWith("/responses") ? normalized : normalized + "/responses";
        }
        return normalized.endsWith("/chat/completions") ? normalized : normalized + "/chat/completions";
    }

    /**
     * 处理normalized。
     * @param value 待处理的值
     * @return 处理结果
     */
    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
