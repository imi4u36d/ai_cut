package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 系统日志持久化实体。
 */
@TableName("biz_system_logs")
public class SystemLogEntity {

    @TableId("system_log_id")
    private String systemLogId;
    @TableField("task_id")
    private String taskId;
    @TableField("trace_id")
    private String traceId;
    private String module;
    private String stage;
    private String event;
    private String level;
    private String message;
    @TableField("payload_json")
    private String payloadJson;
    private String source;
    @TableField("service_name")
    private String serviceName;
    @TableField("host_name")
    private String hostName;
    @TableField("logged_at")
    private OffsetDateTime loggedAt;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回系统日志标识。
     * @return 处理结果
     */
    public String getSystemLogId() { return systemLogId; }
    public void setSystemLogId(String systemLogId) { this.systemLogId = systemLogId; }
    /**
     * 返回任务标识。
     * @return 处理结果
     */
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    /**
     * 返回追踪标识。
     * @return 处理结果
     */
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    /**
     * 返回Module。
     * @return 处理结果
     */
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    /**
     * 返回阶段。
     * @return 处理结果
     */
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    /**
     * 返回事件。
     * @return 处理结果
     */
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    /**
     * 返回Level。
     * @return 处理结果
     */
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    /**
     * 返回Message。
     * @return 处理结果
     */
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    /**
     * 返回负载Json。
     * @return 处理结果
     */
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    /**
     * 返回来源。
     * @return 处理结果
     */
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    /**
     * 返回服务Name。
     * @return 处理结果
     */
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    /**
     * 返回HostName。
     * @return 处理结果
     */
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    /**
     * 返回LoggedAt。
     * @return 处理结果
     */
    public OffsetDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(OffsetDateTime loggedAt) { this.loggedAt = loggedAt; }
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
