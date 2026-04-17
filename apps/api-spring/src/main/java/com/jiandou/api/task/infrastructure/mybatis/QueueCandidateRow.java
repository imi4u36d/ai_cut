package com.jiandou.api.task.infrastructure.mybatis;

/**
 * 队列Candidate投影视图。
 * @param taskAttemptId 任务尝试标识值
 * @param taskId 任务标识
 */
public record QueueCandidateRow(
    String taskAttemptId,
    String taskId
) {
}
