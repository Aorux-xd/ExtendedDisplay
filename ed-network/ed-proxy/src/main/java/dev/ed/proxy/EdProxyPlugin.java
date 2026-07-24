package dev.ed.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.api.support.EDPluginSupportVelocity;
import org.slf4j.Logger;

import java.nio.file.Path;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Plugin(
        id = "edproxy",
        name = "EDProxy",
        version = "2.0.0",
        authors = {"ED"},
        dependencies = {@Dependency(id = "edcore")}
)
public final class EdProxyPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final PluginContainer container;

    private final AtomicReference<ScheduledTask> taskHandle = new AtomicReference<>();
    private volatile EdProxyService service;

    @Inject
    public EdProxyPlugin(ProxyServer server, Logger logger,
                         @DataDirectory Path dataDirectory,
                         PluginContainer container) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.container = container;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        if (!EDPluginSupportVelocity.requireEDCore(server.getPluginManager(), "edcore", logger, "edproxy")) {
            return;
        }
        this.service = new EdProxyService(server, logger, dataDirectory);
        service.reloadConfig();
        long interval = Math.max(1L, service.collectIntervalSeconds());
        var task = server.getScheduler()
                .buildTask(container, service::collectAndBroadcast)
                .repeat(interval, TimeUnit.SECONDS)
                .schedule();
        taskHandle.set(task);
        server.getScheduler()
                .buildTask(container, service::collectAndBroadcast)
                .delay(2, TimeUnit.SECONDS)
                .schedule();
        logger.info("EDProxy запущен (интервал {} с).", interval);
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        ScheduledTask h = taskHandle.getAndSet(null);
        if (h != null) {
            h.cancel();
        }
        if (service != null) {
            service.shutdown();
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (service != null && EDCoreProvider.isReady()) {
            service.collectAndBroadcast();
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (service != null && EDCoreProvider.isReady()) {
            service.collectAndBroadcast();
        }
    }
}
