package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 任务持久化实体。
 */
@TableName("biz_tasks")
public class TaskEntity {

    @TableId("task_id")
    private String taskId;
    @TableField("task_type")
    private String taskType;
    @TableField
    private String title;
    @TableField
    private String description;
    @TableField("aspect_ratio")
    private String aspectRatio;
    @TableField("min_duration_seconds")
    private Integer minDurationSeconds;
    @TableField("max_duration_seconds")
    private Integer maxDurationSeconds;
    @TableField("output_count")
    private Integer outputCount;
    @TableField("source_primary_asset_id")
    private String sourcePrimaryAssetId;
    @TableField("source_file_name")
    private String sourceFileName;
    @TableField("source_asset_ids_json")
    private String sourceAssetIdsJson;
    @TableField("source_file_names_json")
    private String sourceFileNamesJson;
    @TableField("request_payload_json")
    private String requestPayloadJson;
    @TableField("context_json")
    private String contextJson;
    @TableField("intro_template")
    private String introTemplate;
    @TableField("outro_template")
    private String outroTemplate;
    @TableField("creative_prompt")
    private String creativePrompt;
    @TableField("task_seed")
    private Integer taskSeed;
    @TableField("effect_rating")
    private Integer effectRating;
    @TableField("effect_rating_note")
    private String effectRatingNote;
    @TableField("rated_at")
    private OffsetDateTime ratedAt;
    @TableField("model_provider")
    private String modelProvider;
    @TableField("execution_mode")
    private String executionMode;
    @TableField("editing_mode")
    private String editingMode;
    @TableField
    private String status;
    @TableField
    private Integer progress;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_message")
    private String errorMessage;
    @TableField("plan_json")
    private String planJson;
    @TableField("retry_count")
    private Integer retryCount;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("started_at")
    private OffsetDateTime startedAt;
    @TableField("finished_at")
    private OffsetDateTime finishedAt;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回任务标识。
     * @return 处理结果
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 处理set任务标识。
     * @param taskId 任务标识
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * 返回任务类型。
     * @return 处理结果
     */
    public String getTaskType() {
        return taskType;
    }

    /**
     * 处理set任务类型。
     * @param taskType 任务类型值
     */
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    /**
     * 返回Title。
     * @return 处理结果
     */
    public String getTitle() {
        return title;
    }

    /**
     * 处理setTitle。
     * @param title title值
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 返回Description。
     * @return 处理结果
     */
    public String getDescription() {
        return description;
    }

    /**
     * 处理setDescription。
     * @param description description值
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 返回AspectRatio。
     * @return 处理结果
     */
    public String getAspectRatio() {
        return aspectRatio;
    }

    /**
     * 处理setAspectRatio。
     * @param aspectRatio aspectRatio值
     */
    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    /**
     * 返回最小时长Seconds。
     * @return 处理结果
     */
    public Integer getMinDurationSeconds() {
        return minDurationSeconds;
    }

    /**
     * 处理set最小时长Seconds。
     * @param minDurationSeconds 最小时长Seconds值
     */
    public void setMinDurationSeconds(Integer minDurationSeconds) {
        this.minDurationSeconds = minDurationSeconds;
    }

    /**
     * 返回最大时长Seconds。
     * @return 处理结果
     */
    public Integer getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    /**
     * 处理set最大时长Seconds。
     * @param maxDurationSeconds 最大时长Seconds值
     */
    public void setMaxDurationSeconds(Integer maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }

    /**
     * 返回输出数量。
     * @return 处理结果
     */
    public Integer getOutputCount() {
        return outputCount;
    }

    /**
     * 处理set输出数量。
     * @param outputCount 输出数量值
     */
    public void setOutputCount(Integer outputCount) {
        this.outputCount = outputCount;
    }

    /**
     * 返回来源Primary素材标识。
     * @return 处理结果
     */
    public String getSourcePrimaryAssetId() {
        return sourcePrimaryAssetId;
    }

