package dev.ed.edcore.paper;

import dev.ed.edcore.api.PlatformType;
import dev.ed.edcore.internal.EDCoreImpl;
import dev.ed.edcore.internal.EDCoreLifecycle;
import dev.ed.edcore.internal.config.BukkitConfigAdapter;
import dev.ed.edcore.internal.crypto.SimpleCryptoManager;
import dev.ed.edcore.internal.database.JdbcDatabaseManager;
import dev.ed.edcore.internal.license.LicenseConfig;
import dev.ed.edcore.internal.license.LicenseState;
import dev.ed.edcore.internal.license.PaperLicenseBootstrap;
import dev.ed.edcore.internal.logging.JulLoggerAdapter;
import dev.ed.edcore.internal.messaging.EDMessagingImpl;
import dev.ed.edcore.internal.migration.SimpleMigrationManager;
import dev.ed.edcore.internal.scheduler.PaperScheduler;
import dev.ed.edcore.internal.session.InMemorySessionManager;
import dev.ed.edcore.internal.storage.FileStorage;
import dev.ed.edcore.paper.command.EdCoreCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Инициализация EDCore на Paper/Purpur.
 */
public final class EDCoreKernelPaper {

    private EDCoreKernelPaper() {
    }

    public static void start(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (!EDCoreLifecycle.tryBeginStart()) {
            plugin.getLogger().severe("EDCore: ядро уже в состоянии «запущено» без предшествующего onDisable. Проверьте порядок загрузки плагинов.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var log = new JulLoggerAdapter(plugin.getLogger());
        var config = new BukkitConfigAdapter(plugin);
        var licenseConfig = new LicenseConfig(config);
        licenseConfig.ensureServerIdentity();

        var data = plugin.getDataFolder().toPath();
        var scheduler = new PaperScheduler(plugin);
        var storage = new FileStorage(data, log);
        var messaging = new EDMessagingImpl(PlatformType.PAPER);
        var licenseState = new LicenseState();

        var impl = new EDCoreImpl(PlatformType.PAPER, data, log, scheduler, config, storage, messaging,
                new JdbcDatabaseManager(),
                new InMemorySessionManager(),
                new SimpleMigrationManager(),
                new SimpleCryptoManager(),
                EDCoreLifecycle.VERSION,
                licenseState);

        var licenseResult = PaperLicenseBootstrap.bootstrap(plugin, log, scheduler, licenseConfig, licenseState, impl);
        if (licenseResult == PaperLicenseBootstrap.LicenseBootstrapResult.ABORT) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            EDCoreLifecycle.abortStart();
            return;
        }

        EDCoreLifecycle.activate(impl);
        registerCommands(plugin);
        log.info("EDCore " + EDCoreLifecycle.VERSION + " запущен на Paper/Purpur.");
    }

    private static void registerCommands(JavaPlugin plugin) {
        var cmd = plugin.getCommand("edcore");
        if (cmd != null) {
            EdCoreCommand executor = new EdCoreCommand(plugin);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }
}
