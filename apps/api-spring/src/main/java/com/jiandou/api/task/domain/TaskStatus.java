package com.jiandou.api.task.domain;

/**
 * 任务状态枚举。
 */
public enum TaskStatus {
    PENDING,
    PAUSED,
    ANALYZING,
    PLANNING,
    RENDERING,
    COMPLETED,
    FAILED
}
