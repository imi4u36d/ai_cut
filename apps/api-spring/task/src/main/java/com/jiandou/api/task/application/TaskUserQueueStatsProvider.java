package com.jiandou.api.task.application;

import com.jiandou.api.auth.application.UserQueueStats;
import com.jiandou.api.auth.application.UserQueueStatsProvider;
import com.jiandou.api.task.persistence.TaskRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Adapts task persistence queue stats for identity administration.
 */
@Component
public class TaskUserQueueStatsProvider implements UserQueueStatsProvider {

    private final TaskRepository taskRepository;

    public TaskUserQueueStatsProvider(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Map<Long, UserQueueStats> listUserQueueStats() {
        Map<Long, UserQueueStats> result = new LinkedHashMap<>();
        for (Map<String, Object> row : taskRepository.listUserQueueStats()) {
            Long ownerUserId = longObjectValue(row.get("ownerUserId"));
            if (ownerUserId == null) {
                continue;
            }
            result.put(
                ownerUserId,
                new UserQueueStats(longValue(row.get("runningTaskCount")), longValue(row.get("queuedTaskCount")))
            );
        }
        return result;
    }

    private Long longObjectValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private long longValue(Object value) {
        Long parsed = longObjectValue(value);
        return parsed == null ? 0L : parsed;
    }
}
