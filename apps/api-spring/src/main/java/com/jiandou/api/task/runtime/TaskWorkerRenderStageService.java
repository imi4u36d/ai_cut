package com.jiandou.api.task.runtime;

import com.jiandou.api.generation.GenerationModelKinds;
import com.jiandou.api.generation.GenerationRunStatuses;
import com.jiandou.api.generation.application.GenerationApplicationService;
import com.jiandou.api.task.TaskRecord;
import com.jiandou.api.task.application.TaskStoryboardPlanner;
import com.jiandou.api.task.application.TaskExecutionCoordinator;
import com.jiandou.api.task.domain.TaskArtifactNaming;
import com.jiandou.api.task.domain.TaskResultTypes;
import com.jiandou.api.task.domain.TaskStage;
import com.jiandou.api.task.domain.TaskStatus;
import com.jiandou.api.task.persistence.TaskRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务工作节点Render阶段服务。
 */
@Component
final class TaskWorkerRenderStageService {

    private static final long DEFAULT_VIDEO_RUN_POLL_INTERVAL_MILLIS = 1000L;
    private static final int DEFAULT_VIDEO_RUN_MAX_POLLS = 240;

    private final TaskRepository taskRepository;
    private final TaskExecutionCoordinator executionCoordinator;
    private final GenerationApplicationService generationApplicationService;
    private final TaskExecutionRuntimeSupport runtimeSupport;
    private final TaskExecutionArtifactAssembler artifactAssembler;
    private final TaskWorkerStatusStageService statusStageService;
    private final TaskWorkerJoinStageService joinStageService;
    private final long videoRunPollIntervalMillis;
    private final int videoRunMaxPolls;

    @Autowired
    TaskWorkerRenderStageService(
        TaskRepository taskRepository,
        TaskExecutionCoordinator executionCoordinator,
        GenerationApplicationService generationApplicationService,
        TaskExecutionRuntimeSupport runtimeSupport,
        TaskExecutionArtifactAssembler artifactAssembler,
        TaskWorkerStatusStageService statusStageService,
        TaskWorkerJoinStageService joinStageService
    ) {
        this(
            taskRepository,
            executionCoordinator,
            generationApplicationService,
            runtimeSupport,
            artifactAssembler,
            statusStageService,
            joinStageService,
            DEFAULT_VIDEO_RUN_POLL_INTERVAL_MILLIS,
            DEFAULT_VIDEO_RUN_MAX_POLLS
        );
    }

    TaskWorkerRenderStageService(
        TaskRepository taskRepository,
        TaskExecutionCoordinator executionCoordinator,
        GenerationApplicationService generationApplicationService,
        TaskExecutionRuntimeSupport runtimeSupport,
        TaskExecutionArtifactAssembler artifactAssembler,
        TaskWorkerStatusStageService statusStageService,
        TaskWorkerJoinStageService joinStageService,
        long videoRunPollIntervalMillis,
        int videoRunMaxPolls
    ) {
        this.taskRepository = taskRepository;
        this.executionCoordinator = executionCoordinator;
        this.generationApplicationService = generationApplicationService;
        this.runtimeSupport = runtimeSupport;
        this.artifactAssembler = artifactAssembler;
        this.statusStageService = statusStageService;
        this.joinStageService = joinStageService;
        this.videoRunPollIntervalMillis = Math.max(0L, videoRunPollIntervalMillis);
        this.videoRunMaxPolls = Math.max(1, videoRunMaxPolls);
    }

