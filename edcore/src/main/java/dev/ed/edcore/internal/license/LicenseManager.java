package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.license.LicenseStatus;
import dev.ed.edcore.api.logging.EDLogger;

/**
 * Онлайн-проверка лицензии (старт: 3 попытки / 15 с, далее по расписанию).
 */
public final class LicenseManager {

    public static final int STARTUP_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MS = 15_000L;

    private final EdCoreApiClient api;
    private final LicenseConfig licenseConfig;
    private final LicenseState licenseState;
    private final EDLogger log;

    public LicenseManager(EdCoreApiClient api, LicenseConfig licenseConfig, LicenseState licenseState, EDLogger log) {
        this.api = api;
        this.licenseConfig = licenseConfig;
        this.licenseState = licenseState;
        this.log = log;
    }

    /**
     * Блокирующая проверка при старте (вызывать из async-потока).
     */
    public boolean verifyStartupBlocking() {
        String key = licenseConfig.licenseKey();
        if (key.isBlank()) {
            licenseState.setStatus(LicenseStatus.WAITING_KEY);
            log.info(stripColor(licenseConfig.message("messages.license-waiting",
                    "EDCore: сервер зарегистрирован. Привяжите ключ в личном кабинете.")));
            return false;
        }
        if (!LicenseKeyFormat.isValid(key)) {
            licenseState.setStatus(LicenseStatus.INVALID);
            log.severe(stripColor(licenseConfig.message("messages.license-invalid",
                    "EDCore: лицензия недействительна (формат ключа).")));
            return false;
        }
        for (int attempt = 1; attempt <= STARTUP_ATTEMPTS; attempt++) {
            if (verifyOnce(key)) {
                return true;
            }
            if (attempt < STARTUP_ATTEMPTS) {
                log.warn("EDCore: проверка лицензии не удалась (попытка " + attempt + "/" + STARTUP_ATTEMPTS + ").");
                sleep(RETRY_DELAY_MS);
            }
        }
        licenseState.setStatus(LicenseStatus.INVALID);
        log.severe(stripColor(licenseConfig.message("messages.license-invalid",
                "EDCore: лицензия недействительна.")));
        return false;
    }

    public boolean verifyOnce() {
        String key = licenseConfig.licenseKey();
        if (key.isBlank()) {
            licenseState.setStatus(LicenseStatus.WAITING_KEY);
            return false;
        }
        if (!LicenseKeyFormat.isValid(key)) {
            licenseState.setStatus(LicenseStatus.INVALID);
            return false;
        }
        return verifyOnce(key);
    }

    private boolean verifyOnce(String key) {
        EdCoreApiClient.VerifyResponse response = api.verifyLicense(
                key, licenseConfig.serverId(), licenseConfig.hardwareId());
        if (response.valid()) {
            licenseState.setStatus(LicenseStatus.VALID);
            licenseState.setOwner(response.owner());
            licenseState.setRemoteStatus(response.status());
            log.info(stripColor(licenseConfig.message("messages.license-valid",
                    "EDCore: лицензия подтверждена.")));
            return true;
        }
        licenseState.setStatus(LicenseStatus.INVALID);
        return false;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String stripColor(String msg) {
        return msg == null ? "" : msg.replaceAll("&#[0-9A-Fa-f]{6}", "").replaceAll("&#[0-9A-Fa-f]{6}", "");
    }
}
