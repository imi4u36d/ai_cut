package com.jiandou.api.credit.web;

import com.jiandou.api.config.ApiPathConstants;
import com.jiandou.api.credit.application.UserCreditService;
import com.jiandou.api.credit.dto.AdminCreditRuleResponse;
import com.jiandou.api.credit.dto.AdminCreditTransactionResponse;
import com.jiandou.api.credit.dto.AdminCreditUserResponse;
import com.jiandou.api.credit.dto.CreditAdjustmentRequest;
import com.jiandou.api.credit.dto.CreditRuleUpdateRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPathConstants.ADMIN + "/credits")
public class AdminCreditController {

    private final UserCreditService creditService;

    public AdminCreditController(UserCreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/users")
    public List<AdminCreditUserResponse> listUsers(@RequestParam(value = "q", required = false) String q) {
        return creditService.listUsers(q);
    }

    @GetMapping("/users/{id}/transactions")
    public List<AdminCreditTransactionResponse> listTransactions(@PathVariable Long id) {
        return creditService.listTransactions(id);
    }

    @PostMapping("/users/{id}/adjust")
    public AdminCreditUserResponse adjust(@PathVariable Long id, @RequestBody CreditAdjustmentRequest request) {
        return creditService.adjust(id, request == null ? null : request.amount(), request == null ? "" : request.reason());
    }

    @GetMapping("/rules")
    public List<AdminCreditRuleResponse> listRules() {
        return creditService.listRules();
    }

    @PutMapping("/rules/{featureCode}")
    public AdminCreditRuleResponse updateRule(@PathVariable String featureCode, @RequestBody CreditRuleUpdateRequest request) {
        return creditService.updateRule(featureCode, request == null ? null : request.cost());
    }
}
