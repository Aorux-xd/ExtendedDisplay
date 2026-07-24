package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.license.LicenseStatus;
import dev.ed.edcore.api.logging.EDLogger;
import dev.ed.edcore.api.scheduler.EDScheduler;
import dev.ed.edcore.internal.EDCoreImpl;
import dev.ed.edcore.internal.EDCoreLifecycle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Онлайн-лицензия и обновления на Paper (блокирующая проверка при старте, если указан ключ).
 */
public final class PaperLicenseBootstrap {

    private PaperLicenseBootstrap() {
    }

    public static LicenseBootstrapResult bootstrap(JavaPlugin plugin, EDLogger log, EDScheduler scheduler,
                                                   LicenseConfig licenseConfig, LicenseState licenseState,
                                                   EDCoreImpl impl) {
        migrateLegacyConfig(plugin);

        EDCoreLifecycle.setEdCoreChecksum(resolveJarChecksum(plugin));

        EdCoreApiClient api = new EdCoreApiClient(licenseConfig.apiBaseUrl(), log);
        ServerRegistration registration = new ServerRegistration(api, licenseConfig, log);
        LicenseManager licenseManager = new LicenseManager(api, licenseConfig, licenseState, log);

        String key = licenseConfig.licenseKey();
        if (key.isBlank()) {
            licenseState.setStatus(LicenseStatus.WAITING_KEY);
            log.info(strip(licenseConfig.message("messages.license-waiting",
                    "EDCore: сервер зарегистрирован. Привяжите ключ в личном кабинете.")));
            log.info(strip(licenseConfig.message("messages.license-required",
                    "EDCore: требуется лицензия. Зарегистрируйтесь на edcore.vercel.app")));
            scheduler.runAsync(registration::register);
            UpdateManager updates = new UpdateManager(plugin, api, licenseConfig, licenseState, log, scheduler);
            EDCoreLifecycle.setUpdateManager(updates);
            return LicenseBootstrapResult.continueStartup();
        }

        AtomicBoolean verified = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.runAsync(() -> {
            try {
                registration.register();
                verified.set(licenseManager.verifyStartupBlocking());
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(70, TimeUnit.SECONDS)) {
                log.severe("EDCore: таймаут проверки лицензии.");
                return LicenseBootstrapResult.abortStartup();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LicenseBootstrapResult.abortStartup();
        }

        if (!verified.get()) {
            return LicenseBootstrapResult.abortStartup();
        }

        UpdateManager updates = new UpdateManager(plugin, api, licenseConfig, licenseState, log, scheduler);
        EDCoreLifecycle.setUpdateManager(updates);
        updates.scheduleAutoCheck();
        long verifyPeriodMs = Math.max(1, licenseConfig.updateCheckIntervalHours()) * 3_600_000L;
        schedulePeriodicVerify(scheduler, licenseManager, verifyPeriodMs);
        return LicenseBootstrapResult.continueStartup();
    }

    private static void schedulePeriodicVerify(EDScheduler scheduler, LicenseManager licenseManager, long periodMs) {
        scheduler.runLater(periodMs, () -> {
            scheduler.runAsync(() -> {
                licenseManager.verifyOnce();
                schedulePeriodicVerify(scheduler, licenseManager, periodMs);
            });
        });
    }

    private static void migrateLegacyConfig(JavaPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        boolean dirty = false;
        if (cfg.contains("license-key") && !cfg.isSet("license.key")) {
            cfg.set("license.key", cfg.getString("license-key", ""));
            cfg.set("license-key", null);
            dirty = true;
        }
        if (cfg.contains("license-fail-message") && !cfg.isSet("messages.license-invalid")) {
            cfg.set("messages.license-invalid", cfg.getString("license-fail-message"));
            cfg.set("license-fail-message", null);
            dirty = true;
        }
        if (dirty) {
            plugin.saveConfig();
        }
    }

    private static String resolveJarChecksum(JavaPlugin plugin) {
        try {
            var location = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                return EdCoreJarChecksum.sha256Hex(java.nio.file.Path.of(location.toURI()));
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String strip(String msg) {
        return msg == null ? "" : msg.replaceAll("&#[0-9A-Fa-f]{6}", "");
    }

    public enum LicenseBootstrapResult {
        CONTINUE,
        ABORT;

        static LicenseBootstrapResult continueStartup() {
            return CONTINUE;
        }

        static LicenseBootstrapResult abortStartup() {
            return ABORT;
        }
    }
}
