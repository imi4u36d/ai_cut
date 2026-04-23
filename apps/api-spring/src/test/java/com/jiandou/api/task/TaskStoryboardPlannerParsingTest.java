package com.jiandou.api.task;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jiandou.api.task.application.TaskStoryboardPlanner;
import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * 任务分镜规划器Parsing相关测试。
 */
class TaskStoryboardPlannerParsingTest {

    /**
     * 处理parsesStructuredStoryboardTable。
     */
    @Test
    void parsesStructuredStoryboardTable() {
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(new MockEnvironment()));
        TaskRecord task = new TaskRecord();
        task.creativePrompt = "demo";
        task.title = "demo";

        String storyboardMarkdown = """
            | 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |
            | :--- | :--- | :--- | :--- | :--- |
            | 001 | Close-up of a hand pushing a heavy, weathered wooden door, cinematic lighting, dust particles in the air. | Medium shot of Lin Feng standing in the doorway, looking into the dark library, silhouette against the bright outdoor light. | 烟尘在门缝边缘轻微翻涌，镜头先紧贴手部推门动作，随后缓慢后拉并轻微右摇，带出林峰整个人物轮廓和门后的黑暗图书馆空间。木门开启时伴随连续摩擦声与鞋底碾过灰尘的闷响，远处空旷回响持续压低氛围。林峰在镜头后段压低声音说“他们来了”，尾帧固定在他站在门口的逆光剪影上，并明确为下一镜延续同一机位方向与门外逆光关系做衔接。 | 10s |
            | 002 | Close-up of Lin Feng already stepping deeper between the shelves, his shoulder brushing past the dust-covered spines. | Close-up of Lin Feng turning his head toward the right-side bookshelf, pupils tightening as a shadow moves deeper inside. | 镜头从门口剪影位置手持贴近林峰侧脸，他向右迈一步并转头锁定书架深处的异动，背景亮部逐渐收窄，图书馆内部低照度氛围持续加深。全程无人声，仅环境声，衣料摩擦、木地板轻响和书架深处传来的细微碰撞声交替推进。尾帧落在林峰朝右侧书架锁定视线的瞬间，方便下一镜顺接同一视线方向。 | 9s |
            """;

        List<TaskStoryboardPlanner.StoryboardShotPlan> shotPlans = planner.buildStoryboardShotPlans(task, storyboardMarkdown);
        List<int[]> ranges = planner.extractStoryboardShotDurationRanges(storyboardMarkdown);

