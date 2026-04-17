package com.jiandou.api.task.application.port;

import com.jiandou.api.task.persistence.TaskAttemptRow;
import com.jiandou.api.task.persistence.TaskRow;
import com.jiandou.api.task.persistence.TaskStageRunRow;
import com.jiandou.api.task.persistence.TaskStatusHistoryRow;
import java.util.List;

/**
 * 任务Aggregate查询端口定义。
 */
public interface TaskAggregateQueryPort {

    /**
     * 加载任务。
     * @param taskId 任务标识
     * @return 处理结果
     */
    TaskRow loadTask(String taskId);

    /**
     * 列出任务。
     * @param query 查询值
     * @param status 状态值
     * @return 处理结果
     */
    List<TaskRow> listTasks(String query, String status);

    /**
     * 列出Attempts。
     * @param taskId 任务标识
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<TaskAttemptRow> listAttempts(String taskId, int limit);

    /**
     * 列出阶段Runs。
     * @param taskId 任务标识
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<TaskStageRunRow> listStageRuns(String taskId, int limit);

    /**
     * 列出状态History。
     * @param taskId 任务标识
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<TaskStatusHistoryRow> listStatusHistory(String taskId, int limit);
}
