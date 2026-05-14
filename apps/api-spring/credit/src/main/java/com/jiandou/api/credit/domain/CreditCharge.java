package com.jiandou.api.credit.domain;

/**
 * 积分扣费结果。
 */
public record CreditCharge(
    boolean charged,
    Long userId,
    String featureCode,
    int cost,
    int balanceBefore,
    int balanceAfter,
    String transactionId
) {
    public static CreditCharge skipped(Long userId, String featureCode, int cost) {
        return new CreditCharge(false, userId, featureCode, cost, 0, 0, "");
    }
}
