package com.jiandou.api.task.application.port;

import java.util.List;

/**
 * 任务队列端口定义。
 */
public interface TaskQueuePort {

    /**
     * 将enqueue加入队列。
     * @param taskId 任务标识
     */
    void enqueue(String taskId);

    /**
     * 移除remove。
     * @param taskId 任务标识
     */
    void remove(String taskId);

    /**
     * 领取Next。
     * @param workerInstanceId 工作节点实例标识
     * @return 处理结果
     */
    String claimNext(String workerInstanceId);

    /**
     * 处理快照。
     * @return 处理结果
     */
    List<String> snapshot();
}
