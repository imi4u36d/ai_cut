package com.jiandou.api.task.exception;

import com.jiandou.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 任务或任务关联资源不存在时抛出的异常。
 */
public class TaskNotFoundException extends ApiException {

    /**
     * 创建新的任务NotFound异常。
     * @param taskId 任务标识
     */
    public TaskNotFoundException(String taskId) {
        super(HttpStatus.NOT_FOUND, "task_not_found", "task not found: " + taskId);
    }
}
