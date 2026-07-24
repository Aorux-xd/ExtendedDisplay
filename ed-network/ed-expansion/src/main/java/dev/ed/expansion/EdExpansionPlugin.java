package dev.ed.expansion;

import dev.ed.edcore.api.support.EDPluginSupportPaper;
import dev.ed.network.proxy.EdProxyChannel;
import dev.ed.network.proxy.ServerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class EdExpansionPlugin extends JavaPlugin implements PluginMessageListener {

    private final EdProxyDataCache cache = new EdProxyDataCache();
    private EdProxyExpansion expansion;

    @Override
    public void onEnable() {
        if (!EDPluginSupportPaper.requireEDCore(this)) {
            return;
        }
        saveDefaultConfig();
        Bukkit.getMessenger().registerIncomingPluginChannel(this, EdProxyChannel.CHANNEL, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new EdProxyExpansion(this);
            expansion.register();
            getLogger().info("PlaceholderAPI: зарегистрировано расширение edproxy");
        } else {
            getLogger().warning("PlaceholderAPI не найден — плейсхолдеры недоступны (канал ed:proxy всё равно слушается).");
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, EdProxyChannel.CHANNEL);
        if (expansion != null) {
            expansion.unregister();
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, Player player, byte[] message) {
        if (!EdProxyChannel.CHANNEL.equals(channel) || message == null || message.length == 0) {
            return;
        }
        try {
            cache.applyBatch(message);
        } catch (Exception ignored) {
        }
    }

    EdProxyDataCache cache() {
        return cache;
    }

    Optional<ServerSnapshot> fallbackSlp(String serverKeyLower, String serverRaw) {
        String addr = getConfig().getString("servers." + serverRaw);
        if (addr == null) {
            addr = getConfig().getString("servers." + serverKeyLower);
        }
        if (addr == null || addr.isBlank()) {
            return Optional.empty();
        }
        String host;
        int port = 25565;
        int colon = addr.lastIndexOf(':');
        if (colon > 0 && colon < addr.length() - 1) {
            host = addr.substring(0, colon).trim();
            try {
                port = Integer.parseInt(addr.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else {
            host = addr.trim();
        }
        int timeout = Math.max(500, getConfig().getInt("slp-timeout-millis", 1500));
        return dev.ed.expansion.util.SlListPing.ping(host, port, timeout)
                .map(r -> new ServerSnapshot(
                        serverRaw,
                        r.onlinePlayers(),
                        r.maxPlayers(),
                        r.motd(),
                        r.pingMs(),
                        r.version(),
                        r.online()
                ));
    }
}
