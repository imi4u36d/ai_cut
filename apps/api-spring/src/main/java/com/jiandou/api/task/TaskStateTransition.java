package com.jiandou.api.task;

import java.util.Map;

/**
 * Encapsulates one 任务状态 transition so coordinator can persist 任务 rows atomically.
 */
public record TaskStateTransition(
    String nextStatus,
    int progress,
    String stage,
    String event,
    String message,
    String level,
    Map<String, Object> payload,
    String attemptStatus,
    String attemptErrorMessage
) {

    public TaskStateTransition {
        nextStatus = nextStatus == null ? "" : nextStatus.trim();
        stage = stage == null ? "" : stage.trim();
        event = event == null ? "" : event.trim();
        message = message == null ? "" : message.trim();
        level = level == null || level.isBlank() ? "INFO" : level.trim().toUpperCase();
        payload = payload == null ? Map.of() : payload;
        attemptStatus = attemptStatus == null ? "" : attemptStatus.trim();
        attemptErrorMessage = attemptErrorMessage == null ? "" : attemptErrorMessage.trim();
    }

    /**
     * 处理信息。
     * @param nextStatus next状态值
     * @param progress 进度值
     * @param stage 阶段名称
     * @param event 事件名称
     * @param message 消息文本
     * @param payload 附加负载数据
     * @return 处理结果
     */
    public static TaskStateTransition info(
        String nextStatus,
        int progress,
        String stage,
        String event,
        String message,
        Map<String, Object> payload
    ) {
        return new TaskStateTransition(nextStatus, progress, stage, event, message, "INFO", payload, "", "");
    }

    /**
     * 处理warn。
     * @param nextStatus next状态值
     * @param progress 进度值
     * @param stage 阶段名称
     * @param event 事件名称
     * @param message 消息文本
     * @param payload 附加负载数据
     * @return 处理结果
     */
    public static TaskStateTransition warn(
        String nextStatus,
        int progress,
        String stage,
        String event,
        String message,
        Map<String, Object> payload
    ) {
        return new TaskStateTransition(nextStatus, progress, stage, event, message, "WARN", payload, "", "");
    }

    /**
     * 处理error。
     * @param nextStatus next状态值
     * @param progress 进度值
     * @param stage 阶段名称
     * @param event 事件名称
     * @param message 消息文本
     * @param payload 附加负载数据
     * @return 处理结果
     */
    public static TaskStateTransition error(
        String nextStatus,
        int progress,
        String stage,
        String event,
        String message,
        Map<String, Object> payload
    ) {
        return new TaskStateTransition(nextStatus, progress, stage, event, message, "ERROR", payload, "", "");
    }

    /**
     * 处理with尝试。
     * @param status 状态值
     * @param errorMessage errorMessage值
     * @return 处理结果
     */
    public TaskStateTransition withAttempt(String status, String errorMessage) {
        return new TaskStateTransition(
            nextStatus,
            progress,
            stage,
            event,
            message,
            level,
            payload,
            status,
            errorMessage
        );
    }

    /**
     * 检查是否updates尝试。
     * @return 是否满足条件
     */
    public boolean updatesAttempt() {
        return !attemptStatus.isBlank();
    }
}
