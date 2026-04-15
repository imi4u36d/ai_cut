package com.jiandou.api.task;

/**
 * 任务执行Aborted异常。
 */
final class TaskExecutionAbortedException extends RuntimeException {

    private final String taskStatus;

    TaskExecutionAbortedException(String taskStatus, String message) {
        super(message);
        this.taskStatus = taskStatus == null ? "" : taskStatus;
    }

    /**
     * 处理任务状态。
     * @return 处理结果
     */
    String taskStatus() {
        return taskStatus;
    }
}
