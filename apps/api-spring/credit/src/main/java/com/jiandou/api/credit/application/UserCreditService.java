package com.jiandou.api.credit.application;

import com.jiandou.api.auth.application.UserCreditAccountInitializer;
import com.jiandou.api.common.exception.ApiException;
import com.jiandou.api.credit.domain.CreditCharge;
import com.jiandou.api.credit.domain.CreditFeatureCode;
import com.jiandou.api.credit.domain.CreditTransactionContext;
import com.jiandou.api.credit.dto.AdminCreditRuleResponse;
import com.jiandou.api.credit.dto.AdminCreditTransactionResponse;
import com.jiandou.api.credit.dto.AdminCreditUserResponse;
import com.jiandou.api.credit.dto.UserCreditResponse;
import com.jiandou.api.credit.exception.InsufficientCreditsException;
import com.jiandou.api.credit.infrastructure.mybatis.MybatisCreditRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 用户积分服务。
 */
@Service
public class UserCreditService implements UserCreditAccountInitializer {

    private final MybatisCreditRepository creditRepository;

    public UserCreditService(MybatisCreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    public void ensureAccount(Long userId) {
        creditRepository.ensureAccount(userId);
    }

    public UserCreditResponse currentUserCredits(Long userId) {
        boolean exempt = isExempt(userId);
        MybatisCreditRepository.CreditBalance balance = exempt
            ? new MybatisCreditRepository.CreditBalance(0, 0, 0)
            : creditRepository.accountBalance(userId);
        return new UserCreditResponse(
            exempt,
            exempt ? null : balance.balance(),
            creditRepository.listRules()
        );
    }

    public CreditCharge charge(Long userId, String featureCode, String runId, CreditTransactionContext context) {
        String normalizedFeatureCode = CreditFeatureCode.normalize(featureCode);
        int cost = creditRepository.ruleCost(normalizedFeatureCode);
        if (userId == null || isExempt(userId)) {
            return CreditCharge.skipped(userId, normalizedFeatureCode, cost);
        }
        MybatisCreditRepository.ChargeResult result = creditRepository.consume(
            userId,
            normalizedFeatureCode,
            cost,
            context == null ? new CreditTransactionContext(runId, "", "", "", java.util.Map.of()) : context
        );
        if (!result.success()) {
            throw new InsufficientCreditsException(userId, normalizedFeatureCode, cost, result.balanceBefore());
        }
        return new CreditCharge(
            true,
            userId,
            normalizedFeatureCode,
            cost,
            result.balanceBefore(),
            result.balanceAfter(),
            result.transactionId()
        );
    }

    public void refund(CreditCharge charge, CreditTransactionContext context) {
        if (charge == null || !charge.charged() || charge.cost() <= 0) {
            return;
        }
        creditRepository.refund(charge.userId(), charge.featureCode(), charge.cost(), context);
    }

    public List<AdminCreditUserResponse> listUsers(String keyword) {
        return creditRepository.listUsers(keyword);
    }

    public List<AdminCreditTransactionResponse> listTransactions(Long userId) {
        return creditRepository.listTransactions(userId);
    }

    public AdminCreditUserResponse adjust(Long userId, Integer amount, String reason) {
        int normalizedAmount = amount == null ? 0 : amount;
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedAmount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_credit_adjustment", "调整积分不能为 0");
        }
        if (normalizedReason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_credit_adjustment_reason", "调整原因不能为空");
        }
        return creditRepository.adjust(userId, normalizedAmount, normalizedReason);
    }

    public List<AdminCreditRuleResponse> listRules() {
        return creditRepository.listRules();
    }

    public AdminCreditRuleResponse updateRule(String featureCode, Integer cost) {
        if (cost == null || cost < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_credit_rule_cost", "积分消耗值必须为非负整数");
        }
        return creditRepository.updateRule(featureCode, cost);
    }

    private boolean isExempt(Long userId) {
        return creditRepository.isAdminUsername(userId);
    }
}
