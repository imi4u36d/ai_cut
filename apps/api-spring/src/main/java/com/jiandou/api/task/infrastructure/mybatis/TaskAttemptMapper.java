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
        SELECT ta.task_attempt_id AS taskAttemptId, ta.task_id AS taskId
        FROM biz_task_attempts ta
        JOIN biz_tasks t ON t.task_id = ta.task_id AND t.is_deleted = 0
        WHERE ta.is_deleted = 0
          AND ta.status = 'QUEUED'
          AND t.status = 'PENDING'
        ORDER BY ta.queue_entered_at ASC, ta.attempt_no ASC, ta.create_time ASC
        LIMIT #{limit}
        """)
    /**
     * 处理select队列Candidates。
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<QueueCandidateRow> selectQueueCandidates(@Param("limit") int limit);

    @Select("""
        SELECT ta.task_id
        FROM biz_task_attempts ta
        JOIN biz_tasks t ON t.task_id = ta.task_id AND t.is_deleted = 0
        WHERE ta.is_deleted = 0
          AND ta.status = 'QUEUED'
          AND t.status = 'PENDING'
        ORDER BY ta.queue_entered_at ASC, ta.attempt_no ASC, ta.create_time ASC
        LIMIT #{limit}
        """)
    /**
     * 处理selectQueued任务标识列表。
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<String> selectQueuedTaskIds(@Param("limit") int limit);

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
}
