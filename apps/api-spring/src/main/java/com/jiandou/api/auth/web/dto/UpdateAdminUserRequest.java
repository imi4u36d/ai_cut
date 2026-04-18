package com.jiandou.api.auth.web.dto;

/**
 * 管理端更新用户请求。
 * @param displayName 显示名
 * @param role 角色
 * @param status 状态
 */
public record UpdateAdminUserRequest(
    String displayName,
    String role,
    String status
) {
}
