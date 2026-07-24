package dev.ed.edcore.internal.scheduler;

import dev.ed.edcore.api.scheduler.EDScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperScheduler implements EDScheduler {

    private final JavaPlugin plugin;

    public PaperScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runSync(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runLater(long delayMillis, Runnable task) {
        long ticks = Math.max(1, (delayMillis + 49L) / 50L);
        plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
    }
}
