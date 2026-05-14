package com.jiandou.api.task.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructuredApplicationLoggerTest {

    @Test
    void listRecentTracesFiltersByTaskStageLevelAndKeyword() {
        StructuredApplicationLogger.logTaskTrace("task_trace_filter", Map.of(
            "traceId", "trace_filter_1",
            "timestamp", "2026-04-29T07:00:00Z",
            "level", "WARN",
            "stage", "render",
            "event", "task.render.warn",
            "message", "seedance retry",
            "payload", Map.of("detail", "retry")
        ));
        StructuredApplicationLogger.logTaskTrace("task_trace_other", Map.of(
            "traceId", "trace_filter_2",
            "timestamp", "2026-04-29T07:00:01Z",
            "level", "INFO",
            "stage", "planning",
            "event", "task.plan.info",
            "message", "planned",
            "payload", Map.of()
        ));

        List<Map<String, Object>> rows = StructuredApplicationLogger.listRecentTraces(
            "task_trace_filter",
            "render",
            "WARN",
            "seedance",
            10
        );

        assertEquals(1, rows.size());
        assertEquals("trace_filter_1", rows.get(0).get("traceId"));
        assertEquals("task_trace_filter", rows.get(0).get("taskId"));
        assertEquals("WARN", rows.get(0).get("level"));
        assertFalse(rows.get(0).containsKey("systemLogId"));
    }
}