    /**
     * 渲染render。
     * @param task 要处理的任务对象
     * @param runContext 运行Context值
     * @param request 请求体
     * @return 处理结果
     */
    RenderStageResult render(TaskRecord task, TaskWorkerExecutionContext runContext, RenderStageRequest request) {
        List<String> imageRunIds = new ArrayList<>();
        List<String> videoRunIds = new ArrayList<>();
        String previousClipLastFrameUrl = request.previousClipLastFrameUrl();
        String latestVideoOutputUrl = "";
        if (request.reuseStoryboard() && request.renderStartIndex() > 1) {
            executionCoordinator.recordTrace(task, TaskStage.PLANNING.code(), "planning.keyframe_reused_for_resume", "检测到已有进度，跳过已完成镜头并从失败镜头继续。", "INFO", Map.of(
                "completedClipCount", request.completedClipCount(),
                "renderStartIndex", request.renderStartIndex(),
                "existingClipIndices", request.existingVideoClipIndices(),
                "lastFrameUrl", previousClipLastFrameUrl,
                "resumeFromStage", request.requestedResumeStage(),
                "resumeFromClipIndex", request.requestedResumeClipIndex()
            ));
        }

        for (int index = Math.max(0, request.renderStartIndex() - 1); index < request.shotPlans().size(); index++) {
            runtimeSupport.assertTaskStillActive(task);
            int clipIndex = index + 1;
            TaskStoryboardPlanner.StoryboardShotPlan shotPlan = request.shotPlans().get(index);
            String clipPrompt = shotPlan.videoPrompt();
            String firstFramePrompt = firstNonBlank(
                shotPlan.firstFramePrompt(),
                shotPlan.lastFramePrompt(),
                clipPrompt
            );
            String lastFramePrompt = firstNonBlank(
                shotPlan.lastFramePrompt(),
                shotPlan.firstFramePrompt(),
                clipPrompt
            );
            int[] clipDuration = request.clipDurationPlan().get(index);
            int clipDurationSeconds = clipDuration[0];
            int clipMinDuration = clipDuration[1];
            int clipMaxDuration = clipDuration[2];
            FrameResolution startFrame;
            boolean reusePreviousLastFrame = clipIndex > 1;
            if (reusePreviousLastFrame) {
                if (previousClipLastFrameUrl.isBlank()) {
                    throw new IllegalStateException("clip " + clipIndex + " requires previous clip last frame before generating its end frame");
                }
                startFrame = reuseFrame(
                    task,
                    clipIndex,
                    previousClipLastFrameUrl,
                    "first",
                    "previous_video_last_frame"
                );
                executionCoordinator.recordTrace(task, TaskStage.PLANNING.code(), "planning.keyframe_reused_from_last_frame", "复用上一镜尾帧作为当前镜头首帧。", "INFO", Map.of(
                    "clipIndex", clipIndex,
                    "firstFrameUrl", startFrame.videoInputUrl(),
                    "sourceLastFrameUrl", previousClipLastFrameUrl
                ));
            } else {
                startFrame = generateFrame(
                    task,
                    clipIndex,
                    firstFramePrompt,
                    request.width(),
                    request.height(),
                    previousClipLastFrameUrl,
                    clipDurationSeconds,
                    "first",
                    clipIndex == 1 ? "generated_start_frame_keyframe" : "generated_start_frame_keyframe_fallback",
                    imageRunIds
                );
            }
            FrameResolution endFrame = generateFrame(
                task,
                clipIndex,
                buildFrameContinuityPrompt(shotPlan, lastFramePrompt, startFrame.prompt(), startFrame.videoInputUrl(), "last"),
                request.width(),
                request.height(),
                startFrame.videoInputUrl(),
                clipDurationSeconds,
                "last",
                "generated_end_frame_keyframe",
                imageRunIds
            );
            putExecutionContext(task, "imageRunId", firstNonBlank(startFrame.runId(), endFrame.runId()));
            putExecutionContext(task, "keyframeOutputUrl", startFrame.materialUrl());
            putExecutionContext(task, "keyframeRemoteSourceUrl", startFrame.sourceUrl());
            putExecutionContext(task, "firstFrameUrl", startFrame.videoInputUrl());
            putExecutionContext(task, "startFrameUrl", startFrame.videoInputUrl());
            putExecutionContext(task, "startFrameSourceType", startFrame.sourceType());
            putExecutionContext(task, "startFrameSourceUrl", startFrame.sourceUrl());
            putExecutionContext(task, "startFrameKeyframeUrl", startFrame.materialUrl());
            putExecutionContext(task, "startFrameKeyframeRemoteSourceUrl", startFrame.remoteUrl());
            putExecutionContext(task, "startFrameKeyframeRunId", startFrame.runId());
            putExecutionContext(task, "lastFrameImageRunId", endFrame.runId());
            putExecutionContext(task, "requestedLastFrameUrl", endFrame.videoInputUrl());
            putExecutionContext(task, "endFrameConstraintUrl", endFrame.videoInputUrl());
            putExecutionContext(task, "endFrameConstraintSourceType", endFrame.sourceType());
            putExecutionContext(task, "endFrameConstraintSourceUrl", endFrame.sourceUrl());
            putExecutionContext(task, "endFrameKeyframeUrl", endFrame.materialUrl());
            putExecutionContext(task, "endFrameKeyframeRemoteSourceUrl", endFrame.remoteUrl());
            putExecutionContext(task, "endFrameKeyframeRunId", endFrame.runId());
            putClipFrameExecutionContext(
                task,
                clipIndex,
                buildClipFrameContext(shotPlan, clipIndex, clipDurationSeconds, startFrame, endFrame, "", "", "", "")
            );
            taskRepository.save(task);
            executionCoordinator.recordTrace(task, TaskStage.PLANNING.code(), "planning.clip_frames_resolved", "当前分镜首尾帧约束已就绪。", "INFO", Map.of(
                "clipIndex", clipIndex,
                "clipCount", request.shotPlans().size(),
                "startFrameUrl", startFrame.videoInputUrl(),
                "startFrameSourceType", startFrame.sourceType(),
                "startFrameSourceUrl", startFrame.sourceUrl(),
                "endFrameConstraintUrl", endFrame.videoInputUrl(),
                "endFrameConstraintSourceType", endFrame.sourceType(),
                "endFrameConstraintSourceUrl", endFrame.sourceUrl()
            ));
            statusStageService.recordStageRun(
                task,
                runContext,
                100 + clipIndex,
                TaskStage.PLANNING.code(),
                clipIndex,
                buildPlanningStageRequest(task, clipPrompt, firstFramePrompt, lastFramePrompt, clipDurationSeconds),
                buildPlanningStageResponse(startFrame, endFrame, reusePreviousLastFrame)
            );
            if (index == Math.max(0, request.renderStartIndex() - 1)) {
                statusStageService.updateStatus(task, runContext, TaskStatus.RENDERING.value(), 55, TaskStage.RENDER.code(), "task.rendering", "任务开始按分镜生成视频输出。");
            } else {
                task.setProgress(Math.min(94, 55 + (int) Math.round(35.0 * index / Math.max(1, request.shotPlans().size()))));
                taskRepository.save(task);
            }
            Map<String, Object> videoRequest = runtimeSupport.buildVideoRunRequest(
                task,
                clipIndex,
                clipPrompt,
                request.videoSize(),
                clipDurationSeconds,
                clipMinDuration,
                clipMaxDuration,
                startFrame.videoInputUrl(),
                endFrame.videoInputUrl()
            );
            Map<String, Object> videoRun = generationApplicationService.createRun(videoRequest);
            videoRun = awaitCompletedVideoRun(videoRun);
            runtimeSupport.assertTaskStillActive(task);
            Map<String, Object> videoResult = resultMap(videoRun);
            Map<String, Object> videoMetadata = mapValue(videoResult.get("metadata"));
            String extractedLastFrameUrl = artifactAssembler.extractLastFrameUrl(videoResult);
            String providerRequestedLastFrameUrl = stringValue(videoMetadata.get("requestedLastFrameUrl"));
            String resolvedFirstFrameUrl = firstNonBlank(stringValue(videoMetadata.get("firstFrameUrl")), startFrame.videoInputUrl());
            String resolvedLastFrameUrl = firstNonBlank(
                extractedLastFrameUrl,
                providerRequestedLastFrameUrl,
                endFrame.videoInputUrl()
            );
            String resolvedLastFrameSourceType = resolvedLastFrameSourceType(extractedLastFrameUrl, providerRequestedLastFrameUrl, endFrame.videoInputUrl());
            String resolvedLastFrameSourceUrl = resolvedLastFrameSourceUrl(extractedLastFrameUrl, providerRequestedLastFrameUrl, endFrame.videoInputUrl());
            artifactAssembler.normalizeOptionalTaskArtifact(
                task,
                resolvedLastFrameUrl,
                TaskArtifactNaming.lastFrameFileName(clipIndex, fileExtOrDefault(fileNameFromUrl(resolvedLastFrameUrl), "png"))
            );
            putExecutionContext(task, "videoRunId", stringValue(videoRun.get("id")));
            putExecutionContext(task, "videoOutputUrl", stringValue(videoResult.get("outputUrl")));
            putExecutionContext(task, "videoThumbnailUrl", stringValue(videoResult.get("thumbnailUrl")));
            putExecutionContext(task, "firstFrameUrl", resolvedFirstFrameUrl);
            putExecutionContext(task, "startFrameUrl", resolvedFirstFrameUrl);
            putExecutionContext(task, "lastFrameUrl", resolvedLastFrameUrl);
            putExecutionContext(task, "lastFrameSourceType", resolvedLastFrameSourceType);
            putExecutionContext(task, "lastFrameSourceUrl", resolvedLastFrameSourceUrl);
            putExecutionContext(task, "requestedLastFrameUrl", endFrame.videoInputUrl());
            putExecutionContext(task, "videoRemoteTaskId", stringValue(videoMetadata.get("taskId")));
            putExecutionContext(task, "videoRemoteSourceUrl", stringValue(videoMetadata.get("remoteSourceUrl")));
            putClipFrameExecutionContext(
                task,
                clipIndex,
                buildClipFrameContext(
                    shotPlan,
                    clipIndex,
                    clipDurationSeconds,
                    startFrame,
                    endFrame,
                    stringValue(videoRun.get("id")),
                    firstNonBlank(stringValue(videoResult.get("outputUrl")), stringValue(videoMetadata.get("remoteSourceUrl"))),
                    resolvedLastFrameUrl,
                    resolvedLastFrameSourceType
                )
            );
            taskRepository.save(task);
            Map<String, Object> videoModelCall = statusStageService.createModelCall(task, TaskStage.RENDER.code(), "generation.video", videoRequest, videoRun, videoResult, clipIndex, GenerationModelKinds.VIDEO);
            executionCoordinator.recordModelCall(task, videoModelCall);
            statusStageService.recordRunCallChain(task, TaskStage.RENDER.code(), videoRun, videoResult);
            Map<String, Object> videoMaterial = artifactAssembler.createVideoMaterial(task, videoRun, videoResult, clipIndex, clipDurationSeconds);
            executionCoordinator.recordMaterial(task, videoMaterial);
            putExecutionContext(task, "videoOutputUrl", stringValue(videoMaterial.get("fileUrl")));
            putClipFrameExecutionContext(
                task,
                clipIndex,
                buildClipFrameContext(
                    shotPlan,
                    clipIndex,
                    clipDurationSeconds,
                    startFrame,
                    endFrame,
                    stringValue(videoRun.get("id")),
                    stringValue(videoMaterial.get("fileUrl")),
                    resolvedLastFrameUrl,
                    resolvedLastFrameSourceType
                )
            );
            latestVideoOutputUrl = stringValue(videoMaterial.get("fileUrl"));
            task.setCompletedOutputCount(Math.max(task.completedOutputCount(), clipIndex));
            taskRepository.save(task);
            Map<String, Object> videoOutput = artifactAssembler.createResult(
                task,
                videoRun,
                videoResult,
                videoMaterial,
                startFrame.material(),
                videoModelCall,
                resolvedLastFrameUrl,
                clipIndex,
                clipDurationSeconds,
                clipMinDuration,
                clipMaxDuration
            );
            executionCoordinator.recordResult(task, videoOutput);
            statusStageService.recordStageRun(
                task,
                runContext,
                200 + clipIndex,
                TaskStage.RENDER.code(),
                clipIndex,
                buildRenderStageRequest(startFrame, endFrame, clipDurationSeconds),
                buildRenderStageResponse(
                    videoRun,
                    videoMaterial,
                    videoMetadata,
                    resolvedFirstFrameUrl,
                    resolvedLastFrameUrl,
                    resolvedLastFrameSourceType,
                    endFrame.videoInputUrl()
                )
            );
            executionCoordinator.recordTrace(task, TaskStage.RENDER.code(), "render.clip_completed", "当前分镜片段已生成完成。", "INFO", Map.of(
                "clipIndex", clipIndex,
                "clipCount", request.shotPlans().size(),
                "outputUrl", stringValue(videoMaterial.get("fileUrl")),
                "firstFrameUrl", resolvedFirstFrameUrl,
                "firstFrameSourceType", startFrame.sourceType(),
                "requestedLastFrameUrl", endFrame.videoInputUrl(),
                "requestedLastFrameSourceType", endFrame.sourceType(),
                "lastFrameUrl", resolvedLastFrameUrl,
                "lastFrameSourceType", resolvedLastFrameSourceType
            ));
            videoRunIds.add(stringValue(videoRun.get("id")));
            previousClipLastFrameUrl = stringValue(resolvedLastFrameUrl);
            joinStageService.scheduleJoin(task);
        }

        runtimeSupport.assertTaskStillActive(task);
        if (latestVideoOutputUrl.isBlank()) {
            latestVideoOutputUrl = resolveLatestVideoOutputUrl(task);
        }
        joinStageService.scheduleJoin(task);
        putExecutionContext(task, "clipImageRunIds", mergeStringListContext(task.executionContext().get("clipImageRunIds"), imageRunIds));
        putExecutionContext(task, "clipVideoRunIds", mergeStringListContext(task.executionContext().get("clipVideoRunIds"), videoRunIds));
        task.setCompletedOutputCount(request.shotPlans().size());
        putExecutionContext(task, "resumeExistingOutputCount", null);
        putExecutionContext(task, "resumeExistingClipIndices", null);
        putExecutionContext(task, "resumeRenderFromClipIndex", null);
        putExecutionContext(task, "attemptResumeFromStage", null);
        putExecutionContext(task, "attemptResumeFromClipIndex", null);
        taskRepository.save(task);
        return new RenderStageResult(imageRunIds, videoRunIds, latestVideoOutputUrl, request.shotPlans().size());
    }

