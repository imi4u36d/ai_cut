package com.jiandou.api.task;

/**
 * 任务工作节点执行记录结构。
 * @param workerInstanceId 工作节点实例标识
 * @param workerType 工作节点类型值
 * @param executionMode 执行模式值
 */
record TaskWorkerExecutionContext(
    String workerInstanceId,
    String workerType,
    String executionMode
) {
}
