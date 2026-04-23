package com.jiandou.api.generation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jiandou.api.config.JiandouStorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiandou.api.generation.image.ImageModelProviderRegistry;
import com.jiandou.api.generation.orchestration.GenerationRunFactory;
import com.jiandou.api.generation.orchestration.GenerationRunSupport;
import com.jiandou.api.generation.orchestration.LocalGenerationRunStore;
import com.jiandou.api.generation.runtime.GenerationConfigPathLocator;
import com.jiandou.api.generation.runtime.ModelRuntimeProfile;
import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import com.jiandou.api.generation.runtime.PromptTemplateResolver;
import com.jiandou.api.generation.text.TextCompletionInvocation;
import com.jiandou.api.generation.text.TextModelInvocation;
import com.jiandou.api.generation.text.TextModelProvider;
import com.jiandou.api.generation.text.TextModelProviderRegistry;
import com.jiandou.api.generation.video.DashscopeVideoModelProvider;
import com.jiandou.api.generation.video.SeedanceVideoModelProvider;
import com.jiandou.api.generation.video.VideoModelProviderRegistry;
import com.jiandou.api.generation.video.VideoProviderTransport;
import com.jiandou.api.media.LocalMediaArtifactService;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/**
 * 生成运行工厂脚本提示词相关测试。
 */
class GenerationRunFactoryScriptPromptTest {

    @TempDir
    Path tempDir;

    private static final String SCRIPT_SYSTEM_PROMPT = """
        ### AI 短剧分镜生成指令（严格执行版）
        必须输出 Markdown 表格
        分镜内容描述必须尽可能详细
        运镜只能使用极简基础表达
        同一镜头的首帧与尾帧必须被视为同一 seed、同一场景坐标系下的连续画面
        每个镜头时长必须限制在 `5-12s`
        不能凭空添加原文没有的人声内容
        首尾帧描述都必须保留上下文情境信息
        首尾帧描述都必须详细交代场景和布置
        首尾帧描述必须明确写出相同的场景坐标锚点与物件关系
        若该镜头首尾帧发生在同一场景内，尾帧必须沿用与首帧相同的场景描述骨架、空间锚点和布置信息
        同一场景内的首尾帧区别只能主要体现在人物动作状态、视线、手部位置、道具被拿起/放下、局势推进结果上
        若首帧已经写出明确场景锚点或关键物件
        写尾帧时，先检查首帧中出现过的场景锚点、方位词、关键物件名词
        首尾帧描述建议严格按照这个句式组织
        场景布置只能写与当前镜头动作、人物调度、关键道具、空间关系直接相关的内容
        当首帧描述或尾帧描述中出现角色名时，必须把该角色对应的外观锚点紧跟追加在角色名后，格式为 `角色名（外观锚点）`
        | 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |
        """;

