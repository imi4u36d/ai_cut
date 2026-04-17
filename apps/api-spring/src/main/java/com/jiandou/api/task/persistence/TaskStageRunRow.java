package com.jiandou.api.task.persistence;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 任务阶段运行投影视图。
 * @param taskStageRunId 任务阶段运行标识值
 * @param taskId 任务标识
 * @param attemptId 尝试标识值
 * @param stageName 阶段Name值
 * @param stageSeq 阶段Seq值
 * @param clipIndex 片段索引值
 * @param status 状态值
 * @param workerInstanceId 工作节点实例标识
 * @param startedAt startedAt值
 * @param finishedAt finishedAt值
 * @param durationMs 时长毫秒值
 * @param inputSummary 输入摘要值
 * @param outputSummary 输出摘要值
 * @param errorCode errorCode值
 * @param errorMessage errorMessage值
 * @param timezoneOffsetMinutes timezoneOffsetMinutes值
 * @param createTime create时间值
 * @param updateTime update时间值
 */
public record TaskStageRunRow(
    String taskStageRunId,
    String taskId,
    String attemptId,
    String stageName,
    int stageSeq,
    int clipIndex,
    String status,
    String workerInstanceId,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    int durationMs,
    Map<String, Object> inputSummary,
    Map<String, Object> outputSummary,
    String errorCode,
    String errorMessage,
    int timezoneOffsetMinutes,
    OffsetDateTime createTime,
    OffsetDateTime updateTime
) {}
