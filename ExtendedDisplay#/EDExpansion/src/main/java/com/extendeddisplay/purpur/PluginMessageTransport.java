package com.extendeddisplay.purpur;

import com.extendeddisplay.protocol.EdProtocolConstants;
import com.extendeddisplay.protocol.ProtocolCodec;
import com.extendeddisplay.protocol.ServerStatusDto;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class PluginMessageTransport implements PluginMessageListener {
    private final EDExpansionPlugin plugin;
    private final AtomicLong waiters = new AtomicLong();
    private final ConcurrentHashMap<Long, CompletableFuture<Map<String, ServerStatusDto>>> allRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<ServerStatusDto>> singleRequests = new ConcurrentHashMap<>();

    public PluginMessageTransport(EDExpansionPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<ServerStatusDto> requestOne(String server) {
        CompletableFuture<ServerStatusDto> future = new CompletableFuture<>();
        singleRequests.put(server, future);
        String payload = EdProtocolConstants.REQUEST_GET_PREFIX + server;
        plugin.logDebug("Sending request: " + payload);
        if (!send(payload)) {
            singleRequests.remove(server);
            future.completeExceptionally(new IllegalStateException("No online player carrier for plugin message"));
        }
        return future;
    }

    public CompletableFuture<Map<String, ServerStatusDto>> requestAll() {
        long requestId = waiters.incrementAndGet();
        CompletableFuture<Map<String, ServerStatusDto>> future = new CompletableFuture<>();
        allRequests.put(requestId, future);
        plugin.logDebug("Sending request: " + EdProtocolConstants.REQUEST_LIST_ALL + " (id=" + requestId + ")");
        if (!send(EdProtocolConstants.REQUEST_LIST_ALL)) {
            allRequests.remove(requestId);
            future.completeExceptionally(new IllegalStateException("No online player carrier for plugin message"));
        }
        return future.whenComplete((ignored, throwable) -> allRequests.remove(requestId));
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!EdProtocolConstants.CHANNEL_ID.equals(channel)) {
            return;
        }
        String payload = ProtocolCodec.decodeString(message);
        plugin.logDebug("Received plugin message: " + payload
                + " | player=" + (player == null ? "null" : player.getName()));
        Optional<ProtocolCodec.DecodedSingleResponse> single = ProtocolCodec.decodeSingleResponse(payload);
        if (single.isPresent()) {
            ProtocolCodec.DecodedSingleResponse response = single.get();
            plugin.logDebug("Parsed single response for server: " + response.server());
            CompletableFuture<ServerStatusDto> future = singleRequests.remove(response.server());
            if (future != null) {
                future.complete(response.status());
            } else {
                plugin.logDebugWarn("No waiting future for server: " + response.server());
            }
            return;
        }
        Optional<Map<String, ServerStatusDto>> all = ProtocolCodec.decodeAllResponse(payload);
        if (all.isPresent()) {
            Map<String, ServerStatusDto> map = all.get();
            plugin.logDebug("Parsed list_all response entries=" + map.size()
                    + " waitingRequests=" + allRequests.size());
            for (Map.Entry<Long, CompletableFuture<Map<String, ServerStatusDto>>> entry : allRequests.entrySet()) {
                entry.getValue().complete(map);
            }
            allRequests.clear();
            return;
        }
        plugin.logDebugWarn("Could not parse plugin message payload: " + payload);
    }

    public boolean hasCarrierPlayer() {
        return !Bukkit.getOnlinePlayers().isEmpty();
    }

    private boolean send(String message) {
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            plugin.logDebugWarn("No online players available for plugin messaging transport");
            return false;
        }
        carrier.sendPluginMessage(plugin, EdProtocolConstants.CHANNEL_ID, ProtocolCodec.encodeString(message));
        return true;
    }
}
