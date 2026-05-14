package com.jiandou.api.auth.web.dto;

import java.time.OffsetDateTime;

/**
 * 管理端用户响应。
 * @param id 主键ID
 * @param username 用户名
 * @param displayName 显示名
 * @param role 角色
 * @param status 状态
 * @param taskConcurrencyLimit 任务并发额度
 * @param runningTaskCount 运行中任务数
 * @param queuedTaskCount 排队中任务数
 * @param lastLoginAt 最近登录时间
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AdminUserResponse(
    Long id,
    String username,
    String displayName,
    String role,
    String status,
    int taskConcurrencyLimit,
    long runningTaskCount,
    long queuedTaskCount,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
