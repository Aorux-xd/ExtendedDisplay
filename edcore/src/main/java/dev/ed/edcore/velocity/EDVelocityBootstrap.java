package dev.ed.edcore.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.ed.edcore.internal.EDCoreLifecycle;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Точка входа Velocity.
 * Не регистрируйте события в конструкторе (см. Pitfalls Velocity, docs.papermc.io — «Accessing the API at construction time»).
 * Методы с аннотацией {@link Subscribe} на главном классе вызываются после инициализации контейнера плагина.
 */
@Plugin(id = "edcore", name = "EDCore", version = "2.0.0", authors = {"ED"})
public final class EDVelocityBootstrap {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final PluginContainer container;

    @Inject
    public EDVelocityBootstrap(ProxyServer server, Logger logger,
                               @DataDirectory Path dataDirectory,
                               PluginContainer container) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.container = container;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            java.nio.file.Path jarPath = null;
            var location = EDVelocityBootstrap.class.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                jarPath = java.nio.file.Path.of(location.toURI());
            }
            EDCoreKernelVelocity.start(server, logger, dataDirectory, container, jarPath);
        } catch (Exception e) {
            logger.error("EDCore: ошибка запуска", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        EDCoreLifecycle.shutdown();
    }
}
