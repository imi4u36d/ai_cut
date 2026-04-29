package com.jiandou.api.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jiandou.api.auth.infrastructure.mybatis.MybatisAuthRepository;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserEntity;
import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.config.JiandouStorageProperties;
import com.jiandou.api.config.JiandouTaskOpsProperties;
import com.jiandou.api.task.application.TaskDiagnosisService;
import com.jiandou.api.task.application.TaskExecutionCoordinator;
import com.jiandou.api.task.application.TaskQueryService;
import com.jiandou.api.task.application.port.TaskQueuePort;
import com.jiandou.api.task.exception.TaskNotFoundException;
import com.jiandou.api.task.persistence.TaskPersistenceMutation;
import com.jiandou.api.task.persistence.TaskRepository;
import com.jiandou.api.task.view.TaskViewMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 任务查询相关测试。
 */
class TaskQueryServiceTest {

    private FakeTaskRepository taskRepository;
    private RecordingTaskExecutionCoordinator executionCoordinator;
    private TaskQueryService service;

    /**
     * 处理setUp。
     */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authenticate(88L);
        taskRepository = new FakeTaskRepository();
        executionCoordinator = new RecordingTaskExecutionCoordinator(taskRepository);
        TaskViewMapper taskViewMapper = new TaskViewMapper(storageProperties("../../storage"));
        TaskDiagnosisService diagnosisService = new TaskDiagnosisService(taskViewMapper);
        service = new TaskQueryService(
            taskRepository,
            taskViewMapper,
            executionCoordinator,
            diagnosisService,
            new JiandouTaskOpsProperties(),
            fakeAuthRepository()
        );
    }

    /**
     * 列出任务SupportsRunningAlias状态筛选。
     */
    @Test
    void listTasksSupportsRunningAliasStatusFilter() {
        TaskRecord running = task("task_running", "RUNNING", "2026-04-14T00:00:02Z");
        TaskRecord planning = task("task_planning", "PLANNING", "2026-04-14T00:00:01Z");
        TaskRecord pending = task("task_pending", "PENDING", "2026-04-14T00:00:00Z");
        taskRepository.tasks = List.of(running, planning, pending);

        List<Map<String, Object>> rows = service.listTasks(null, "RUNNING", "updated_desc");

        assertEquals(2, rows.size());
        assertEquals("task_running", rows.get(0).get("id"));
        assertEquals("task_planning", rows.get(1).get("id"));
        assertEquals(1, executionCoordinator.recomputeCalls);
    }

    /**
     * 列出任务SupportsQueuedAlias状态筛选。
     */
    @Test
    void listTasksSupportsQueuedAliasStatusFilter() {
        TaskRecord queued = task("task_queued", "PENDING", "2026-04-14T00:00:02Z");
        queued.setQueued(true);
        queued.setQueuePosition(1);
        TaskRecord pending = task("task_not_queued", "PENDING", "2026-04-14T00:00:01Z");
        taskRepository.tasks = List.of(queued, pending);
        executionCoordinator.snapshot = List.of("task_queued");

        List<Map<String, Object>> rows = service.listTasks(null, "queued", "updated_desc");

        assertEquals(1, rows.size());
        assertEquals("task_queued", rows.get(0).get("id"));
    }

    /**
     * 处理管理概览CountsRunning任务WithRunning状态Alias。
     */
    @Test
    void adminOverviewCountsRunningTasksWithRunningStatusAlias() {
        TaskRecord running = task("task_running", "RUNNING", "2026-04-14T00:00:02Z");
        TaskRecord rendering = task("task_rendering", "RENDERING", "2026-04-14T00:00:01Z");
        TaskRecord completed = task("task_completed", "COMPLETED", "2026-04-14T00:00:00Z");
        taskRepository.tasks = List.of(running, rendering, completed);
        taskRepository.queueEvents = List.of();
        taskRepository.workerInstances = List.of(Map.of("status", "RUNNING"));
        taskRepository.traces = List.of();
        taskRepository.tasksById.put("task_running", running);
        executionCoordinator.snapshot = List.of("task_running");

        Map<String, Object> payload = service.adminOverview();

        Map<?, ?> counts = assertInstanceOf(Map.class, payload.get("counts"));
        assertEquals(2, counts.get("runningTasks"));
        List<?> recentRunningTasks = assertInstanceOf(List.class, payload.get("recentRunningTasks"));
        assertEquals(2, recentRunningTasks.size());
    }

    @Test
    void listTasksOnlyReturnsCurrentUserTasks() {
        TaskRecord own = task("task_own", "COMPLETED", "2026-04-14T00:00:02Z");
        own.setOwnerUserId(88L);
        TaskRecord other = task("task_other", "COMPLETED", "2026-04-14T00:00:01Z");
        other.setOwnerUserId(99L);
        TaskRecord legacy = task("task_legacy", "COMPLETED", "2026-04-14T00:00:00Z");
        legacy.setOwnerUserId(null);
        taskRepository.tasks = List.of(own, other, legacy);

        List<Map<String, Object>> rows = service.listTasks(null, null, "updated_desc");

        assertEquals(1, rows.size());
        assertEquals("task_own", rows.get(0).get("id"));
    }

    @Test
    void requireOwnedTaskRejectsOtherUserTask() {
        TaskRecord own = task("task_own", "COMPLETED", "2026-04-14T00:00:02Z");
        own.setOwnerUserId(88L);
        TaskRecord other = task("task_other", "COMPLETED", "2026-04-14T00:00:01Z");
        other.setOwnerUserId(99L);
        taskRepository.tasks = List.of(own, other);

        assertEquals("task_own", service.getTask("task_own").get("id"));
        assertThrows(TaskNotFoundException.class, () -> service.getTask("task_other"));
        assertThrows(TaskNotFoundException.class, () -> service.getTrace("task_other", 10));
    }

    /**
     * 处理任务。
     * @param id 标识值
     * @param status 状态值
     * @param updatedAt updatedAt值
     * @return 处理结果
     */
    private TaskRecord task(String id, String status, String updatedAt) {
        TaskRecord task = new TaskRecord();
        task.setId(id);
        task.setOwnerUserId(88L);
        task.setTitle(id);
        task.setStatus(status);
        task.setCreatedAt(updatedAt);
        task.setUpdatedAt(updatedAt);
        task.setExecutionContext(new LinkedHashMap<>());
        taskRepository.tasksById.put(id, task);
        return task;
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new CurrentUserPrincipal(userId, "tester", "Tester", "USER", "ACTIVE"),
                null,
                List.of()
            )
        );
    }

    private JiandouStorageProperties storageProperties(String rootDir) {
        JiandouStorageProperties properties = new JiandouStorageProperties();
        properties.setRootDir(rootDir);
        return properties;
    }

    private MybatisAuthRepository fakeAuthRepository() {
        return new MybatisAuthRepository(null) {
            @Override
            public Map<Long, SysUserEntity> findUsersByIds(Collection<Long> ids) {
                return Map.of();
            }
        };
    }

    private static final class RecordingTaskExecutionCoordinator extends TaskExecutionCoordinator {

        private int recomputeCalls;
        private List<String> snapshot = List.of();

        /**
         * 处理Recording任务执行协调器。
         * @param taskRepository 任务仓储值
         */
        private RecordingTaskExecutionCoordinator(TaskRepository taskRepository) {
            super(new NoopQueuePort(), taskRepository);
        }

        /**
         * 重新计算队列Positions。
         * @param tasks 任务值
         */
        @Override
        public void recomputeQueuePositions(Collection<TaskRecord> tasks) {
            recomputeCalls += 1;
            super.recomputeQueuePositions(tasks);
        }

        /**
         * 处理队列快照。
         * @return 处理结果
         */
        @Override
        public List<String> queueSnapshot() {
            return new ArrayList<>(snapshot);
        }
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

        private Collection<TaskRecord> tasks = List.of();
        private final Map<String, TaskRecord> tasksById = new LinkedHashMap<>();
        private List<Map<String, Object>> queueEvents = List.of();
        private List<Map<String, Object>> workerInstances = List.of();
        private List<Map<String, Object>> traces = List.of();

        /**
         * 查找All。
         * @return 处理结果
         */
        @Override
        public Collection<TaskRecord> findAll() {
            return tasks;
        }

        /**
         * 查找By标识。
         * @param taskId 任务标识
         * @return 处理结果
         */
        @Override
        public TaskRecord findById(String taskId) {
            return tasksById.get(taskId);
        }

        /**
         * 列出队列Events。
         * @param taskId 任务标识
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<Map<String, Object>> listQueueEvents(String taskId, int limit) {
            return queueEvents;
        }

        /**
         * 列出工作节点Instances。
         * @param limit 返回的最大条目数
         * @return 处理结果
         */
        @Override
        public List<Map<String, Object>> listWorkerInstances(int limit) {
            return workerInstances;
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
            return traces;
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
         * 删除删除。
         * @param taskId 任务标识
         */
        @Override
        public void delete(String taskId) {
        }
    }
}
