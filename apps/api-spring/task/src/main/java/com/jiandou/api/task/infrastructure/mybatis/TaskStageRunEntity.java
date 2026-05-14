package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 任务阶段运行持久化实体。
 */
@TableName("biz_task_stage_runs")
public class TaskStageRunEntity {

    @TableId("task_stage_run_id")
    private String taskStageRunId;
    @TableField("task_id")
    private String taskId;
    @TableField("attempt_id")
    private String attemptId;
    @TableField("stage_name")
    private String stageName;
    @TableField("stage_seq")
    private Integer stageSeq;
    @TableField("clip_index")
    private Integer clipIndex;
    private String status;
    @TableField("worker_instance_id")
    private String workerInstanceId;
    @TableField("started_at")
    private OffsetDateTime startedAt;
    @TableField("finished_at")
    private OffsetDateTime finishedAt;
    @TableField("duration_ms")
    private Integer durationMs;
    @TableField("input_summary_json")
    private String inputSummaryJson;
    @TableField("output_summary_json")
    private String outputSummaryJson;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_message")
    private String errorMessage;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回任务阶段运行标识。
     * @return 处理结果
     */
    public String getTaskStageRunId() {
        return taskStageRunId;
    }

    /**
     * 处理set任务阶段运行标识。
     * @param taskStageRunId 任务阶段运行标识值
     */
    public void setTaskStageRunId(String taskStageRunId) {
        this.taskStageRunId = taskStageRunId;
    }

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
     * 返回尝试标识。
     * @return 处理结果
     */
    public String getAttemptId() {
        return attemptId;
    }

    /**
     * 处理set尝试标识。
     * @param attemptId 尝试标识值
     */
    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    /**
     * 返回阶段Name。
     * @return 处理结果
     */
    public String getStageName() {
        return stageName;
    }

    /**
     * 处理set阶段Name。
     * @param stageName 阶段Name值
     */
    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    /**
     * 返回阶段Seq。
     * @return 处理结果
     */
    public Integer getStageSeq() {
        return stageSeq;
    }

    /**
     * 处理set阶段Seq。
     * @param stageSeq 阶段Seq值
     */
    public void setStageSeq(Integer stageSeq) {
        this.stageSeq = stageSeq;
    }

    /**
     * 返回片段索引。
     * @return 处理结果
     */
    public Integer getClipIndex() {
        return clipIndex;
    }

    /**
     * 处理set片段索引。
     * @param clipIndex 片段索引值
     */
    public void setClipIndex(Integer clipIndex) {
        this.clipIndex = clipIndex;
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
     * 返回工作节点Instance标识。
     * @return 处理结果
     */
    public String getWorkerInstanceId() {
        return workerInstanceId;
    }

    /**
     * 处理set工作节点Instance标识。
     * @param workerInstanceId 工作节点实例标识
     */
    public void setWorkerInstanceId(String workerInstanceId) {
        this.workerInstanceId = workerInstanceId;
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
     * 返回时长毫秒。
     * @return 处理结果
     */
    public Integer getDurationMs() {
        return durationMs;
    }

    /**
     * 处理set时长毫秒。
     * @param durationMs 时长毫秒值
     */
    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 返回输入摘要Json。
     * @return 处理结果
     */
    public String getInputSummaryJson() {
        return inputSummaryJson;
    }

    /**
     * 处理set输入摘要Json。
     * @param inputSummaryJson 输入摘要Json值
     */
    public void setInputSummaryJson(String inputSummaryJson) {
        this.inputSummaryJson = inputSummaryJson;
    }

    /**
     * 返回输出摘要Json。
     * @return 处理结果
     */
    public String getOutputSummaryJson() {
        return outputSummaryJson;
    }

    /**
     * 处理set输出摘要Json。
     * @param outputSummaryJson 输出摘要Json值
     */
    public void setOutputSummaryJson(String outputSummaryJson) {
        this.outputSummaryJson = outputSummaryJson;
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
