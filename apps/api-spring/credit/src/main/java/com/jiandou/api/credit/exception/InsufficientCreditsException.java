package com.jiandou.api.credit.exception;

import com.jiandou.api.common.exception.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * 积分不足异常。
 */
public class InsufficientCreditsException extends ApiException {

    private final Map<String, Object> details;

    public InsufficientCreditsException(Long userId, String featureCode, int required, int available) {
        super(HttpStatus.PAYMENT_REQUIRED, "insufficient_credits", "积分不足，请联系管理员充值");
        this.details = Map.of(
            "userId", userId == null ? 0L : userId,
            "featureCode", featureCode == null ? "" : featureCode,
            "required", Math.max(0, required),
            "available", Math.max(0, available)
        );
    }

    public Map<String, Object> details() {
        return details;
    }
}
