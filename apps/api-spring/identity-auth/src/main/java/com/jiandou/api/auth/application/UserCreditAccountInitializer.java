package com.jiandou.api.auth.application;

/**
 * Initializes credit state for newly activated or created users.
 */
public interface UserCreditAccountInitializer {

    void ensureAccount(Long userId);
}
