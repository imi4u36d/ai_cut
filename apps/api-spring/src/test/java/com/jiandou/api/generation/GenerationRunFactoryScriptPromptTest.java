package com.jiandou.api.generation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiandou.api.media.LocalMediaArtifactService;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class GenerationRunFactoryScriptPromptTest {

    @TempDir
    Path tempDir;

    private static final String SCRIPT_SYSTEM_PROMPT = """
        ### AI 短剧分镜生成指令（严格执行版）
        必须输出 Markdown 表格
        画面提示词（Visual Prompt）+ 表演/动作节拍 + 对白/人声
        10 秒一段
        不能凭空添加原文没有的人声内容
        | 镜号 | 景别/运镜 | 剧情画面与声音描述（长段） | 时长 |
        """;

    @Test
    void createScriptRunUsesRefactoredStoryboardPromptContract() {
        List<String> promptRows = List.of(
            "### AI 短剧分镜生成指令（严格执行版）",
            "必须输出 Markdown 表格",
            "画面提示词（Visual Prompt）+ 表演/动作节拍 + 对白/人声",
            "10 秒一段",
            "不能凭空添加原文没有的人声内容",
            "| 镜号 | 景别/运镜 | 剧情画面与声音描述（长段） | 时长 |"
        );
        String[] capturedSystemPrompt = new String[1];
        String[] capturedUserPrompt = new String[1];
        CompatibleTextModelClient textModelClient = new CompatibleTextModelClient(new ObjectMapper(), java.util.List.of()) {
            @Override
            public TextModelResponse generateText(
                ModelRuntimeProfile profile,
                String systemPrompt,
                String userPrompt,
                double temperature,
                int maxTokens
            ) {
                capturedSystemPrompt[0] = systemPrompt;
                capturedUserPrompt[0] = userPrompt;
                return new TextModelResponse(
                    "# 分镜脚本\n\n| 镜号 | 场景 | 景别角度 | 运镜 | 人物外观 | 动作 | 情绪 | 光线 | 氛围 | 首帧提示词 | 尾帧提示词 | 统一提示词 | 动态与运镜 | 时长 |\n| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n| 1 | 场景 | 中景 | static | 角色 | 抬头 | 警觉 | 夜间侧光 | 压迫 | 首帧 | 尾帧 | 统一画面 | 抬头并停住 | 10 |",
                    "https://api.example.com/v1/responses",
                    "api.example.com",
                    10,
                    true,
                    "resp_script_1"
                );
            }
        };
        GenerationRunFactory factory = createFactory(textModelClient);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", "script");
        request.put("input", Map.of("text", "女主在雨夜街头看见失踪多年的哥哥。"));
        request.put("model", Map.of("textAnalysisModel", "gpt-text"));
        request.put("options", Map.of("visualStyle", "电影写实"));

        factory.createScriptRun("run_script_prompt_1", request);

        for (String row : promptRows) {
            assertTrue(capturedSystemPrompt[0].contains(row));
        }
        assertTrue(capturedUserPrompt[0].contains("请严格遵循 system prompt 的输出格式与规则"));
    }

    @Test
    void createScriptRunThrowsWhenInputMissingText() {
        CompatibleTextModelClient textModelClient = new CompatibleTextModelClient(new ObjectMapper(), java.util.List.of());
        GenerationRunFactory factory = createFactory(textModelClient);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", "script");
        request.put("model", Map.of("textAnalysisModel", "gpt-text"));
        request.put("options", Map.of("visualStyle", "电影写实"));

        assertThrows(IllegalArgumentException.class, () -> factory.createScriptRun("run_script_prompt_2", request));
    }

    @Test
    void createScriptRunThrowsWhenInputTextBlank() {
        CompatibleTextModelClient textModelClient = new CompatibleTextModelClient(new ObjectMapper(), java.util.List.of());
        GenerationRunFactory factory = createFactory(textModelClient);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", "script");
        request.put("input", Map.of("text", ""));
        request.put("model", Map.of("textAnalysisModel", "gpt-text"));
        request.put("options", Map.of("visualStyle", "电影写实"));

        assertThrows(IllegalArgumentException.class, () -> factory.createScriptRun("run_script_prompt_3", request));
    }

    private GenerationRunFactory createFactory(CompatibleTextModelClient textModelClient) {
        ModelRuntimeProfile textProfile = new ModelRuntimeProfile(
            "openai",
            "gpt-text",
            "",
            "k",
            "https://api.example.com/v1",
            60,
            0.2,
            2048,
            "test"
        );
        ModelRuntimePropertiesResolver modelResolver = new ModelRuntimePropertiesResolver(new MockEnvironment()) {
            @Override
            public ModelRuntimeProfile resolveTextProfile(String requestedModel) {
                return textProfile;
            }

            @Override
            public String value(String section, String key, String fallback) {
                return fallback;
            }
        };
        PromptTemplateResolver promptTemplateResolver = new PromptTemplateResolver(
            new MockEnvironment(),
            modelResolver,
            new GenerationConfigPathLocator(new MockEnvironment())
        ) {
            @Override
            public String systemPrompt(String promptName, String key) {
                return SCRIPT_SYSTEM_PROMPT;
            }
        };
        LocalMediaArtifactService localMediaArtifactService = new LocalMediaArtifactService(tempDir.toString(), "ffmpeg");
        GenerationRunSupport support = new GenerationRunSupport(localMediaArtifactService, modelResolver, textModelClient);
        return new GenerationRunFactory(
            modelResolver,
            promptTemplateResolver,
            textModelClient,
            new RemoteMediaGenerationClient(new ObjectMapper()),
            support
        );
    }
}
