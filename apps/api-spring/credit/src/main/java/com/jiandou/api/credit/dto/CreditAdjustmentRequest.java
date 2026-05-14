package com.jiandou.api.credit.dto;

public record CreditAdjustmentRequest(
    Integer amount,
    String reason
) {}
