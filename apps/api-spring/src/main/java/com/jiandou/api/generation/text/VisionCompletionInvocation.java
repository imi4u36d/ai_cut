package com.jiandou.api.generation.text;

import java.util.List;

/**
 * 视觉Completion记录结构。
 * @param systemPrompt 系统提示词值
 * @param userPrompt user提示词值
 * @param temperature temperature值
 * @param maxTokens 最大Tokens值
 * @param imageUrls 图像Urls值
 * @param seed 种子值
 */
public record VisionCompletionInvocation(
    String systemPrompt,
    String userPrompt,
    double temperature,
    int maxTokens,
    List<String> imageUrls,
    Integer seed
) implements TextModelInvocation {

    /**
     * 检查是否视觉。
     * @return 是否满足条件
     */
    @Override
    public boolean vision() {
        return true;
    }
}
