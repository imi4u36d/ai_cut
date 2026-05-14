package com.jiandou.api.workflow.web.dto;

public record CreateWorkflowRequest(
    String title,
    String transcriptText,
    String aspectRatio,
    String stylePreset,
    String textAnalysisModel,
    String imageModel,
    String videoModel,
    String videoSize,
    Integer keyframeSeed,
    Integer videoSeed,
    Integer seed,
    String durationMode,
    Integer minDurationSeconds,
    Integer maxDurationSeconds
) {}
