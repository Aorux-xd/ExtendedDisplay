package dev.ed.edchat;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatManager {
    private volatile boolean chatEnabled = true;
    private final Map<UUID, Long> mutes = new ConcurrentHashMap<>();
    private final Set<UUID> adminMuted = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(boolean enabled) {
        this.chatEnabled = enabled;
    }

    public boolean isMuted(UUID playerId) {
        Long until = mutes.get(playerId);
        if (until == null) {
            return false;
        }
        if (until < 0) {
            return true;
        }
        if (System.currentTimeMillis() >= until) {
            mutes.remove(playerId);
            adminMuted.remove(playerId);
            return false;
        }
        return true;
    }

    public boolean isAdminMuted(UUID playerId) {
        return adminMuted.contains(playerId) && isMuted(playerId);
    }

    public void mute(UUID playerId, long durationMs) {
        if (durationMs <= 0) {
            mutes.put(playerId, -1L);
        } else {
            mutes.put(playerId, System.currentTimeMillis() + durationMs);
        }
    }

    public void adminMute(UUID playerId) {
        mute(playerId, 0);
        adminMuted.add(playerId);
    }

    public void unmute(UUID playerId) {
        mutes.remove(playerId);
        adminMuted.remove(playerId);
    }

    public void setLastPartner(UUID from, UUID to) {
        lastPartner.put(from, to);
    }

    public UUID getLastPartner(UUID from) {
        return lastPartner.get(from);
    }

    public static Map<String, String> basePlaceholders(Player player, String message) {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("%player_name%", player.getName());
        map.put("%player_displayname%",
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(player.displayName()));
        map.put("%message%", message);
        map.put("%world%", player.getWorld().getName());
        return map;
    }
}
