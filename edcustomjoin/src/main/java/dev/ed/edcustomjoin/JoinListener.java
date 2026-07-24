package dev.ed.edcustomjoin;

import dev.ed.edcore.api.EDCoreProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class JoinListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager config;

    public JoinListener(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (config.hideDefault()) {
            event.joinMessage(null);
        }

        if (!player.hasPlayedBefore()) {
            if (!config.enableFirstJoin()) {
                return;
            }
            sendFormattedMessage(player, config.firstJoinMessage(), true);
        } else {
            if (!config.enableJoin()) {
                return;
            }
            sendFormattedMessage(player, config.joinMessage(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        if (!config.enableQuit()) {
            if (config.hideDefault()) {
                event.quitMessage(null);
            }
            return;
        }

        if (config.hideDefault()) {
            event.quitMessage(null);
        }

        sendFormattedMessage(event.getPlayer(), config.quitMessage(), false);
    }

    private void sendFormattedMessage(Player player, String template, boolean joining) {
        if (template == null || template.isBlank()) {
            return;
        }
        var formatter = EDCoreProvider.get().getMessageFormatter();
        Component formatted = formatter.format(player, template, placeholders(player, joining));
        broadcast(formatted, config.delayTicks());
    }

    private void broadcast(Component message, int delayTicks) {
        Runnable task = () -> Bukkit.getServer().broadcast(message);
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        } else {
            task.run();
        }
    }

    private static Map<String, String> placeholders(Player player, boolean joining) {
        int online = Bukkit.getOnlinePlayers().size();
        if (!joining) {
            online = Math.max(0, online - 1);
        }
        Map<String, String> map = new HashMap<>();
        map.put("%player_name%", player.getName());
        map.put("%player_displayname%", PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        map.put("%player_world%", player.getWorld().getName());
        map.put("%online%", String.valueOf(online));
        map.put("%max%", String.valueOf(Bukkit.getMaxPlayers()));
        return map;
    }
}
