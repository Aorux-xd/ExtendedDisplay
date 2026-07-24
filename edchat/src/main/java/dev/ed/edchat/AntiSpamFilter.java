package dev.ed.edchat;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiSpamFilter {
    private final ConfigManager config;
    private final ChatManager chatManager;
    private final Map<UUID, Deque<Long>> timestamps = new ConcurrentHashMap<>();

    public AntiSpamFilter(ConfigManager config, ChatManager chatManager) {
        this.config = config;
        this.chatManager = chatManager;
    }

    public boolean allow(Player player) {
        if (!config.antiSpamEnabled) {
            return true;
        }
        long now = System.currentTimeMillis();
        Deque<Long> q = timestamps.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > 1000L) {
                q.pollFirst();
            }
            if (q.size() >= config.maxMessagesPerSecond) {
                if (config.muteOnSpam) {
                    chatManager.mute(player.getUniqueId(), config.muteDurationSeconds * 1000L);
                }
                return false;
            }
            q.addLast(now);
        }
        return true;
    }
}
