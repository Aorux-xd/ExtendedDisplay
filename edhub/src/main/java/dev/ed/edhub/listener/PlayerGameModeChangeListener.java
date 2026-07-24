package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public final class PlayerGameModeChangeListener implements Listener {
    private final HubManager manager;

    public PlayerGameModeChangeListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onModeChange(PlayerGameModeChangeEvent event) {
        WorldConfig cfg = manager.getWorldConfig(event.getPlayer().getWorld().getName());
        if (!manager.matchesWorld(event.getPlayer().getWorld(), cfg)) {
            return;
        }

        var ws = manager.whitelist();
        if (ws != null && ws.canChangeGamemode(event.getPlayer(), event.getNewGameMode())) {
            return;
        }

        if (event.getNewGameMode() != cfg.defaultGamemode()) {
            event.setCancelled(true);
        }
    }
}
