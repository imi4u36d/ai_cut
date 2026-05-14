package com.jiandou.api.credit.domain;

import java.util.Map;

/**
 * 积分流水上下文。
 */
public record CreditTransactionContext(
    String runId,
    String taskId,
    String workflowId,
    String reason,
    Map<String, Object> metadata
) {
    public static CreditTransactionContext generationRun(
        String runId,
        String taskId,
        String workflowId,
        String reason,
        Map<String, Object> metadata
    ) {
        return new CreditTransactionContext(runId, taskId, workflowId, reason, metadata == null ? Map.of() : metadata);
    }
}
