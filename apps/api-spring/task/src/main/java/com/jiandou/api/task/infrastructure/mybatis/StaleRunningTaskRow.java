package com.jiandou.api.task.infrastructure.mybatis;

/**
 * StaleRunning任务投影视图。
 * @param taskId 任务标识
 * @param workerInstanceId 工作节点实例标识
 */
public record StaleRunningTaskRow(
    String taskId,
    String workerInstanceId
) {
}
