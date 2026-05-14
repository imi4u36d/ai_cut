package com.jiandou.api.credit.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("sys_credit_transaction")
public class SysCreditTransactionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("transaction_id")
    private String transactionId;
    @TableField("user_id")
    private Long userId;
    @TableField("feature_code")
    private String featureCode;
    @TableField("transaction_type")
    private String transactionType;
    @TableField("amount_delta")
    private Integer amountDelta;
    @TableField("balance_before")
    private Integer balanceBefore;
    @TableField("balance_after")
    private Integer balanceAfter;
    @TableField("related_run_id")
    private String relatedRunId;
    @TableField("related_task_id")
    private String relatedTaskId;
    @TableField("related_workflow_id")
    private String relatedWorkflowId;
    private String reason;
    @TableField("metadata_json")
    private String metadataJson;
    @TableField("created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public Integer getAmountDelta() { return amountDelta; }
    public void setAmountDelta(Integer amountDelta) { this.amountDelta = amountDelta; }
    public Integer getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Integer balanceBefore) { this.balanceBefore = balanceBefore; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getRelatedRunId() { return relatedRunId; }
    public void setRelatedRunId(String relatedRunId) { this.relatedRunId = relatedRunId; }
    public String getRelatedTaskId() { return relatedTaskId; }
    public void setRelatedTaskId(String relatedTaskId) { this.relatedTaskId = relatedTaskId; }
    public String getRelatedWorkflowId() { return relatedWorkflowId; }
    public void setRelatedWorkflowId(String relatedWorkflowId) { this.relatedWorkflowId = relatedWorkflowId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
