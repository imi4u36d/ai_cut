package com.jiandou.api.task.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建生成任务的请求体。
 * 这里保留现有 JSON 字段形态，避免影响前端调用。
 */
public record CreateGenerationTaskRequest(
    @NotBlank String title,
    String taskType,
    String assetType,
    String creativePrompt,
    String aspectRatio,
    String imageSize,
    String textAnalysisModel,
    String imageModel,
    String videoModel,
    String videoSize,
    Integer seed,
    Object videoDurationSeconds,
    Object outputCount,
    Integer minDurationSeconds,
    Integer maxDurationSeconds,
    String transcriptText,
    Boolean stopBeforeVideoGeneration,
    java.util.List<String> referenceImageUrls,
    java.util.List<String> referenceAssetIds
) {

    public CreateGenerationTaskRequest(
        String title,
        String creativePrompt,
        String aspectRatio,
        String textAnalysisModel,
        String imageModel,
        String videoModel,
        String videoSize,
        Integer seed,
        Object videoDurationSeconds,
        Object outputCount,
        Integer minDurationSeconds,
        Integer maxDurationSeconds,
        String transcriptText,
        Boolean stopBeforeVideoGeneration
    ) {
        this(
            title,
            null,
            null,
            creativePrompt,
            aspectRatio,
            null,
            textAnalysisModel,
            imageModel,
            videoModel,
            videoSize,
            seed,
            videoDurationSeconds,
            outputCount,
            minDurationSeconds,
            maxDurationSeconds,
            transcriptText,
            stopBeforeVideoGeneration,
            java.util.List.of(),
            java.util.List.of()
        );
    }
}
