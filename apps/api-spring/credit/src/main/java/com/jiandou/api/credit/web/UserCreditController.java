package com.jiandou.api.credit.web;

import com.jiandou.api.auth.security.SecurityCurrentUser;
import com.jiandou.api.config.ApiPathConstants;
import com.jiandou.api.credit.application.UserCreditService;
import com.jiandou.api.credit.dto.UserCreditResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPathConstants.AUTH)
public class UserCreditController {

    private final UserCreditService creditService;

    public UserCreditController(UserCreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/credits")
    public UserCreditResponse credits() {
        return creditService.currentUserCredits(SecurityCurrentUser.requireCurrentUserId());
    }
}
