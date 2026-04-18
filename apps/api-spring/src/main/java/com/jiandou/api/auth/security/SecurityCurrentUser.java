package com.jiandou.api.auth.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文读取工具。
 */
public final class SecurityCurrentUser {

    private SecurityCurrentUser() {
    }

    /**
     * 返回当前登录用户 ID。
     * @return 处理结果
     */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken
            || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }
}
