package com.jiandou.api.task.infrastructure.mybatis;

/**
 * 队列Candidate投影视图。
 * @param taskAttemptId 任务尝试标识值
 * @param taskId 任务标识
 * @param ownerUserId 归属用户标识
 * @param taskConcurrencyLimit 任务并发额度
 * @param runningTaskCount 运行中任务数量
 */
public record QueueCandidateRow(
    String taskAttemptId,
    String taskId,
    Long ownerUserId,
    Integer taskConcurrencyLimit,
    Long runningTaskCount
) {
}
