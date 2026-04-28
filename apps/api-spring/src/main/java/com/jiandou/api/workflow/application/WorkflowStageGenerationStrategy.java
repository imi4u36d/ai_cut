package com.jiandou.api.workflow.application;

import com.jiandou.api.generation.GenerationModelKinds;
import com.jiandou.api.generation.GenerationRunKinds;
import com.jiandou.api.generation.runtime.MediaProviderProfile;
import com.jiandou.api.generation.runtime.ModelRuntimeProfile;
import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import com.jiandou.api.workflow.WorkflowConstants;
import com.jiandou.api.workflow.infrastructure.mybatis.StageWorkflowEntity;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class WorkflowStageGenerationStrategy {

    private final String stage;
    private final String strategyKey;
    private final String runKind;
    private final String modelKind;
    private final String requestedModel;
    private final String provider;
    private final String providerModel;
    private final boolean supportsSeed;
    private final boolean supportsImageDataUriReferences;
    private final String referenceInputMode;
    private final String generationMode;
    private final String configSource;

    WorkflowStageGenerationStrategy(
        String stage,
        String strategyKey,
        String runKind,
        String modelKind,
        String requestedModel,
        String provider,
        String providerModel,
        boolean supportsSeed,
        boolean supportsImageDataUriReferences,
        String referenceInputMode,
        String generationMode,
        String configSource
    ) {
        this.stage = blankTo(stage, "");
        this.strategyKey = blankTo(strategyKey, "");
        this.runKind = blankTo(runKind, "");
        this.modelKind = blankTo(modelKind, "");
        this.requestedModel = blankTo(requestedModel, "");
        this.provider = blankTo(provider, "");
        this.providerModel = blankTo(providerModel, "");
        this.supportsSeed = supportsSeed;
        this.supportsImageDataUriReferences = supportsImageDataUriReferences;
        this.referenceInputMode = blankTo(referenceInputMode, "");
        this.generationMode = blankTo(generationMode, "");
        this.configSource = blankTo(configSource, "");
    }

    String runKind() {
        return runKind;
    }

    String requestedModel() {
        return requestedModel;
    }

    boolean supportsSeed() {
        return supportsSeed;
    }

    boolean supportsImageDataUriReferences() {
        return supportsImageDataUriReferences;
    }

    Map<String, Object> modelSection(String textAnalysisModel) {
        Map<String, Object> model = new LinkedHashMap<>();
        if (GenerationRunKinds.SCRIPT.equals(runKind) || GenerationRunKinds.SCRIPT_ADJUST.equals(runKind)) {
            model.put("textAnalysisModel", requestedModel);
            return model;
        }
        model.put("textAnalysisModel", blankTo(textAnalysisModel, ""));
        model.put("providerModel", requestedModel);
        return model;
    }

    Map<String, Object> metadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("stage", stage);
        metadata.put("key", strategyKey);
        metadata.put("strategyKey", strategyKey);
        metadata.put("runKind", runKind);
        metadata.put("modelKind", modelKind);
        metadata.put("requestedModel", requestedModel);
        metadata.put("provider", provider);
        metadata.put("providerModel", providerModel);
        metadata.put("supportsSeed", supportsSeed);
        metadata.put("supportsImageDataUriReferences", supportsImageDataUriReferences);
        metadata.put("referenceInputMode", referenceInputMode);
        metadata.put("generationMode", generationMode);
        metadata.put("configSource", configSource);
        return metadata;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

final class WorkflowStageGenerationStrategyResolver {

    private final ModelRuntimePropertiesResolver modelResolver;

    WorkflowStageGenerationStrategyResolver(ModelRuntimePropertiesResolver modelResolver) {
        this.modelResolver = modelResolver;
    }

    WorkflowStageGenerationStrategy storyboard(StageWorkflowEntity workflow) {
        String requestedModel = trim(workflow == null ? "" : workflow.getTextAnalysisModel());
        ModelRuntimeProfile profile = modelResolver == null ? null : modelResolver.resolveTextProfile(requestedModel, workflow == null ? null : workflow.getOwnerUserId());
        return new WorkflowStageGenerationStrategy(
            WorkflowConstants.STAGE_STORYBOARD,
            "storyboard.text",
            GenerationRunKinds.SCRIPT,
            GenerationModelKinds.TEXT,
            requestedModel,
            profile == null ? "" : profile.provider(),
            profile == null ? requestedModel : profile.modelName(),
            profile == null ? supportsSeedFallback(requestedModel) : profile.supportsSeed(),
            false,
            "none",
            "",
            profile == null ? "" : profile.source()
        );
    }

    WorkflowStageGenerationStrategy storyboardAdjust(StageWorkflowEntity workflow) {
        WorkflowStageGenerationStrategy base = storyboard(workflow);
        return new WorkflowStageGenerationStrategy(
            WorkflowConstants.STAGE_STORYBOARD,
            "storyboard.adjust.text",
            GenerationRunKinds.SCRIPT_ADJUST,
            GenerationModelKinds.TEXT,
            base.requestedModel(),
            stringValue(base.metadata().get("provider")),
            stringValue(base.metadata().get("providerModel")),
            base.supportsSeed(),
            false,
            "none",
            "",
            stringValue(base.metadata().get("configSource"))
        );
    }

    WorkflowStageGenerationStrategy characterSheet(StageWorkflowEntity workflow) {
        return image(workflow, "character_sheet");
    }

    WorkflowStageGenerationStrategy keyframe(StageWorkflowEntity workflow) {
        return image(workflow, "keyframe");
    }

    WorkflowStageGenerationStrategy materialImage(Long userId, String imageModel, String assetType) {
        return image(userId, imageModel, "material_" + trim(assetType));
    }

    WorkflowStageGenerationStrategy video(StageWorkflowEntity workflow) {
        String requestedModel = trim(workflow == null ? "" : workflow.getVideoModel());
        MediaProviderProfile profile = modelResolver == null ? null : modelResolver.resolveMediaProfile(
            requestedModel,
            GenerationModelKinds.VIDEO,
            workflow == null ? null : workflow.getOwnerUserId()
        );
        return new WorkflowStageGenerationStrategy(
            WorkflowConstants.STAGE_VIDEO,
            "video." + providerKey(profile, requestedModel),
            GenerationRunKinds.VIDEO,
            GenerationModelKinds.VIDEO,
            requestedModel,
            profile == null ? "" : profile.provider(),
            profile == null ? requestedModel : profile.modelName(),
            profile == null ? supportsSeedFallback(requestedModel) : profile.supportsSeed(),
            false,
            "frame_url",
            profile == null ? "" : profile.generationMode(),
            profile == null ? "" : profile.source()
        );
    }

    private WorkflowStageGenerationStrategy image(StageWorkflowEntity workflow, String strategySuffix) {
        return image(workflow == null ? null : workflow.getOwnerUserId(), workflow == null ? "" : workflow.getImageModel(), strategySuffix);
    }

    private WorkflowStageGenerationStrategy image(Long userId, String imageModel, String strategySuffix) {
        String requestedModel = trim(imageModel);
        MediaProviderProfile profile = modelResolver == null ? null : modelResolver.resolveMediaProfile(
            requestedModel,
            GenerationModelKinds.IMAGE,
            userId
        );
        boolean supportsDataUriReferences = profile == null
            ? requestedModel.toLowerCase(Locale.ROOT).contains("gpt-image")
            : profile.supportsImageDataUriReferences();
        return new WorkflowStageGenerationStrategy(
            WorkflowConstants.STAGE_KEYFRAME,
            trim(strategySuffix) + "." + providerKey(profile, requestedModel),
            GenerationRunKinds.IMAGE,
            GenerationModelKinds.IMAGE,
            requestedModel,
            profile == null ? "" : profile.provider(),
            profile == null ? requestedModel : profile.modelName(),
            profile == null ? supportsSeedFallback(requestedModel) : profile.supportsSeed(),
            supportsDataUriReferences,
            supportsDataUriReferences ? "http_or_data_uri" : "http_only",
            profile == null ? "" : profile.generationMode(),
            profile == null ? "" : profile.source()
        );
    }

    private boolean supportsSeedFallback(String requestedModel) {
        return modelResolver != null && modelResolver.supportsSeed(requestedModel);
    }

    private static String providerKey(MediaProviderProfile profile, String requestedModel) {
        String provider = profile == null ? "" : trim(profile.provider());
        if (!provider.isBlank()) {
            return provider;
        }
        String model = trim(requestedModel).toLowerCase(Locale.ROOT);
        if (model.contains("gpt-image")) {
            return "gpt-image";
        }
        if (model.contains("seedream")) {
            return "seedream";
        }
        return model.isBlank() ? "default" : model;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
