package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jiandou.api.task.TaskRecord;
import com.jiandou.api.task.domain.TaskQueueFairScheduler;
import com.jiandou.api.task.observability.StructuredApplicationLogger;
import com.jiandou.api.task.persistence.TaskPersistenceMutation;
import com.jiandou.api.task.persistence.TaskRecordAssembler;
import com.jiandou.api.task.persistence.TaskRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

/**
 * MyBatis任务仓储契约。
 */
@Repository
public class MybatisTaskRepository implements TaskRepository {

    private static final String SYSTEM_QUEUE_LOCK = "jiandou:task_queue:system";
    private static final int QUEUE_CANDIDATES_PER_OWNER_LIMIT = 100;
    private static final int QUEUE_SNAPSHOT_PER_OWNER_LIMIT = 500;

    private final SqlSessionFactory sqlSessionFactory;
    private final TaskRecordAssembler taskRecordAssembler;
    private final TaskMybatisWriteMapper writeMapper;
    private final TaskMybatisReadMapper readMapper;

    /**
     * 创建新的MyBatis任务仓储。
     * @param sqlSessionFactory sqlSession工厂值
     * @param taskRecordAssembler 任务记录Assembler值
     */
    public MybatisTaskRepository(
        SqlSessionFactory sqlSessionFactory,
        TaskRecordAssembler taskRecordAssembler
    ) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.taskRecordAssembler = taskRecordAssembler;
        this.writeMapper = new TaskMybatisWriteMapper();
        this.readMapper = new TaskMybatisReadMapper();
    }

    /**
     * 保存save。
     * @param task 要处理的任务对象
     */
    @Override
    public void save(TaskRecord task) {
        TaskRecordAssembler.TaskWriteModel model = taskRecordAssembler.toWriteModel(task);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            saveTask(session, task);
        }
    }

    /**
     * 保存变更。
     * @param mutation 变更值
     */
    @Override
    public void saveMutation(TaskPersistenceMutation mutation) {
        if (mutation == null) {
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                TaskRecord task = mutation.task();
                String taskId = mutation.taskId();
                if ((taskId == null || taskId.isBlank()) && task != null) {
                    taskId = taskRecordAssembler.toWriteModel(task).taskId();
                }
                if ((taskId == null || taskId.isBlank()) && requiresTaskId(mutation)) {
                    throw new IllegalArgumentException("Task persistence mutation requires taskId for task-scoped rows.");
                }
                if (task != null) {
                    saveTask(session, task);
                }
                for (Map<String, Object> attempt : mutation.attempts()) {
                    saveAttempt(session, taskId, attempt);
                }
                for (Map<String, Object> statusHistory : mutation.statusHistoryRows()) {
                    saveStatusHistory(session, taskId, statusHistory);
                }
                for (Map<String, Object> trace : mutation.traceRows()) {
                    StructuredApplicationLogger.logTaskTrace(taskId, trace);
                }
                for (Map<String, Object> stageRun : mutation.stageRunRows()) {
                    saveStageRun(session, taskId, stageRun);
                }
                for (Map<String, Object> modelCall : mutation.modelCallRows()) {
                    saveModelCall(session, taskId, modelCall);
                }
                for (Map<String, Object> requestLog : mutation.requestLogRows()) {
                    saveRequestLog(session, taskId, requestLog);
                }
                for (Map<String, Object> material : mutation.materialRows()) {
                    saveMaterial(session, taskId, material);
                }
                for (Map<String, Object> result : mutation.resultRows()) {
                    saveResult(session, taskId, result);
                }
                for (Map<String, Object> queueEvent : mutation.queueEventRows()) {
                    saveQueueEvent(session, taskId, queueEvent);
                }
                for (Map<String, Object> workerInstance : mutation.workerInstanceRows()) {
                    saveWorkerInstance(session, workerInstance);
                }
                session.commit();
            } catch (RuntimeException ex) {
                session.rollback();
                throw ex;
            }
        }
    }

    /**
     * 查找工作节点Instance。
     * @param workerInstanceId 工作节点实例标识
     * @return 处理结果
     */
    @Override
    public Map<String, Object> findWorkerInstance(String workerInstanceId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            WorkerInstanceMapper mapper = session.getMapper(WorkerInstanceMapper.class);
            WorkerInstanceEntity entity = mapper.selectOne(
                Wrappers.<WorkerInstanceEntity>lambdaQuery()
                    .eq(WorkerInstanceEntity::getWorkerInstanceId, workerInstanceId)
                    .eq(WorkerInstanceEntity::getIsDeleted, 0)
                    .last("LIMIT 1")
            );
            return entity == null ? Map.of() : readMapper.toWorkerInstanceMap(entity);
        }
    }

    /**
     * 列出工作节点Instances。
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    @Override
    public List<Map<String, Object>> listWorkerInstances(int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            WorkerInstanceMapper mapper = session.getMapper(WorkerInstanceMapper.class);
            return mapper.selectList(
                Wrappers.<WorkerInstanceEntity>lambdaQuery()
                    .eq(WorkerInstanceEntity::getIsDeleted, 0)
                    .orderByDesc(WorkerInstanceEntity::getLastHeartbeatAt)
                    .last("LIMIT " + Math.max(1, limit))
            ).stream().map(readMapper::toWorkerInstanceMap).toList();
        }
    }

    /**
     * 移除Queued任务。
     * @param taskId 任务标识
     */
    @Override
    public void removeQueuedTask(String taskId) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            TaskAttemptMapper mapper = session.getMapper(TaskAttemptMapper.class);
            mapper.removeQueuedAttempts(taskId, OffsetDateTime.now());
        }
    }

    /**
     * 领取NextQueued任务。
     * @param workerInstanceId 工作节点实例标识
     * @return 处理结果
     */
    @Override
    public String claimNextQueuedTask(String workerInstanceId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            TaskAttemptMapper mapper = session.getMapper(TaskAttemptMapper.class);
            List<QueueCandidateRow> candidates = fairOrder(mapper.selectQueueCandidates(QUEUE_CANDIDATES_PER_OWNER_LIMIT));
            OffsetDateTime now = OffsetDateTime.now();
            for (QueueCandidateRow candidate : candidates) {
                String lockName = null;
                boolean locked = false;
                try {
                    if (candidate.ownerUserId() == null) {
                        lockName = SYSTEM_QUEUE_LOCK;
                        locked = Integer.valueOf(1).equals(mapper.acquireNamedLock(lockName, 1));
                        if (!locked) {
                            continue;
                        }
                    } else {
                        Long lockedOwnerUserId = mapper.lockOwnerUser(candidate.ownerUserId());
                        if (lockedOwnerUserId == null) {
                            session.rollback();
                            continue;
                        }
                        locked = true;
                    }
                    int runningCount = candidate.ownerUserId() == null
                        ? mapper.countRunningSystemTasks()
                        : mapper.countRunningTasksForOwner(candidate.ownerUserId());
                    int limit = taskConcurrencyLimit(candidate);
                    if (runningCount >= limit) {
                        session.rollback();
                        continue;
                    }
                    int updated = mapper.claimAttempt(candidate.taskAttemptId(), workerInstanceId, now);
                    if (updated == 1) {
                        session.commit();
                        rememberLastDispatchedOwner(candidate.ownerUserId());
                        return candidate.taskId();
                    }
                    session.rollback();
                } finally {
                    if (lockName != null && locked) {
                        mapper.releaseNamedLock(lockName);
                    }
                }
            }
            session.rollback();
            return "";
        }
    }

    /**
     * 列出Queued任务标识列表。
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    @Override
    public List<String> listQueuedTaskIds(int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskAttemptMapper mapper = session.getMapper(TaskAttemptMapper.class);
            return fairOrder(mapper.selectQueuedTaskCandidates(QUEUE_SNAPSHOT_PER_OWNER_LIMIT)).stream()
                .map(QueueCandidateRow::taskId)
                .limit(Math.max(1, limit))
                .toList();
        }
    }

    /**
     * 返回用户队列状态。
     * @return 处理结果
     */
    @Override
    public List<Map<String, Object>> listUserQueueStats() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskAttemptMapper mapper = session.getMapper(TaskAttemptMapper.class);
            return mapper.selectUserQueueStats().stream().map(row -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("ownerUserId", row.ownerUserId());
                item.put("runningTaskCount", row.runningTaskCount() == null ? 0L : row.runningTaskCount());
                item.put("queuedTaskCount", row.queuedTaskCount() == null ? 0L : row.queuedTaskCount());
                item.put("oldestQueuedTaskId", row.oldestQueuedTaskId() == null ? "" : row.oldestQueuedTaskId());
                item.put("oldestQueuedTaskTitle", row.oldestQueuedTaskTitle() == null ? "" : row.oldestQueuedTaskTitle());
                item.put("oldestQueuedTaskCreatedAt", TaskMybatisValueSupport.format(row.oldestQueuedTaskCreatedAt()));
                return item;
            }).toList();
        }
    }

    /**
     * 列出StaleRunningClaims。
     * @param staleBefore staleBefore值
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    @Override
    public List<Map<String, Object>> listStaleRunningClaims(OffsetDateTime staleBefore, int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskAttemptMapper mapper = session.getMapper(TaskAttemptMapper.class);
            return mapper.selectStaleRunningTasks(staleBefore, Math.max(1, limit)).stream()
                .map(readMapper::toStaleRunningClaimMap)
                .toList();
        }
    }

    /**
     * 列出Stale工作节点Instance标识列表。
     * @param staleBefore staleBefore值
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    @Override
    public List<String> listStaleWorkerInstanceIds(OffsetDateTime staleBefore, int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            WorkerInstanceMapper mapper = session.getMapper(WorkerInstanceMapper.class);
            return mapper.selectStaleWorkerInstanceIds(staleBefore, Math.max(1, limit));
        }
    }

    /**
     * 列出队列Events。
     * @param taskId 任务标识
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    @Override
    public List<Map<String, Object>> listQueueEvents(String taskId, int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskQueueEventMapper mapper = session.getMapper(TaskQueueEventMapper.class);
            var query = Wrappers.<TaskQueueEventEntity>lambdaQuery()
                .eq(TaskQueueEventEntity::getIsDeleted, 0)
                .orderByDesc(TaskQueueEventEntity::getEventTime)
                .last("LIMIT " + Math.max(1, limit));
            if (taskId != null && !taskId.isBlank()) {
                query.eq(TaskQueueEventEntity::getTaskId, taskId);
            }
            return mapper.selectList(query).stream().map(readMapper::toQueueEventMap).toList();
        }
    }

    /**
     * 列出Traces。
     * @param taskId 任务标识
     * @param stage 阶段名称
     * @param level level值
     * @param queryText 查询文本值
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    @Override
    public List<Map<String, Object>> listTraces(String taskId, String stage, String level, String queryText, int limit) {
        return StructuredApplicationLogger.listRecentTraces(taskId, stage, level, queryText, limit);
    }

    /**
     * 查找By标识。
     * @param taskId 任务标识
     * @return 处理结果
     */
    @Override
    public TaskRecord findById(String taskId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskMapper taskMapper = session.getMapper(TaskMapper.class);
            TaskAttemptMapper taskAttemptMapper = session.getMapper(TaskAttemptMapper.class);
            TaskStageRunMapper taskStageRunMapper = session.getMapper(TaskStageRunMapper.class);
            TaskStatusHistoryMapper taskStatusHistoryMapper = session.getMapper(TaskStatusHistoryMapper.class);
            TaskModelCallMapper taskModelCallMapper = session.getMapper(TaskModelCallMapper.class);
            TaskResultMapper taskResultMapper = session.getMapper(TaskResultMapper.class);
            MaterialAssetMapper materialAssetMapper = session.getMapper(MaterialAssetMapper.class);

            TaskEntity entity = taskMapper.selectOne(
                Wrappers.<TaskEntity>lambdaQuery()
                    .eq(TaskEntity::getTaskId, taskId)
                    .eq(TaskEntity::getIsDeleted, 0)
                    .last("LIMIT 1")
            );
            if (entity == null) {
                return null;
            }
            TaskRecord task = taskRecordAssembler.fromTaskRow(readMapper.toTaskRow(entity));
            readMapper.applyAttempts(task, taskAttemptMapper.selectList(
                Wrappers.<TaskAttemptEntity>lambdaQuery()
                    .eq(TaskAttemptEntity::getTaskId, taskId)
                    .eq(TaskAttemptEntity::getIsDeleted, 0)
                    .orderByDesc(TaskAttemptEntity::getAttemptNo)
            ));
            taskRecordAssembler.applyStatusHistory(task, taskStatusHistoryMapper.selectList(
                Wrappers.<TaskStatusHistoryEntity>lambdaQuery()
                    .eq(TaskStatusHistoryEntity::getTaskId, taskId)
                    .eq(TaskStatusHistoryEntity::getIsDeleted, 0)
                    .orderByDesc(TaskStatusHistoryEntity::getChangeTime)
            ).stream().map(readMapper::toStatusHistoryRow).toList());
            readMapper.applyStageRuns(task, taskStageRunMapper.selectList(
                Wrappers.<TaskStageRunEntity>lambdaQuery()
                    .eq(TaskStageRunEntity::getTaskId, taskId)
                    .eq(TaskStageRunEntity::getIsDeleted, 0)
                    .orderByDesc(TaskStageRunEntity::getStartedAt)
            ));
            readMapper.applyModelCalls(task, taskModelCallMapper.selectList(
                Wrappers.<TaskModelCallEntity>lambdaQuery()
                    .eq(TaskModelCallEntity::getTaskId, taskId)
                    .eq(TaskModelCallEntity::getIsDeleted, 0)
                    .orderByAsc(TaskModelCallEntity::getStartedAt)
            ));
            readMapper.applyMaterials(task, materialAssetMapper.selectList(
                Wrappers.<MaterialAssetEntity>lambdaQuery()
                    .eq(MaterialAssetEntity::getTaskId, taskId)
                    .eq(MaterialAssetEntity::getIsDeleted, 0)
                    .orderByAsc(MaterialAssetEntity::getCreateTime)
            ));
            readMapper.applyResults(task, taskResultMapper.selectList(
                Wrappers.<TaskResultEntity>lambdaQuery()
                    .eq(TaskResultEntity::getTaskId, taskId)
                    .eq(TaskResultEntity::getIsDeleted, 0)
                    .orderByAsc(TaskResultEntity::getClipIndex)
            ));
            return task;
        }
    }

    /**
     * 查找All。
     * @return 处理结果
     */
    @Override
    public Collection<TaskRecord> findAll() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskMapper taskMapper = session.getMapper(TaskMapper.class);
            return taskMapper.selectList(
                Wrappers.<TaskEntity>lambdaQuery()
                    .eq(TaskEntity::getIsDeleted, 0)
                    .orderByDesc(TaskEntity::getCreateTime)
            ).stream().map(readMapper::toTaskRow).map(taskRecordAssembler::fromTaskRow).toList();
        }
    }

    /**
     * 删除删除。
     * @param taskId 任务标识
     */
    @Override
    public void delete(String taskId) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            TaskMapper taskMapper = session.getMapper(TaskMapper.class);
            TaskEntity entity = new TaskEntity();
            entity.setIsDeleted(1);
            taskMapper.update(entity, Wrappers.<TaskEntity>lambdaUpdate().eq(TaskEntity::getTaskId, taskId));
        }
    }

    /**
     * 保存任务。
     * @param session session值
     * @param task 要处理的任务对象
     */
    private void saveTask(SqlSession session, TaskRecord task) {
        TaskMapper mapper = session.getMapper(TaskMapper.class);
        TaskRecordAssembler.TaskWriteModel model = taskRecordAssembler.toWriteModel(task);
        TaskEntity entity = writeMapper.toTaskEntity(model);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<TaskEntity>lambdaQuery()
                    .eq(TaskEntity::getTaskId, model.taskId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(entity, Wrappers.<TaskEntity>lambdaUpdate().eq(TaskEntity::getTaskId, model.taskId()))
        );
    }

    /**
     * 保存尝试。
     * @param session session值
     * @param taskId 任务标识
     * @param attempt 尝试值
     */
    private void saveAttempt(SqlSession session, String taskId, Map<String, Object> attempt) {
        TaskAttemptMapper mapper = session.getMapper(TaskAttemptMapper.class);
        TaskAttemptEntity entity = writeMapper.toAttemptEntity(taskId, attempt);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<TaskAttemptEntity>lambdaQuery()
                    .eq(TaskAttemptEntity::getTaskAttemptId, entity.getTaskAttemptId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<TaskAttemptEntity>lambdaUpdate()
                    .eq(TaskAttemptEntity::getTaskAttemptId, entity.getTaskAttemptId())
            )
        );
    }

    /**
     * 保存状态History。
     * @param session session值
     * @param taskId 任务标识
     * @param statusHistory 状态History值
     */
    private void saveStatusHistory(SqlSession session, String taskId, Map<String, Object> statusHistory) {
        TaskStatusHistoryMapper mapper = session.getMapper(TaskStatusHistoryMapper.class);
        TaskStatusHistoryEntity entity = writeMapper.toStatusHistoryEntity(taskId, statusHistory);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<TaskStatusHistoryEntity>lambdaQuery()
                    .eq(TaskStatusHistoryEntity::getTaskStatusHistoryId, entity.getTaskStatusHistoryId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<TaskStatusHistoryEntity>lambdaUpdate()
                    .eq(TaskStatusHistoryEntity::getTaskStatusHistoryId, entity.getTaskStatusHistoryId())
            )
        );
    }

    /**
     * 保存阶段运行。
     * @param session session值
     * @param taskId 任务标识
     * @param stageRun 阶段运行值
     */
    private void saveStageRun(SqlSession session, String taskId, Map<String, Object> stageRun) {
        TaskStageRunMapper mapper = session.getMapper(TaskStageRunMapper.class);
        TaskStageRunEntity entity = writeMapper.toStageRunEntity(taskId, stageRun);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<TaskStageRunEntity>lambdaQuery()
                    .eq(TaskStageRunEntity::getTaskStageRunId, entity.getTaskStageRunId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<TaskStageRunEntity>lambdaUpdate()
                    .eq(TaskStageRunEntity::getTaskStageRunId, entity.getTaskStageRunId())
            )
        );
    }

    /**
     * 保存模型调用。
     * @param session session值
     * @param taskId 任务标识
     * @param modelCall 模型调用值
     */
    private void saveModelCall(SqlSession session, String taskId, Map<String, Object> modelCall) {
        TaskModelCallMapper mapper = session.getMapper(TaskModelCallMapper.class);
        TaskModelCallEntity entity = writeMapper.toModelCallEntity(taskId, modelCall);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<TaskModelCallEntity>lambdaQuery()
                    .eq(TaskModelCallEntity::getTaskModelCallId, entity.getTaskModelCallId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<TaskModelCallEntity>lambdaUpdate()
                    .eq(TaskModelCallEntity::getTaskModelCallId, entity.getTaskModelCallId())
            )
        );
    }

    /**
     * 保存素材。
     * @param session session值
     * @param taskId 任务标识
     * @param material 素材值
     */
    private void saveMaterial(SqlSession session, String taskId, Map<String, Object> material) {
        MaterialAssetMapper mapper = session.getMapper(MaterialAssetMapper.class);
        MaterialAssetEntity entity = writeMapper.toMaterialAssetEntity(taskId, material);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<MaterialAssetEntity>lambdaQuery()
                    .eq(MaterialAssetEntity::getMaterialAssetId, entity.getMaterialAssetId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<MaterialAssetEntity>lambdaUpdate()
                    .eq(MaterialAssetEntity::getMaterialAssetId, entity.getMaterialAssetId())
            )
        );
    }

    /**
     * 保存结果。
     * @param session session值
     * @param taskId 任务标识
     * @param result 结果值
     */
    private void saveResult(SqlSession session, String taskId, Map<String, Object> result) {
        TaskResultMapper mapper = session.getMapper(TaskResultMapper.class);
        TaskResultEntity entity = writeMapper.toResultEntity(taskId, result);
        TaskResultEntity existing = mapper.selectOne(
            Wrappers.<TaskResultEntity>lambdaQuery()
                .eq(TaskResultEntity::getTaskResultId, entity.getTaskResultId())
                .last("LIMIT 1")
        );
        if (existing == null) {
            existing = mapper.selectOne(
                Wrappers.<TaskResultEntity>lambdaQuery()
                    .eq(TaskResultEntity::getTaskId, taskId)
                    .eq(TaskResultEntity::getClipIndex, entity.getClipIndex())
                    .eq(TaskResultEntity::getIsDeleted, 0)
                    .last("LIMIT 1")
            );
        }
        if (existing == null) {
            mapper.insert(entity);
            return;
        }
        mapper.update(
            entity,
            Wrappers.<TaskResultEntity>lambdaUpdate()
                .eq(TaskResultEntity::getTaskId, taskId)
                .eq(TaskResultEntity::getClipIndex, entity.getClipIndex())
        );
    }

    /**
     * 保存队列事件。
     * @param session session值
     * @param taskId 任务标识
     * @param queueEvent 队列事件值
     */
    private void saveQueueEvent(SqlSession session, String taskId, Map<String, Object> queueEvent) {
        TaskQueueEventMapper mapper = session.getMapper(TaskQueueEventMapper.class);
        TaskQueueEventEntity entity = writeMapper.toTaskQueueEventEntity(taskId, queueEvent);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<TaskQueueEventEntity>lambdaQuery()
                    .eq(TaskQueueEventEntity::getTaskQueueEventId, entity.getTaskQueueEventId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<TaskQueueEventEntity>lambdaUpdate()
                    .eq(TaskQueueEventEntity::getTaskQueueEventId, entity.getTaskQueueEventId())
            )
        );
    }

    /**
     * 保存工作节点Instance。
     * @param session session值
     * @param workerInstance 工作节点Instance值
     */
    private void saveWorkerInstance(SqlSession session, Map<String, Object> workerInstance) {
        WorkerInstanceMapper mapper = session.getMapper(WorkerInstanceMapper.class);
        WorkerInstanceEntity entity = writeMapper.toWorkerInstanceEntity(workerInstance);
        upsert(
            () -> mapper.selectOne(
                Wrappers.<WorkerInstanceEntity>lambdaQuery()
                    .eq(WorkerInstanceEntity::getWorkerInstanceId, entity.getWorkerInstanceId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<WorkerInstanceEntity>lambdaUpdate()
                    .eq(WorkerInstanceEntity::getWorkerInstanceId, entity.getWorkerInstanceId())
            )
        );
    }

    /**
     * 处理upsert。
     * @param existingSupplier existingSupplier值
     * @param insertAction insertAction值
     * @param updateAction updateAction值
     */
    private <T> void upsert(
        Supplier<T> existingSupplier,
        Runnable insertAction,
        Runnable updateAction
    ) {
        if (existingSupplier.get() == null) {
            insertAction.run();
        } else {
            updateAction.run();
        }
    }

    private void saveRequestLog(SqlSession session, String taskId, Map<String, Object> requestLog) {
        RequestLogMapper mapper = session.getMapper(RequestLogMapper.class);
        RequestLogEntity entity = writeMapper.toRequestLogEntity(taskId, requestLog);
        if (entity.getRequestLogId() == null || entity.getRequestLogId().isBlank()) {
            return;
        }
        upsert(
            () -> mapper.selectOne(
                Wrappers.<RequestLogEntity>lambdaQuery()
                    .eq(RequestLogEntity::getRequestLogId, entity.getRequestLogId())
                    .last("LIMIT 1")
            ),
            () -> mapper.insert(entity),
            () -> mapper.update(
                entity,
                Wrappers.<RequestLogEntity>lambdaUpdate()
                    .eq(RequestLogEntity::getRequestLogId, entity.getRequestLogId())
            )
        );
    }

    /**
     * 检查是否requires任务标识。
     * @param mutation 变更值
     * @return 是否满足条件
     */
    private boolean requiresTaskId(TaskPersistenceMutation mutation) {
        return !mutation.attempts().isEmpty()
            || !mutation.statusHistoryRows().isEmpty()
            || !mutation.traceRows().isEmpty()
            || !mutation.stageRunRows().isEmpty()
            || !mutation.modelCallRows().isEmpty()
            || !mutation.materialRows().isEmpty()
            || !mutation.resultRows().isEmpty()
            || !mutation.queueEventRows().isEmpty();
    }

    private static final AtomicReference<String> LAST_DISPATCHED_OWNER = new AtomicReference<>("");

    private List<QueueCandidateRow> fairOrder(List<QueueCandidateRow> candidates) {
        return TaskQueueFairScheduler.fairOrder(candidates, QueueCandidateRow::ownerUserId, LAST_DISPATCHED_OWNER.get());
    }

    private void rememberLastDispatchedOwner(Long ownerUserId) {
        LAST_DISPATCHED_OWNER.set(ownerKey(ownerUserId));
    }

    private String ownerKey(Long ownerUserId) {
        return TaskQueueFairScheduler.ownerKey(ownerUserId);
    }

    private int taskConcurrencyLimit(QueueCandidateRow candidate) {
        Integer value = candidate == null ? null : candidate.taskConcurrencyLimit();
        return value == null || value < 1 ? 1 : value;
    }
}
