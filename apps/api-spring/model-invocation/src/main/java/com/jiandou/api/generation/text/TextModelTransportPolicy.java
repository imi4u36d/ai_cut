package com.jiandou.api.generation.text;

import com.jiandou.api.generation.runtime.ModelRuntimeProfile;
import java.util.Locale;

/**
 * 文本模型传输Policy。
 */
public final class TextModelTransportPolicy {

    private static final String DASHSCOPE_HOST = "dashscope.aliyuncs.com";
    private static final String DASHSCOPE_INTL_HOST = "dashscope-intl.aliyuncs.com";
    private static final String DASHSCOPE_CHAT_BASE = "/compatible-mode/v1";
    private static final String DASHSCOPE_RESPONSES_BASE = "/api/v2/apps/protocols/compatible-mode/v1";

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
        return profile != null && profile.supportsResponsesApi();
    }

    /**
     * 处理解析Endpoint。
     * @param baseUrl 基础 URL
     * @param responsesApi responsesAPI值
     * @return 处理结果
     */
    public static String resolveEndpoint(String baseUrl, boolean responsesApi) {
        String normalized = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        String dashscopeEndpoint = resolveDashscopeEndpoint(normalized, responsesApi);
        if (!dashscopeEndpoint.isBlank()) {
            return dashscopeEndpoint;
        }
        if (responsesApi) {
            return normalized.endsWith("/responses") ? normalized : normalized + "/responses";
        }
        return normalized.endsWith("/chat/completions") ? normalized : normalized + "/chat/completions";
    }

    private static String resolveDashscopeEndpoint(String normalizedBaseUrl, boolean responsesApi) {
        String lower = normalizedBaseUrl.toLowerCase(Locale.ROOT);
        if (!lower.contains(DASHSCOPE_HOST) && !lower.contains(DASHSCOPE_INTL_HOST)) {
            return "";
        }
        String canonicalBase = normalizedBaseUrl
            .replace(DASHSCOPE_RESPONSES_BASE, DASHSCOPE_CHAT_BASE)
            .replace("/responses", "")
            .replace("/chat/completions", "");
        if (responsesApi) {
            return canonicalBase + "/responses";
        }
        return canonicalBase + "/chat/completions";
    }

}
