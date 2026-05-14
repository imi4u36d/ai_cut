package com.jiandou.api.generation.text;

/**
 * 文本模型接口定义。
 */
public sealed interface TextModelInvocation permits TextCompletionInvocation {

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

}