    /**
     * 处理set来源Primary素材标识。
     * @param sourcePrimaryAssetId 来源Primary素材标识值
     */
    public void setSourcePrimaryAssetId(String sourcePrimaryAssetId) {
        this.sourcePrimaryAssetId = sourcePrimaryAssetId;
    }

    /**
     * 返回来源文件Name。
     * @return 处理结果
     */
    public String getSourceFileName() {
        return sourceFileName;
    }

    /**
     * 处理set来源文件Name。
     * @param sourceFileName 来源文件Name值
     */
    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    /**
     * 返回来源素材标识列表Json。
     * @return 处理结果
     */
    public String getSourceAssetIdsJson() {
        return sourceAssetIdsJson;
    }

    /**
     * 处理set来源素材标识列表Json。
     * @param sourceAssetIdsJson 来源素材标识列表Json值
     */
    public void setSourceAssetIdsJson(String sourceAssetIdsJson) {
        this.sourceAssetIdsJson = sourceAssetIdsJson;
    }

    /**
     * 返回来源文件NamesJson。
     * @return 处理结果
     */
    public String getSourceFileNamesJson() {
        return sourceFileNamesJson;
    }

    /**
     * 处理set来源文件NamesJson。
     * @param sourceFileNamesJson 来源文件NamesJson值
     */
    public void setSourceFileNamesJson(String sourceFileNamesJson) {
        this.sourceFileNamesJson = sourceFileNamesJson;
    }

    /**
     * 返回请求负载Json。
     * @return 处理结果
     */
    public String getRequestPayloadJson() {
        return requestPayloadJson;
    }

    /**
     * 处理set请求负载Json。
     * @param requestPayloadJson 请求负载Json值
     */
    public void setRequestPayloadJson(String requestPayloadJson) {
        this.requestPayloadJson = requestPayloadJson;
    }

    /**
     * 返回ContextJson。
     * @return 处理结果
     */
    public String getContextJson() {
        return contextJson;
    }

    /**
     * 处理setContextJson。
     * @param contextJson contextJson值
     */
    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    /**
     * 返回Intro模板。
     * @return 处理结果
     */
    public String getIntroTemplate() {
        return introTemplate;
    }

    /**
     * 处理setIntro模板。
     * @param introTemplate intro模板值
     */
    public void setIntroTemplate(String introTemplate) {
        this.introTemplate = introTemplate;
    }

    /**
     * 返回Outro模板。
     * @return 处理结果
     */
    public String getOutroTemplate() {
        return outroTemplate;
    }

    /**
     * 处理setOutro模板。
     * @param outroTemplate outro模板值
     */
    public void setOutroTemplate(String outroTemplate) {
        this.outroTemplate = outroTemplate;
    }

    /**
     * 返回创意提示词。
     * @return 处理结果
     */
    public String getCreativePrompt() {
        return creativePrompt;
    }

    /**
     * 处理set创意提示词。
     * @param creativePrompt 创意提示词值
     */
    public void setCreativePrompt(String creativePrompt) {
        this.creativePrompt = creativePrompt;
    }

    /**
     * 返回任务种子。
     * @return 处理结果
     */
    public Integer getTaskSeed() {
        return taskSeed;
    }

    /**
     * 处理set任务种子。
     * @param taskSeed 任务种子值
     */
    public void setTaskSeed(Integer taskSeed) {
        this.taskSeed = taskSeed;
    }

    /**
     * 返回效果评分。
     * @return 处理结果
     */
    public Integer getEffectRating() {
        return effectRating;
    }

    /**
     * 处理set效果评分。
     * @param effectRating 效果评分值
     */
    public void setEffectRating(Integer effectRating) {
        this.effectRating = effectRating;
    }

    /**
     * 返回效果评分Note。
     * @return 处理结果
     */
    public String getEffectRatingNote() {
        return effectRatingNote;
    }