    /**
     * 处理awaitCompleted视频运行。
     * @param initialRun 初始运行值
     * @return 处理结果
     */
    Map<String, Object> awaitCompletedVideoRun(Map<String, Object> initialRun) {
        String currentStatus = normalizedRunStatus(initialRun);
        if (!isVideoRunActive(currentStatus)) {
            assertVideoRunSucceeded(initialRun, currentStatus);
            return initialRun;
        }
        String runId = stringValue(initialRun.get("id"));
        if (runId.isBlank()) {
            throw new IllegalStateException("video run is active but missing run id");
        }
        Map<String, Object> currentRun = initialRun;
        for (int poll = 0; poll < videoRunMaxPolls; poll++) {
            currentRun = generationApplicationService.getRun(runId);
            currentStatus = normalizedRunStatus(currentRun);
            if (!isVideoRunActive(currentStatus)) {
                assertVideoRunSucceeded(currentRun, currentStatus);
                return currentRun;
            }
            sleepBeforeNextVideoPoll();
        }
        throw new IllegalStateException(
            "video run wait timeout: runId=" + runId + ", status=" + currentStatus + ", maxPolls=" + videoRunMaxPolls
        );
    }

    private String buildFrameContinuityPrompt(
        TaskStoryboardPlanner.StoryboardShotPlan shotPlan,
        String prompt,
        String startFramePrompt,
        String referenceImageUrl,
        String frameRole
    ) {
        String basePrompt = firstNonBlank(
            prompt,
            shotPlan == null ? "" : shotPlan.lastFramePrompt(),
            shotPlan == null ? "" : shotPlan.firstFramePrompt(),
            shotPlan == null ? "" : shotPlan.videoPrompt(),
            shotPlan == null ? "" : shotPlan.scene()
        );
        if (!"last".equalsIgnoreCase(stringValue(frameRole)) || stringValue(referenceImageUrl).isBlank()) {
            return basePrompt;
        }
        List<String> parts = new ArrayList<>();
        parts.add("你现在要生成同一镜头连续动作后的尾帧，必须严格沿用参考图已经确定的同一场景、同一机位体系、同一空间锚点、同一人物外观与服装、同一道具位置关系，禁止漂移到新的场景。");
        parts.add("尾帧只允许在参考首帧基础上推进人物动作状态、视线方向、手部位置或道具使用结果，禁止新增、删除或替换背景布局、门窗桌椅书架等场景元素。");
        String resolvedStartFramePrompt = firstNonBlank(
            startFramePrompt,
            shotPlan == null ? "" : shotPlan.firstFramePrompt(),
            shotPlan == null ? "" : shotPlan.lastFramePrompt()
        );
        if (!resolvedStartFramePrompt.isBlank()) {
            parts.add("参考首帧描述：" + resolvedStartFramePrompt);
            parts.add("场景锁定基准：" + resolvedStartFramePrompt);
        }
        if (shotPlan != null && !shotPlan.scene().isBlank()) {
            parts.add("场景锚点：" + shotPlan.scene());
        }
        if (shotPlan != null && !shotPlan.cameraMovement().isBlank() && !"static".equalsIgnoreCase(shotPlan.cameraMovement())) {
            parts.add("运镜：" + shotPlan.cameraMovement());
        }
        if (!basePrompt.isBlank()) {
            parts.add("尾帧目标：" + basePrompt);
        }
        return String.join("\n", parts);
    }

