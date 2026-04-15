package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 任务尝试持久化实体。
 */
@TableName("biz_task_attempts")
public class TaskAttemptEntity {

    @TableId("task_attempt_id")
    private String taskAttemptId;
    @TableField("task_id")
    private String taskId;
    @TableField("attempt_no")
    private Integer attemptNo;
    @TableField("trigger_type")
    private String triggerType;
    private String status;
    @TableField("queue_name")
    private String queueName;
    @TableField("worker_instance_id")
    private String workerInstanceId;
    @TableField("queue_entered_at")
    private OffsetDateTime queueEnteredAt;
    @TableField("queue_left_at")
    private OffsetDateTime queueLeftAt;
    @TableField("claimed_at")
    private OffsetDateTime claimedAt;
    @TableField
    private OffsetDateTime startedAt;
    @TableField
    private OffsetDateTime finishedAt;
    @TableField("resume_from_stage")
    private String resumeFromStage;
    @TableField("resume_from_clip_index")
    private Integer resumeFromClipIndex;
    @TableField("failure_code")
    private String failureCode;
    @TableField("failure_message")
    private String failureMessage;
    @TableField("payload_json")
    private String payloadJson;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回任务尝试标识。
     * @return 处理结果
     */
    public String getTaskAttemptId() {
        return taskAttemptId;
    }

    /**
     * 处理set任务尝试标识。
     * @param taskAttemptId 任务尝试标识值
     */
    public void setTaskAttemptId(String taskAttemptId) {
        this.taskAttemptId = taskAttemptId;
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
     * 返回尝试No。
     * @return 处理结果
     */
    public Integer getAttemptNo() {
        return attemptNo;
    }

    /**
     * 处理set尝试No。
     * @param attemptNo 尝试No值
     */
    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    /**
     * 返回Trigger类型。
     * @return 处理结果
     */
    public String getTriggerType() {
        return triggerType;
    }

    /**
     * 处理setTrigger类型。
     * @param triggerType trigger类型值
     */
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
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
     * 返回队列Name。
     * @return 处理结果
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * 处理set队列Name。
     * @param queueName 队列Name值
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName;
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
     * 返回队列EnteredAt。
     * @return 处理结果
     */
    public OffsetDateTime getQueueEnteredAt() {
        return queueEnteredAt;
    }

    /**
     * 处理set队列EnteredAt。
     * @param queueEnteredAt 队列EnteredAt值
     */
    public void setQueueEnteredAt(OffsetDateTime queueEnteredAt) {
        this.queueEnteredAt = queueEnteredAt;
    }

    /**
     * 返回队列LeftAt。
     * @return 处理结果
     */
    public OffsetDateTime getQueueLeftAt() {
        return queueLeftAt;
    }

    /**
     * 处理set队列LeftAt。
     * @param queueLeftAt 队列LeftAt值
     */
    public void setQueueLeftAt(OffsetDateTime queueLeftAt) {
        this.queueLeftAt = queueLeftAt;
    }

    /**
     * 返回ClaimedAt。
     * @return 处理结果
     */
    public OffsetDateTime getClaimedAt() {
        return claimedAt;
    }

    /**
     * 处理setClaimedAt。
     * @param claimedAt claimedAt值
     */
    public void setClaimedAt(OffsetDateTime claimedAt) {
        this.claimedAt = claimedAt;
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
     * 返回ResumeFrom阶段。
     * @return 处理结果
     */
    public String getResumeFromStage() {
        return resumeFromStage;
    }

    /**
     * 处理setResumeFrom阶段。
     * @param resumeFromStage resumeFrom阶段值
     */
    public void setResumeFromStage(String resumeFromStage) {
        this.resumeFromStage = resumeFromStage;
    }

    /**
     * 返回ResumeFrom片段索引。
     * @return 处理结果
     */
    public Integer getResumeFromClipIndex() {
        return resumeFromClipIndex;
    }

    /**
     * 处理setResumeFrom片段索引。
     * @param resumeFromClipIndex resumeFrom片段索引值
     */
    public void setResumeFromClipIndex(Integer resumeFromClipIndex) {
        this.resumeFromClipIndex = resumeFromClipIndex;
    }

    /**
     * 返回失败Code。
     * @return 处理结果
     */
    public String getFailureCode() {
        return failureCode;
    }

    /**
     * 处理set失败Code。
     * @param failureCode 失败Code值
     */
    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    /**
     * 返回失败Message。
     * @return 处理结果
     */
    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * 处理set失败Message。
     * @param failureMessage 失败Message值
     */
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    /**
     * 返回负载Json。
     * @return 处理结果
     */
    public String getPayloadJson() {
        return payloadJson;
    }

    /**
     * 处理set负载Json。
     * @param payloadJson 负载Json值
     */
    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
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
