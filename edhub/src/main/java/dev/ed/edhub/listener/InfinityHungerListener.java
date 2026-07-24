package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class InfinityHungerListener implements Listener {
    private final HubManager manager;

    public InfinityHungerListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        WorldConfig cfg = manager.getWorldConfig(p.getWorld().getName());
        if (!manager.matchesWorld(p.getWorld(), cfg)) return;
        if (!cfg.infinityHunger()) return;
        event.setCancelled(true);
        event.setFoodLevel(20);
        if (p.getFoodLevel() != 20) p.setFoodLevel(20);
        if (p.getSaturation() < 5f) p.setSaturation(5f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExhaust(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        WorldConfig cfg = manager.getWorldConfig(p.getWorld().getName());
        if (!manager.matchesWorld(p.getWorld(), cfg)) return;
        if (!cfg.infinityHunger()) return;
        event.setCancelled(true);
    }
}