    /**
     * 生成关键帧并落库素材。
     * @param task 要处理的任务对象
     * @param clipIndex 片段索引值
     * @param prompt 提示词值
     * @param width width值
     * @param height height值
     * @param referenceImageUrl reference图像URL值
     * @param durationSeconds 时长Seconds值
     * @param frameRole frameRole值
     * @param sourceType 来源类型值
     * @param imageRunIds 图像运行标识列表值
     * @return 处理结果
     */
    private FrameResolution generateFrame(
        TaskRecord task,
        int clipIndex,
        String prompt,
        int width,
        int height,
        String referenceImageUrl,
        int durationSeconds,
        String frameRole,
        String sourceType,
        List<String> imageRunIds
    ) {
        Map<String, Object> imageRequest = runtimeSupport.buildImageRunRequest(
            task,
            clipIndex,
            prompt,
            width,
            height,
            referenceImageUrl,
            durationSeconds,
            frameRole
        );
        Map<String, Object> imageRun = generationApplicationService.createRun(imageRequest);
        runtimeSupport.assertTaskStillActive(task);
        Map<String, Object> imageResult = resultMap(imageRun);
        Map<String, Object> imageMetadata = mapValue(imageResult.get("metadata"));
        String keyframeSourceUrl = firstNonBlank(
            stringValue(imageMetadata.get("remoteSourceUrl")),
            stringValue(imageResult.get("outputUrl"))
        );
        Map<String, Object> imageModelCall = statusStageService.createModelCall(
            task,
            TaskStage.PLANNING.code(),
            "generation.image",
            imageRequest,
            imageRun,
            imageResult,
            clipIndex,
            GenerationModelKinds.IMAGE
        );
        executionCoordinator.recordModelCall(task, imageModelCall);
        statusStageService.recordRunCallChain(task, TaskStage.PLANNING.code(), imageRun, imageResult);
        Map<String, Object> imageMaterial = artifactAssembler.createImageMaterial(task, imageRun, imageResult, clipIndex, frameRole);
        executionCoordinator.recordMaterial(task, imageMaterial);
        imageRunIds.add(stringValue(imageRun.get("id")));
        return new FrameResolution(
            stringValue(prompt),
            stringValue(frameRole),
            stringValue(sourceType),
            keyframeSourceUrl,
            stringValue(imageMaterial.get("fileUrl")),
            firstNonBlank(stringValue(imageMaterial.get("remoteUrl")), keyframeSourceUrl),
            firstNonBlank(keyframeSourceUrl, stringValue(imageMaterial.get("remoteUrl")), stringValue(imageMaterial.get("fileUrl"))),
            stringValue(imageRun.get("id")),
            imageMaterial
        );
    }

