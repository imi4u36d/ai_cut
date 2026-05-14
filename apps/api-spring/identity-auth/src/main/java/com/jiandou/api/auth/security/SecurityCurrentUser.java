package com.jiandou.api.auth.security;

import com.jiandou.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;
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

    /**
     * 返回当前登录用户 ID，未登录时抛出统一未授权异常。
     * @return 处理结果
     */
    public static Long requireCurrentUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "请先登录");
        }
        return userId;
    }
}
