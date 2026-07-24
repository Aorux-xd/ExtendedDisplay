package dev.ed.edhub.listener;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public final class WeatherChangeListener implements Listener {
    private final HubManager manager;

    public WeatherChangeListener(HubManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent event) {
        WorldConfig cfg = manager.getWorldConfig(event.getWorld().getName());
        if (!manager.matchesWorld(event.getWorld(), cfg)) {
            return;
        }
        if (!cfg.weatherChange() || cfg.weatherFreeze()) {
            event.setCancelled(true);
        }
    }
}