    /**
     * 复用已有关键帧作为当前输入。
     * @param task 要处理的任务对象
     * @param clipIndex 片段索引值
     * @param sourceUrl 来源URL值
     * @param frameRole frameRole值
     * @param sourceType 来源类型值
     * @return 处理结果
     */
    private FrameResolution reuseFrame(TaskRecord task, int clipIndex, String sourceUrl, String frameRole, String sourceType) {
        Map<String, Object> imageMaterial = artifactAssembler.createReferenceFrameMaterial(task, clipIndex, sourceUrl, frameRole);
        executionCoordinator.recordMaterial(task, imageMaterial);
        String remoteUrl = firstNonBlank(stringValue(imageMaterial.get("remoteUrl")), sourceUrl);
        return new FrameResolution(
            "",
            stringValue(frameRole),
            stringValue(sourceType),
            stringValue(sourceUrl),
            stringValue(imageMaterial.get("fileUrl")),
            remoteUrl,
            firstNonBlank(remoteUrl, stringValue(imageMaterial.get("fileUrl"))),
            "",
            imageMaterial
        );
    }

    /**
     * 构建planning阶段请求。
     * @param task 要处理的任务对象
     * @param clipPrompt 片段提示词值
     * @param firstFramePrompt 首帧提示词值
     * @param lastFramePrompt 尾帧提示词值
     * @param clipDurationSeconds 片段时长Seconds值
     * @return 处理结果
     */
    private Map<String, Object> buildPlanningStageRequest(
        TaskRecord task,
        String clipPrompt,
        String firstFramePrompt,
        String lastFramePrompt,
        int clipDurationSeconds
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("aspectRatio", task.aspectRatio());
        request.put("clipPrompt", truncateText(clipPrompt, 160));
        request.put("firstFramePrompt", truncateText(firstFramePrompt, 160));
        request.put("lastFramePrompt", truncateText(lastFramePrompt, 160));
        request.put("targetDurationSeconds", clipDurationSeconds);
        return request;
    }

