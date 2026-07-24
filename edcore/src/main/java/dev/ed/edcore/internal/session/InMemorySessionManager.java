package dev.ed.edcore.internal.session;

import dev.ed.edcore.api.session.SessionManager;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySessionManager implements SessionManager {
    private final Map<String, Long> map = new ConcurrentHashMap<>();

    @Override
    public void put(String key, long expiresAtEpochSeconds) {
        map.put(key, expiresAtEpochSeconds);
    }

    @Override
    public boolean isValid(String key, long nowEpochSeconds) {
        Long exp = map.get(key);
        return exp != null && exp >= nowEpochSeconds;
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public void clearExpired(long nowEpochSeconds) {
        Iterator<Map.Entry<String, Long>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() < nowEpochSeconds) {
                it.remove();
            }
        }
    }
}
