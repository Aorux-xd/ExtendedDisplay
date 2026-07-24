package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;

public final class TimeChangeListener implements Listener {
    private final HubManager manager;

    public TimeChangeListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        WorldConfig cfg = manager.getWorldConfig(event.getWorld().getName());
        if (!manager.matchesWorld(event.getWorld(), cfg)) {
            return;
        }
        if (!cfg.timeCycle() && event.getSkipReason() != TimeSkipEvent.SkipReason.CUSTOM) {
            event.setCancelled(true);
            manager.logDebug("time skip blocked: " + event.getSkipReason());
        }
    }
}
