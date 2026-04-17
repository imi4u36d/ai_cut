package com.jiandou.api.generation.text;

import java.util.List;

/**
 * 文本模型接口定义。
 */
public sealed interface TextModelInvocation permits TextCompletionInvocation, VisionCompletionInvocation {

    /**
     * 处理系统提示词。
     * @return 处理结果
     */
    String systemPrompt();

    /**
     * 处理user提示词。
     * @return 处理结果
     */
    String userPrompt();

    /**
     * 处理temperature。
     * @return 处理结果
     */
    double temperature();

    /**
     * 处理最大Tokens。
     * @return 处理结果
     */
    int maxTokens();

    /**
     * 检查是否视觉。
     * @return 是否满足条件
     */
    default boolean vision() {
        return false;
    }

    /**
     * 处理图像Urls。
     * @return 处理结果
     */
    default List<String> imageUrls() {
        return List.of();
    }

    /**
     * 处理种子。
     * @return 处理结果
     */
    default Integer seed() {
        return null;
    }
}
