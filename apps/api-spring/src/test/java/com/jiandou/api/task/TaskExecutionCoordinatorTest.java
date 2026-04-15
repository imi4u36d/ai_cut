package com.jiandou.api.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.jiandou.api.task.application.port.TaskQueuePort;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 任务执行相关测试。
 */
class TaskExecutionCoordinatorTest {

    /**
     * 执行任务Persists任务追踪And状态HistoryInSingle变更流转。
     */
    @Test
    void transitionTaskPersistsTaskTraceAndStatusHistoryInSingleMutation() {
        FakeTaskRepository repository = new FakeTaskRepository();
        TaskExecutionCoordinator coordinator = new TaskExecutionCoordinator(new NoopQueuePort(), repository);
        TaskRecord task = task("task_transition", "PENDING");

        coordinator.transitionTask(
            task,
            TaskStateTransition.info("ANALYZING", 10, "analysis", "task.analyzing", "任务开始分析。", Map.of("worker", "w1"))
        );

        assertEquals("ANALYZING", task.status);
        assertEquals(10, task.progress);
        assertEquals(1, task.traceView().size());
        assertEquals(1, task.statusHistory.size());
        assertNotNull(task.updatedAt);
        assertSame(task, repository.lastMutation.task());
        assertEquals(1, repository.lastMutation.traceRows().size());
        assertEquals(1, repository.lastMutation.statusHistoryRows().size());
        assertEquals(0, repository.lastMutation.attempts().size());
        assertEquals(0, repository.lastMutation.queueEventRows().size());
    }

    /**
     * 执行任务CanFinishActive尝试Atomically流转。
     */
    @Test
    void transitionTaskCanFinishActiveAttemptAtomically() {
        FakeTaskRepository repository = new FakeTaskRepository();
        TaskExecutionCoordinator coordinator = new TaskExecutionCoordinator(new NoopQueuePort(), repository);
        TaskRecord task = task("task_failed", "RENDERING");
        task.activeAttemptId = "att_1";
        Map<String, Object> attempt = new LinkedHashMap<>();
        attempt.put("attemptId", "att_1");
        attempt.put("status", "RUNNING");
        task.attempts.add(attempt);

        coordinator.transitionTask(
            task,
            TaskStateTransition.error("FAILED", 77, "pipeline", "task.failed", "执行失败。", Map.of("error", "boom")).withAttempt("FAILED", "boom"),
            currentTask -> {
                currentTask.errorMessage = "boom";
                currentTask.finishedAt = currentTask.nowIso();
            }
        );

        assertEquals("FAILED", task.status);
        assertEquals("boom", task.errorMessage);
        assertEquals("FAILED", attempt.get("status"));
        assertNotNull(attempt.get("finishedAt"));
        assertSame(task, repository.lastMutation.task());
        assertEquals(1, repository.lastMutation.attempts().size());
        assertEquals(1, repository.lastMutation.queueEventRows().size());
        assertEquals(1, repository.lastMutation.traceRows().size());
        assertEquals(1, repository.lastMutation.statusHistoryRows().size());
    }

    /**
     * 处理任务。
     * @param id 标识值
     * @param status 状态值
     * @return 处理结果
     */
    private TaskRecord task(String id, String status) {
        TaskRecord task = new TaskRecord();
        task.id = id;
        task.title = id;
        task.status = status;
        task.createdAt = "2026-04-14T00:00:00Z";
        task.updatedAt = task.createdAt;
        return task;
    }

    private static final class NoopQueuePort implements TaskQueuePort {

        /**
         * 将enqueue加入队列。
         * @param taskId 任务标识
         */
        @Override
        public void enqueue(String taskId) {
        }

        /**
         * 移除remove。
         * @param taskId 任务标识
         */
        @Override
        public void remove(String taskId) {
        }

        /**
         * 领取Next。
         * @param workerInstanceId 工作节点实例标识
         * @return 处理结果
         */
        @Override
        public String claimNext(String workerInstanceId) {
            return "";
        }

        /**
         * 处理快照。
         * @return 处理结果
         */
        @Override
        public List<String> snapshot() {
            return List.of();
        }
    }

    private static final class FakeTaskRepository implements TaskRepository {

        private TaskPersistenceMutation lastMutation;

        /**
         * 保存save。
         * @param task 要处理的任务对象
         */
        @Override
        public void save(TaskRecord task) {
        }

        /**
         * 保存变更。
         * @param mutation 变更值
         */
        @Override
        public void saveMutation(TaskPersistenceMutation mutation) {
            lastMutation = mutation;
        }

        /**
         * 查找工作节点Instance。
         * @param workerInstanceId 工作节点实例标识
         * @return 处理结果
         */
        @Override
        public Map<String, Object> findWorkerInstance(String workerInstanceId) {
            return Map.of();
        }

        /**
         * 列出工作节点Instances。
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<Map<String, Object>> listWorkerInstances(int limit) {
            return List.of();
        }

        /**
         * 移除Queued任务。
         * @param taskId 任务标识
         */
        @Override
        public void removeQueuedTask(String taskId) {
        }

        /**
         * 领取NextQueued任务。
         * @param workerInstanceId 工作节点实例标识
         * @return 处理结果
         */
        @Override
        public String claimNextQueuedTask(String workerInstanceId) {
            return "";
        }

        /**
         * 列出Queued任务标识列表。
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<String> listQueuedTaskIds(int limit) {
            return List.of();
        }

        /**
         * 列出StaleRunningClaims。
         * @param staleBefore staleBefore值
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<Map<String, Object>> listStaleRunningClaims(OffsetDateTime staleBefore, int limit) {
            return List.of();
        }

        /**
         * 列出Stale工作节点Instance标识列表。
         * @param staleBefore staleBefore值
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<String> listStaleWorkerInstanceIds(OffsetDateTime staleBefore, int limit) {
            return List.of();
        }

        /**
         * 列出队列Events。
         * @param taskId 任务标识
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<Map<String, Object>> listQueueEvents(String taskId, int limit) {
            return List.of();
        }

        /**
         * 列出Traces。
         * @param taskId 任务标识
         * @param stage 阶段名称
         * @param level level值
         * @param query 查询值
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<Map<String, Object>> listTraces(String taskId, String stage, String level, String query, int limit) {
            return List.of();
        }

        /**
         * 查找By标识。
         * @param taskId 任务标识
         * @return 处理结果
         */
        @Override
        public TaskRecord findById(String taskId) {
            return null;
        }

        /**
         * 查找All。
         * @return 处理结果
         */
        @Override
        public Collection<TaskRecord> findAll() {
            return List.of();
        }

        /**
         * 删除删除。
         * @param taskId 任务标识
         */
        @Override
        public void delete(String taskId) {
        }
    }
}
