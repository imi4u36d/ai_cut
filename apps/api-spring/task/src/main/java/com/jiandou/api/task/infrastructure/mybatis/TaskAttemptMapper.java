package com.jiandou.api.task.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 任务尝试MyBatis Mapper。
 */
@Mapper
public interface TaskAttemptMapper extends BaseMapper<TaskAttemptEntity> {

    @Select("""
        SELECT
          ranked.taskAttemptId AS taskAttemptId,
          ranked.taskId AS taskId,
          ranked.ownerUserId AS ownerUserId,
          ranked.taskConcurrencyLimit AS taskConcurrencyLimit,
          ranked.runningTaskCount AS runningTaskCount
        FROM (
          SELECT
            ta.task_attempt_id AS taskAttemptId,
            ta.task_id AS taskId,
            t.owner_user_id AS ownerUserId,
            CASE
              WHEN t.owner_user_id IS NULL THEN 1
              ELSE COALESCE(NULLIF(u.task_concurrency_limit, 0), 1)
            END AS taskConcurrencyLimit,
            (
              SELECT COUNT(*)
              FROM biz_task_attempts running_ta
              JOIN biz_tasks running_t
                ON running_t.task_id = running_ta.task_id
               AND running_t.is_deleted = 0
              WHERE running_ta.is_deleted = 0
                AND running_ta.status = 'RUNNING'
                AND running_t.status NOT IN ('COMPLETED', 'FAILED', 'PAUSED')
                AND (
                  (t.owner_user_id IS NULL AND running_t.owner_user_id IS NULL)
                  OR (t.owner_user_id IS NOT NULL AND running_t.owner_user_id = t.owner_user_id)
                )
            ) AS runningTaskCount,
            ta.queue_entered_at AS queueEnteredAt,
            ta.attempt_no AS attemptNo,
            ta.create_time AS createTime,
            ROW_NUMBER() OVER (
              PARTITION BY COALESCE(CONCAT('u:', t.owner_user_id), 'system')
              ORDER BY ta.queue_entered_at ASC, ta.attempt_no ASC, ta.create_time ASC
            ) AS ownerQueueRank
          FROM biz_task_attempts ta
          JOIN biz_tasks t ON t.task_id = ta.task_id AND t.is_deleted = 0
          LEFT JOIN sys_user u ON u.id = t.owner_user_id
          WHERE ta.is_deleted = 0
            AND ta.status = 'QUEUED'
            AND t.status = 'PENDING'
        ) ranked
        WHERE ranked.runningTaskCount < ranked.taskConcurrencyLimit
          AND ranked.ownerQueueRank <= #{perOwnerLimit}
        ORDER BY ranked.ownerQueueRank ASC, ranked.queueEnteredAt ASC, ranked.attemptNo ASC, ranked.createTime ASC
        """)
    /**
     * 处理select队列Candidates。
     * @param perOwnerLimit 每个用户队列返回的最大条目数
     * @return 处理结果
     */
    List<QueueCandidateRow> selectQueueCandidates(@Param("perOwnerLimit") int perOwnerLimit);

