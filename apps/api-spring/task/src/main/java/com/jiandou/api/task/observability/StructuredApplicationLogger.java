package com.jiandou.api.task.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiandou.api.generation.observability.ProviderPayloadSanitizer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes task/workflow diagnostic events to the application log instead of DB log tables.
 */
public final class StructuredApplicationLogger {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_RECENT_EVENTS = 1000;
    private static final Object RECENT_EVENTS_LOCK = new Object();
    private static final Deque<Map<String, Object>> RECENT_EVENTS = new ArrayDeque<>();

    private StructuredApplicationLogger() {
    }

    /**
     * 写入任务追踪日志。
     * @param taskId 任务ID
     * @param trace trace行
     */
    public static void logTaskTrace(String taskId, Map<String, Object> trace) {
        Map<String, Object> safeTrace = trace == null ? Map.of() : trace;
        Map<String, Object> event = taskTraceEvent(taskId, safeTrace);
        remember(event);
        log(LoggerFactory.getLogger("com.jiandou.api.task.trace"), stringValue(event.get("level")), event);
    }

    /**
     * 写入工作流诊断日志。
     * @param ownerRefId 所属ID
     * @param module 模块
     * @param stage 阶段
     * @param eventName 事件
     * @param level 级别
     * @param message 消息
     * @param payload payload
     */
    public static void logWorkflowEvent(
        String ownerRefId,
        String module,
        String stage,
        String eventName,
        String level,
        String message,
        Map<String, Object> payload
    ) {
        Map<String, Object> event = workflowEvent(ownerRefId, module, stage, eventName, level, message, payload);
        remember(event);
        log(LoggerFactory.getLogger("com.jiandou.api.workflow.trace"), level, event);
    }

    /**
     * 返回当前进程最近的结构化Trace事件。
     * @param taskId 任务标识
     * @param stage 阶段名称
     * @param level level值
     * @param queryText 查询文本
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    public static List<Map<String, Object>> listRecentTraces(String taskId, String stage, String level, String queryText, int limit) {
        int resolvedLimit = Math.max(1, limit);
        List<Map<String, Object>> snapshot;
        synchronized (RECENT_EVENTS_LOCK) {
            snapshot = new ArrayList<>(RECENT_EVENTS);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> event : snapshot) {
            if (!matches(event, taskId, stage, level, queryText)) {
                continue;
            }
            rows.add(new LinkedHashMap<>(event));
            if (rows.size() >= resolvedLimit) {
                break;
            }
        }
        return rows;
    }

    public static Map<String, Object> taskTraceEvent(String taskId, Map<String, Object> trace) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("logType", "task_trace");
        event.put("taskId", stringValue(taskId));
        event.put("traceId", firstNonBlank(stringValue(trace.get("traceId")), ""));
        event.put("timestamp", firstNonBlank(stringValue(trace.get("timestamp")), nowIso()));
        event.put("level", normalizeLevel(stringValue(trace.get("level"))));
        event.put("stage", stringValue(trace.get("stage")));
        event.put("event", stringValue(trace.get("event")));
        event.put("message", stringValue(trace.get("message")));
        event.put("payload", ProviderPayloadSanitizer.sanitize(trace.getOrDefault("payload", Map.of())));
        event.put("source", "spring-api");
        event.put("serviceName", "api-spring");
        return event;
    }

    public static Map<String, Object> workflowEvent(
        String ownerRefId,
        String module,
        String stage,
        String eventName,
        String level,
        String message,
        Map<String, Object> payload
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("logType", "workflow_event");
        event.put("taskId", stringValue(ownerRefId));
        event.put("ownerRefId", stringValue(ownerRefId));
        event.put("traceId", "");
        event.put("timestamp", nowIso());
        event.put("level", normalizeLevel(level));
        event.put("module", stringValue(module));
        event.put("stage", stringValue(stage));
        event.put("event", stringValue(eventName));
        event.put("message", stringValue(message));
        event.put("payload", ProviderPayloadSanitizer.sanitize(payload == null ? Map.of() : payload));
        event.put("source", "spring-api");
        event.put("serviceName", "api-spring");
        return event;
    }

    private static void log(Logger logger, String rawLevel, Map<String, Object> event) {
        String message = write(event);
        switch (normalizeLevel(rawLevel)) {
            case "ERROR" -> logger.error(message);
            case "WARN" -> logger.warn(message);
            case "DEBUG" -> logger.debug(message);
            case "TRACE" -> logger.trace(message);
            default -> logger.info(message);
        }
    }

    private static void remember(Map<String, Object> event) {
        synchronized (RECENT_EVENTS_LOCK) {
            RECENT_EVENTS.addFirst(new LinkedHashMap<>(event));
            while (RECENT_EVENTS.size() > MAX_RECENT_EVENTS) {
                RECENT_EVENTS.removeLast();
            }
        }
    }

    private static String normalizeLevel(String rawLevel) {
        String normalized = rawLevel == null ? "" : rawLevel.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "INFO" : normalized;
    }

    private static boolean matches(Map<String, Object> event, String taskId, String stage, String level, String queryText) {
        if (!isBlank(taskId) && !stringValue(event.get("taskId")).equals(taskId.trim())) {
            return false;
        }
        if (!isBlank(stage) && !stringValue(event.get("stage")).equals(stage.trim())) {
            return false;
        }
        if (!isBlank(level) && !stringValue(event.get("level")).equalsIgnoreCase(level.trim())) {
            return false;
        }
        if (isBlank(queryText)) {
            return true;
        }
        String query = queryText.trim().toLowerCase(Locale.ROOT);
        return contains(event.get("taskId"), query)
            || contains(event.get("traceId"), query)
            || contains(event.get("event"), query)
            || contains(event.get("message"), query)
            || contains(event.get("stage"), query);
    }

    private static boolean contains(Object value, String query) {
        return stringValue(value).toLowerCase(Locale.ROOT).contains(query);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String write(Map<String, Object> event) {
        try {
            return OBJECT_MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            return String.valueOf(event);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static String nowIso() {
        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }
}
