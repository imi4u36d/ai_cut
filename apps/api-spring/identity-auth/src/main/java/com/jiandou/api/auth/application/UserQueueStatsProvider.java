package com.jiandou.api.auth.application;

import java.util.Map;

/**
 * Provides per-user task queue statistics to identity management without coupling it to task storage.
 */
public interface UserQueueStatsProvider {

    Map<Long, UserQueueStats> listUserQueueStats();
}
