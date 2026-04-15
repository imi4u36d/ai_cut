package com.jiandou.api.task.persistence;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 任务状态History投影视图。
 * @param taskStatusHistoryId 任务状态History标识值
 * @param taskId 任务标识
 * @param previousStatus previous状态值
 * @param currentStatus current状态值
 * @param progress 进度值
 * @param stage 阶段名称
 * @param event 事件名称
 * @param message 消息文本
 * @param payload 附加负载数据
 * @param changeTime change时间值
 * @param operatorType operator类型值
 * @param operatorId operator标识值
 * @param timezoneOffsetMinutes timezoneOffsetMinutes值
 * @param createTime create时间值
 * @param updateTime update时间值
 */
public record TaskStatusHistoryRow(
    String taskStatusHistoryId,
    String taskId,
    String previousStatus,
    String currentStatus,
    int progress,
    String stage,
    String event,
    String message,
    Map<String, Object> payload,
    OffsetDateTime changeTime,
    String operatorType,
    String operatorId,
    int timezoneOffsetMinutes,
    OffsetDateTime createTime,
    OffsetDateTime updateTime
) {}
