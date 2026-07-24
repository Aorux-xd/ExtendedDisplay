package dev.ed.edcore.internal;

import dev.ed.edcore.api.EDCoreAPI;
import dev.ed.edcore.api.PlatformType;
import dev.ed.edcore.api.auth.EDAuthAPI;
import dev.ed.edcore.api.license.LicenseStatus;
import dev.ed.edcore.internal.license.LicenseState;
import dev.ed.edcore.api.config.EDConfig;
import dev.ed.edcore.api.crypto.CryptoManager;
import dev.ed.edcore.api.database.DatabaseManager;
import dev.ed.edcore.api.logging.EDLogger;
import dev.ed.edcore.api.messaging.EDMessaging;
import dev.ed.edcore.api.migration.MigrationManager;
import dev.ed.edcore.api.scheduler.EDScheduler;
import dev.ed.edcore.api.session.SessionManager;
import dev.ed.edcore.api.storage.EDStorage;
import dev.ed.edcore.util.MessageFormatter;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EDCoreImpl implements EDCoreAPI {

    private final PlatformType platform;
    private final Path dataDirectory;
    private final EDLogger logger;
    private final EDScheduler scheduler;
    private final EDConfig config;
    private final EDStorage storage;
    private final EDMessaging messaging;
    private final DatabaseManager databaseManager;
    private final SessionManager sessionManager;
    private final MigrationManager migrationManager;
    private final CryptoManager cryptoManager;
    private final MessageFormatter messageFormatter = new MessageFormatter();
    private final String version;
    private final LicenseState licenseState;
    private volatile EDAuthAPI authApi;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public EDCoreImpl(PlatformType platform, Path dataDirectory, EDLogger logger, EDScheduler scheduler,
                      EDConfig config, EDStorage storage, EDMessaging messaging,
                      DatabaseManager databaseManager, SessionManager sessionManager,
                      MigrationManager migrationManager, CryptoManager cryptoManager,
                      String version, LicenseState licenseState) {
        this.platform = platform;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.scheduler = scheduler;
        this.config = config;
        this.storage = storage;
        this.messaging = messaging;
        this.databaseManager = databaseManager;
        this.sessionManager = sessionManager;
        this.migrationManager = migrationManager;
        this.cryptoManager = cryptoManager;
        this.version = version;
        this.licenseState = licenseState;
    }

    void markShutdown() {
        shutdown.set(true);
        storage.save();
    }

    @Override
    public PlatformType getPlatform() {
        return platform;
    }

    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public EDLogger getLogger() {
        return logger;
    }

    @Override
    public EDScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public EDConfig getConfig() {
        return config;
    }

    @Override
    public EDStorage getStorage() {
        return storage;
    }

    @Override
    public EDMessaging getMessaging() {
        return messaging;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public MigrationManager getMigrationManager() {
        return migrationManager;
    }

    @Override
    public CryptoManager getCryptoManager() {
        return cryptoManager;
    }

    @Override
    public MessageFormatter getMessageFormatter() {
        return messageFormatter;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isLicenseValid() {
        return licenseState.isValid();
    }

    @Override
    public LicenseStatus getLicenseStatus() {
        return licenseState.getStatus();
    }

    @Override
    public String getLicenseOwner() {
        return licenseState.getOwner();
    }

    @Override
    public Optional<EDAuthAPI> getAuthApi() {
        return Optional.ofNullable(authApi);
    }

    @Override
    public void setAuthApi(EDAuthAPI api) {
        this.authApi = api;
    }

    @Override
    public void clearAuthApi() {
        this.authApi = null;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}
