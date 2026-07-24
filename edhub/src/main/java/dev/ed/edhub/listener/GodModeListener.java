package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class GodModeListener implements Listener {
    private final JavaPlugin plugin;
    private final HubManager manager;

    public GodModeListener(JavaPlugin plugin, HubManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        WorldConfig cfg = manager.getWorldConfig(p.getWorld().getName());
        if (!manager.matchesWorld(p.getWorld(), cfg)) return;
        if (!cfg.infinityHp()) return;
        event.setCancelled(true);
        healNow(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        WorldConfig cfg = manager.getWorldConfig(p.getWorld().getName());
        if (!manager.matchesWorld(p.getWorld(), cfg)) return;
        if (!cfg.infinityHp()) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                p.spigot().respawn();
            } catch (Throwable ignored) {
            }
            healNow(p);
        });
    }

    private void healNow(Player p) {
        double max = 20.0;
        try {
            max = p.getMaxHealth();
        } catch (Throwable ignored) {
        }
        try {
            p.setFireTicks(0);
        } catch (Throwable ignored) {
        }
        try {
            p.setHealth(Math.max(1.0, max));
        } catch (Throwable ignored) {
        }
    }
}