    /**
     * 创建脚本运行UsesRefactored分镜提示词Contract。
     */
    @Test
    void createScriptRunUsesRefactoredStoryboardPromptContract() {
        List<String> promptRows = List.of(
            "### AI 短剧分镜生成指令（严格执行版）",
            "必须输出 Markdown 表格",
            "分镜内容描述必须尽可能详细",
            "运镜只能使用极简基础表达",
            "同一镜头的首帧与尾帧必须被视为同一 seed、同一场景坐标系下的连续画面",
            "每个镜头时长必须限制在 `5-12s`",
            "不能凭空添加原文没有的人声内容",
            "首尾帧描述都必须保留上下文情境信息",
            "首尾帧描述都必须详细交代场景和布置",
            "首尾帧描述必须明确写出相同的场景坐标锚点与物件关系",
            "若该镜头首尾帧发生在同一场景内，尾帧必须沿用与首帧相同的场景描述骨架、空间锚点和布置信息",
            "同一场景内的首尾帧区别只能主要体现在人物动作状态、视线、手部位置、道具被拿起/放下、局势推进结果上",
            "若首帧已经写出明确场景锚点或关键物件",
            "写尾帧时，先检查首帧中出现过的场景锚点、方位词、关键物件名词",
            "首尾帧描述建议严格按照这个句式组织",
            "场景布置只能写与当前镜头动作、人物调度、关键道具、空间关系直接相关的内容",
            "当首帧描述或尾帧描述中出现角色名时，必须把该角色对应的外观锚点紧跟追加在角色名后，格式为 `角色名（外观锚点）`",
            "| 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |"
        );
        String[] capturedSystemPrompt = new String[1];
        String[] capturedUserPrompt = new String[1];
        String[] capturedReviewSystemPrompt = new String[1];
        String[] capturedReviewUserPrompt = new String[1];
        int[] invocationCount = new int[1];
        TextModelProviderRegistry textModelProviderRegistry = registry(new TextModelProvider() {
            /**
             * 检查是否supports。
             * @param profile profile值
             * @return 是否满足条件
             */
            @Override
            public boolean supports(ModelRuntimeProfile profile) {
                return true;
            }

            /**
             * 处理generate。
             * @param profile profile值
             * @param invocation 调用值
             * @return 处理结果
             */
            @Override
            public TextModelResponse generate(
                ModelRuntimeProfile profile,
                TextModelInvocation invocation
            ) {
                TextCompletionInvocation textInvocation = (TextCompletionInvocation) invocation;
                invocationCount[0]++;
                if (invocationCount[0] == 1) {
                    capturedSystemPrompt[0] = textInvocation.systemPrompt();
                    capturedUserPrompt[0] = textInvocation.userPrompt();
                    return new TextModelResponse(
                        """
                            【角色定义信息】
                            - 林舒：外观锚点未明确。

                            【分镜脚本】
                            | 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |
                            | :--- | :--- | :--- | :--- | :--- |
                            | 001 | 林舒站在桌边。 | 林舒转身。 | 林舒动作推进。 | 8s |
                            """,
                        "https://api.example.com/v1/responses",
                        "api.example.com",
                        10,
                        true,
                        "resp_script_draft"
                    );
                }
                capturedReviewSystemPrompt[0] = textInvocation.systemPrompt();
                capturedReviewUserPrompt[0] = textInvocation.userPrompt();
                return new TextModelResponse(
                    """
                        【角色定义信息】
                        - 林舒：外观锚点未明确。

                        【分镜脚本】
                        | 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |
                        | :--- | :--- | :--- | :--- | :--- |
                        | 001 | 林舒站在桌子左侧，桌上放着文件和杯子，左侧是窗户，右侧是书架，她正低头看向文件。 | 林舒仍站在同一张桌子左侧，桌上仍放着文件和杯子，左侧是窗户，右侧是书架，她抬头转向门口。 | 林舒先低头翻看桌上的文件，听到门外动静后停手，抬头转向门口。 | 8s |
                        """,
                    "https://api.example.com/v1/responses",
                    "api.example.com",
                    10,
                    true,
                    "resp_script_review"
                );
            }
        });
        GenerationRunFactory factory = createFactory(textModelProviderRegistry);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", "script");
        request.put("input", Map.of("text", "女主在雨夜街头看见失踪多年的哥哥。"));
        request.put("model", Map.of("textAnalysisModel", "gpt-text"));
        request.put("options", Map.of("visualStyle", "电影写实"));

        Map<String, Object> run = factory.createScriptRun("run_script_prompt_1", request);

        for (String row : promptRows) {
            assertTrue(capturedSystemPrompt[0].contains(row));
        }
        assertEquals(2, invocationCount[0]);
        assertTrue(capturedUserPrompt[0].contains("请严格遵循 system prompt 的输出格式与规则"));
        assertTrue(capturedReviewSystemPrompt[0].contains("二次审校要求"));
        assertTrue(capturedReviewUserPrompt[0].contains("当前分镜初稿"));
        assertTrue(capturedReviewUserPrompt[0].contains("林舒站在桌边。"));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) run.get("result");
        assertTrue(String.valueOf(result.get("scriptMarkdown")).contains("同一张桌子左侧"));
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
        assertTrue(String.valueOf(metadata.get("draftScriptMarkdown")).contains("林舒站在桌边。"));
        assertEquals(true, metadata.get("reviewApplied"));
    }

    /**
     * 创建脚本运行FallsBack到初稿When审校结果无效。
     */
    @Test
    void createScriptRunFallsBackToDraftWhenReviewOutputInvalid() {
        int[] invocationCount = new int[1];
        TextModelProviderRegistry textModelProviderRegistry = registry(new TextModelProvider() {
            @Override
            public boolean supports(ModelRuntimeProfile profile) {
                return true;
            }

            @Override
            public TextModelResponse generate(
                ModelRuntimeProfile profile,
                TextModelInvocation invocation
            ) {
                invocationCount[0]++;
                if (invocationCount[0] == 1) {
                    return new TextModelResponse(
                        """
                            【角色定义信息】
                            - 林舒：外观锚点未明确。

                            【分镜脚本】
                            | 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |
                            | :--- | :--- | :--- | :--- | :--- |
                            | 001 | 林舒站在桌子左侧，桌上放着文件和杯子，左侧是窗户，右侧是书架，她正低头看向文件。 | 林舒仍站在同一张桌子左侧，桌上仍放着文件和杯子，左侧是窗户，右侧是书架，她抬头转向门口。 | 林舒先低头翻看桌上的文件，听到门外动静后停手，抬头转向门口。 | 8s |
                            """,
                        "https://api.example.com/v1/responses",
                        "api.example.com",
                        10,
                        true,
                        "resp_script_draft"
                    );
                }
                return new TextModelResponse(
                    "审校后建议：请加强首尾帧一致性。",
                    "https://api.example.com/v1/responses",
                    "api.example.com",
                    12,
                    true,
                    "resp_script_review_invalid"
                );
            }
        });
        GenerationRunFactory factory = createFactory(textModelProviderRegistry);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", "script");
        request.put("input", Map.of("text", "女主在雨夜街头看见失踪多年的哥哥。"));
        request.put("model", Map.of("textAnalysisModel", "gpt-text"));
        request.put("options", Map.of("visualStyle", "电影写实"));

        Map<String, Object> run = factory.createScriptRun("run_script_prompt_4", request);

        assertEquals(2, invocationCount[0]);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) run.get("result");
        assertTrue(String.valueOf(result.get("scriptMarkdown")).contains("同一张桌子左侧"));
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
        assertEquals(false, metadata.get("reviewApplied"));
        assertEquals("resp_script_draft", metadata.get("finalResponseId"));
        assertEquals("resp_script_review_invalid", metadata.get("reviewResponseId"));
        assertEquals("review output missing character definitions", metadata.get("reviewFallbackReason"));
    }

    /**
     * 创建脚本运行ThrowsWhen输入Missing文本。
     */
    @Test
    void createScriptRunThrowsWhenInputMissingText() {
        GenerationRunFactory factory = createFactory(registry());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", "script");
        request.put("model", Map.of("textAnalysisModel", "gpt-text"));
        request.put("options", Map.of("visualStyle", "电影写实"));

        assertThrows(IllegalArgumentException.class, () -> factory.createScriptRun("run_script_prompt_2", request));
    }

    /**
     * 创建脚本运行ThrowsWhen输入文本空白。
     */
    @Test
    void createScriptRunThrowsWhenInputTextBlank() {
        GenerationRunFactory factory = createFactory(registry());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", "script");
        request.put("input", Map.of("text", ""));
        request.put("model", Map.of("textAnalysisModel", "gpt-text"));
        request.put("options", Map.of("visualStyle", "电影写实"));

        assertThrows(IllegalArgumentException.class, () -> factory.createScriptRun("run_script_prompt_3", request));
    }

    /**
     * 创建工厂。
     * @param textModelProviderRegistry 文本模型 provider 注册表值
     * @return 处理结果
     */
    private GenerationRunFactory createFactory(TextModelProviderRegistry textModelProviderRegistry) {
        ModelRuntimeProfile textProfile = new ModelRuntimeProfile(
            "openai",
            "gpt-text",
            "k",
            "https://api.example.com/v1",
            60,
            0.2,
            2048,
            "test"
        );
        ModelRuntimePropertiesResolver modelResolver = new ModelRuntimePropertiesResolver(new MockEnvironment()) {
            /**
             * 处理解析文本Profile。
             * @param requestedModel requested模型值
             * @return 处理结果
             */
            @Override
            public ModelRuntimeProfile resolveTextProfile(String requestedModel) {
                return textProfile;
            }

            /**
             * 处理值。
             * @param section section值
             * @param key key值
             * @param fallback 兜底值
             * @return 处理结果
             */
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
            /**
             * 处理系统提示词。
             * @param promptName 提示词Name值
             * @param key key值
             * @return 处理结果
             */
            @Override
            public String systemPrompt(String promptName, String key) {
                return SCRIPT_SYSTEM_PROMPT;
            }
        };
        LocalMediaArtifactService localMediaArtifactService = new LocalMediaArtifactService(storageProperties(tempDir), "ffmpeg");
        GenerationRunSupport support = new GenerationRunSupport(localMediaArtifactService, textModelProviderRegistry);
        ImageModelProviderRegistry imageModelProviderRegistry = new ImageModelProviderRegistry(java.util.List.of());
        VideoProviderTransport videoProviderTransport = new VideoProviderTransport(new ObjectMapper());
        VideoModelProviderRegistry videoModelProviderRegistry = new VideoModelProviderRegistry(java.util.List.of(
            new SeedanceVideoModelProvider(videoProviderTransport),
            new DashscopeVideoModelProvider(videoProviderTransport)
        ));
        return new GenerationRunFactory(
            modelResolver,
            promptTemplateResolver,
            textModelProviderRegistry,
            imageModelProviderRegistry,
            videoModelProviderRegistry,
            support
        );
    }

    private TextModelProviderRegistry registry(TextModelProvider... providers) {
        return new TextModelProviderRegistry(java.util.Arrays.asList(providers));
    }

    private JiandouStorageProperties storageProperties(Path rootDir) {
        JiandouStorageProperties properties = new JiandouStorageProperties();
        properties.setRootDir(rootDir.toString());
        return properties;
    }
}
