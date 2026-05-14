package com.jiandou.api.auth.web.dto;

/**
 * 管理端更新用户密码请求。
 * @param password 新密码
 */
public record UpdateAdminUserPasswordRequest(
    String password
) {
}
