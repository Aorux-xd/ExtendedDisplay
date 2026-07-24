package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.logging.EDLogger;

/**
 * Регистрация server_id + hardware_id в БД сайта (без привязки ключа).
 */
public final class ServerRegistration {

    private final EdCoreApiClient api;
    private final LicenseConfig licenseConfig;
    private final EDLogger log;

    public ServerRegistration(EdCoreApiClient api, LicenseConfig licenseConfig, EDLogger log) {
        this.api = api;
        this.licenseConfig = licenseConfig;
        this.log = log;
    }

    public boolean register() {
        licenseConfig.ensureServerIdentity();
        boolean ok = api.registerServer(licenseConfig.serverId(), licenseConfig.hardwareId());
        if (ok) {
            log.info("EDCore: сервер зарегистрирован (server_id=" + licenseConfig.serverId() + ").");
        } else {
            log.warn("EDCore: не удалось зарегистрировать сервер на API. Повторим при следующей проверке.");
        }
        return ok;
    }
}
