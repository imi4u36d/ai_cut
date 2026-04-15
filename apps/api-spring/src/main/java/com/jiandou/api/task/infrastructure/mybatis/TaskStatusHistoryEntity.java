package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 任务状态History持久化实体。
 */
@TableName("biz_task_status_history")
public class TaskStatusHistoryEntity {

    @TableId("task_status_history_id")
    private String taskStatusHistoryId;
    @TableField("task_id")
    private String taskId;
    @TableField("previous_status")
    private String previousStatus;
    @TableField("current_status")
    private String currentStatus;
    private Integer progress;
    private String stage;
    private String event;
    private String message;
    @TableField("payload_json")
    private String payloadJson;
    @TableField("change_time")
    private OffsetDateTime changeTime;
    @TableField("operator_type")
    private String operatorType;
    @TableField("operator_id")
    private String operatorId;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回任务状态History标识。
     * @return 处理结果
     */
    public String getTaskStatusHistoryId() {
        return taskStatusHistoryId;
    }

    /**
     * 处理set任务状态History标识。
     * @param taskStatusHistoryId 任务状态History标识值
     */
    public void setTaskStatusHistoryId(String taskStatusHistoryId) {
        this.taskStatusHistoryId = taskStatusHistoryId;
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
     * 返回Previous状态。
     * @return 处理结果
     */
    public String getPreviousStatus() {
        return previousStatus;
    }

    /**
     * 处理setPrevious状态。
     * @param previousStatus previous状态值
     */
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    /**
     * 返回Current状态。
     * @return 处理结果
     */
    public String getCurrentStatus() {
        return currentStatus;
    }

    /**
     * 处理setCurrent状态。
     * @param currentStatus current状态值
     */
    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
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
     * 返回阶段。
     * @return 处理结果
     */
    public String getStage() {
        return stage;
    }

    /**
     * 处理set阶段。
     * @param stage 阶段名称
     */
    public void setStage(String stage) {
        this.stage = stage;
    }

    /**
     * 返回事件。
     * @return 处理结果
     */
    public String getEvent() {
        return event;
    }

    /**
     * 处理set事件。
     * @param event 事件名称
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * 返回Message。
     * @return 处理结果
     */
    public String getMessage() {
        return message;
    }

    /**
     * 处理setMessage。
     * @param message 消息文本
     */
    public void setMessage(String message) {
        this.message = message;
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
     * 返回Change时间。
     * @return 处理结果
     */
    public OffsetDateTime getChangeTime() {
        return changeTime;
    }

    /**
     * 处理setChange时间。
     * @param changeTime change时间值
     */
    public void setChangeTime(OffsetDateTime changeTime) {
        this.changeTime = changeTime;
    }

    /**
     * 返回Operator类型。
     * @return 处理结果
     */
    public String getOperatorType() {
        return operatorType;
    }

    /**
     * 处理setOperator类型。
     * @param operatorType operator类型值
     */
    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    /**
     * 返回Operator标识。
     * @return 处理结果
     */
    public String getOperatorId() {
        return operatorId;
    }

    /**
     * 处理setOperator标识。
     * @param operatorId operator标识值
     */
    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
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
