package com.jiandou.api.task.infrastructure.mybatis;

import java.time.OffsetDateTime;

/**
 * 用户队列统计投影视图。
 * @param ownerUserId 归属用户标识
 * @param runningTaskCount 运行中任务数量
 * @param queuedTaskCount 排队中任务数量
 * @param oldestQueuedTaskId 最早排队任务标识
 * @param oldestQueuedTaskTitle 最早排队任务标题
 * @param oldestQueuedTaskCreatedAt 最早排队任务创建时间
 */
public record UserQueueStatsRow(
    Long ownerUserId,
    Long runningTaskCount,
    Long queuedTaskCount,
    String oldestQueuedTaskId,
    String oldestQueuedTaskTitle,
    OffsetDateTime oldestQueuedTaskCreatedAt
) {
}
