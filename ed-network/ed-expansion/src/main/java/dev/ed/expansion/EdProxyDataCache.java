package dev.ed.expansion;

import dev.ed.network.proxy.EdProxyBatchCodec;
import dev.ed.network.proxy.ServerSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class EdProxyDataCache {

    private final ConcurrentHashMap<String, ServerSnapshot> byName = new ConcurrentHashMap<>();
    private volatile long lastUpdateEpochMs;

    void applyBatch(byte[] payload) {
        try {
            EdProxyBatchCodec.Batch batch = EdProxyBatchCodec.decode(payload);
            Map<String, ServerSnapshot> next = new HashMap<>();
            for (ServerSnapshot s : batch.snapshots()) {
                next.put(s.name().toLowerCase(), s);
            }
            byName.clear();
            byName.putAll(next);
            lastUpdateEpochMs = System.currentTimeMillis();
        } catch (Exception ignored) {
        }
    }

    Optional<ServerSnapshot> get(String serverKeyLower) {
        return Optional.ofNullable(byName.get(serverKeyLower));
    }

    long lastUpdateMs() {
        return lastUpdateEpochMs;
    }

    Map<String, ServerSnapshot> snapshotView() {
        return Collections.unmodifiableMap(new HashMap<>(byName));
    }
}
