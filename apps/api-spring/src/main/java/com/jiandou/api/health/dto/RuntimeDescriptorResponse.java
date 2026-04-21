package com.jiandou.api.health.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 运行时描述响应体。
 * @param ok ok值
 * @param runtime 运行时值
 */
public record RuntimeDescriptorResponse(
    boolean ok,
    RuntimeInfo runtime
) {

    /**
     * 处理运行时信息。
     * @param name name值
     * @param env env值
     * @param executionMode 执行模式值
     * @param databaseUrl 数据库URL值
     * @param modelProvider 模型Provider值
     * @param storageRoot storageRoot值
     * @param model 模型值
     * @param planningCapabilities 规划能力值
     * @return 处理结果
     */
    public record RuntimeInfo(
        String name,
        String env,
        @JsonProperty("execution_mode") String executionMode,
        @JsonProperty("database_url") String databaseUrl,
        @JsonProperty("model_provider") String modelProvider,
        @JsonProperty("storage_root") String storageRoot,
        ModelInfo model,
        @JsonProperty("planning_capabilities") PlanningCapabilities planningCapabilities
    ) {
    }

    /**
     * 处理模型信息。
     * @param provider provider值
     * @param primaryModel primary模型值
     * @param textAnalysisProvider 文本分析Provider值
     * @param textAnalysisModel 文本分析模型值
     * @param visionModel 视觉模型值
     * @param endpointHost endpointHost值
     * @param apiKeyPresent APIKeyPresent值
     * @param ready ready值
     * @param temperature temperature值
     * @param maxTokens 最大Tokens值
     * @param configSource 配置来源值
     * @param configErrors 配置Errors值
     * @return 处理结果
     */
    public record ModelInfo(
        String provider,
        @JsonProperty("primary_model") String primaryModel,
        @JsonProperty("text_analysis_provider") String textAnalysisProvider,
        @JsonProperty("text_analysis_model") String textAnalysisModel,
        @JsonProperty("vision_model") String visionModel,
        @JsonProperty("endpoint_host") String endpointHost,
        @JsonProperty("api_key_present") boolean apiKeyPresent,
        boolean ready,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens,
        @JsonProperty("config_source") String configSource,
        @JsonProperty("config_errors") List<String> configErrors
    ) {
    }

    /**
     * 处理规划能力。
     * @param timedTranscriptSupported timed正文Supported值
     * @param transcriptSemanticPlanning 正文Semantic规划值
     * @param visualContentAnalysis visualContent分析值
     * @param visualEventReasoning visual事件Reasoning值
     * @param subtitleVisualFusion subtitleVisualFusion值
     * @param audioPeakSignal audioPeakSignal值
     * @param sceneBoundarySignal sceneBoundarySignal值
     * @param fusionTimelinePlanning fusion时间线规划值
     * @param fallbackHeuristicEnabled 兜底HeuristicEnabled值
     * @return 处理结果
     */
    public record PlanningCapabilities(
        @JsonProperty("timed_transcript_supported") boolean timedTranscriptSupported,
        @JsonProperty("transcript_semantic_planning") boolean transcriptSemanticPlanning,
        @JsonProperty("visual_content_analysis") boolean visualContentAnalysis,
        @JsonProperty("visual_event_reasoning") boolean visualEventReasoning,
        @JsonProperty("subtitle_visual_fusion") boolean subtitleVisualFusion,
        @JsonProperty("audio_peak_signal") boolean audioPeakSignal,
        @JsonProperty("scene_boundary_signal") boolean sceneBoundarySignal,
        @JsonProperty("fusion_timeline_planning") boolean fusionTimelinePlanning,
        @JsonProperty("fallback_heuristic_enabled") boolean fallbackHeuristicEnabled
    ) {
    }
}
