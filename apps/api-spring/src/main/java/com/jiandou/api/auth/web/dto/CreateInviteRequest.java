package com.jiandou.api.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建邀请码请求。
 * @param role 角色
 */
public record CreateInviteRequest(
    @NotBlank(message = "角色不能为空")
    String role
) {
}
