package com.jiandou.api.generation;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
            "当首帧描述或尾帧描述中出现角色名时，必须把该角色对应的外观锚点紧跟追加在角色名后，格式为 `角色名（外观锚点）`",
            "| 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |"
        );
        String[] capturedSystemPrompt = new String[1];
        String[] capturedUserPrompt = new String[1];
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
                capturedSystemPrompt[0] = textInvocation.systemPrompt();
                capturedUserPrompt[0] = textInvocation.userPrompt();
                return new TextModelResponse(
                    "# 分镜脚本\n\n| 镜号 | 场景 | 景别角度 | 运镜 | 人物外观 | 动作 | 情绪 | 光线 | 氛围 | 首帧提示词 | 尾帧提示词 | 统一提示词 | 动态与运镜 | 时长 |\n| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n| 1 | 场景 | 中景 | static | 角色 | 抬头 | 警觉 | 夜间侧光 | 压迫 | 首帧 | 尾帧 | 统一画面 | 抬头并停住 | 10 |",
                    "https://api.example.com/v1/responses",
                    "api.example.com",
                    10,
                    true,
                    "resp_script_1"
                );
            }
        });
        GenerationRunFactory factory = createFactory(textModelProviderRegistry);

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
