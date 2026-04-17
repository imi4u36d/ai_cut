package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 工作节点Instance持久化实体。
 */
@TableName("biz_worker_instances")
public class WorkerInstanceEntity {

    @TableId("worker_instance_id")
    private String workerInstanceId;
    @TableField("worker_type")
    private String workerType;
    @TableField("queue_name")
    private String queueName;
    @TableField("host_name")
    private String hostName;
    @TableField("process_id")
    private Integer processId;
    private String status;
    @TableField("started_at")
    private OffsetDateTime startedAt;
    @TableField("last_heartbeat_at")
    private OffsetDateTime lastHeartbeatAt;
    @TableField("stopped_at")
    private OffsetDateTime stoppedAt;
    @TableField("metadata_json")
    private String metadataJson;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

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
     * 返回工作节点类型。
     * @return 处理结果
     */
    public String getWorkerType() {
        return workerType;
    }

    /**
     * 处理set工作节点类型。
     * @param workerType 工作节点类型值
     */
    public void setWorkerType(String workerType) {
        this.workerType = workerType;
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
     * 返回HostName。
     * @return 处理结果
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * 处理setHostName。
     * @param hostName hostName值
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * 返回Process标识。
     * @return 处理结果
     */
    public Integer getProcessId() {
        return processId;
    }

    /**
     * 处理setProcess标识。
     * @param processId process标识值
     */
    public void setProcessId(Integer processId) {
        this.processId = processId;
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
     * 返回LastHeartbeatAt。
     * @return 处理结果
     */
    public OffsetDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    /**
     * 处理setLastHeartbeatAt。
     * @param lastHeartbeatAt lastHeartbeatAt值
     */
    public void setLastHeartbeatAt(OffsetDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    /**
     * 返回StoppedAt。
     * @return 处理结果
     */
    public OffsetDateTime getStoppedAt() {
        return stoppedAt;
    }

    /**
     * 处理setStoppedAt。
     * @param stoppedAt stoppedAt值
     */
    public void setStoppedAt(OffsetDateTime stoppedAt) {
        this.stoppedAt = stoppedAt;
    }

    /**
     * 返回MetadataJson。
     * @return 处理结果
     */
    public String getMetadataJson() {
        return metadataJson;
    }

    /**
     * 处理setMetadataJson。
     * @param metadataJson metadataJson值
     */
    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
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
