package dev.ed.edcore.internal.scheduler;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.ed.edcore.api.scheduler.EDScheduler;

import java.util.concurrent.TimeUnit;

public final class VelocitySchedulerAdapter implements EDScheduler {

    private final ProxyServer server;
    private final PluginContainer plugin;

    public VelocitySchedulerAdapter(ProxyServer server, PluginContainer plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void runSync(Runnable task) {
        server.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override
    public void runAsync(Runnable task) {
        server.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override
    public void runLater(long delayMillis, Runnable task) {
        server.getScheduler()
                .buildTask(plugin, task)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .schedule();
    }
}
