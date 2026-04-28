package com.jiandou.api.task.runtime;

import com.jiandou.api.generation.GenerationRunKinds;
import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import com.jiandou.api.media.LocalMediaArtifactService;
import com.jiandou.api.task.TaskRecord;
import com.jiandou.api.task.domain.TaskArtifactNaming;
import com.jiandou.api.task.domain.TaskStatus;
import com.jiandou.api.task.exception.TaskExecutionAbortedException;
import com.jiandou.api.task.persistence.TaskRepository;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 任务执行运行时支持。
 */
@Component
class TaskExecutionRuntimeSupport {

    private final TaskRepository taskRepository;
    private final ModelRuntimePropertiesResolver modelResolver;
    private final LocalMediaArtifactService localMediaArtifactService;

    TaskExecutionRuntimeSupport(
        TaskRepository taskRepository,
        ModelRuntimePropertiesResolver modelResolver,
        LocalMediaArtifactService localMediaArtifactService
    ) {
        this.taskRepository = taskRepository;
        this.modelResolver = modelResolver;
        this.localMediaArtifactService = localMediaArtifactService;
    }

    /**
     * 处理active尝试。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    Map<String, Object> activeAttempt(TaskRecord task) {
        if (task.activeAttemptId() == null || task.activeAttemptId().isBlank()) {
            return null;
        }
        for (Map<String, Object> row : task.attemptsView()) {
            if (task.activeAttemptId().equals(stringValue(row.get("attemptId")))) {
                return row;
            }
        }
        return null;
    }

    /**
     * 处理解析Dimensions。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    int[] resolveDimensions(TaskRecord task) {
        String requestedVideoSize = task.requestSnapshot() == null ? "" : stringValue(task.requestSnapshot().videoSize());
        if (!requestedVideoSize.isBlank()) {
            String normalized = requestedVideoSize.toLowerCase().replace("x", "*");
            String[] parts = normalized.split("\\*");
            if (parts.length == 2) {
                try {
                    return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if ("16:9".equals(task.aspectRatio())) {
            return new int[] {1280, 720};
        }
        return new int[] {720, 1280};
    }

    /**
     * 处理解析时长Seconds。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    int resolveDurationSeconds(TaskRecord task) {
        if (task.requestSnapshot() != null && task.requestSnapshot().videoDuration() != null && !task.requestSnapshot().videoDuration().auto()) {
            Integer requestedSeconds = task.requestSnapshot().videoDuration().seconds();
            if (requestedSeconds != null) {
                return Math.max(1, requestedSeconds);
            }
        }
        if (task.maxDurationSeconds() > 0) {
            return task.maxDurationSeconds();
        }
        if (task.minDurationSeconds() > 0) {
            return task.minDurationSeconds();
        }
        int configuredDefault = modelResolver.intValue("catalog.defaults", "video_duration_seconds", 10);
        return Math.max(1, configuredDefault);
    }

    /**
     * 处理assert任务StillActive。
     * @param task 要处理的任务对象
     */
    void assertTaskStillActive(TaskRecord task) {
        TaskRecord latest = taskRepository.findById(task.id());
        if (latest == null) {
            throw new TaskExecutionAbortedException("MISSING", "任务不存在，停止执行。");
        }
        if (isTaskExecutionActive(latest.status())) {
            return;
        }
        task.setStatus(latest.status());
        task.setProgress(latest.progress());
        task.setErrorMessage(latest.errorMessage());
        task.setFinishedAt(latest.finishedAt());
        task.setQueued(latest.isQueued());
        task.setQueuePosition(latest.queuePosition());
        task.setActiveAttemptId(latest.activeAttemptId());
        task.setExecutionContext(latest.executionContext());
        throw new TaskExecutionAbortedException(
            latest.status(),
            latest.errorMessage() == null || latest.errorMessage().isBlank() ? "任务已停止执行。" : latest.errorMessage()
        );
    }

    /**
     * 构建脚本运行请求。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    Map<String, Object> buildScriptRunRequest(TaskRecord task) {
        String sourceText = !task.transcriptText().isBlank() ? task.transcriptText() : (!task.creativePrompt().isBlank() ? task.creativePrompt() : task.title());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", GenerationRunKinds.SCRIPT);
        request.put("input", Map.of("text", sourceText));
        request.put("model", Map.of("textAnalysisModel", textAnalysisModel(task)));
        request.put("options", Map.of("visualStyle", "AI 自动决策"));
        request.put("storage", Map.of(
            "relativeDir", TaskArtifactNaming.taskRunningRelativeDir(task),
            "fileName", TaskArtifactNaming.storyboardFileName(task, "md")
        ));
        putUserAuth(request, task);
        return Map.copyOf(request);
    }

    /**
     * 构建图像运行请求。
     * @param task 要处理的任务对象
     * @param clipIndex 片段索引值
     * @param prompt 提示词值
     * @param width width值
     * @param height height值
     * @param referenceImageUrl reference图像URL值
     * @return 处理结果
     */
    Map<String, Object> buildImageRunRequest(TaskRecord task, int clipIndex, String prompt, int width, int height, String referenceImageUrl) {
        return buildImageRunRequest(task, clipIndex, prompt, width, height, referenceImageUrl, 0, "first");
    }

