package dev.ed.edauth.util;

import java.util.concurrent.ConcurrentHashMap;

public final class ConfirmableActionStore {
    private final ConcurrentHashMap<String, Long> pending = new ConcurrentHashMap<>();
    private final long ttlMs;

    public ConfirmableActionStore(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public boolean confirmNow(String actor, String actionKey) {
        long now = System.currentTimeMillis();
        String key = actor + "||" + actionKey;
        Long[] oldRef = new Long[1];
        pending.compute(key, (k, old) -> {
            oldRef[0] = old;
            if (old == null || old < now) {
                return now + ttlMs;
            }
            return null; // consume once confirmed
        });
        Long old = oldRef[0];
        return old != null && old >= now;
    }
}
