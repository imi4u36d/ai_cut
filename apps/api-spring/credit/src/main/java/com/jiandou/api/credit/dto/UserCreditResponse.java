package com.jiandou.api.credit.dto;

import java.util.List;

public record UserCreditResponse(
    boolean exempt,
    Integer balance,
    List<AdminCreditRuleResponse> rules
) {}
