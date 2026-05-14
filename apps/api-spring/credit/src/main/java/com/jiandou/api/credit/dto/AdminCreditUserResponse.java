package com.jiandou.api.credit.dto;

import java.time.OffsetDateTime;

public record AdminCreditUserResponse(
    Long id,
    String username,
    String displayName,
    String role,
    String status,
    int balance,
    int totalConsumed,
    int totalAdjusted,
    long imageGenerationCount,
    long videoGenerationCount,
    OffsetDateTime lastUsedAt
) {}
