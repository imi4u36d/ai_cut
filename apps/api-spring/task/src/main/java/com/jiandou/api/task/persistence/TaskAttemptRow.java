package com.jiandou.api.task.persistence;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 任务尝试投影视图。
 * @param taskAttemptId 任务尝试标识值
 * @param taskId 任务标识
 * @param attemptNo 尝试No值
 * @param triggerType trigger类型值
 * @param status 状态值
 * @param queueName 队列Name值
 * @param workerInstanceId 工作节点实例标识
 * @param queueEnteredAt 队列EnteredAt值
 * @param queueLeftAt 队列LeftAt值
 * @param claimedAt claimedAt值
 * @param startedAt startedAt值
 * @param finishedAt finishedAt值
 * @param resumeFromStage resumeFrom阶段值
 * @param resumeFromClipIndex resumeFrom片段索引值
 * @param failureCode 失败Code值
 * @param failureMessage 失败Message值
 * @param payload 附加负载数据
 * @param timezoneOffsetMinutes timezoneOffsetMinutes值
 * @param createTime create时间值
 * @param updateTime update时间值
 */
public record TaskAttemptRow(
    String taskAttemptId,
    String taskId,
    int attemptNo,
    String triggerType,
    String status,
    String queueName,
    String workerInstanceId,
    OffsetDateTime queueEnteredAt,
    OffsetDateTime queueLeftAt,
    OffsetDateTime claimedAt,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    String resumeFromStage,
    int resumeFromClipIndex,
    String failureCode,
    String failureMessage,
    Map<String, Object> payload,
    int timezoneOffsetMinutes,
    OffsetDateTime createTime,
    OffsetDateTime updateTime
) {}