    /**
     * 构建图像运行请求。
     * @param task 要处理的任务对象
     * @param clipIndex 片段索引值
     * @param prompt 提示词值
     * @param width width值
     * @param height height值
     * @param referenceImageUrl reference图像URL值
     * @param durationSeconds 时长Seconds值
     * @param frameRole frameRole值
     * @return 处理结果
     */
    Map<String, Object> buildImageRunRequest(
        TaskRecord task,
        int clipIndex,
        String prompt,
        int width,
        int height,
        String referenceImageUrl,
        int durationSeconds,
        String frameRole
    ) {
        String normalizedFrameRole = normalizeFrameRole(frameRole);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", prompt);
        input.put("width", width);
        input.put("height", height);
        input.put("frameRole", normalizedFrameRole);
        if (durationSeconds > 0) {
            input.put("durationSeconds", durationSeconds);
        }
        Integer imageSeed = imageSeed(task, clipIndex);
        if (imageSeed != null && imageModelSupportsSeed(task)) {
            input.put("seed", imageSeed);
        }
        if (referenceImageUrl != null && !referenceImageUrl.isBlank()) {
            List<String> referenceImageUrls = compatibleImageReferenceUrls(referenceImageUrl, imageModel(task));
            if (!referenceImageUrls.isEmpty()) {
                input.put("referenceImageUrl", referenceImageUrls.get(0));
                input.put("referenceImageUrls", referenceImageUrls);
            }
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", GenerationRunKinds.IMAGE);
        request.put("input", input);
        request.put("model", Map.of(
                /**
                 * 处理文本分析模型。
                 * @param task 要处理的任务对象
                 * @return 处理结果
                 */
                "textAnalysisModel", textAnalysisModel(task),
                /**
                 * 处理图像模型。
                 * @param task 要处理的任务对象
                 * @return 处理结果
                 */
                "providerModel", imageModel(task)
            ));
        request.put("options", Map.of("stylePreset", stylePreset(task)));
        request.put("storage", Map.of(
            "relativeDir", TaskArtifactNaming.taskRunningRelativeDir(task),
            "fileStem", "clip" + Math.max(1, clipIndex) + "-" + normalizedFrameRole
        ));
        putUserAuth(request, task);
        return Map.copyOf(request);
    }

    /**
     * 构建视频运行请求。
     * @param task 要处理的任务对象
     * @param clipIndex 片段索引值
     * @param prompt 提示词值
     * @param videoSize 视频Size值
     * @param durationSeconds 时长Seconds值
     * @param minDurationSeconds 最小时长Seconds值
     * @param maxDurationSeconds 最大时长Seconds值
     * @param firstFrameUrl 首个FrameURL值
     * @param lastFrameUrl lastFrameURL值
     * @return 处理结果
     */
    Map<String, Object> buildVideoRunRequest(
        TaskRecord task,
        int clipIndex,
        String prompt,
        String videoSize,
        int durationSeconds,
        int minDurationSeconds,
        int maxDurationSeconds,
        String firstFrameUrl,
        String lastFrameUrl
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", buildVideoClipExecutionPrompt(prompt));
        input.put("videoSize", videoSize);
        input.put("durationSeconds", durationSeconds);
        input.put("minDurationSeconds", minDurationSeconds);
        input.put("maxDurationSeconds", maxDurationSeconds);
        input.put("firstFrameUrl", firstFrameUrl);
        if (lastFrameUrl != null && !lastFrameUrl.isBlank()) {
            input.put("lastFrameUrl", lastFrameUrl);
        }
        input.put("generateAudio", defaultVideoGenerateAudio());
        input.put("returnLastFrame", true);
        Integer taskSeed = taskSeed(task);
        if (taskSeed != null) {
            input.put("seed", taskSeed);
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("kind", GenerationRunKinds.VIDEO);
        request.put("input", input);
        request.put("model", Map.of(
                /**
                 * 处理文本分析模型。
                 * @param task 要处理的任务对象
                 * @return 处理结果
                 */
                "textAnalysisModel", textAnalysisModel(task),
                /**
                 * 处理视频模型。
                 * @param task 要处理的任务对象
                 * @return 处理结果
                 */
                "providerModel", videoModel(task)
            ));
        request.put("options", Map.of("stylePreset", stylePreset(task)));
        request.put("storage", Map.of(
            "relativeDir", TaskArtifactNaming.taskRunningRelativeDir(task),
            "fileStem", "clip" + Math.max(1, clipIndex)
        ));
        putUserAuth(request, task);
        return Map.copyOf(request);
    }

    /**
     * 检查是否任务执行Active。
     * @param status 状态值
     * @return 是否满足条件
     */
    private boolean isTaskExecutionActive(String status) {
        return TaskStatus.isExecutionActive(status);
    }

    /**
     * 构建视频片段执行提示词。
     * @param prompt 提示词值
     * @return 处理结果
     */
    private String buildVideoClipExecutionPrompt(String prompt) {
        return truncateText(prompt, 2200);
    }

    /**
     * 检查是否默认视频GenerateAudio。
     * @return 是否满足条件
     */
    private boolean defaultVideoGenerateAudio() {
        return boolValue(modelResolver.value("catalog.defaults", "video_generate_audio", "true"), true);
    }

    /**
     * 处理文本分析模型。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    private String textAnalysisModel(TaskRecord task) {
        return requiredSnapshotModel(task, "textAnalysisModel", "文本模型");
    }

    /**
     * 处理图像模型。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    private String imageModel(TaskRecord task) {
        return requiredSnapshotModel(task, "imageModel", "关键帧模型");
    }

    private boolean imageModelSupportsSeed(TaskRecord task) {
        return modelResolver.supportsSeed(imageModel(task));
    }

    private List<String> compatibleImageReferenceUrls(String referenceImageUrl, String imageModel) {
        String normalized = stringValue(referenceImageUrl);
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.startsWith("/storage/")) {
            String publicUrl = localMediaArtifactService == null ? "" : localMediaArtifactService.buildExternallyAccessibleUrl(normalized);
            if (!publicUrl.isBlank()) {
                return List.of(publicUrl);
            }
            if (supportsImageDataUriReferences(imageModel)) {
                try {
                    String dataUri = localMediaArtifactService == null ? "" : localMediaArtifactService.imageDataUriFromPublicUrl(normalized);
                    if (!dataUri.isBlank()) {
                        return List.of(dataUri);
                    }
                } catch (RuntimeException ex) {
                    throw new IllegalStateException("referenceImageUrl local image cannot be converted to data URI: " + ex.getMessage(), ex);
                }
            }
            throw new IllegalStateException("referenceImageUrl is local storage address; configure JIANDOU_STORAGE_PUBLIC_BASE_URL or use an image model that supports data URI references");
        }
        return List.of(normalized);
    }

    private boolean supportsImageDataUriReferences(String imageModel) {
        return stringValue(imageModel).toLowerCase().contains("gpt-image");
    }

    /**
     * 处理视频模型。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    private String videoModel(TaskRecord task) {
        return requiredSnapshotModel(task, "videoModel", "视频模型");
    }

    /**
     * 处理风格预设。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    private String stylePreset(TaskRecord task) {
        String configured = task.requestSnapshot() == null ? "" : stringValue(task.requestSnapshot().stylePreset());
        return configured.isBlank() ? "cinematic" : configured;
    }

    private void putUserAuth(Map<String, Object> request, TaskRecord task) {
        if (task == null || task.ownerUserId() == null) {
            return;
        }
        request.put("auth", Map.of("userId", task.ownerUserId()));
    }

    /**
     * 处理任务种子。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    private Integer taskSeed(TaskRecord task) {
        Integer configured = task.requestSnapshot() == null ? null : task.requestSnapshot().seed();
        if (configured != null) {
            return configured;
        }
        return task == null ? null : task.taskSeed();
    }

    private Integer imageSeed(TaskRecord task, int clipIndex) {
        Integer configured = taskSeed(task);
        if (configured != null) {
            return configured;
        }
        String taskIdentity = firstNonBlank(
            task == null ? "" : task.id(),
            task == null ? "" : task.title(),
            task == null ? "" : task.creativePrompt(),
            "task"
        );
        String seedSource = taskIdentity + ":clip:" + Math.max(1, clipIndex) + ":keyframe";
        int raw = UUID.nameUUIDFromBytes(seedSource.getBytes(StandardCharsets.UTF_8)).hashCode();
        return Math.floorMod(raw, Integer.MAX_VALUE - 1) + 1;
    }

    /**
     * 处理required快照模型。
     * @param task 要处理的任务对象
     * @param fieldName fieldName值
     * @param label 标签值
     * @return 处理结果
     */
    private String requiredSnapshotModel(TaskRecord task, String fieldName, String label) {
        String configured = task.requestSnapshot() == null ? "" : stringValue(task.requestSnapshot().modelValue(fieldName));
        if (!configured.isBlank()) {
            return configured;
        }
        throw new IllegalStateException("任务缺少必选模型：" + label + "（" + fieldName + "）");
    }

    /**
     * 处理truncate文本。
     * @param value 待处理的值
     * @param maxLength 最大Length值
     * @return 处理结果
     */
    private String truncateText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /**
     * 规范化FrameRole。
     * @param frameRole frameRole值
     * @return 处理结果
     */
    private String normalizeFrameRole(String frameRole) {
        return "last".equalsIgnoreCase(stringValue(frameRole)) ? "last" : "first";
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
     * 处理integer值。
     * @param value 待处理的值
     * @return 处理结果
     */
    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * 检查是否布尔值。
     * @param value 待处理的值
     * @param fallback 兜底值
     * @return 是否满足条件
     */
    private boolean boolValue(String value, boolean fallback) {
        String normalized = stringValue(value).toLowerCase();
        if (normalized.isBlank()) {
            return fallback;
        }
        return "1".equals(normalized)
            || "true".equals(normalized)
            || "yes".equals(normalized)
            || "on".equals(normalized);
    }
}
