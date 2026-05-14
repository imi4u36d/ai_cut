package com.jiandou.api.credit.domain;

/**
 * 积分流水类型。
 */
public final class CreditTransactionType {

    public static final String CONSUME = "CONSUME";
    public static final String REFUND = "REFUND";
    public static final String ADJUST = "ADJUST";
    public static final String USAGE = "USAGE";

    private CreditTransactionType() {
    }
}
