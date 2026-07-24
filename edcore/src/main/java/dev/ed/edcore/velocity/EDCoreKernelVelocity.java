package dev.ed.edcore.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.ed.edcore.api.PlatformType;
import dev.ed.edcore.internal.EDCoreImpl;
import dev.ed.edcore.internal.EDCoreLifecycle;
import dev.ed.edcore.internal.config.YamlConfigVelocity;
import dev.ed.edcore.internal.crypto.SimpleCryptoManager;
import dev.ed.edcore.internal.database.JdbcDatabaseManager;
import dev.ed.edcore.internal.license.LicenseConfig;
import dev.ed.edcore.internal.license.LicenseState;
import dev.ed.edcore.internal.license.VelocityLicenseBootstrap;
import dev.ed.edcore.internal.logging.Slf4jLoggerAdapter;
import dev.ed.edcore.internal.messaging.EDMessagingImpl;
import dev.ed.edcore.internal.migration.SimpleMigrationManager;
import dev.ed.edcore.internal.scheduler.VelocitySchedulerAdapter;
import dev.ed.edcore.internal.session.InMemorySessionManager;
import dev.ed.edcore.internal.storage.FileStorage;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Инициализация EDCore на Velocity. Не импортирует Bukkit.
 */
public final class EDCoreKernelVelocity {

    private EDCoreKernelVelocity() {
    }

    public static void start(ProxyServer server, Logger slf4j, Path dataDirectory, PluginContainer container,
                             Path pluginJarPath) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(slf4j, "slf4j");
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(container, "container");

        if (!EDCoreLifecycle.tryBeginStart()) {
            slf4j.error("EDCore: повторная инициализация не поддерживается.");
            return;
        }

        var log = new Slf4jLoggerAdapter(slf4j);
        var yaml = new YamlConfigVelocity(dataDirectory, log);
        try (var in = EDCoreKernelVelocity.class.getClassLoader().getResourceAsStream("config.yml")) {
            yaml.installDefaultFromResource(in);
        } catch (java.io.IOException e) {
            log.warn("EDCore: не удалось установить config.yml по умолчанию: " + e.getMessage());
            yaml.reload();
        }

        migrateLegacyVelocity(yaml);

        var licenseConfig = new LicenseConfig(yaml);
        licenseConfig.ensureServerIdentity();
        VelocityLicenseBootstrap.setChecksumFromJar(pluginJarPath);

        var scheduler = new VelocitySchedulerAdapter(server, container);
        var storage = new FileStorage(dataDirectory, log);
        var messaging = new EDMessagingImpl(PlatformType.VELOCITY);
        var licenseState = new LicenseState();

        if (!VelocityLicenseBootstrap.bootstrap(log, scheduler, licenseConfig, licenseState)) {
            EDCoreLifecycle.abortStart();
            return;
        }

        var impl = new EDCoreImpl(PlatformType.VELOCITY, dataDirectory, log, scheduler, yaml, storage, messaging,
                new JdbcDatabaseManager(),
                new InMemorySessionManager(),
                new SimpleMigrationManager(),
                new SimpleCryptoManager(),
                EDCoreLifecycle.VERSION,
                licenseState);
        EDCoreLifecycle.activate(impl);
        log.info("EDCore " + EDCoreLifecycle.VERSION + " запущен на Velocity.");
    }

    private static void migrateLegacyVelocity(YamlConfigVelocity yaml) {
        if (yaml.getString("license-key", "").isBlank()) {
            return;
        }
        if (!yaml.getString("license.key", "").isBlank()) {
            return;
        }
        yaml.set("license.key", yaml.getString("license-key", ""));
        yaml.set("license-key", null);
        yaml.save();
        yaml.reload();
    }
}
