package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class BlockPlaceListener implements Listener {
    private final HubManager manager;

    public BlockPlaceListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        WorldConfig cfg = manager.getWorldConfig(event.getBlock().getWorld().getName());
        if (!manager.matchesWorld(event.getBlock().getWorld(), cfg)) {
            return;
        }

        var ws = manager.whitelist();
        Boolean allow = ws != null ? ws.allowBlockPlace(event.getPlayer()) : null;
        if (allow != null) {
            if (allow) return;
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВы не можете здесь строить!");
            manager.logDebug("place blocked (whitelist): " + event.getPlayer().getName());
            return;
        }

        boolean protectedZone = manager.isSpawnProtected(event.getBlock().getLocation());
        if (ws != null && ws.bypassesSpawnProtection(event.getPlayer())) {
            protectedZone = false;
        }
        if (protectedZone && !cfg.spawnProtection().allowBlockPlace()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВы не можете здесь строить!");
            manager.logDebug("place blocked: " + event.getPlayer().getName());
        }
    }
}
