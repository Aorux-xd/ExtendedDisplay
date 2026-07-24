package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PlayerMoveListener implements Listener {
    private final HubManager manager;

    public PlayerMoveListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getY() == event.getTo().getY()) {
            return;
        }
        Player player = event.getPlayer();
        WorldConfig cfg = manager.getWorldConfig(player.getWorld().getName());
        var anti = cfg.antiVoid();
        if (!anti.enabled()) {
            return;
        }
        if (!cfg.applyToAllWorlds() && !cfg.worldName().equalsIgnoreCase(player.getWorld().getName())) {
            return;
        }
        double y = event.getTo().getY();
        if (y >= anti.minY() && y <= anti.maxY()) {
            return;
        }
        event.setCancelled(true);
        var target = manager.resolveAntiVoidTarget(player, cfg);
        if (target != null) {
            player.teleportAsync(target);
        }
        if (anti.message() != null && !anti.message().isBlank()) {
            player.sendMessage(anti.message());
        }
        manager.logDebug("anti-void: " + player.getName() + " y=" + y);
    }
}
