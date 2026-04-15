package com.jiandou.api.task.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务记录结构。
 * @param taskId 任务标识
 * @param taskType 任务类型值
 * @param title title值
 * @param description description值
 * @param aspectRatio aspectRatio值
 * @param minDurationSeconds 最小时长Seconds值
 * @param maxDurationSeconds 最大时长Seconds值
 * @param outputCount 输出数量值
 * @param sourcePrimaryAssetId 来源Primary素材标识值
 * @param sourceFileName 来源文件Name值
 * @param sourceAssetIds 来源素材标识列表值
 * @param sourceFileNames 来源文件Names值
 * @param requestPayload 请求负载值
 * @param context context值
 * @param introTemplate intro模板值
 * @param outroTemplate outro模板值
 * @param creativePrompt 创意提示词值
 * @param modelProvider 模型Provider值
 * @param executionMode 执行模式值
 * @param editingMode 编辑模式值
 * @param status 状态值
 * @param progress 进度值
 * @param errorCode errorCode值
 * @param errorMessage errorMessage值
 * @param planJson 规划Json值
 * @param retryCount 重试数量值
 * @param timezoneOffsetMinutes timezoneOffsetMinutes值
 * @param startedAt startedAt值
 * @param finishedAt finishedAt值
 * @param attempts attempts值
 * @param stageRuns 阶段Runs值
 */
public record TaskAggregate(
    String taskId,
    String taskType,
    String title,
    String description,
    String aspectRatio,
    int minDurationSeconds,
    int maxDurationSeconds,
    int outputCount,
    String sourcePrimaryAssetId,
    String sourceFileName,
    List<String> sourceAssetIds,
    List<String> sourceFileNames,
    Map<String, Object> requestPayload,
    Map<String, Object> context,
    String introTemplate,
    String outroTemplate,
    String creativePrompt,
    String modelProvider,
    String executionMode,
    String editingMode,
    TaskStatus status,
    int progress,
    String errorCode,
    String errorMessage,
    String planJson,
    int retryCount,
    int timezoneOffsetMinutes,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    List<TaskAttemptSnapshot> attempts,
    List<TaskStageRunSnapshot> stageRuns
) {
    public TaskAggregate {
        sourceAssetIds = sourceAssetIds == null ? new ArrayList<>() : new ArrayList<>(sourceAssetIds);
        sourceFileNames = sourceFileNames == null ? new ArrayList<>() : new ArrayList<>(sourceFileNames);
        requestPayload = requestPayload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(requestPayload);
        context = context == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context);
        attempts = attempts == null ? new ArrayList<>() : new ArrayList<>(attempts);
        stageRuns = stageRuns == null ? new ArrayList<>() : new ArrayList<>(stageRuns);
    }
}
