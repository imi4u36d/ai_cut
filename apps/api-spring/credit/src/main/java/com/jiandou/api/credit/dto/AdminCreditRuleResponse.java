package com.jiandou.api.credit.dto;

import java.time.OffsetDateTime;

public record AdminCreditRuleResponse(
    String featureCode,
    String displayName,
    int cost,
    OffsetDateTime updatedAt
) {}
