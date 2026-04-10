package com.extendeddisplay.velocity;

import com.extendeddisplay.protocol.ServerStatusDto;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ServerStateService {
    private final ProxyServer proxyServer;
    private final Set<String> blacklist;
    private final long ttlNanos;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public ServerStateService(ProxyServer proxyServer, Set<String> blacklist, int ttlSeconds) {
        this.proxyServer = proxyServer;
        this.blacklist = blacklist;
        this.ttlNanos = TimeUnit.SECONDS.toNanos(Math.max(1, ttlSeconds));
    }

    public Map<String, ServerStatusDto> getAllStatuses() {
        Map<String, ServerStatusDto> result = new ConcurrentHashMap<>();
        for (RegisteredServer server : proxyServer.getAllServers()) {
            String name = server.getServerInfo().getName();
            if (blacklist.contains(name.toLowerCase())) {
                continue;
            }
            result.put(name, getStatus(name).orElse(ServerStatusDto.fallbackOffline()));
        }
        return Collections.unmodifiableMap(result);
    }

    public Optional<ServerStatusDto> getStatus(String serverName) {
        RegisteredServer server = proxyServer.getServer(serverName).orElse(null);
        if (server == null || blacklist.contains(serverName.toLowerCase())) {
            return Optional.empty();
        }
        String address = server.getServerInfo().getAddress().getHostString() + ":" + server.getServerInfo().getAddress().getPort();
        CacheEntry cached = cache.get(serverName);
        long now = System.nanoTime();
        if (cached != null && (now - cached.cachedAtNanos) <= ttlNanos) {
            return Optional.of(cached.status);
        }

        int players = server.getPlayersConnected().size();
        ServerStatusDto current = new ServerStatusDto(!server.getPlayersConnected().isEmpty() || players >= 0, players, 0, address);
        cache.put(serverName, new CacheEntry(current, now));

        try {
            ServerPing ping = server.ping().get(900, TimeUnit.MILLISECONDS);
            int max = ping.getPlayers().map(ServerPing.Players::getMax).orElse(0);
            int online = ping.getPlayers().map(ServerPing.Players::getOnline).orElse(players);
            ServerStatusDto withPing = new ServerStatusDto(true, Math.max(0, online), Math.max(0, max), address);
            cache.put(serverName, new CacheEntry(withPing, System.nanoTime()));
            return Optional.of(withPing);
        } catch (Exception ignored) {
            ServerStatusDto fallback = new ServerStatusDto(false, 0, 0, address);
            cache.put(serverName, new CacheEntry(fallback, System.nanoTime()));
            return Optional.of(fallback);
        }
    }

    private record CacheEntry(ServerStatusDto status, long cachedAtNanos) {
    }
}
