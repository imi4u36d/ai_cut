package com.jiandou.api.workflow.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jiandou.api.generation.observability.ProviderPayloadSanitizer;
import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetEntity;
import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetMapper;
import com.jiandou.api.task.infrastructure.mybatis.RequestLogEntity;
import com.jiandou.api.task.infrastructure.mybatis.RequestLogMapper;
import com.jiandou.api.task.infrastructure.mybatis.TaskModelCallEntity;
import com.jiandou.api.task.infrastructure.mybatis.TaskModelCallMapper;
import com.jiandou.api.task.observability.StructuredApplicationLogger;
import com.jiandou.api.workflow.infrastructure.WorkflowJsonSupport;
import com.jiandou.api.workflow.infrastructure.mybatis.StageVersionEntity;
import com.jiandou.api.workflow.infrastructure.mybatis.StageVersionMapper;
import com.jiandou.api.workflow.infrastructure.mybatis.StageWorkflowEntity;
import com.jiandou.api.workflow.infrastructure.mybatis.StageWorkflowMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

@Repository
public class WorkflowRepository {

    private final SqlSessionFactory sqlSessionFactory;

    public WorkflowRepository(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void saveWorkflow(StageWorkflowEntity entity) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            StageWorkflowMapper mapper = session.getMapper(StageWorkflowMapper.class);
            StageWorkflowEntity existing = mapper.selectById(entity.getWorkflowId());
            if (existing == null) {
                mapper.insert(entity);
            } else {
                mapper.updateById(entity);
            }
        }
    }

    public StageWorkflowEntity findWorkflow(String workflowId, Long ownerUserId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(StageWorkflowMapper.class).selectOne(
                Wrappers.<StageWorkflowEntity>lambdaQuery()
                    .eq(StageWorkflowEntity::getWorkflowId, workflowId)
                    .eq(ownerUserId != null, StageWorkflowEntity::getOwnerUserId, ownerUserId)
                    .eq(StageWorkflowEntity::getIsDeleted, 0)
                    .last("LIMIT 1")
            );
        }
    }

    public List<StageWorkflowEntity> listWorkflows(Long ownerUserId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(StageWorkflowMapper.class).selectList(
                Wrappers.<StageWorkflowEntity>lambdaQuery()
                    .eq(ownerUserId != null, StageWorkflowEntity::getOwnerUserId, ownerUserId)
                    .eq(StageWorkflowEntity::getIsDeleted, 0)
                    .orderByDesc(StageWorkflowEntity::getUpdateTime)
            );
        }
    }

    public void saveStageVersion(StageVersionEntity entity) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            StageVersionMapper mapper = session.getMapper(StageVersionMapper.class);
            StageVersionEntity existing = mapper.selectById(entity.getStageVersionId());
            if (existing == null) {
                mapper.insert(entity);
            } else {
                mapper.updateById(entity);
            }
        }
    }

    public StageVersionEntity findStageVersion(String workflowId, String versionId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(StageVersionMapper.class).selectOne(
                Wrappers.<StageVersionEntity>lambdaQuery()
                    .eq(StageVersionEntity::getWorkflowId, workflowId)
                    .eq(StageVersionEntity::getStageVersionId, versionId)
                    .eq(StageVersionEntity::getIsDeleted, 0)
                    .last("LIMIT 1")
            );
        }
    }

    public StageVersionEntity findStageVersionByMaterialAssetId(String materialAssetId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(StageVersionMapper.class).selectOne(
                Wrappers.<StageVersionEntity>lambdaQuery()
                    .eq(StageVersionEntity::getMaterialAssetId, materialAssetId)
                    .eq(StageVersionEntity::getIsDeleted, 0)
                    .orderByDesc(StageVersionEntity::getCreateTime)
                    .last("LIMIT 1")
            );
        }
    }

    public List<StageVersionEntity> listStageVersions(String workflowId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(StageVersionMapper.class).selectList(
                Wrappers.<StageVersionEntity>lambdaQuery()
                    .eq(StageVersionEntity::getWorkflowId, workflowId)
                    .eq(StageVersionEntity::getIsDeleted, 0)
                    .orderByAsc(StageVersionEntity::getStageType)
                    .orderByAsc(StageVersionEntity::getClipIndex)
                    .orderByDesc(StageVersionEntity::getVersionNo)
            );
        }
    }

    public int nextStageVersionNo(String workflowId, String stageType, int clipIndex) {
        List<StageVersionEntity> versions = listStageVersions(workflowId);
        return versions.stream()
            .filter(item -> stageType.equals(item.getStageType()) && clipIndex == defaultInt(item.getClipIndex()))
            .map(StageVersionEntity::getVersionNo)
            .filter(item -> item != null && item > 0)
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    public void markSelectedStageVersion(String workflowId, String stageType, int clipIndex, String selectedVersionId) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            StageVersionMapper mapper = session.getMapper(StageVersionMapper.class);
            List<StageVersionEntity> versions = mapper.selectList(
                Wrappers.<StageVersionEntity>lambdaQuery()
                    .eq(StageVersionEntity::getWorkflowId, workflowId)
                    .eq(StageVersionEntity::getStageType, stageType)
                    .eq(StageVersionEntity::getClipIndex, clipIndex)
                    .eq(StageVersionEntity::getIsDeleted, 0)
            );
            for (StageVersionEntity item : versions) {
                item.setSelected(item.getStageVersionId().equals(selectedVersionId) ? 1 : 0);
                mapper.updateById(item);
            }
        }
    }

    public void clearSelectedStageVersions(String workflowId, String stageType, Integer clipIndex) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            StageVersionMapper mapper = session.getMapper(StageVersionMapper.class);
            var query = Wrappers.<StageVersionEntity>lambdaQuery()
                .eq(StageVersionEntity::getWorkflowId, workflowId)
                .eq(StageVersionEntity::getStageType, stageType)
                .eq(StageVersionEntity::getIsDeleted, 0);
            if (clipIndex != null) {
                query.eq(StageVersionEntity::getClipIndex, clipIndex);
            }
            List<StageVersionEntity> versions = mapper.selectList(query);
            for (StageVersionEntity item : versions) {
                item.setSelected(0);
                mapper.updateById(item);
            }
        }
    }

    public void saveMaterialAsset(MaterialAssetEntity entity) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            MaterialAssetMapper mapper = session.getMapper(MaterialAssetMapper.class);
            MaterialAssetEntity existing = mapper.selectById(entity.getMaterialAssetId());
            if (existing == null) {
                mapper.insert(entity);
            } else {
                mapper.updateById(entity);
            }
        }
    }

    public MaterialAssetEntity findMaterialAsset(String assetId, Long ownerUserId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(MaterialAssetMapper.class).selectOne(
                Wrappers.<MaterialAssetEntity>lambdaQuery()
                    .eq(MaterialAssetEntity::getMaterialAssetId, assetId)
                    .eq(ownerUserId != null, MaterialAssetEntity::getOwnerUserId, ownerUserId)
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
                    .last("LIMIT 1")
            );
        }
    }

    public List<MaterialAssetEntity> listMaterialAssets(Long ownerUserId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(MaterialAssetMapper.class).selectList(
                Wrappers.<MaterialAssetEntity>lambdaQuery()
                    .eq(ownerUserId != null, MaterialAssetEntity::getOwnerUserId, ownerUserId)
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
                    .orderByDesc(MaterialAssetEntity::getCreateTime)
            );
        }
    }

    public List<MaterialAssetEntity> listMaterialAssetsPage(Long ownerUserId, int offset, int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(MaterialAssetMapper.class).selectList(
                materialAssetListQuery()
                    .eq(ownerUserId != null, MaterialAssetEntity::getOwnerUserId, ownerUserId)
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
                    .orderByDesc(MaterialAssetEntity::getCreateTime)
                    .last("LIMIT " + Math.max(0, offset) + ", " + Math.max(1, limit))
            );
        }
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MaterialAssetEntity> materialAssetListQuery() {
        return Wrappers.<MaterialAssetEntity>lambdaQuery()
            .select(
                MaterialAssetEntity::getId,
                MaterialAssetEntity::getMaterialAssetId,
                MaterialAssetEntity::getOwnerUserId,
                MaterialAssetEntity::getTaskId,
                MaterialAssetEntity::getWorkflowId,
                MaterialAssetEntity::getSourceTaskId,
                MaterialAssetEntity::getSourceMaterialId,
                MaterialAssetEntity::getAssetRole,
                MaterialAssetEntity::getStageType,
                MaterialAssetEntity::getClipIndex,
                MaterialAssetEntity::getVersionNo,
                MaterialAssetEntity::getSelectedForNext,
                MaterialAssetEntity::getUserRating,
                MaterialAssetEntity::getRatingNote,
                MaterialAssetEntity::getMediaType,
                MaterialAssetEntity::getTitle,
                MaterialAssetEntity::getOriginProvider,
                MaterialAssetEntity::getOriginModel,
                MaterialAssetEntity::getRemoteTaskId,
                MaterialAssetEntity::getRemoteAssetId,
                MaterialAssetEntity::getOriginalFileName,
                MaterialAssetEntity::getStoredFileName,
                MaterialAssetEntity::getFileExt,
                MaterialAssetEntity::getStorageProvider,
                MaterialAssetEntity::getMimeType,
                MaterialAssetEntity::getSizeBytes,
                MaterialAssetEntity::getSha256,
                MaterialAssetEntity::getDurationSeconds,
                MaterialAssetEntity::getWidth,
                MaterialAssetEntity::getHeight,
                MaterialAssetEntity::getHasAudio,
                MaterialAssetEntity::getLocalStoragePath,
                MaterialAssetEntity::getLocalFilePath,
                MaterialAssetEntity::getPublicUrl,
                MaterialAssetEntity::getThumbnailUrl,
                MaterialAssetEntity::getThirdPartyUrl,
                MaterialAssetEntity::getRemoteUrl,
                MaterialAssetEntity::getCapturedAt,
                MaterialAssetEntity::getTimezoneOffsetMinutes,
                MaterialAssetEntity::getCreateTime,
                MaterialAssetEntity::getUpdateTime,
                MaterialAssetEntity::getIsDeleted
            );
    }

    public Long countMaterialAssets(Long ownerUserId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(MaterialAssetMapper.class).selectCount(
                Wrappers.<MaterialAssetEntity>lambdaQuery()
                    .eq(ownerUserId != null, MaterialAssetEntity::getOwnerUserId, ownerUserId)
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
            );
        }
    }

    public List<MaterialAssetEntity> listMaterialAssetsMissingThumbnailsAfterId(long afterId, int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(MaterialAssetMapper.class).selectList(
                Wrappers.<MaterialAssetEntity>lambdaQuery()
                    .gt(MaterialAssetEntity::getId, Math.max(0L, afterId))
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
                    .in(MaterialAssetEntity::getMediaType, List.of("image", "video"))
                    .and(query -> query.isNull(MaterialAssetEntity::getThumbnailUrl).or().eq(MaterialAssetEntity::getThumbnailUrl, ""))
                    .orderByAsc(MaterialAssetEntity::getId)
                    .last("LIMIT " + Math.max(1, limit))
            );
        }
    }

    public void updateMaterialAssetThumbnail(String materialAssetId, String thumbnailUrl) {
        if (materialAssetId == null || materialAssetId.isBlank() || thumbnailUrl == null || thumbnailUrl.isBlank()) {
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(MaterialAssetMapper.class).update(
                null,
                Wrappers.<MaterialAssetEntity>lambdaUpdate()
                    .eq(MaterialAssetEntity::getMaterialAssetId, materialAssetId)
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
                    .set(MaterialAssetEntity::getThumbnailUrl, thumbnailUrl)
                    .set(MaterialAssetEntity::getUpdateTime, OffsetDateTime.now())
            );
        }
    }

    public Map<String, MaterialAssetEntity> findMaterialAssetsByIds(Set<String> assetIds, Long ownerUserId) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Map.of();
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<MaterialAssetEntity> entities = session.getMapper(MaterialAssetMapper.class).selectList(
                Wrappers.<MaterialAssetEntity>lambdaQuery()
                    .in(MaterialAssetEntity::getMaterialAssetId, assetIds)
                    .eq(ownerUserId != null, MaterialAssetEntity::getOwnerUserId, ownerUserId)
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
            );
            Map<String, MaterialAssetEntity> result = new LinkedHashMap<>();
            for (MaterialAssetEntity item : entities) {
                result.put(item.getMaterialAssetId(), item);
            }
            return result;
        }
    }

    public void saveSystemLog(
        String ownerRefId,
        String module,
        String stage,
        String event,
        String level,
        String message,
        Map<String, Object> payload
    ) {
        StructuredApplicationLogger.logWorkflowEvent(ownerRefId, module, stage, event, level, message, payload);
    }

    public void saveModelCall(String ownerRefId, Map<String, Object> modelCall) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            TaskModelCallMapper mapper = session.getMapper(TaskModelCallMapper.class);
            TaskModelCallEntity entity = new TaskModelCallEntity();
            OffsetDateTime now = OffsetDateTime.now();
            entity.setTaskModelCallId(stringValue(modelCall.get("modelCallId")));
            entity.setTaskId(ownerRefId == null ? "" : ownerRefId);
            entity.setCallKind(stringValue(modelCall.get("callKind")));
            entity.setStage(stringValue(modelCall.get("stage")));
            entity.setOperation(stringValue(modelCall.get("operation")));
            entity.setProvider(stringValue(modelCall.get("provider")));
            entity.setProviderModel(stringValue(modelCall.get("providerModel")));
            entity.setRequestedModel(stringValue(modelCall.get("requestedModel")));
            entity.setResolvedModel(stringValue(modelCall.get("resolvedModel")));
            entity.setModelName(stringValue(modelCall.get("modelName")));
            entity.setModelAlias(stringValue(modelCall.get("modelAlias")));
            entity.setEndpointHost(stringValue(modelCall.get("endpointHost")));
            entity.setRequestId(stringValue(modelCall.get("requestId")));
            entity.setRequestPayloadJson(WorkflowJsonSupport.write(ProviderPayloadSanitizer.sanitize(modelCall.get("requestPayload"))));
            entity.setResponsePayloadJson(WorkflowJsonSupport.write(ProviderPayloadSanitizer.sanitize(modelCall.get("responsePayload"))));
            entity.setHttpStatus(intValue(modelCall.get("httpStatus"), 0));
            entity.setResponseStatusCode(intValue(modelCall.get("responseCode"), entity.getHttpStatus()));
            entity.setSuccess(booleanValue(modelCall.get("success")) ? 1 : 0);
            entity.setErrorCode(stringValue(modelCall.get("errorCode")));
            entity.setErrorMessage(stringValue(modelCall.get("errorMessage")));
            entity.setLatencyMs(intValue(modelCall.get("latencyMs"), 0));
            entity.setDurationMs(intValue(modelCall.get("durationMs"), entity.getLatencyMs()));
            entity.setInputTokens(intValue(modelCall.get("inputTokens"), 0));
            entity.setOutputTokens(intValue(modelCall.get("outputTokens"), 0));
            entity.setStartedAt(offsetDateTimeValue(modelCall.get("startedAt"), now));
            entity.setFinishedAt(offsetDateTimeValue(modelCall.get("finishedAt"), entity.getStartedAt()));
            entity.setTimezoneOffsetMinutes(480);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setIsDeleted(0);
            upsertModelCall(mapper, entity);
            RequestLogMapper requestLogMapper = session.getMapper(RequestLogMapper.class);
            upsertRequestLog(requestLogMapper, toRequestLogEntity(ownerRefId, modelCall, now));
        }
    }

    private void upsertModelCall(TaskModelCallMapper mapper, TaskModelCallEntity entity) {
        TaskModelCallEntity existing = mapper.selectOne(
            Wrappers.<TaskModelCallEntity>lambdaQuery()
                .eq(TaskModelCallEntity::getTaskModelCallId, entity.getTaskModelCallId())
                .last("LIMIT 1")
        );
        if (existing == null) {
            mapper.insert(entity);
            return;
        }
        mapper.update(
            entity,
            Wrappers.<TaskModelCallEntity>lambdaUpdate()
                .eq(TaskModelCallEntity::getTaskModelCallId, entity.getTaskModelCallId())
        );
    }

    private void upsertRequestLog(RequestLogMapper mapper, RequestLogEntity entity) {
        if (entity.getRequestLogId() == null || entity.getRequestLogId().isBlank()) {
            return;
        }
        RequestLogEntity existing = mapper.selectOne(
            Wrappers.<RequestLogEntity>lambdaQuery()
                .eq(RequestLogEntity::getRequestLogId, entity.getRequestLogId())
                .last("LIMIT 1")
        );
        if (existing == null) {
            mapper.insert(entity);
            return;
        }
        mapper.update(
            entity,
            Wrappers.<RequestLogEntity>lambdaUpdate()
                .eq(RequestLogEntity::getRequestLogId, entity.getRequestLogId())
        );
    }

    private RequestLogEntity toRequestLogEntity(String ownerRefId, Map<String, Object> modelCall, OffsetDateTime now) {
        RequestLogEntity entity = new RequestLogEntity();
        boolean success = booleanValue(modelCall.get("success"));
        String workflowId = firstNonBlank(stringValue(modelCall.get("workflowId")), ownerRefId);
        String requestLogSuffix = firstNonBlank(stringValue(modelCall.get("modelCallId")), stringValue(modelCall.get("requestId")));
        entity.setRequestLogId(requestLogSuffix.isBlank() ? "" : "reqlog_" + requestLogSuffix);
        entity.setOwnerUserId(longObjectValue(modelCall.get("ownerUserId")));
        entity.setOwnerRefId(firstNonBlank(stringValue(modelCall.get("ownerRefId")), workflowId));
        entity.setTaskId("");
        entity.setWorkflowId(workflowId);
        entity.setRequestType(firstNonBlank(stringValue(modelCall.get("requestType")), stringValue(modelCall.get("callKind"))));
        entity.setStage(stringValue(modelCall.get("stage")));
        entity.setOperation(stringValue(modelCall.get("operation")));
        entity.setProvider(stringValue(modelCall.get("provider")));
        entity.setProviderModel(stringValue(modelCall.get("providerModel")));
        entity.setRequestedModel(stringValue(modelCall.get("requestedModel")));
        entity.setResolvedModel(stringValue(modelCall.get("resolvedModel")));
        entity.setEndpointHost(stringValue(modelCall.get("endpointHost")));
        entity.setRequestId(stringValue(modelCall.get("requestId")));
        entity.setStatus(firstNonBlank(
            stringValue(modelCall.get("status")),
            success ? "success" : "failed"
        ));
        entity.setSuccess(success ? 1 : 0);
        entity.setHttpStatus(intValue(modelCall.get("httpStatus"), 0));
        entity.setErrorCode(stringValue(modelCall.get("errorCode")));
        entity.setErrorMessage(stringValue(modelCall.get("errorMessage")));
        entity.setStartedAt(offsetDateTimeValue(modelCall.get("startedAt"), now));
        entity.setFinishedAt(offsetDateTimeValue(modelCall.get("finishedAt"), entity.getStartedAt()));
        entity.setDurationMs(intValue(modelCall.get("durationMs"), intValue(modelCall.get("latencyMs"), 0)));
        entity.setTimezoneOffsetMinutes(480);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setIsDeleted(0);
        return entity;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = stringValue(value).toLowerCase();
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }

    private Long longObjectValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = stringValue(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private OffsetDateTime offsetDateTimeValue(Object value, OffsetDateTime fallback) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        String text = stringValue(value);
        if (text.isBlank()) {
            return fallback;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
