package com.jiandou.api.workflow.web.dto;

import java.util.List;

public record CreateMaterialGenerationRequest(
    String assetType,
    String title,
    String description,
    List<String> styleKeywords,
    List<String> referenceImageUrls,
    List<String> referenceAssetIds,
    String aspectRatio,
    String textAnalysisModel,
    String imageModel,
    Integer seed
) {}
