package dev.ed.edcore.api;

import dev.ed.edcore.api.config.EDConfig;
import dev.ed.edcore.api.crypto.CryptoManager;
import dev.ed.edcore.api.database.DatabaseManager;
import dev.ed.edcore.api.auth.EDAuthAPI;
import dev.ed.edcore.api.logging.EDLogger;
import dev.ed.edcore.api.messaging.EDMessaging;
import dev.ed.edcore.api.migration.MigrationManager;
import dev.ed.edcore.api.scheduler.EDScheduler;
import dev.ed.edcore.api.session.SessionManager;
import dev.ed.edcore.api.storage.EDStorage;
import dev.ed.edcore.api.license.LicenseStatus;
import dev.ed.edcore.util.MessageFormatter;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Центральный API EDCore для зависимых ED-плагинов.
 */
public interface EDCoreAPI {

    PlatformType getPlatform();

    Path getDataDirectory();

    EDLogger getLogger();

    EDScheduler getScheduler();

    EDConfig getConfig();

    EDStorage getStorage();

    EDMessaging getMessaging();

    DatabaseManager getDatabaseManager();

    SessionManager getSessionManager();

    MigrationManager getMigrationManager();

    CryptoManager getCryptoManager();

    MessageFormatter getMessageFormatter();

    String getVersion();

    /** {@code true} после успешной онлайн-проверки лицензии. */
    boolean isLicenseValid();

    LicenseStatus getLicenseStatus();

    String getLicenseOwner();

    Optional<EDAuthAPI> getAuthApi();

    void setAuthApi(EDAuthAPI api);

    void clearAuthApi();

    /**
     * {@code true} после корректного отключения ядра (reload / stop).
     */
    boolean isShutdown();
}
