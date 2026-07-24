package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

public final class EntityChangeBlockListener implements Listener {
    private final HubManager manager;

    public EntityChangeBlockListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWaterFreeze(BlockFormEvent event) {
        WorldConfig cfg = manager.getWorldConfig(event.getBlock().getWorld().getName());
        if (!cfg.freezeWater() && event.getNewState().getType() == Material.ICE) {
            event.setCancelled(true);
        }
    }
}
