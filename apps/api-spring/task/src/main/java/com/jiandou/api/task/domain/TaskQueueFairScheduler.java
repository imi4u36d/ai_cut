package com.jiandou.api.task.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按用户队列做公平轮询的纯领域调度器。
 */
public final class TaskQueueFairScheduler {

    private static final String SYSTEM_OWNER_KEY = "system";

    private TaskQueueFairScheduler() {
    }

    public static <T> List<T> fairOrder(List<T> candidates, OwnerResolver<T> ownerResolver, String lastDispatchedOwnerKey) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<String, List<T>> byOwner = new LinkedHashMap<>();
        for (T candidate : candidates) {
            byOwner.computeIfAbsent(ownerKey(ownerResolver.ownerUserId(candidate)), ignored -> new ArrayList<>()).add(candidate);
        }
        List<String> ownerKeys = new ArrayList<>(byOwner.keySet());
        int start = 0;
        int lastIndex = ownerKeys.indexOf(lastDispatchedOwnerKey);
        if (lastIndex >= 0) {
            start = (lastIndex + 1) % ownerKeys.size();
        }
        List<T> ordered = new ArrayList<>();
        int remaining = candidates.size();
        int ownerOffset = start;
        while (remaining > 0) {
            boolean consumed = false;
            for (int index = 0; index < ownerKeys.size(); index++) {
                String ownerKey = ownerKeys.get((ownerOffset + index) % ownerKeys.size());
                List<T> ownerQueue = byOwner.get(ownerKey);
                if (ownerQueue == null || ownerQueue.isEmpty()) {
                    continue;
                }
                ordered.add(ownerQueue.remove(0));
                remaining--;
                consumed = true;
            }
            if (!consumed) {
                break;
            }
        }
        return ordered;
    }

    public static String ownerKey(Long ownerUserId) {
        return ownerUserId == null ? SYSTEM_OWNER_KEY : "user:" + ownerUserId;
    }

    @FunctionalInterface
    public interface OwnerResolver<T> {
        Long ownerUserId(T candidate);
    }
}
