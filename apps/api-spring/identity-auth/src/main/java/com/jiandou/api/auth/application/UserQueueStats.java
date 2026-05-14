package com.jiandou.api.auth.application;

/**
 * User-level task queue counters used by identity administration.
 */
public record UserQueueStats(long runningTaskCount, long queuedTaskCount) {

    public static final UserQueueStats EMPTY = new UserQueueStats(0L, 0L);
}
