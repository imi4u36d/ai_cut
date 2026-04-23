package com.jiandou.api.task.application;

import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import com.jiandou.api.task.TaskRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 封装分镜脚本解析、镜头提示词生成和时长规划逻辑，降低 worker 状态机体积。
 */
@Service
public class TaskStoryboardPlanner {
    private static final int CLIP_MIN_SECONDS = 5;
    private static final int CLIP_MAX_SECONDS = 12;

    private static final Pattern SCRIPT_DURATION_RANGE_PATTERN = Pattern.compile(
        "(?<left>\\d{1,3}(?:\\.\\d+)?)\\s*(?:-|~|～|—|到)\\s*(?<right>\\d{1,3}(?:\\.\\d+)?)\\s*(?:s|sec|secs|second|seconds|秒)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SCRIPT_DURATION_VALUE_PATTERN = Pattern.compile(
        "(?<![\\d.])(?<value>\\d{1,3}(?:\\.\\d+)?)\\s*(?:s|sec|secs|second|seconds|秒)(?![a-zA-Z])",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PLAIN_DURATION_RANGE_PATTERN = Pattern.compile(
        "^\\s*(?<left>\\d{1,3}(?:\\.\\d+)?)\\s*(?:-|~|～|—|到)\\s*(?<right>\\d{1,3}(?:\\.\\d+)?)\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PLAIN_DURATION_VALUE_PATTERN = Pattern.compile(
        "^\\s*(?<value>\\d{1,3}(?:\\.\\d+)?)\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CHARACTER_DEFINITION_LINE_PATTERN = Pattern.compile(
        "^\\s*[-*]\\s*(?<name>[^：:]+?)\\s*[：:]\\s*(?<details>.+)$"
    );
    private static final Pattern CHARACTER_APPEARANCE_PATTERN = Pattern.compile(
        "(?:外观锚点|外形定义|外观定义|外貌特征|人物外观)\\s*[：:](?<appearance>.+?)(?=(?:行为特征|说话风格|性格特征|角色关系|$))"
    );

    private final ModelRuntimePropertiesResolver modelResolver;

    /**
     * 创建新的任务分镜规划器。
     * @param modelResolver 模型解析器值
     */
    public TaskStoryboardPlanner(ModelRuntimePropertiesResolver modelResolver) {
        this.modelResolver = modelResolver;
    }

    /**
     * 构建Sequential片段Prompts。
     * @param task 要处理的任务对象
     * @param storyboardMarkdown 分镜Markdown值
     * @return 处理结果
     */
    public List<String> buildSequentialClipPrompts(TaskRecord task, String storyboardMarkdown) {
        return buildStoryboardShotPlans(task, storyboardMarkdown).stream().map(StoryboardShotPlan::videoPrompt).toList();
    }

    /**
     * 构建分镜ShotPlans。
     * @param task 要处理的任务对象
     * @param storyboardMarkdown 分镜Markdown值
     * @return 处理结果
     */
    public List<StoryboardShotPlan> buildStoryboardShotPlans(TaskRecord task, String storyboardMarkdown) {
        return extractStoryboardShotPlans(storyboardMarkdown);
    }

    /**
     * 构建分镜视频Prompts。
     * @param storyboardMarkdown 分镜Markdown值
     * @return 处理结果
     */
    public List<String> buildStoryboardVideoPrompts(String storyboardMarkdown) {
        return extractStoryboardShotPlans(storyboardMarkdown).stream().map(StoryboardShotPlan::videoPrompt).toList();
    }

    /**
     * 提取角色定义。
     * @param storyboardMarkdown 分镜Markdown值
     * @return 角色定义列表
     */
    public List<CharacterDefinition> extractCharacterDefinitions(String storyboardMarkdown) {
        String normalized = stringValue(storyboardMarkdown);
        if (normalized.isBlank()) {
            return List.of();
        }
        int definitionsStart = normalized.indexOf("【角色定义信息】");
        if (definitionsStart < 0) {
            return List.of();
        }
        int scriptStart = normalized.indexOf("【分镜脚本】");
        String definitionsBlock = scriptStart > definitionsStart
            ? normalized.substring(definitionsStart, scriptStart)
            : normalized.substring(definitionsStart);
        List<CharacterDefinition> definitions = new ArrayList<>();
        for (String rawLine : definitionsBlock.split("\\R")) {
            Matcher matcher = CHARACTER_DEFINITION_LINE_PATTERN.matcher(rawLine);
            if (!matcher.matches()) {
                continue;
            }
            String name = normalizeStoryboardPromptValue(matcher.group("name"));
            String details = normalizeStoryboardPromptValue(matcher.group("details"));
            String appearance = extractAppearanceDefinition(details);
            if (!name.isBlank() && !appearance.isBlank() && !details.isBlank()) {
                definitions.add(new CharacterDefinition(name, appearance, details));
            }
        }
        return definitions;
    }

    /**
     * 处理解析Requested输出数量。
     * @param task 要处理的任务对象
     * @param storyboardClipCount 分镜片段数量值
     * @return 处理结果
     */
    public int resolveRequestedOutputCount(TaskRecord task, int storyboardClipCount) {
        int availableClipCount = Math.max(1, storyboardClipCount);
        if (task.requestSnapshot() == null || task.requestSnapshot().outputCount().auto()) {
            return availableClipCount;
        }
        Integer requested = task.requestSnapshot().outputCount().count();
        if (requested == null) {
            return availableClipCount;
        }
        return Math.max(1, Math.min(requested, availableClipCount));
    }

    /**
     * 处理请求快照输出数量。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    public Object requestSnapshotOutputCount(TaskRecord task) {
        if (task.requestSnapshot() == null) {
            return "auto";
        }
        return task.requestSnapshot().outputCount().toValue();
    }

    /**
     * 构建片段时长规划Context。
     * @param clipDurationPlan 片段时长规划值
     * @param storyboardDurationRanges 分镜时长Ranges值
     * @return 处理结果
     */
    public List<Map<String, Object>> buildClipDurationPlanContext(List<int[]> clipDurationPlan, List<int[]> storyboardDurationRanges) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < clipDurationPlan.size(); index++) {
            int[] plan = clipDurationPlan.get(index);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("clipIndex", index + 1);
            row.put("targetDurationSeconds", plan[0]);
            row.put("minDurationSeconds", plan[1]);
            row.put("maxDurationSeconds", plan[2]);
            if (index < storyboardDurationRanges.size()) {
                int[] scripted = storyboardDurationRanges.get(index);
                row.put("durationSource", "storyboard");
                row.put("scriptMinDurationSeconds", scripted[0]);
                row.put("scriptMaxDurationSeconds", scripted[1]);
            } else {
                row.put("durationSource", "task_average");
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * 构建片段时长规划。
     * @param task 要处理的任务对象
     * @param defaultDurationSeconds 默认时长Seconds值
     * @param clipCount 片段数量值
     * @param storyboardMarkdown 分镜Markdown值
     * @return 处理结果
     */
    public List<int[]> buildClipDurationPlan(TaskRecord task, int defaultDurationSeconds, int clipCount, String storyboardMarkdown) {
        int normalizedClipCount = Math.max(1, clipCount);
        int totalMin = Math.max(1, task.minDurationSeconds() > 0 ? task.minDurationSeconds() : defaultDurationSeconds);
        int totalMax = Math.max(totalMin, task.maxDurationSeconds() > 0 ? task.maxDurationSeconds() : defaultDurationSeconds);
        List<int[]> ranges = extractStoryboardShotDurationRanges(storyboardMarkdown);
        int scriptedClipCount = Math.min(ranges.size(), normalizedClipCount);
        int scriptedMinSum = 0;
        int scriptedMaxSum = 0;
        for (int index = 0; index < scriptedClipCount; index++) {
            int[] range = ranges.get(index);
            scriptedMinSum += Math.max(1, range[0]);
            scriptedMaxSum += Math.max(Math.max(1, range[0]), range[1]);
        }
        int unresolvedClipCount = Math.max(0, normalizedClipCount - scriptedClipCount);
        int globalMin = unresolvedClipCount == 0
            ? CLIP_MIN_SECONDS
            : Math.max(1, Math.round((float) Math.max(unresolvedClipCount, totalMin - scriptedMinSum) / unresolvedClipCount));
        int globalMax = unresolvedClipCount == 0
            ? globalMin
            : Math.max(globalMin, Math.round((float) Math.max(unresolvedClipCount, totalMax - scriptedMaxSum) / unresolvedClipCount));
        globalMin = clamp(globalMin, CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
        globalMax = clamp(globalMax, globalMin, CLIP_MAX_SECONDS);
        List<int[]> plan = new ArrayList<>();
        for (int index = 0; index < normalizedClipCount; index++) {
            boolean scripted = index < ranges.size();
            int clipMin = scripted ? ranges.get(index)[0] : globalMin;
            int clipMax = scripted ? Math.max(clipMin, ranges.get(index)[1]) : globalMax;
            clipMin = clamp(clipMin, CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
            clipMax = clamp(clipMax, clipMin, CLIP_MAX_SECONDS);
            int clipTarget = scripted
                ? clipMin
                : Math.max(clipMin, Math.min(clipMax, Math.round((clipMin + clipMax) / 2.0f)));
            clipTarget = clamp(clipTarget, clipMin, clipMax);
            plan.add(new int[] {clipTarget, clipMin, clipMax});
        }
        return plan;
    }

    /**
     * 规范化片段时长规划。
     * @param requestedVideoModel requested视频模型值
     * @param clipDurationPlan 片段时长规划值
     * @return 处理结果
     */
    public List<int[]> normalizeClipDurationPlan(String requestedVideoModel, List<int[]> clipDurationPlan) {
        if (clipDurationPlan == null || clipDurationPlan.isEmpty()) {
            return List.of();
        }
        List<Integer> supportedDurations = supportedVideoDurations(requestedVideoModel);
        if (supportedDurations.isEmpty()) {
            return clipDurationPlan;
        }
        List<int[]> normalizedPlan = new ArrayList<>(clipDurationPlan.size());
        for (int[] planItem : clipDurationPlan) {
            if (planItem == null || planItem.length < 3) {
                continue;
            }
            normalizedPlan.add(normalizeClipDurationRange(supportedDurations, planItem[0], planItem[1], planItem[2]));
        }
        return normalizedPlan;
    }

    /**
     * 处理extract分镜Shot时长Ranges。
     * @param storyboardMarkdown 分镜Markdown值
     * @return 处理结果
     */
    public List<int[]> extractStoryboardShotDurationRanges(String storyboardMarkdown) {
        String normalized = stringValue(storyboardMarkdown);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> lines = List.of(normalized.split("\\R"));
        StoryboardTableSchema schema = detectStoryboardTableSchema(lines);
        if (schema.headerCells().isEmpty() || !schema.missingStructuredRequiredColumns().isEmpty()) {
            return List.of();
        }
        List<int[]> ranges = new ArrayList<>();
        for (String rawLine : lines) {
            String stripped = rawLine.trim();
            if (!stripped.startsWith("|")) {
                continue;
            }
            List<String> cells = splitTableRow(stripped);
            if (cells.size() < 2 || isDividerRow(cells) || schema.isHeaderRow(cells)) {
                continue;
            }
            String first = schema.cell(cells, schema.shotNoIndex(), 0);
            if (first.isBlank() || first.contains("镜号") || first.toLowerCase().contains("shot")) {
                continue;
            }
            String durationCell = schema.cell(cells, schema.durationIndex(), -1);
            int[] parsed = parseDurationRangeHint(durationCell);
            if (parsed != null) {
                ranges.add(parsed);
            }
        }
        return ranges;
    }

    /**
     * 处理extract分镜ShotPlans。
     * @param storyboardMarkdown 分镜Markdown值
     * @return 处理结果
     */
    private List<StoryboardShotPlan> extractStoryboardShotPlans(String storyboardMarkdown) {
        String normalized = stringValue(storyboardMarkdown);
        if (normalized.isBlank()) {
            throw new IllegalStateException("分镜解析失败，分镜脚本不能为空，且必须是结构化 Markdown 表格。");
        }
        Map<String, String> characterAppearances = extractCharacterAppearanceMap(normalized);
        List<String> lines = List.of(normalized.split("\\R"));
        StoryboardTableSchema schema = detectStoryboardTableSchema(lines);
        validateStructuredStoryboardSchema(schema);
        List<StoryboardShotPlan> shotPlans = new ArrayList<>();
        int dataRowCount = 0;
        String previousLastFramePrompt = "";

        for (String rawLine : lines) {
            String stripped = rawLine.trim();
            if (!stripped.startsWith("|")) {
                continue;
            }
            List<String> cells = splitTableRow(stripped);
            if (cells.size() < 2 || isDividerRow(cells) || schema.isHeaderRow(cells)) {
                continue;
            }
            String first = schema.cell(cells, schema.shotNoIndex(), 0);
            if (first.isBlank() || first.contains("镜号") || first.toLowerCase().contains("shot")) {
                continue;
            }
            dataRowCount++;
            String shotIndex = normalizeStoryboardPromptValue(first);
            int sequentialIndex = shotPlans.size() + 1;
            String durationHint = normalizeStoryboardPromptValue(schema.cell(cells, schema.durationIndex(), -1));
            String parsedFirstFramePrompt = augmentCharacterAppearanceDefinitions(
                normalizeStoryboardPromptValue(schema.cell(cells, schema.firstFramePromptIndex(), -1)),
                characterAppearances
            );
            String lastFramePrompt = augmentCharacterAppearanceDefinitions(
                normalizeStoryboardPromptValue(schema.cell(cells, schema.lastFramePromptIndex(), -1)),
                characterAppearances
            );
            String contentDescription = normalizeStoryboardPromptValue(schema.cell(cells, schema.contentDescriptionIndex(), -1));
            String cameraMovement = extractCameraMovement(contentDescription);
            if (cameraMovement.isBlank()) {
                cameraMovement = "static";
            }
            assertStructuredStoryboardRow(shotIndex, parsedFirstFramePrompt, lastFramePrompt, contentDescription, durationHint);
            String firstFramePrompt = sequentialIndex > 1 && !previousLastFramePrompt.isBlank()
                ? previousLastFramePrompt
                : parsedFirstFramePrompt;
            String scene = firstNonBlank(firstFramePrompt, parsedFirstFramePrompt, lastFramePrompt);
            String imagePrompt = sequentialIndex == 1 ? firstFramePrompt : "";
            String videoPrompt = buildContinuousClipPrompt(
                firstFramePrompt,
                lastFramePrompt,
                contentDescription,
                cameraMovement,
                durationHint
            );
            shotPlans.add(new StoryboardShotPlan(
                sequentialIndex,
                shotIndex,
                scene,
                firstFramePrompt,
                lastFramePrompt,
                contentDescription,
                cameraMovement,
                durationHint,
                imagePrompt,
                videoPrompt
            ));
            previousLastFramePrompt = lastFramePrompt;
        }
        if (dataRowCount == 0) {
            throw new IllegalStateException("分镜解析失败，结构化分镜表未识别到任何镜头数据行。");
        }
        if (!shotPlans.isEmpty()) {
            return shotPlans;
        }
        throw new IllegalStateException("分镜解析失败，结构化分镜表未生成有效镜头。");
    }

    /**
     * 校验结构化分镜Schema。
     * @param schema 分镜表Schema值
     */
    private void validateStructuredStoryboardSchema(StoryboardTableSchema schema) {
        if (schema.headerCells().isEmpty()) {
            throw new IllegalStateException(
                "分镜解析失败，必须提供结构化 Markdown 分镜表，表头需包含：镜号、首帧描述、尾帧描述、分镜内容描述、时长。"
            );
        }
        List<String> missingColumns = schema.missingStructuredRequiredColumns();
        if (!missingColumns.isEmpty()) {
            throw new IllegalStateException("分镜解析失败，结构化分镜表缺少必填列：" + String.join("、", missingColumns));
        }
    }

    /**
     * 校验结构化分镜行。
     * @param shotLabel 镜头标签值
     * @param firstFramePrompt 首帧描述值
     * @param lastFramePrompt 尾帧描述值
     * @param contentDescription 分镜内容描述值
     * @param durationHint 时长值
     */
    private void assertStructuredStoryboardRow(
        String shotLabel,
        String firstFramePrompt,
        String lastFramePrompt,
        String contentDescription,
        String durationHint
    ) {
        List<String> missingFields = new ArrayList<>();
        if (normalizeStoryboardPromptValue(shotLabel).isBlank()) {
            missingFields.add("镜号");
        }
        if (normalizeStoryboardPromptValue(firstFramePrompt).isBlank()) {
            missingFields.add("首帧描述");
        }
        if (normalizeStoryboardPromptValue(lastFramePrompt).isBlank()) {
            missingFields.add("尾帧描述");
        }
        if (normalizeStoryboardPromptValue(contentDescription).isBlank()) {
            missingFields.add("分镜内容描述");
        }
        if (normalizeStoryboardPromptValue(durationHint).isBlank()) {
            missingFields.add("时长");
        }
        if (!missingFields.isEmpty()) {
            throw new IllegalStateException("分镜解析失败，镜头 " + shotLabel + " 缺少必填字段：" + String.join("、", missingFields));
        }
    }

    /**
     * 规范化片段时长范围。
     * @param supportedDurations supportedDurations值
     * @param targetDurationSeconds target时长Seconds值
     * @param minDurationSeconds 最小时长Seconds值
     * @param maxDurationSeconds 最大时长Seconds值
     * @return 处理结果
     */
    private int[] normalizeClipDurationRange(
        List<Integer> supportedDurations,
        int targetDurationSeconds,
        int minDurationSeconds,
        int maxDurationSeconds
    ) {
        int normalizedTarget = clamp(targetDurationSeconds, CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
        int normalizedMin = clamp(Math.min(minDurationSeconds, maxDurationSeconds), CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
        int normalizedMax = clamp(Math.max(minDurationSeconds, maxDurationSeconds), normalizedMin, CLIP_MAX_SECONDS);
        List<Integer> inRange = supportedDurations.stream()
            .filter(candidate -> candidate >= normalizedMin && candidate <= normalizedMax)
            .toList();
        if (!inRange.isEmpty()) {
            return new int[] {
                closestSupportedDuration(inRange, normalizedTarget),
                inRange.get(0),
                inRange.get(inRange.size() - 1)
            };
        }
        int resolved = closestSupportedDuration(supportedDurations, normalizedTarget);
        return new int[] {resolved, resolved, resolved};
    }

    /**
     * 处理supported视频Durations。
     * @param requestedVideoModel requested视频模型值
     * @return 处理结果
     */
    private List<Integer> supportedVideoDurations(String requestedVideoModel) {
        String normalizedModel = stringValue(requestedVideoModel);
        if (normalizedModel.isBlank()) {
            return List.of();
        }
        Map<String, String> section = modelResolver.section("model.models.\"" + normalizedModel + "\"");
        String raw = stringValue(section.get("supported_durations"));
        if (raw.isBlank()) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        for (String token : raw.split(",")) {
            try {
                int value = Integer.parseInt(token.trim());
                if (value > 0 && !values.contains(value)) {
                    values.add(value);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        values.sort(Integer::compareTo);
        return values;
    }

    /**
     * 处理closestSupported时长。
     * @param candidates candidates值
     * @param requestedDurationSeconds requested时长Seconds值
     * @return 处理结果
     */
    private int closestSupportedDuration(List<Integer> candidates, int requestedDurationSeconds) {
        int resolved = candidates.get(0);
        int smallestDistance = Math.abs(resolved - requestedDurationSeconds);
        for (int candidate : candidates) {
            int distance = Math.abs(candidate - requestedDurationSeconds);
            if (distance < smallestDistance || (distance == smallestDistance && candidate > resolved)) {
                resolved = candidate;
                smallestDistance = distance;
            }
        }
        return resolved;
    }

    /**
     * 解析时长范围提示。
     * @param text 文本值
     * @return 处理结果
     */
    private int[] parseDurationRangeHint(String text) {
        String normalized = stringValue(text);
        if (normalized.isBlank()) {
            return null;
        }
        Matcher rangeMatcher = SCRIPT_DURATION_RANGE_PATTERN.matcher(normalized);
        if (rangeMatcher.find()) {
            int left = safeRoundedSeconds(rangeMatcher.group("left"));
            int right = safeRoundedSeconds(rangeMatcher.group("right"));
            int low = clamp(Math.min(left, right), CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
            int high = clamp(Math.max(left, right), low, CLIP_MAX_SECONDS);
            return new int[] {low, high};
        }
        Matcher valueMatcher = SCRIPT_DURATION_VALUE_PATTERN.matcher(normalized);
        if (valueMatcher.find()) {
            int value = clamp(safeRoundedSeconds(valueMatcher.group("value")), CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
            return new int[] {value, value};
        }
        Matcher plainRangeMatcher = PLAIN_DURATION_RANGE_PATTERN.matcher(normalized);
        if (plainRangeMatcher.find()) {
            int left = safeRoundedSeconds(plainRangeMatcher.group("left"));
            int right = safeRoundedSeconds(plainRangeMatcher.group("right"));
            int low = clamp(Math.min(left, right), CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
            int high = clamp(Math.max(left, right), low, CLIP_MAX_SECONDS);
            return new int[] {low, high};
        }
        Matcher plainValueMatcher = PLAIN_DURATION_VALUE_PATTERN.matcher(normalized);
        if (plainValueMatcher.find()) {
            int value = clamp(safeRoundedSeconds(plainValueMatcher.group("value")), CLIP_MIN_SECONDS, CLIP_MAX_SECONDS);
            return new int[] {value, value};
        }
        return null;
    }

    /**
     * 处理safeRoundedSeconds。
     * @param value 待处理的值
     * @return 处理结果
     */
    private int safeRoundedSeconds(String value) {
        try {
            return Math.max(1, Math.min(120, (int) Math.round(Double.parseDouble(stringValue(value)))));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    /**
     * 处理clamp。
     * @param value 待处理的值
     * @param min 最小值
     * @param max 最大值
     * @return 处理结果
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 处理splitTable行。
     * @param row 行值
     * @return 处理结果
     */
    private List<String> splitTableRow(String row) {
        String trimmed = row.trim();
        if (!trimmed.startsWith("|")) {
            return List.of();
        }
        String[] parts = trimmed.substring(1, trimmed.endsWith("|") ? trimmed.length() - 1 : trimmed.length()).split("\\|");
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(part.trim());
        }
        return cells;
    }

    /**
     * 处理detect分镜TableSchema。
     * @param lines lines值
     * @return 处理结果
     */
    private StoryboardTableSchema detectStoryboardTableSchema(List<String> lines) {
        for (String rawLine : lines) {
            String stripped = rawLine.trim();
            if (!stripped.startsWith("|")) {
                continue;
            }
            List<String> cells = splitTableRow(stripped);
            if (cells.isEmpty() || isDividerRow(cells)) {
                continue;
            }
            if (looksLikeHeaderRow(cells)) {
                return StoryboardTableSchema.fromHeader(cells);
            }
        }
        return StoryboardTableSchema.empty();
    }

    /**
     * 检查是否looksLikeHeader行。
     * @param cells cells值
     * @return 是否满足条件
     */
    private boolean looksLikeHeaderRow(List<String> cells) {
        for (String cell : cells) {
            String normalized = normalizeStoryboardHeader(cell);
            if (normalized.contains("shot") || normalized.contains("镜号")
                || normalized.contains("首帧") || normalized.contains("尾帧")
                || normalized.contains("startframe") || normalized.contains("endframe")
                || normalized.contains("分镜内容描述") || normalized.contains("剧情画面与声音描述")
                || normalized.contains("合并长段描述") || normalized.contains("画面叙述")
                || normalized.contains("storydescription") || normalized.contains("contentdescription")
                || normalized.contains("duration") || normalized.contains("时长")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规范化分镜Header。
     * @param text 文本值
     * @return 处理结果
     */
    private String normalizeStoryboardHeader(String text) {
        return stringValue(text)
            .trim()
            .toLowerCase()
            .replaceAll("[\\s_\\-()（）/\\\\+:：·,.，]", "");
    }

    /**
     * 规范化分镜提示词值。
     * @param value 待处理的值
     * @return 处理结果
     */
    private String normalizeStoryboardPromptValue(String value) {
        return stringValue(value)
            .replace("<br>", " ")
            .replace("<br/>", " ")
            .replace("<br />", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * 提取角色外观定义。
     * @param storyboardMarkdown 分镜Markdown值
     * @return 角色外观映射
     */
    private Map<String, String> extractCharacterAppearanceMap(String storyboardMarkdown) {
        Map<String, String> appearances = new LinkedHashMap<>();
        for (CharacterDefinition definition : extractCharacterDefinitions(storyboardMarkdown)) {
            if (!definition.name().isBlank() && !definition.appearance().isBlank()) {
                appearances.put(definition.name(), definition.appearance());
            }
        }
        return appearances;
    }

    /**
     * 提取单个角色外观定义。
     * @param details 角色定义详情
     * @return 外观定义
     */
    private String extractAppearanceDefinition(String details) {
        String normalized = normalizeStoryboardPromptValue(details);
        if (normalized.isBlank()) {
            return "";
        }
        Matcher matcher = CHARACTER_APPEARANCE_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return trimAppearanceDefinition(normalizeStoryboardPromptValue(matcher.group("appearance")));
        }
        int behaviorIndex = indexOfFirst(normalized, "行为特征", "说话风格", "性格特征", "角色关系");
        return behaviorIndex > 0 ? trimAppearanceDefinition(normalizeStoryboardPromptValue(normalized.substring(0, behaviorIndex))) : "";
    }

    /**
     * 为关键帧提示词补充角色外观定义。
     * @param prompt 原始提示词
     * @param characterAppearances 角色外观映射
     * @return 增强后的提示词
     */
    private String augmentCharacterAppearanceDefinitions(String prompt, Map<String, String> characterAppearances) {
        String normalizedPrompt = normalizeStoryboardPromptValue(prompt);
        if (normalizedPrompt.isBlank() || characterAppearances == null || characterAppearances.isEmpty()) {
            return normalizedPrompt;
        }
        String augmentedPrompt = normalizedPrompt;
        List<Map.Entry<String, String>> entries = new ArrayList<>(characterAppearances.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()));
        for (Map.Entry<String, String> entry : entries) {
            String characterName = normalizeStoryboardPromptValue(entry.getKey());
            String appearance = normalizeStoryboardPromptValue(entry.getValue());
            if (characterName.isBlank()
                || appearance.isBlank()
                || !augmentedPrompt.contains(characterName)
                || augmentedPrompt.contains(characterName + "（" + appearance + "）")
                || augmentedPrompt.contains(characterName + "(" + appearance + ")")) {
                continue;
            }
            Pattern pattern = Pattern.compile(Pattern.quote(characterName) + "(?!\\s*[（(])");
            Matcher matcher = pattern.matcher(augmentedPrompt);
            if (matcher.find()) {
                augmentedPrompt = matcher.replaceFirst(
                    Matcher.quoteReplacement(characterName + "（" + appearance + "）")
                );
            }
        }
        return augmentedPrompt;
    }

    /**
     * 查找多个token中的最早位置。
     * @param value 原始文本
     * @param tokens token列表
     * @return 最早位置
     */
    private int indexOfFirst(String value, String... tokens) {
        int resolved = -1;
        String normalized = stringValue(value);
        for (String token : tokens) {
            int current = normalized.indexOf(token);
            if (current >= 0 && (resolved < 0 || current < resolved)) {
                resolved = current;
            }
        }
        return resolved;
    }

    /**
     * 清理外观定义末尾标点，避免重复包裹时出现冗余停顿。
     * @param value 原始外观定义
     * @return 清理后的外观定义
     */
    private String trimAppearanceDefinition(String value) {
        return normalizeStoryboardPromptValue(value).replaceAll("[。；;，,]+$", "").trim();
    }

    /**
     * 角色定义。
     * @param name 角色名
     * @param appearance 外观定义
     */
    public record CharacterDefinition(String name, String appearance, String definition) {

        public CharacterDefinition(String name, String appearance) {
            this(name, appearance, appearance);
        }
    }

    /**
     * 构建Continuous片段提示词。
     * @param firstFramePrompt 首帧提示词值
     * @param lastFramePrompt 尾帧提示词值
     * @param contentDescription 分镜内容描述值
     * @param cameraMovement cameraMovement值
     * @param durationHint 时长值
     * @return 处理结果
     */
    private String buildContinuousClipPrompt(
        String firstFramePrompt,
        String lastFramePrompt,
        String contentDescription,
        String cameraMovement,
        String durationHint
    ) {
        List<String> parts = new ArrayList<>();
        if (!firstFramePrompt.isBlank()) {
            parts.add("首帧：" + firstFramePrompt);
        }
        if (!lastFramePrompt.isBlank()) {
            parts.add("尾帧：" + lastFramePrompt);
        }
        if (!contentDescription.isBlank()) {
            parts.add("分镜内容：" + contentDescription);
        }
        if (!cameraMovement.isBlank() && !"static".equalsIgnoreCase(cameraMovement)) {
            parts.add("运镜关键词：" + cameraMovement);
        }
        if (!durationHint.isBlank()) {
            parts.add("时长：" + durationHint);
        }
        return truncateText(String.join("；", parts), 2200);
    }

    /**
     * 处理extractCameraMovement。
     * @param value 待处理的值
     * @return 处理结果
     */
    private String extractCameraMovement(String value) {
        String normalized = normalizeStoryboardPromptValue(value);
        if (normalized.isBlank()) {
            return "";
        }
        for (String token : normalized.split("[/／+＋,，;；|｜]")) {
            String trimmed = token.trim();
            if (looksLikeCameraMovement(trimmed)) {
                return trimmed;
            }
        }
        return looksLikeCameraMovement(normalized) ? normalized : "";
    }

    /**
     * 检查是否looksLikeCameraMovement。
     * @param value 待处理的值
     * @return 是否满足条件
     */
    private boolean looksLikeCameraMovement(String value) {
        String normalized = normalizeStoryboardPromptValue(value).toLowerCase();
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.contains("推")
            || normalized.contains("拉")
            || normalized.contains("摇")
            || normalized.contains("移")
            || normalized.contains("跟")
            || normalized.contains("甩")
            || normalized.contains("升")
            || normalized.contains("降")
            || normalized.contains("环绕")
            || normalized.contains("环拍")
            || normalized.contains("手持")
            || normalized.contains("dolly")
            || normalized.contains("push")
            || normalized.contains("pull")
            || normalized.contains("pan")
            || normalized.contains("tilt")
            || normalized.contains("truck")
            || normalized.contains("track")
            || normalized.contains("orbit")
            || normalized.contains("handheld")
            || normalized.contains("whip");
    }

    /**
     * 检查是否Divider行。
     * @param cells cells值
     * @return 是否满足条件
     */
    private boolean isDividerRow(List<String> cells) {
        for (String cell : cells) {
            if (!cell.matches("[:\\-\\s]*")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 处理truncate文本。
     * @param value 待处理的值
     * @param maxLength 最大Length值
     * @return 处理结果
     */
    private String truncateText(String value, int maxLength) {
        String normalized = stringValue(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /**
     * 处理string值。
     * @param value 待处理的值
     * @return 处理结果
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 处理分镜TableSchema。
     * @param headerCells headerCells值
     * @param shotNoIndex shotNo索引值
     * @param firstFramePromptIndex 首帧描述索引值
     * @param lastFramePromptIndex 尾帧描述索引值
     * @param contentDescriptionIndex 分镜内容描述索引值
     * @param durationIndex 时长索引值
     * @return 处理结果
     */
    private record StoryboardTableSchema(
        List<String> headerCells,
        Integer shotNoIndex,
        Integer firstFramePromptIndex,
        Integer lastFramePromptIndex,
        Integer contentDescriptionIndex,
        Integer durationIndex
    ) {
        /**
         * 处理empty。
         * @return 处理结果
         */
        static StoryboardTableSchema empty() {
            return new StoryboardTableSchema(List.of(), null, null, null, null, null);
        }

        /**
         * 处理fromHeader。
         * @param headers headers值
         * @return 处理结果
         */
        static StoryboardTableSchema fromHeader(List<String> headers) {
            return new StoryboardTableSchema(
                List.copyOf(headers),
                resolve(headers, "镜号"),
                resolve(headers, "首帧描述startframe", "首帧描述", "startframe"),
                resolve(headers, "尾帧描述endframe", "尾帧描述", "endframe"),
                resolve(headers, "分镜内容描述"),
                resolve(headers, "时长", "duration")
            );
        }

        /**
         * 处理解析。
         * @param headers headers值
         * @param aliases aliases值
         * @return 处理结果
         */
        private static Integer resolve(List<String> headers, String... aliases) {
            for (int index = 0; index < headers.size(); index++) {
                String header = headers.get(index)
                    .trim()
                    .toLowerCase()
                    .replaceAll("[\\s_\\-()（）/\\\\+:：·,.，]", "");
                for (String alias : aliases) {
                    if (header.contains(alias)) {
                        return index;
                    }
                }
            }
            return null;
        }

        /**
         * 获取缺失的结构化必填列。
         * @return 缺失列列表
         */
        List<String> missingStructuredRequiredColumns() {
            List<String> missing = new ArrayList<>();
            if (shotNoIndex == null) {
                missing.add("镜号");
            }
            if (firstFramePromptIndex == null) {
                missing.add("首帧描述");
            }
            if (lastFramePromptIndex == null) {
                missing.add("尾帧描述");
            }
            if (contentDescriptionIndex == null) {
                missing.add("分镜内容描述");
            }
            if (durationIndex == null) {
                missing.add("时长");
            }
            return missing;
        }

        /**
         * 检查是否Header行。
         * @param cells cells值
         * @return 是否满足条件
         */
        boolean isHeaderRow(List<String> cells) {
            if (headerCells.isEmpty() || cells.size() != headerCells.size()) {
                return false;
            }
            for (int index = 0; index < cells.size(); index++) {
                String left = cells.get(index).trim();
                String right = headerCells.get(index).trim();
                if (!left.equalsIgnoreCase(right)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * 处理cell。
         * @param cells cells值
         * @param index 索引值
         * @param fallbackIndex 兜底索引值
         * @return 处理结果
         */
        String cell(List<String> cells, Integer index, int fallbackIndex) {
            int resolvedIndex = index != null ? index : fallbackIndex;
            if (resolvedIndex < 0 || resolvedIndex >= cells.size()) {
                return "";
            }
            Object value = cells.get(resolvedIndex);
            return value == null ? "" : String.valueOf(value).trim();
        }
    }

    /**
     * 处理分镜Shot规划。
     * @param sequentialIndex sequential索引值
     * @param shotLabel shot标签值
     * @param scene scene值
     * @param firstFramePrompt 首个Frame提示词值
     * @param lastFramePrompt lastFrame提示词值
     * @param motion motion值
     * @param cameraMovement cameraMovement值
     * @param durationHint 时长提示值
     * @param imagePrompt 图像提示词值
     * @param videoPrompt 视频提示词值
     * @return 处理结果
     */
    public record StoryboardShotPlan(
        int sequentialIndex,
        String shotLabel,
        String scene,
        String firstFramePrompt,
        String lastFramePrompt,
        String motion,
        String cameraMovement,
        String durationHint,
        String imagePrompt,
        String videoPrompt
    ) {
        /**
         * 获取首帧提示词语义别名。
         * @return 处理结果
         */
        public String startFramePrompt() {
            return firstFramePrompt;
        }

        /**
         * 获取尾帧提示词语义别名。
         * @return 处理结果
         */
        public String endFramePrompt() {
            return lastFramePrompt;
        }

        /**
         * 获取动作路径语义别名。
         * @return 处理结果
         */
        public String actionPath() {
            return motion;
        }
    }
}
