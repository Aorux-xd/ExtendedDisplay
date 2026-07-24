package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class CreatureSpawnListener implements Listener {
    private final HubManager manager;

    public CreatureSpawnListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        WorldConfig cfg = manager.getWorldConfig(event.getLocation().getWorld().getName());
        EntityType type = event.getEntityType();
        boolean block = false;
        if (type == EntityType.WANDERING_TRADER && !cfg.trader()) {
            block = true;
        } else if ((type == EntityType.PILLAGER || type == EntityType.VINDICATOR || type == EntityType.RAVAGER) && !cfg.patrols()) {
            block = true;
        } else if ((type == EntityType.COW || type == EntityType.PIG || type == EntityType.SHEEP || type == EntityType.CHICKEN) && !cfg.animals()) {
            block = true;
        } else if ((type == EntityType.ZOMBIE || type == EntityType.SKELETON || type == EntityType.CREEPER
                || type == EntityType.SPIDER || type == EntityType.ENDERMAN) && !cfg.monsters()) {
            block = true;
        }
        if (manager.isSpawnProtected(event.getLocation()) && !cfg.spawnProtection().allowMobSpawning()) {
            block = true;
        }
        if (block) {
            event.setCancelled(true);
        }
    }
}
