package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class EntityDamageListener implements Listener {
    private final HubManager manager;

    public EntityDamageListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        WorldConfig cfg = manager.getWorldConfig(victim.getWorld().getName());
        if (!manager.matchesWorld(victim.getWorld(), cfg)) {
            return;
        }

        var ws = manager.whitelist();
        Player damagerPlayer = event.getDamager() instanceof Player p ? p : null;
        if (damagerPlayer != null && ws != null) {
            Boolean allow = ws.allowPvp(damagerPlayer);
            if (allow != null) {
                if (!allow) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (!cfg.pvpEnabled()) {
            event.setCancelled(true);
            return;
        }
        if (!cfg.allowProjectiles() && event.getDamager() instanceof AbstractArrow) {
            event.setCancelled(true);
            return;
        }
        boolean protectedZone = manager.isSpawnProtected(victim.getLocation());
        if (ws != null && damagerPlayer != null && ws.bypassesSpawnProtection(damagerPlayer)) {
            protectedZone = false;
        }
        if (protectedZone && !cfg.spawnProtection().allowPvp()) {
            event.setCancelled(true);
        }
    }
}
