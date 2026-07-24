package com.extendeddisplay.purpur;

import com.extendeddisplay.protocol.ServerStatusDto;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StatusCacheService {
    private final EDExpansionPlugin plugin;
    private final Map<String, ServerStatusDto> cache = new ConcurrentHashMap<>();
    private final Map<String, String> knownAddresses = new ConcurrentHashMap<>();
    private final Set<String> dynamicServers = ConcurrentHashMap.newKeySet();
    private final Set<String> velocityKnownServers = ConcurrentHashMap.newKeySet();

    public StatusCacheService(EDExpansionPlugin plugin) {
        this.plugin = plugin;
    }

    public ServerStatusDto get(String server) {
        return cache.getOrDefault(server, ServerStatusDto.fallbackOffline());
    }

    public void put(String server, ServerStatusDto status) {
        plugin.logDebug("Updating cache for " + server
                + ": online=" + status.online() + ", players=" + status.players() + ", max=" + status.max());
        if (status.address() != null && !status.address().isBlank()) {
            knownAddresses.put(server, status.address());
        }
        cache.put(server, status);
    }

    public void putAll(Map<String, ServerStatusDto> statuses) {
        plugin.logDebug("Bulk cache update, entries=" + statuses.size());
        velocityKnownServers.addAll(statuses.keySet());
        statuses.forEach(cache::put);
        statuses.forEach((name, status) -> {
            if (status.address() != null && !status.address().isBlank()) {
                knownAddresses.put(name, status.address());
            }
        });
    }

    public void trackServer(String server) {
        if (server != null && !server.isBlank()) {
            dynamicServers.add(server);
        }
    }

    public Set<String> getDynamicServers() {
        return Set.copyOf(dynamicServers);
    }

    public Set<String> getKnownServers() {
        Set<String> all = ConcurrentHashMap.newKeySet();
        all.addAll(velocityKnownServers);
        all.addAll(dynamicServers);
        return Set.copyOf(all);
    }

    public String getKnownAddress(String server) {
        return knownAddresses.get(server);
    }

    public Map<String, String> getKnownAddresses() {
        return Map.copyOf(knownAddresses);
    }

    public void loadKnownAddresses(Map<String, String> addresses) {
        if (addresses != null) {
            knownAddresses.putAll(addresses);
        }
    }
}
