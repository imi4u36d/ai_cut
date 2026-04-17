package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 任务结果持久化实体。
 */
@TableName("biz_task_results")
public class TaskResultEntity {

    @TableId("task_result_id")
    private String taskResultId;
    @TableField("task_id")
    private String taskId;
    @TableField("result_type")
    private String resultType;
    @TableField("clip_index")
    private Integer clipIndex;
    private String title;
    private String reason;
    @TableField("source_model_call_id")
    private String sourceModelCallId;
    @TableField("material_asset_id")
    private String materialAssetId;
    @TableField("start_seconds")
    private Double startSeconds;
    @TableField("end_seconds")
    private Double endSeconds;
    @TableField("duration_seconds")
    private Double durationSeconds;
    @TableField("preview_path")
    private String previewPath;
    @TableField("download_path")
    private String downloadPath;
    private Integer width;
    private Integer height;
    @TableField("mime_type")
    private String mimeType;
    @TableField("size_bytes")
    private Long sizeBytes;
    @TableField("remote_url")
    private String remoteUrl;
    @TableField("extra_json")
    private String extraJson;
    @TableField("produced_at")
    private OffsetDateTime producedAt;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回任务结果标识。
     * @return 处理结果
     */
    public String getTaskResultId() { return taskResultId; }
    public void setTaskResultId(String taskResultId) { this.taskResultId = taskResultId; }
    /**
     * 返回任务标识。
     * @return 处理结果
     */
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    /**
     * 返回结果类型。
     * @return 处理结果
     */
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    /**
     * 返回片段索引。
     * @return 处理结果
     */
    public Integer getClipIndex() { return clipIndex; }
    public void setClipIndex(Integer clipIndex) { this.clipIndex = clipIndex; }
    /**
     * 返回Title。
     * @return 处理结果
     */
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    /**
     * 返回Reason。
     * @return 处理结果
     */
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    /**
     * 返回来源模型调用标识。
     * @return 处理结果
     */
    public String getSourceModelCallId() { return sourceModelCallId; }
    public void setSourceModelCallId(String sourceModelCallId) { this.sourceModelCallId = sourceModelCallId; }
    /**
     * 返回素材素材标识。
     * @return 处理结果
     */
    public String getMaterialAssetId() { return materialAssetId; }
    public void setMaterialAssetId(String materialAssetId) { this.materialAssetId = materialAssetId; }
    /**
     * 返回StartSeconds。
     * @return 处理结果
     */
    public Double getStartSeconds() { return startSeconds; }
    public void setStartSeconds(Double startSeconds) { this.startSeconds = startSeconds; }
    /**
     * 返回EndSeconds。
     * @return 处理结果
     */
    public Double getEndSeconds() { return endSeconds; }
    public void setEndSeconds(Double endSeconds) { this.endSeconds = endSeconds; }
    /**
     * 返回时长Seconds。
     * @return 处理结果
     */
    public Double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }
    /**
     * 返回Preview路径。
     * @return 处理结果
     */
    public String getPreviewPath() { return previewPath; }
    public void setPreviewPath(String previewPath) { this.previewPath = previewPath; }
    /**
     * 返回Download路径。
     * @return 处理结果
     */
    public String getDownloadPath() { return downloadPath; }
    public void setDownloadPath(String downloadPath) { this.downloadPath = downloadPath; }
    /**
     * 返回Width。
     * @return 处理结果
     */
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    /**
     * 返回Height。
     * @return 处理结果
     */
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    /**
     * 返回Mime类型。
     * @return 处理结果
     */
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    /**
     * 返回SizeBytes。
     * @return 处理结果
     */
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    /**
     * 返回远程URL。
     * @return 处理结果
     */
    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }
    /**
     * 返回ExtraJson。
     * @return 处理结果
     */
    public String getExtraJson() { return extraJson; }
    public void setExtraJson(String extraJson) { this.extraJson = extraJson; }
    /**
     * 返回ProducedAt。
     * @return 处理结果
     */
    public OffsetDateTime getProducedAt() { return producedAt; }
    public void setProducedAt(OffsetDateTime producedAt) { this.producedAt = producedAt; }
    /**
     * 返回TimezoneOffsetMinutes。
     * @return 处理结果
     */
    public Integer getTimezoneOffsetMinutes() { return timezoneOffsetMinutes; }
    public void setTimezoneOffsetMinutes(Integer timezoneOffsetMinutes) { this.timezoneOffsetMinutes = timezoneOffsetMinutes; }
    /**
     * 返回Create时间。
     * @return 处理结果
     */
    public OffsetDateTime getCreateTime() { return createTime; }
    public void setCreateTime(OffsetDateTime createTime) { this.createTime = createTime; }
    /**
     * 返回Update时间。
     * @return 处理结果
     */
    public OffsetDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(OffsetDateTime updateTime) { this.updateTime = updateTime; }
    /**
     * 返回IsDeleted。
     * @return 处理结果
     */
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
