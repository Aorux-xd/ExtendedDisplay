package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final HubManager manager;

    public PlayerJoinListener(JavaPlugin plugin, HubManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        WorldConfig cfg = manager.getWorldConfig(player.getWorld().getName());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean first = manager.isFirstJoin(player);
            manager.teleportToConfiguredSpawn(player, first).thenAccept(ok -> {
                if (!ok) {
                    return;
                }
                manager.clearInventoryWithWhitelist(player, cfg);
                if (player.getGameMode() != cfg.defaultGamemode()) {
                    player.setGameMode(cfg.defaultGamemode());
                }
                if (manager.matchesWorld(player.getWorld(), cfg) && cfg.infinityHp()) {
                    try {
                        player.setHealth(player.getMaxHealth());
                    } catch (Throwable ignored) {
                    }
                }
                if (manager.matchesWorld(player.getWorld(), cfg) && cfg.infinityHunger()) {
                    player.setFoodLevel(20);
                    player.setSaturation(5f);
                }
                var ws = manager.whitelist();
                if (ws != null && ws.canFly(player)) {
                    player.setAllowFlight(true);
                }
                manager.logDebug("join teleport: player=" + player.getName() + " first=" + first);
            });
        }, 1L);
    }
}
