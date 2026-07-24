package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class FallDamageListener implements Listener {
    private final HubManager manager;

    public FallDamageListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player p)) return;
        WorldConfig cfg = manager.getWorldConfig(p.getWorld().getName());
        if (!manager.matchesWorld(p.getWorld(), cfg)) return;
        if (cfg.infinityHp()) return;
        if (!cfg.fallDamage()) {
            event.setCancelled(true);
        }
    }
}
