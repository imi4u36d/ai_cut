package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 任务队列事件持久化实体。
 */
@TableName("biz_task_queue_events")
public class TaskQueueEventEntity {

    @TableId("task_queue_event_id")
    private String taskQueueEventId;
    @TableField("task_id")
    private String taskId;
    @TableField("attempt_id")
    private String attemptId;
    @TableField("queue_name")
    private String queueName;
    @TableField("event_type")
    private String eventType;
    @TableField("worker_instance_id")
    private String workerInstanceId;
    @TableField("queue_position_hint")
    private Integer queuePositionHint;
    @TableField("payload_json")
    private String payloadJson;
    @TableField("event_time")
    private OffsetDateTime eventTime;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回任务队列事件标识。
     * @return 处理结果
     */
    public String getTaskQueueEventId() {
        return taskQueueEventId;
    }

    /**
     * 处理set任务队列事件标识。
     * @param taskQueueEventId 任务队列事件标识值
     */
    public void setTaskQueueEventId(String taskQueueEventId) {
        this.taskQueueEventId = taskQueueEventId;
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
     * 返回事件类型。
     * @return 处理结果
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 处理set事件类型。
     * @param eventType 事件类型值
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
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
     * 返回队列Position提示。
     * @return 处理结果
     */
    public Integer getQueuePositionHint() {
        return queuePositionHint;
    }

    /**
     * 处理set队列Position提示。
     * @param queuePositionHint 队列Position提示值
     */
    public void setQueuePositionHint(Integer queuePositionHint) {
        this.queuePositionHint = queuePositionHint;
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
     * 返回事件时间。
     * @return 处理结果
     */
    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    /**
     * 处理set事件时间。
     * @param eventTime 事件时间值
     */
    public void setEventTime(OffsetDateTime eventTime) {
        this.eventTime = eventTime;
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
