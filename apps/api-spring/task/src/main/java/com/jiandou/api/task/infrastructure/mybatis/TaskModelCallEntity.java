package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

/**
 * 任务模型调用持久化实体。
 */
@TableName("biz_task_model_calls")
public class TaskModelCallEntity {

    @TableId("task_model_call_id")
    private String taskModelCallId;
    @TableField("task_id")
    private String taskId;
    @TableField("call_kind")
    private String callKind;
    private String stage;
    private String operation;
    private String provider;
    @TableField("provider_model")
    private String providerModel;
    @TableField("requested_model")
    private String requestedModel;
    @TableField("resolved_model")
    private String resolvedModel;
    @TableField("model_name")
    private String modelName;
    @TableField("model_alias")
    private String modelAlias;
    @TableField("endpoint_host")
    private String endpointHost;
    @TableField("request_id")
    private String requestId;
    @TableField("request_payload_json")
    private String requestPayloadJson;
    @TableField("response_payload_json")
    private String responsePayloadJson;
    @TableField("http_status")
    private Integer httpStatus;
    @TableField("response_status_code")
    private Integer responseStatusCode;
    private Integer success;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_message")
    private String errorMessage;
    @TableField("latency_ms")
    private Integer latencyMs;
    @TableField("duration_ms")
    private Integer durationMs;
    @TableField("input_tokens")
    private Integer inputTokens;
    @TableField("output_tokens")
    private Integer outputTokens;
    @TableField("started_at")
    private OffsetDateTime startedAt;
    @TableField("finished_at")
    private OffsetDateTime finishedAt;
    @TableField("timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;
    @TableField("create_time")
    private OffsetDateTime createTime;
    @TableField("update_time")
    private OffsetDateTime updateTime;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 返回任务模型调用标识。
     * @return 处理结果
     */
    public String getTaskModelCallId() { return taskModelCallId; }
    public void setTaskModelCallId(String taskModelCallId) { this.taskModelCallId = taskModelCallId; }
    /**
     * 返回任务标识。
     * @return 处理结果
     */
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    /**
     * 返回调用类型。
     * @return 处理结果
     */
    public String getCallKind() { return callKind; }
    public void setCallKind(String callKind) { this.callKind = callKind; }
    /**
     * 返回阶段。
     * @return 处理结果
     */
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    /**
     * 返回Operation。
     * @return 处理结果
     */
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    /**
     * 返回Provider。
     * @return 处理结果
     */
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    /**
     * 返回Provider模型。
     * @return 处理结果
     */
    public String getProviderModel() { return providerModel; }
    public void setProviderModel(String providerModel) { this.providerModel = providerModel; }
    /**
     * 返回Requested模型。
     * @return 处理结果
     */
    public String getRequestedModel() { return requestedModel; }
    public void setRequestedModel(String requestedModel) { this.requestedModel = requestedModel; }
    /**
     * 返回Resolved模型。
     * @return 处理结果
     */
    public String getResolvedModel() { return resolvedModel; }
    public void setResolvedModel(String resolvedModel) { this.resolvedModel = resolvedModel; }
    /**
     * 返回模型Name。
     * @return 处理结果
     */
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    /**
     * 返回模型Alias。
     * @return 处理结果
     */
    public String getModelAlias() { return modelAlias; }
    public void setModelAlias(String modelAlias) { this.modelAlias = modelAlias; }
    /**
     * 返回EndpointHost。
     * @return 处理结果
     */
    public String getEndpointHost() { return endpointHost; }
    public void setEndpointHost(String endpointHost) { this.endpointHost = endpointHost; }
    /**
     * 返回请求标识。
     * @return 处理结果
     */
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    /**
     * 返回请求负载Json。
     * @return 处理结果
     */
    public String getRequestPayloadJson() { return requestPayloadJson; }
    public void setRequestPayloadJson(String requestPayloadJson) { this.requestPayloadJson = requestPayloadJson; }
    /**
     * 返回响应负载Json。
     * @return 处理结果
     */
    public String getResponsePayloadJson() { return responsePayloadJson; }
    public void setResponsePayloadJson(String responsePayloadJson) { this.responsePayloadJson = responsePayloadJson; }
    /**
     * 返回Http状态。
     * @return 处理结果
     */
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    /**
     * 返回响应状态Code。
     * @return 处理结果
     */
    public Integer getResponseStatusCode() { return responseStatusCode; }
    public void setResponseStatusCode(Integer responseStatusCode) { this.responseStatusCode = responseStatusCode; }
    /**
     * 返回Success。
     * @return 处理结果
     */
    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }
    /**
     * 返回ErrorCode。
     * @return 处理结果
     */
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    /**
     * 返回ErrorMessage。
     * @return 处理结果
     */
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    /**
     * 返回Latency毫秒。
     * @return 处理结果
     */
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    /**
     * 返回时长毫秒。
     * @return 处理结果
     */
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    /**
     * 返回输入Tokens。
     * @return 处理结果
     */
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    /**
     * 返回输出Tokens。
     * @return 处理结果
     */
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    /**
     * 返回StartedAt。
     * @return 处理结果
     */
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    /**
     * 返回FinishedAt。
     * @return 处理结果
     */
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
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