    @Select("""
        SELECT
          ranked.taskAttemptId AS taskAttemptId,
          ranked.taskId AS taskId,
          ranked.ownerUserId AS ownerUserId,
          ranked.taskConcurrencyLimit AS taskConcurrencyLimit,
          ranked.runningTaskCount AS runningTaskCount
        FROM (
          SELECT
            ta.task_attempt_id AS taskAttemptId,
            ta.task_id AS taskId,
            t.owner_user_id AS ownerUserId,
            CASE
              WHEN t.owner_user_id IS NULL THEN 1
              ELSE COALESCE(NULLIF(u.task_concurrency_limit, 0), 1)
            END AS taskConcurrencyLimit,
            0 AS runningTaskCount,
            ta.queue_entered_at AS queueEnteredAt,
            ta.attempt_no AS attemptNo,
            ta.create_time AS createTime,
            ROW_NUMBER() OVER (
              PARTITION BY COALESCE(CONCAT('u:', t.owner_user_id), 'system')
              ORDER BY ta.queue_entered_at ASC, ta.attempt_no ASC, ta.create_time ASC
            ) AS ownerQueueRank
          FROM biz_task_attempts ta
          JOIN biz_tasks t ON t.task_id = ta.task_id AND t.is_deleted = 0
          LEFT JOIN sys_user u ON u.id = t.owner_user_id
          WHERE ta.is_deleted = 0
            AND ta.status = 'QUEUED'
            AND t.status = 'PENDING'
        ) ranked
        WHERE ranked.ownerQueueRank <= #{perOwnerLimit}
        ORDER BY ranked.ownerQueueRank ASC, ranked.queueEnteredAt ASC, ranked.attemptNo ASC, ranked.createTime ASC
        """)
    /**
     * 处理selectQueued任务标识列表。
     * @param perOwnerLimit 每个用户队列返回的最大条目数
     * @return 处理结果
     */
    List<QueueCandidateRow> selectQueuedTaskCandidates(@Param("perOwnerLimit") int perOwnerLimit);

    @Update("""
        UPDATE biz_task_attempts
        SET status = 'RUNNING',
            worker_instance_id = #{workerInstanceId},
            claimed_at = #{claimedAt},
            queue_left_at = #{claimedAt},
            started_at = COALESCE(started_at, #{claimedAt}),
            update_time = CURRENT_TIMESTAMP
        WHERE task_attempt_id = #{attemptId}
          AND status = 'QUEUED'
          AND is_deleted = 0
        """)
    /**
     * 领取尝试。
     * @param attemptId 尝试标识值
     * @param workerInstanceId 工作节点实例标识
     * @param claimedAt claimedAt值
     * @return 处理结果
     */
    int claimAttempt(
        @Param("attemptId") String attemptId,
        @Param("workerInstanceId") String workerInstanceId,
        @Param("claimedAt") OffsetDateTime claimedAt
    );

    @Update("""
        UPDATE biz_task_attempts
        SET status = 'REMOVED',
            queue_left_at = #{removedAt},
            update_time = CURRENT_TIMESTAMP
        WHERE task_id = #{taskId}
          AND status = 'QUEUED'
          AND is_deleted = 0
        """)
    /**
     * 移除QueuedAttempts。
     * @param taskId 任务标识
     * @param removedAt removedAt值
     * @return 处理结果
     */
    int removeQueuedAttempts(@Param("taskId") String taskId, @Param("removedAt") OffsetDateTime removedAt);

