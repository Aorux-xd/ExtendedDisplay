package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.license.LicenseStatus;
import dev.ed.edcore.api.logging.EDLogger;
import dev.ed.edcore.api.scheduler.EDScheduler;
import dev.ed.edcore.internal.EDCoreLifecycle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Онлайн-лицензия на Velocity (без менеджера обновлений плагинов).
 */
public final class VelocityLicenseBootstrap {

    private VelocityLicenseBootstrap() {
    }

    public static boolean bootstrap(EDLogger log, EDScheduler scheduler, LicenseConfig licenseConfig,
                                    LicenseState licenseState) {
        EdCoreApiClient api = new EdCoreApiClient(licenseConfig.apiBaseUrl(), log);
        ServerRegistration registration = new ServerRegistration(api, licenseConfig, log);
        LicenseManager licenseManager = new LicenseManager(api, licenseConfig, licenseState, log);

        String key = licenseConfig.licenseKey();
        if (key.isBlank()) {
            licenseState.setStatus(LicenseStatus.WAITING_KEY);
            scheduler.runAsync(registration::register);
            return true;
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
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!verified.get()) {
            return false;
        }
        long periodMs = Math.max(1, licenseConfig.updateCheckIntervalHours()) * 3_600_000L;
        scheduler.runLater(periodMs, () -> scheduler.runAsync(licenseManager::verifyOnce));
        return true;
    }

    public static void setChecksumFromJar(java.nio.file.Path jarPath) {
        EDCoreLifecycle.setEdCoreChecksum(EdCoreJarChecksum.sha256Hex(jarPath));
    }
}
