package com.jiandou.api.generation.text;

/**
 * 文本Completion记录结构。
 * @param systemPrompt 系统提示词值
 * @param userPrompt user提示词值
 * @param temperature temperature值
 * @param maxTokens 最大Tokens值
 */
public record TextCompletionInvocation(
    String systemPrompt,
    String userPrompt,
    double temperature,
    int maxTokens
) implements TextModelInvocation {}