    /**
     * 构建planning阶段响应。
     * @param startFrame 首帧处理结果值
     * @param endFrame 尾帧处理结果值
     * @param reusedPreviousStart 是否复用上一镜尾帧值
     * @return 处理结果
     */
    private Map<String, Object> buildPlanningStageResponse(FrameResolution startFrame, FrameResolution endFrame, boolean reusedPreviousStart) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", reusedPreviousStart ? "已复用上一镜尾帧作为首帧，并生成当前镜头尾帧关键画面" : "当前镜头首尾关键画面已生成");
        response.put("imageRunId", firstNonBlank(startFrame.runId(), endFrame.runId()));
        response.put("imageUrl", startFrame.materialUrl());
        response.put("remoteImageUrl", startFrame.videoInputUrl());
        response.put("startFrameUrl", startFrame.videoInputUrl());
        response.put("startFrameSourceType", startFrame.sourceType());
        response.put("startFrameSourceUrl", startFrame.sourceUrl());
        response.put("startFrameKeyframeUrl", startFrame.materialUrl());
        response.put("startFrameImageRunId", startFrame.runId());
        response.put("endFrameConstraintUrl", endFrame.videoInputUrl());
        response.put("endFrameSourceType", endFrame.sourceType());
        response.put("endFrameSourceUrl", endFrame.sourceUrl());
        response.put("endFrameKeyframeUrl", endFrame.materialUrl());
        response.put("endFrameImageRunId", endFrame.runId());
        return response;
    }

    /**
     * 构建render阶段请求。
     * @param startFrame 首帧处理结果值
     * @param endFrame 尾帧处理结果值
     * @param clipDurationSeconds 片段时长Seconds值
     * @return 处理结果
     */
    private Map<String, Object> buildRenderStageRequest(FrameResolution startFrame, FrameResolution endFrame, int clipDurationSeconds) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("imageRunId", firstNonBlank(startFrame.runId(), endFrame.runId()));
        request.put("posterUrl", startFrame.materialUrl());
        request.put("targetDurationSeconds", clipDurationSeconds);
        request.put("firstFrameUrl", startFrame.videoInputUrl());
        request.put("firstFrameSourceType", startFrame.sourceType());
        request.put("requestedLastFrameUrl", endFrame.videoInputUrl());
        request.put("requestedLastFrameSourceType", endFrame.sourceType());
        return request;
    }

    /**
     * 构建render阶段响应。
     * @param videoRun 视频运行值
     * @param videoMaterial 视频素材值
     * @param videoMetadata 视频Metadata值
     * @param resolvedFirstFrameUrl 首帧URL值
     * @param resolvedLastFrameUrl 尾帧URL值
     * @param resolvedLastFrameSourceType 尾帧来源类型值
     * @param requestedLastFrameUrl 请求尾帧URL值
     * @return 处理结果
     */
    private Map<String, Object> buildRenderStageResponse(
        Map<String, Object> videoRun,
        Map<String, Object> videoMaterial,
        Map<String, Object> videoMetadata,
        String resolvedFirstFrameUrl,
        String resolvedLastFrameUrl,
        String resolvedLastFrameSourceType,
        String requestedLastFrameUrl
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("videoRunId", stringValue(videoRun.get("id")));
        response.put("outputUrl", stringValue(videoMaterial.get("fileUrl")));
        response.put("remoteTaskId", stringValue(videoMetadata.get("taskId")));
        response.put("firstFrameUrl", resolvedFirstFrameUrl);
        response.put("requestedLastFrameUrl", requestedLastFrameUrl);
        response.put("lastFrameUrl", resolvedLastFrameUrl);
        response.put("lastFrameSourceType", resolvedLastFrameSourceType);
        return response;
    }

    /**
     * 构建片段Frame上下文。
     * @param shotPlan 分镜片段规划值
     * @param clipIndex 片段索引值
     * @param clipDurationSeconds 片段时长Seconds值
     * @param startFrame 首帧处理结果值
     * @param endFrame 尾帧处理结果值
     * @param videoRunId 视频运行标识值
     * @param videoOutputUrl 视频输出URL值
     * @param resolvedLastFrameUrl 解析后的尾帧URL值
     * @param resolvedLastFrameSourceType 解析后的尾帧来源类型值
     * @return 处理结果
     */
    private Map<String, Object> buildClipFrameContext(
        TaskStoryboardPlanner.StoryboardShotPlan shotPlan,
        int clipIndex,
        int clipDurationSeconds,
        FrameResolution startFrame,
        FrameResolution endFrame,
        String videoRunId,
        String videoOutputUrl,
        String resolvedLastFrameUrl,
        String resolvedLastFrameSourceType
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("clipIndex", clipIndex);
        row.put("shotLabel", shotPlan.shotLabel());
        row.put("scene", shotPlan.scene());
        row.put("targetDurationSeconds", clipDurationSeconds);
        row.put("startFramePrompt", firstNonBlank(startFrame.prompt(), shotPlan.firstFramePrompt(), shotPlan.lastFramePrompt()));
        row.put("startFrameUrl", startFrame.videoInputUrl());
        row.put("startFrameSourceType", startFrame.sourceType());
        row.put("startFrameSourceUrl", startFrame.sourceUrl());
        row.put("startFrameKeyframeUrl", startFrame.materialUrl());
        row.put("startFrameKeyframeRemoteSourceUrl", startFrame.remoteUrl());
        row.put("startFrameKeyframeRunId", startFrame.runId());
        row.put("endFramePrompt", firstNonBlank(endFrame.prompt(), shotPlan.lastFramePrompt()));
        row.put("endFrameConstraintUrl", endFrame.videoInputUrl());
        row.put("endFrameSourceType", endFrame.sourceType());
        row.put("endFrameSourceUrl", endFrame.sourceUrl());
        row.put("endFrameKeyframeUrl", endFrame.materialUrl());
        row.put("endFrameKeyframeRemoteSourceUrl", endFrame.remoteUrl());
        row.put("endFrameKeyframeRunId", endFrame.runId());
        row.put("videoRunId", videoRunId);
        row.put("videoOutputUrl", videoOutputUrl);
        row.put("returnedLastFrameUrl", resolvedLastFrameUrl);
        row.put("returnedLastFrameSourceType", resolvedLastFrameSourceType);
        return row;
    }

    /**
     * 写入片段Frame上下文。
     * @param task 要处理的任务对象
     * @param clipIndex 片段索引值
     * @param clipFrameContext 片段Frame上下文值
     */
    private void putClipFrameExecutionContext(TaskRecord task, int clipIndex, Map<String, Object> clipFrameContext) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (task.executionContext() != null && task.executionContext().get("clipFrameContexts") instanceof List<?> existingList) {
            for (Object item : existingList) {
                if (item instanceof Map<?, ?> rawMap) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                        row.put(stringValue(entry.getKey()), entry.getValue());
                    }
                    if (intValue(row.get("clipIndex"), 0) != clipIndex) {
                        rows.add(row);
                    }
                }
            }
        }
        rows.add(clipFrameContext);
        rows.sort((left, right) -> Integer.compare(intValue(left.get("clipIndex"), 0), intValue(right.get("clipIndex"), 0)));
        putExecutionContext(task, "clipFrameContexts", rows);
    }

    /**
     * 解析尾帧来源类型。
     * @param extractedLastFrameUrl 提取到的尾帧URL值
     * @param providerRequestedLastFrameUrl provider请求尾帧URL值
     * @param requestedLastFrameUrl 请求尾帧URL值
     * @return 处理结果
     */
    private String resolvedLastFrameSourceType(String extractedLastFrameUrl, String providerRequestedLastFrameUrl, String requestedLastFrameUrl) {
        if (!stringValue(extractedLastFrameUrl).isBlank()) {
            return "video_result_last_frame";
        }
        if (!stringValue(providerRequestedLastFrameUrl).isBlank()) {
            return "video_requested_last_frame";
        }
        if (!stringValue(requestedLastFrameUrl).isBlank()) {
            return "end_frame_keyframe_fallback";
        }
        return "";
    }

    /**
     * 解析尾帧来源URL。
     * @param extractedLastFrameUrl 提取到的尾帧URL值
     * @param providerRequestedLastFrameUrl provider请求尾帧URL值
     * @param requestedLastFrameUrl 请求尾帧URL值
     * @return 处理结果
     */
    private String resolvedLastFrameSourceUrl(String extractedLastFrameUrl, String providerRequestedLastFrameUrl, String requestedLastFrameUrl) {
        return firstNonBlank(
            extractedLastFrameUrl,
            providerRequestedLastFrameUrl,
            requestedLastFrameUrl
        );
    }

    /**
     * 处理结果Map。
     * @param run 运行值
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resultMap(Map<String, Object> run) {
        Object result = run.get("result");
        if (result instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    /**
     * 映射值。
     * @param value 待处理的值
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    /**
     * 处理解析Latest视频输出URL。
     * @param task 要处理的任务对象
     * @return 处理结果
     */
    private String resolveLatestVideoOutputUrl(TaskRecord task) {
        int latestClipIndex = 0;
        String latestOutputUrl = "";
        for (Map<String, Object> output : task.outputsView()) {
            if (!TaskResultTypes.isPrimaryVideo(output.get("resultType"))) {
                continue;
            }
            int clipIndex = intValue(output.get("clipIndex"), 0);
            if (clipIndex >= latestClipIndex) {
                latestClipIndex = clipIndex;
                latestOutputUrl = firstNonBlank(stringValue(output.get("downloadUrl")), stringValue(output.get("previewUrl")));
            }
        }
        return latestOutputUrl;
    }

    /**
     * 处理put执行Context。
     * @param task 要处理的任务对象
     * @param key key值
     * @param value 待处理的值
     */
    private void putExecutionContext(TaskRecord task, String key, Object value) {
        if (task.executionContext() == null) {
            task.setExecutionContext(new LinkedHashMap<>());
        }
        if (value == null) {
            task.executionContext().remove(key);
            return;
        }
        String normalized = value instanceof String str ? str.trim() : null;
        if (normalized != null && normalized.isEmpty()) {
            task.executionContext().remove(key);
            return;
        }
        task.executionContext().put(key, value);
    }

    /**
     * 处理mergeString列表Context。
     * @param existing existing值
     * @param appended appended值
     * @return 处理结果
     */
    private List<String> mergeStringListContext(Object existing, List<String> appended) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (existing instanceof List<?> list) {
            for (Object item : list) {
                String value = stringValue(item);
                if (!value.isBlank()) {
                    merged.add(value);
                }
            }
        }
        for (String item : appended) {
            String value = stringValue(item);
            if (!value.isBlank()) {
                merged.add(value);
            }
        }
        return new ArrayList<>(merged);
    }

    /**
     * 处理文件NameFromURL。
     * @param url URL值
     * @return 处理结果
     */
    private String fileNameFromUrl(String url) {
        String normalized = stringValue(url)
            .replaceAll("[?#].*$", "")
            .replaceAll("/+$", "");
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    /**
     * 处理文件ExtOr默认。
     * @param fileName 文件Name值
     * @param fallback 兜底值
     * @return 处理结果
     */
    private String fileExtOrDefault(String fileName, String fallback) {
        String resolved = fileExt(fileName);
        return resolved.isBlank() ? fallback : resolved;
    }

    /**
     * 处理文件Ext。
     * @param fileName 文件Name值
     * @return 处理结果
     */
    private String fileExt(String fileName) {
        String normalized = stringValue(fileName).replaceAll("[?#].*$", "");
        int index = normalized.lastIndexOf('.');
        if (index < 0 || index == normalized.length() - 1) {
            return "";
        }
        String candidate = normalized.substring(index + 1).toLowerCase();
        if (!candidate.matches("[a-z0-9]{1,10}")) {
            return "";
        }
        return candidate;
    }

    /**
     * 处理首个非空白。
     * @param values 值
     * @return 处理结果
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
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
     * 处理string值。
     * @param value 待处理的值
     * @return 处理结果
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 处理int值。
     * @param value 待处理的值
     * @param defaultValue 默认值
     * @return 处理结果
     */
    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 处理normalized运行状态。
     * @param run 运行值
     * @return 处理结果
     */
    private String normalizedRunStatus(Map<String, Object> run) {
        return stringValue(run == null ? null : run.get("status")).toLowerCase();
    }

    /**
     * 检查是否视频运行Active。
     * @param status 状态值
     * @return 是否满足条件
     */
    private boolean isVideoRunActive(String status) {
        return GenerationRunStatuses.isActive(status);
    }

    /**
     * 处理assert视频运行Succeeded。
     * @param run 运行值
     * @param status 状态值
     */
    private void assertVideoRunSucceeded(Map<String, Object> run, String status) {
        if (GenerationRunStatuses.isSuccessful(status)) {
            return;
        }
        Map<String, Object> result = resultMap(run);
        Map<String, Object> metadata = mapValue(result.get("metadata"));
        String message = firstNonBlank(
            stringValue(result.get("error")),
            stringValue(metadata.get("taskMessage")),
            stringValue(metadata.get("message"))
        );
        throw new IllegalStateException(
            "video run did not complete successfully: runId=" + stringValue(run == null ? null : run.get("id"))
                + ", status=" + status
                + (message.isBlank() ? "" : ", error=" + message)
        );
    }

    /**
     * 处理sleepBeforeNext视频Poll。
     */
    private void sleepBeforeNextVideoPoll() {
        if (videoRunPollIntervalMillis <= 0L) {
            return;
        }
        try {
            Thread.sleep(videoRunPollIntervalMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("video run wait interrupted", ex);
        }
    }

    /**
     * 渲染阶段请求。
     * @param reuseStoryboard reuse分镜值
     * @param renderStartIndex renderStart索引值
     * @param completedClipCount completed片段数量值
     * @param requestedResumeStage requestedResume阶段值
     * @param requestedResumeClipIndex requestedResume片段索引值
     * @param existingVideoClipIndices existing视频片段Indices值
     * @param shotPlans shotPlans值
     * @param clipDurationPlan 片段时长规划值
     * @param width width值
     * @param height height值
     * @param durationSeconds 时长Seconds值
     * @param videoSize 视频Size值
     * @param previousClipLastFrameUrl previous片段LastFrameURL值
     * @return 处理结果
     */
    record RenderStageRequest(
        boolean reuseStoryboard,
        int renderStartIndex,
        int completedClipCount,
        String requestedResumeStage,
        int requestedResumeClipIndex,
        List<Integer> existingVideoClipIndices,
        List<TaskStoryboardPlanner.StoryboardShotPlan> shotPlans,
        List<int[]> clipDurationPlan,
        int width,
        int height,
        int durationSeconds,
        String videoSize,
        String previousClipLastFrameUrl
    ) {
    }

    /**
     * 渲染阶段结果。
     * @param imageRunIds 图像运行标识列表值
     * @param videoRunIds 视频运行标识列表值
     * @param latestVideoOutputUrl latest视频输出URL值
     * @param clipCount 片段数量值
     * @return 处理结果
     */
    record RenderStageResult(
        List<String> imageRunIds,
        List<String> videoRunIds,
        String latestVideoOutputUrl,
        int clipCount
    ) {
    }

    /**
     * Frame处理结果。
     * @param prompt 提示词值
     * @param frameRole frameRole值
     * @param sourceType 来源类型值
     * @param sourceUrl 来源URL值
     * @param materialUrl 素材URL值
     * @param remoteUrl 远程URL值
     * @param videoInputUrl 视频输入URL值
     * @param runId 运行标识值
     * @param material 素材值
     * @return 处理结果
     */
    private record FrameResolution(
        String prompt,
        String frameRole,
        String sourceType,
        String sourceUrl,
        String materialUrl,
        String remoteUrl,
        String videoInputUrl,
        String runId,
        Map<String, Object> material
    ) {
    }
}
