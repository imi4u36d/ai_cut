package com.jiandou.api.workflow.web.dto;

/**
 * 工作流设置更新请求。
 */
public record UpdateWorkflowSettingsRequest(
    String aspectRatio,
    String stylePreset,
    String textAnalysisModel,
    String imageModel,
    String videoModel,
    String videoSize,
    Integer keyframeSeed,
    Integer videoSeed,
    String durationMode,
    Integer minDurationSeconds,
    Integer maxDurationSeconds
) {}
