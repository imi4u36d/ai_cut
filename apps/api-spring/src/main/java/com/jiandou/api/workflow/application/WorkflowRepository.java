package com.jiandou.api.workflow.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetEntity;
import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetMapper;
import com.jiandou.api.task.infrastructure.mybatis.SystemLogEntity;
import com.jiandou.api.task.infrastructure.mybatis.SystemLogMapper;
import com.jiandou.api.task.infrastructure.mybatis.TaskModelCallEntity;
import com.jiandou.api.task.infrastructure.mybatis.TaskModelCallMapper;
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
import java.util.UUID;
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
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SystemLogMapper mapper = session.getMapper(SystemLogMapper.class);
            SystemLogEntity entity = new SystemLogEntity();
            OffsetDateTime now = OffsetDateTime.now();
            entity.setSystemLogId("slog_" + UUID.randomUUID().toString().replace("-", ""));
            entity.setTaskId(ownerRefId);
            entity.setTraceId(entity.getSystemLogId());
            entity.setModule(module);
            entity.setStage(stage);
            entity.setEvent(event);
            entity.setLevel(level == null || level.isBlank() ? "INFO" : level.toUpperCase());
            entity.setMessage(message == null ? "" : message);
            entity.setPayloadJson(WorkflowJsonSupport.write(payload == null ? Map.of() : payload));
            entity.setSource("spring-api");
            entity.setServiceName("api-spring");
            entity.setHostName("");
            entity.setLoggedAt(now);
            entity.setTimezoneOffsetMinutes(480);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setIsDeleted(0);
            mapper.insert(entity);
        }
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
            entity.setRequestPayloadJson(WorkflowJsonSupport.write(modelCall.get("requestPayload")));
            entity.setResponsePayloadJson(WorkflowJsonSupport.write(modelCall.get("responsePayload")));
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
            mapper.insert(entity);
        }
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