        assertEquals(2, shotPlans.size());
        assertTrue(shotPlans.get(0).imagePrompt().contains("hand pushing a heavy"));
        assertTrue(shotPlans.get(0).videoPrompt().contains("首帧：Close-up of a hand pushing"));
        assertTrue(shotPlans.get(0).videoPrompt().contains("尾帧：Medium shot of Lin Feng standing in the doorway"));
        assertTrue(shotPlans.get(0).videoPrompt().contains("分镜内容：烟尘在门缝边缘轻微翻涌"));
        assertEquals("Close-up of a hand pushing a heavy, weathered wooden door, cinematic lighting, dust particles in the air.", shotPlans.get(0).startFramePrompt());
        assertEquals("Medium shot of Lin Feng standing in the doorway, looking into the dark library, silhouette against the bright outdoor light.", shotPlans.get(0).endFramePrompt());
        assertEquals(shotPlans.get(0).endFramePrompt(), shotPlans.get(1).startFramePrompt());
        assertTrue(shotPlans.get(1).videoPrompt().contains("首帧：Medium shot of Lin Feng standing in the doorway, looking into the dark library"));
        assertTrue(shotPlans.get(1).videoPrompt().contains("无人声，仅环境声"));
        assertArrayEquals(new int[] {10, 10}, ranges.get(0));
        assertArrayEquals(new int[] {9, 9}, ranges.get(1));
    }

    /**
     * 处理appendsCharacterAppearanceDefinitionsToKeyframePrompts。
     */
    @Test
    void appendsCharacterAppearanceDefinitionsToKeyframePrompts() {
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(new MockEnvironment()));
        TaskRecord task = new TaskRecord();
        task.creativePrompt = "demo";
        task.title = "demo";

        String storyboardMarkdown = """
            【角色定义信息】
            - 林舒：女性，约28岁。外观锚点：鬓角垂落一缕碎发，身着素色针织开衫与深色长裤，随身携带皮质单肩包。行为特征：情绪内敛克制，说话时常回避直视。说话风格：语气平静略带自嘲。
            - 周泽：男性，约29岁。外观锚点：穿着深灰休闲西装外套，内搭素色衬衫，坐姿微前倾。行为特征：主动开口但声音干涩。说话风格：低沉缓慢。

            【分镜脚本】
            | 镜号 | 首帧描述 (Start Frame) | 尾帧描述 (End Frame) | 分镜内容描述 | 时长 |
            | :--- | :--- | :--- | :--- | :--- |
            | 001 | 周泽身体微前倾，双手交握置于桌下，目光落在对面林舒侧脸。 | 林舒视线仍停留在窗外雨痕，周泽将小布袋推至桌面中央。 | 镜头采用过肩视角拍摄周泽开口询问，随后轻微横移跟拍布袋移动轨迹。全程无人声旁白，仅保留呼吸声、雨声与布袋滑动声。 | 10s |
            """;

        List<TaskStoryboardPlanner.StoryboardShotPlan> shotPlans = planner.buildStoryboardShotPlans(task, storyboardMarkdown);

        assertEquals(1, shotPlans.size());
        assertEquals(
            "周泽（穿着深灰休闲西装外套，内搭素色衬衫，坐姿微前倾）身体微前倾，双手交握置于桌下，目光落在对面林舒（鬓角垂落一缕碎发，身着素色针织开衫与深色长裤，随身携带皮质单肩包）侧脸。",
            shotPlans.get(0).startFramePrompt()
        );
        assertEquals(
            "林舒（鬓角垂落一缕碎发，身着素色针织开衫与深色长裤，随身携带皮质单肩包）视线仍停留在窗外雨痕，周泽（穿着深灰休闲西装外套，内搭素色衬衫，坐姿微前倾）将小布袋推至桌面中央。",
            shotPlans.get(0).endFramePrompt()
        );
        assertTrue(shotPlans.get(0).videoPrompt().contains("周泽（穿着深灰休闲西装外套"));
        assertTrue(shotPlans.get(0).videoPrompt().contains("林舒（鬓角垂落一缕碎发"));
    }

    /**
     * 处理extractsCharacterDefinitionsInStoryboardOrder。
     */
    @Test
    void extractsCharacterDefinitionsInStoryboardOrder() {
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(new MockEnvironment()));

        String storyboardMarkdown = """
            【角色定义信息】
            - 林舒：女性，约28岁。外观锚点：鬓角垂落一缕碎发，身着素色针织开衫与深色长裤，随身携带皮质单肩包。行为特征：情绪内敛克制。
            - 周泽：男性，约29岁。外观锚点：穿着深灰休闲西装外套，内搭素色衬衫，坐姿微前倾。行为特征：主动开口但声音干涩。

            【分镜脚本】
            | 镜号 | 首帧描述 | 尾帧描述 | 分镜内容描述 | 时长 |
            | :--- | :--- | :--- | :--- | :--- |
            | 001 | 周泽坐在桌前。 | 林舒看向窗外。 | 镜头轻微横移。 | 10s |
            """;

        List<TaskStoryboardPlanner.CharacterDefinition> definitions = planner.extractCharacterDefinitions(storyboardMarkdown);

        assertEquals(2, definitions.size());
        assertEquals("林舒", definitions.get(0).name());
        assertEquals("鬓角垂落一缕碎发，身着素色针织开衫与深色长裤，随身携带皮质单肩包", definitions.get(0).appearance());
        assertEquals("女性，约28岁。外观锚点：鬓角垂落一缕碎发，身着素色针织开衫与深色长裤，随身携带皮质单肩包。行为特征：情绪内敛克制。", definitions.get(0).definition());
        assertEquals("周泽", definitions.get(1).name());
        assertEquals("穿着深灰休闲西装外套，内搭素色衬衫，坐姿微前倾", definitions.get(1).appearance());
        assertEquals("男性，约29岁。外观锚点：穿着深灰休闲西装外套，内搭素色衬衫，坐姿微前倾。行为特征：主动开口但声音干涩。", definitions.get(1).definition());
    }

    /**
     * 处理rejectsLegacyMergedDescriptionColumn。
     */
    @Test
    void rejectsLegacyMergedDescriptionColumn() {
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(new MockEnvironment()));
        TaskRecord task = new TaskRecord();
        task.creativePrompt = "demo";
        task.title = "demo";

        String storyboardMarkdown = """
            | 镜号 | 合并长段描述 | 时长 |
            | :--- | :--- | :--- |
            | 001 | 破旧仓库的顶灯忽明忽暗，女孩靠着木箱缓慢起身，目光在门缝和楼梯口之间反复游移；她没有说话，背景持续有远处警笛和屋顶滴水声。 | 10秒 |
            """;

        IllegalStateException parseException = assertThrows(
            IllegalStateException.class,
            () -> planner.buildStoryboardShotPlans(task, storyboardMarkdown)
        );

        assertTrue(parseException.getMessage().contains("缺少必填列"));
        assertTrue(parseException.getMessage().contains("首帧描述"));
        assertTrue(parseException.getMessage().contains("尾帧描述"));
        assertTrue(parseException.getMessage().contains("分镜内容描述"));
    }

    /**
     * 处理throwsWhenStructuredStoryboardRequiredColumnMissing。
     */
    @Test
    void throwsWhenStructuredStoryboardRequiredColumnMissing() {
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(new MockEnvironment()));
        TaskRecord task = new TaskRecord();
        task.creativePrompt = "demo";
        task.title = "demo";

        String storyboardMarkdown = """
            | 镜号 | 首帧描述 | 尾帧描述 | 时长 |
            | :--- | :--- | :--- | :--- |
            | 001 | 女主站在街口，霓虹反光。 | 她回头望向巷口尽头，神色骤然紧绷。 | 10s |
            """;

        IllegalStateException parseException = assertThrows(
            IllegalStateException.class,
            () -> planner.buildStoryboardShotPlans(task, storyboardMarkdown)
        );
        assertTrue(parseException.getMessage().contains("缺少必填列"));
        assertTrue(parseException.getMessage().contains("分镜内容描述"));
        assertThrows(IllegalStateException.class, () -> planner.buildStoryboardVideoPrompts(storyboardMarkdown));
    }

    /**
     * 处理throwsWhenStructuredStoryboardRequiredValueMissing。
     */
    @Test
    void throwsWhenStructuredStoryboardRequiredValueMissing() {
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(new MockEnvironment()));
        TaskRecord task = new TaskRecord();
        task.creativePrompt = "demo";
        task.title = "demo";

        String storyboardMarkdown = """
            | 镜号 | 首帧描述 | 尾帧描述 | 分镜内容描述 | 时长 |
            | :--- | :--- | :--- | :--- | :--- |
            | 001 | 女主站在街口，霓虹反光。 | 她回头望向巷口尽头，神色骤然紧绷。 |  | 10s |
            """;

        IllegalStateException parseException = assertThrows(
            IllegalStateException.class,
            () -> planner.buildStoryboardShotPlans(task, storyboardMarkdown)
        );
        assertTrue(parseException.getMessage().contains("镜头 001"));
        assertTrue(parseException.getMessage().contains("分镜内容描述"));
    }

    /**
     * 规范化时长规划KeepsSupportedTenSecondClips。
     */
    @Test
    void normalizeDurationPlanKeepsSupportedTenSecondClips() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("JIANDOU_CONFIG_CACHE_TTL_SECONDS", "0")
            .withProperty("spring.config.additional-location", "file:src/test/resources/task-duration-config/");
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(environment));
        TaskRecord task = new TaskRecord();
        task.creativePrompt = "demo";
        task.title = "demo";
        task.minDurationSeconds = 10;
        task.maxDurationSeconds = 10;

        String storyboardMarkdown = """
            | 镜号 | 首帧描述 | 尾帧描述 | 分镜内容描述 | 时长 |
            | :--- | :--- | :--- | :--- | :--- |
            | 001 | 夜色高架桥下，男主靠在栏杆上低头查看手机定位，冷色车灯从背后掠过。 | 男主抬头望向高架桥出口，手机屏幕冷光映在脸侧，神情更警觉。 | 镜头保持中景稳定构图，男主先低头确认定位，再缓慢抬头转向出口方向。全程无人声，仅环境声，高架桥车流呼啸、风声掠过栏杆、手机震动声短促出现。本镜头尾帧锁定男主望向出口的方向，下一镜头首帧应承接这一视线和桥下冷色灯光。 | 10s |
            """;

        List<TaskStoryboardPlanner.StoryboardShotPlan> shotPlans = planner.buildStoryboardShotPlans(task, storyboardMarkdown);
        List<int[]> normalized = planner.normalizeClipDurationPlan(
            "seedance-1.5-pro",
            planner.buildClipDurationPlan(task, 10, shotPlans.size(), storyboardMarkdown)
        );

        assertEquals(1, normalized.size());
        assertArrayEquals(new int[] {10, 10, 10}, normalized.get(0));
    }

    /**
     * 处理clampsStructuredStoryboardDurationsToSupportedRange。
     */
    @Test
    void clampsStructuredStoryboardDurationsToSupportedRange() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("JIANDOU_CONFIG_CACHE_TTL_SECONDS", "0")
            .withProperty("spring.config.additional-location", "file:src/test/resources/task-duration-config/");
        TaskStoryboardPlanner planner = new TaskStoryboardPlanner(new ModelRuntimePropertiesResolver(environment));
        TaskRecord task = new TaskRecord();
        task.creativePrompt = "demo";
        task.title = "demo";
        task.minDurationSeconds = 1;
        task.maxDurationSeconds = 20;

        String storyboardMarkdown = """
            | 镜号 | 首帧描述 | 尾帧描述 | 分镜内容描述 | 时长 |
            | :--- | :--- | :--- | :--- | :--- |
            | 001 | 雨夜门廊下，角色扶着栏杆观察远处车灯，雨水沿着外套袖口滴落。 | 角色微微前倾，手仍扶栏杆，视线锁定前方车灯方向，呼吸变得更急促。 | 镜头保持中景稳定构图，角色先停顿观察，再缓慢收紧肩背向前探身。全程无人声，仅环境声，雨滴敲打铁棚、远处车轮碾水声和角色急促呼吸持续叠加。当前尾帧固定角色前倾并望向前方，下一镜首帧延续这一视线与雨夜门廊空间关系。 | 4s |
            | 002 | 角色前倾望向巷口，脸侧被车灯扫过，眼神骤然收紧。 | 近景定格在他压低身体、目光锁死巷口深处的瞬间，背景反光更冷。 | 镜头从脸侧近景缓慢推近，角色向前一步压低身体，眼神持续锁定巷口动静。全程无人声，仅环境声，水滴从檐角坠落、远处巷口回声渐强、鞋底挤压积水。当前尾帧落在角色压低身体的姿态，下一镜首帧应承接该低位姿态和冷色反光。 | 12s |
            """;

        List<int[]> ranges = planner.extractStoryboardShotDurationRanges(storyboardMarkdown);
        List<TaskStoryboardPlanner.StoryboardShotPlan> shotPlans = planner.buildStoryboardShotPlans(task, storyboardMarkdown);
        List<int[]> normalized = planner.normalizeClipDurationPlan(
            "seedance-1.5-pro",
            planner.buildClipDurationPlan(task, 10, shotPlans.size(), storyboardMarkdown)
        );

        assertArrayEquals(new int[] {5, 5}, ranges.get(0));
        assertArrayEquals(new int[] {12, 12}, ranges.get(1));
        assertArrayEquals(new int[] {6, 6, 6}, normalized.get(0));
        assertArrayEquals(new int[] {10, 10, 10}, normalized.get(1));
    }
}