    @Select("""
        SELECT ta.task_id AS taskId, ta.worker_instance_id AS workerInstanceId
        FROM biz_task_attempts ta
        JOIN biz_tasks t ON t.task_id = ta.task_id AND t.is_deleted = 0
        LEFT JOIN biz_worker_instances wi ON wi.worker_instance_id = ta.worker_instance_id AND wi.is_deleted = 0
        WHERE ta.is_deleted = 0
          AND ta.status = 'RUNNING'
          AND t.status NOT IN ('COMPLETED', 'FAILED', 'PAUSED')
          AND (
            wi.worker_instance_id IS NULL
            OR wi.status IN ('STOPPED', 'FAILED', 'STALE')
            OR (wi.status = 'RUNNING' AND wi.last_heartbeat_at < #{staleBefore})
          )
        ORDER BY ta.claimed_at ASC, ta.update_time ASC
        LIMIT #{limit}
        """)
    /**
     * 处理selectStaleRunning任务。
     * @param staleBefore staleBefore值
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<StaleRunningTaskRow> selectStaleRunningTasks(
        @Param("staleBefore") OffsetDateTime staleBefore,
        @Param("limit") int limit
    );

    @Select("""
        SELECT id
        FROM sys_user
        WHERE id = #{ownerUserId}
        FOR UPDATE
        """)
    Long lockOwnerUser(@Param("ownerUserId") Long ownerUserId);

    @Select("SELECT GET_LOCK(#{lockName}, #{timeoutSeconds})")
    Integer acquireNamedLock(@Param("lockName") String lockName, @Param("timeoutSeconds") int timeoutSeconds);

    @Select("SELECT RELEASE_LOCK(#{lockName})")
    Integer releaseNamedLock(@Param("lockName") String lockName);

    @Select("""
        SELECT COUNT(*)
        FROM biz_task_attempts ta
        JOIN biz_tasks t ON t.task_id = ta.task_id AND t.is_deleted = 0
        WHERE ta.is_deleted = 0
          AND ta.status = 'RUNNING'
          AND t.status NOT IN ('COMPLETED', 'FAILED', 'PAUSED')
          AND t.owner_user_id = #{ownerUserId}
        """)
    int countRunningTasksForOwner(@Param("ownerUserId") Long ownerUserId);

    @Select("""
        SELECT COUNT(*)
        FROM biz_task_attempts ta
        JOIN biz_tasks t ON t.task_id = ta.task_id AND t.is_deleted = 0
        WHERE ta.is_deleted = 0
          AND ta.status = 'RUNNING'
          AND t.status NOT IN ('COMPLETED', 'FAILED', 'PAUSED')
          AND t.owner_user_id IS NULL
        """)
    int countRunningSystemTasks();

    @Select("""
        SELECT
          totals.ownerUserId AS ownerUserId,
          totals.runningTaskCount AS runningTaskCount,
          totals.queuedTaskCount AS queuedTaskCount,
          oldest.taskId AS oldestQueuedTaskId,
          oldest.title AS oldestQueuedTaskTitle,
          oldest.queuedAt AS oldestQueuedTaskCreatedAt
        FROM (
          SELECT
            t.owner_user_id AS ownerUserId,
            SUM(CASE WHEN ta.status = 'RUNNING' AND t.status NOT IN ('COMPLETED', 'FAILED', 'PAUSED') THEN 1 ELSE 0 END) AS runningTaskCount,
            SUM(CASE WHEN ta.status = 'QUEUED' AND t.status = 'PENDING' THEN 1 ELSE 0 END) AS queuedTaskCount
          FROM biz_tasks t
          JOIN biz_task_attempts ta ON ta.task_id = t.task_id AND ta.is_deleted = 0
          WHERE t.is_deleted = 0
            AND (
              (ta.status = 'RUNNING' AND t.status NOT IN ('COMPLETED', 'FAILED', 'PAUSED'))
              OR (ta.status = 'QUEUED' AND t.status = 'PENDING')
            )
          GROUP BY t.owner_user_id
        ) totals
        LEFT JOIN (
          SELECT ownerUserId, taskId, title, queuedAt
          FROM (
            SELECT
              t.owner_user_id AS ownerUserId,
              t.task_id AS taskId,
              t.title AS title,
              ta.queue_entered_at AS queuedAt,
              ROW_NUMBER() OVER (
                PARTITION BY COALESCE(CONCAT('u:', t.owner_user_id), 'system')
                ORDER BY ta.queue_entered_at ASC, ta.attempt_no ASC, ta.create_time ASC
              ) AS ownerQueueRank
            FROM biz_tasks t
            JOIN biz_task_attempts ta ON ta.task_id = t.task_id AND ta.is_deleted = 0
            WHERE t.is_deleted = 0
              AND ta.status = 'QUEUED'
              AND t.status = 'PENDING'
          ) ranked_oldest
          WHERE ownerQueueRank = 1
        ) oldest ON (
          (totals.ownerUserId IS NULL AND oldest.ownerUserId IS NULL)
          OR totals.ownerUserId = oldest.ownerUserId
        )
        ORDER BY totals.ownerUserId IS NULL ASC, totals.ownerUserId ASC
        """)
    List<UserQueueStatsRow> selectUserQueueStats();
}
