package com.jiandou.api.auth.web.dto;

/**
 * 管理端创建用户请求。
 * @param username 用户名
 * @param displayName 显示名
 * @param password 初始密码
 * @param role 角色
 * @param status 状态
 */
public record CreateAdminUserRequest(
    String username,
    String displayName,
    String password,
    String role,
    String status
) {
}