    /**
     * 处理set效果评分Note。
     * @param effectRatingNote 效果评分Note值
     */
    public void setEffectRatingNote(String effectRatingNote) {
        this.effectRatingNote = effectRatingNote;
    }

    /**
     * 返回RatedAt。
     * @return 处理结果
     */
    public OffsetDateTime getRatedAt() {
        return ratedAt;
    }

    /**
     * 处理setRatedAt。
     * @param ratedAt ratedAt值
     */
    public void setRatedAt(OffsetDateTime ratedAt) {
        this.ratedAt = ratedAt;
    }

    /**
     * 返回模型Provider。
     * @return 处理结果
     */
    public String getModelProvider() {
        return modelProvider;
    }

    /**
     * 处理set模型Provider。
     * @param modelProvider 模型Provider值
     */
    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
    }

    /**
     * 返回执行模式。
     * @return 处理结果
     */
    public String getExecutionMode() {
        return executionMode;
    }

    /**
     * 处理set执行模式。
     * @param executionMode 执行模式值
     */
    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    /**
     * 返回编辑模式。
     * @return 处理结果
     */
    public String getEditingMode() {
        return editingMode;
    }

    /**
     * 处理set编辑模式。
     * @param editingMode 编辑模式值
     */
    public void setEditingMode(String editingMode) {
        this.editingMode = editingMode;
    }

    /**
     * 返回状态。
     * @return 处理结果
     */
    public String getStatus() {
        return status;
    }

    /**
     * 处理set状态。
     * @param status 状态值
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回进度。
     * @return 处理结果
     */
    public Integer getProgress() {
        return progress;
    }

    /**
     * 处理set进度。
     * @param progress 进度值
     */
    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    /**
     * 返回ErrorCode。
     * @return 处理结果
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 处理setErrorCode。
     * @param errorCode errorCode值
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 返回ErrorMessage。
     * @return 处理结果
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 处理setErrorMessage。
     * @param errorMessage errorMessage值
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 返回规划Json。
     * @return 处理结果
     */
    public String getPlanJson() {
        return planJson;
    }

    /**
     * 处理set规划Json。
     * @param planJson 规划Json值
     */
    public void setPlanJson(String planJson) {
        this.planJson = planJson;
    }

    /**
     * 返回重试数量。
     * @return 处理结果
     */
    public Integer getRetryCount() {
        return retryCount;
    }

    /**
     * 处理set重试数量。
     * @param retryCount 重试数量值
     */
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * 返回TimezoneOffsetMinutes。
     * @return 处理结果
     */
    public Integer getTimezoneOffsetMinutes() {
        return timezoneOffsetMinutes;
    }

    /**
     * 处理setTimezoneOffsetMinutes。
     * @param timezoneOffsetMinutes timezoneOffsetMinutes值
     */
    public void setTimezoneOffsetMinutes(Integer timezoneOffsetMinutes) {
        this.timezoneOffsetMinutes = timezoneOffsetMinutes;
    }

    /**
     * 返回StartedAt。
     * @return 处理结果
     */
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * 处理setStartedAt。
     * @param startedAt startedAt值
     */
    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * 返回FinishedAt。
     * @return 处理结果
     */
    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    /**
     * 处理setFinishedAt。
     * @param finishedAt finishedAt值
     */
    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * 返回Create时间。
     * @return 处理结果
     */
    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 处理setCreate时间。
     * @param createTime create时间值
     */
    public void setCreateTime(OffsetDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 返回Update时间。
     * @return 处理结果
     */
    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 处理setUpdate时间。
     * @param updateTime update时间值
     */
    public void setUpdateTime(OffsetDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 返回IsDeleted。
     * @return 处理结果
     */
    public Integer getIsDeleted() {
        return isDeleted;
    }

    /**
     * 处理setIsDeleted。
     * @param isDeleted isDeleted值
     */
    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
