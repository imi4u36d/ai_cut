package com.jiandou.api.media;

import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetEntity;
import com.jiandou.api.workflow.infrastructure.WorkflowJsonSupport;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 素材列表缩略图服务。
 */
@Service
public class MaterialAssetThumbnailService {

    private final LocalMediaArtifactService localMediaArtifactService;

    public MaterialAssetThumbnailService(LocalMediaArtifactService localMediaArtifactService) {
        this.localMediaArtifactService = localMediaArtifactService;
    }

    public String ensureThumbnail(MaterialAssetEntity asset) {
        if (asset == null) {
            return "";
        }
        String existing = normalizeString(asset.getThumbnailUrl());
        if (!existing.isBlank()) {
            return existing;
        }
        String generated = generateThumbnail(asset);
        if (!generated.isBlank()) {
            asset.setThumbnailUrl(generated);
        }
        return generated;
    }

    public String generateThumbnail(MaterialAssetEntity asset) {
        if (asset == null) {
            return "";
        }
        Map<String, Object> metadata = WorkflowJsonSupport.readMap(asset.getMetadataJson());
        List<String> candidateImageUrls = List.of(firstNonBlank(
            stringValue(metadata.get("thumbnailUrl")),
            stringValue(metadata.get("posterUrl")),
            stringValue(metadata.get("firstFrameUrl")),
            stringValue(metadata.get("startFrameUrl"))
        ));
        return stringValue(localMediaArtifactService.ensureMediaThumbnail(
            normalizeString(asset.getMediaType()),
            normalizeString(asset.getPublicUrl()),
            candidateImageUrls,
            480
        ));
    }

    public String ensureThumbnail(String mediaType, String mediaUrl, List<String> candidateImageUrls) {
        return stringValue(localMediaArtifactService.ensureMediaThumbnail(
            normalizeString(mediaType),
            normalizeString(mediaUrl),
            candidateImageUrls == null ? List.of() : candidateImageUrls,
            480
        ));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = normalizeString(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalizeString(String value) {
        return value == null ? "" : value.trim();
    }
}
