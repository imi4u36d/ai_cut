package com.jiandou.api.credit.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record AdminCreditTransactionResponse(
    String transactionId,
    Long userId,
    String featureCode,
    String transactionType,
    int amountDelta,
    int balanceBefore,
    int balanceAfter,
    String relatedRunId,
    String relatedTaskId,
    String relatedWorkflowId,
    String reason,
    Map<String, Object> metadata,
    OffsetDateTime createdAt
) {}
