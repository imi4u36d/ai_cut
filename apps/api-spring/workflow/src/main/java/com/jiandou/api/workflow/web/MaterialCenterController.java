package com.jiandou.api.workflow.web;

import com.jiandou.api.config.ApiPathConstants;
import com.jiandou.api.auth.security.SecurityCurrentUser;
import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetEntity;
import com.jiandou.api.task.application.TaskApplicationService;
import com.jiandou.api.task.web.dto.CreateGenerationTaskRequest;
import com.jiandou.api.workflow.application.WorkflowRepository;
import com.jiandou.api.workflow.web.dto.CreateMaterialGenerationRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPathConstants.MATERIAL_CENTER)
public class MaterialCenterController {

    private final TaskApplicationService taskService;
    private final WorkflowRepository workflowRepository;

    public MaterialCenterController(TaskApplicationService taskService, WorkflowRepository workflowRepository) {
        this.taskService = taskService;
        this.workflowRepository = workflowRepository;
    }

    @PostMapping("/generations")
    public Map<String, Object> createMaterialGeneration(@RequestBody CreateMaterialGenerationRequest request) {
        return taskService.createGenerationTask(toTaskRequest(request));
    }

    private CreateGenerationTaskRequest toTaskRequest(CreateMaterialGenerationRequest request) {
        String assetType = trimmed(request == null ? "" : request.assetType(), "free");
        List<String> referenceAssetIds = request == null || request.referenceAssetIds() == null ? List.of() : request.referenceAssetIds();
        List<String> referenceImageUrls = resolveReferenceImageUrls(
            request == null || request.referenceImageUrls() == null ? List.of() : request.referenceImageUrls(),
            referenceAssetIds
        );
        return new CreateGenerationTaskRequest(
            trimmed(request == null ? "" : request.title(), defaultTitle(assetType)),
            taskType(assetType, referenceImageUrls),
            assetType,
            trimmed(request == null ? "" : request.description(), ""),
            trimmed(request == null ? "" : request.aspectRatio(), "9:16"),
            trimmed(request == null ? "" : request.imageSize(), ""),
            trimmed(request == null ? "" : request.textAnalysisModel(), ""),
            trimmed(request == null ? "" : request.imageModel(), ""),
            null,
            null,
            request == null ? null : request.seed(),
            null,
            1,
            null,
            null,
            "",
            false,
            referenceImageUrls,
            referenceAssetIds
        );
    }

    private List<String> resolveReferenceImageUrls(List<String> requestUrls, List<String> referenceAssetIds) {
        List<String> urls = new ArrayList<>(normalizeStringList(requestUrls));
        for (MaterialAssetEntity referenceAsset : workflowRepository.findMaterialAssetsByIds(
            new LinkedHashSet<>(normalizeStringList(referenceAssetIds)),
            SecurityCurrentUser.requireCurrentUserId()
        ).values()) {
            String referenceUrl = firstNonBlank(referenceAsset.getRemoteUrl(), referenceAsset.getPublicUrl());
            if (!referenceUrl.isBlank() && !urls.contains(referenceUrl)) {
                urls.add(referenceUrl);
            }
        }
        return urls;
    }

    private String taskType(String assetType, List<String> referenceImageUrls) {
        if ("character_sheet".equals(assetType)) {
            return "character_sheet";
        }
        return referenceImageUrls == null || referenceImageUrls.isEmpty() ? "image_generation" : "image_to_image";
    }

    private String defaultTitle(String assetType) {
        return "character_sheet".equals(assetType) ? "角色三视图" : "图片生成";
    }

    private String trimmed(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return fallback == null ? "" : fallback.trim();
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String normalized = trimmed(value, "");
            if (!normalized.isBlank() && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimmed(value, "");
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }
}
