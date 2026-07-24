package dev.ed.edauth.bootstrap;

import dev.ed.edauth.auth.AuthService;
import dev.ed.edcore.util.AuditLogger;
import dev.ed.edcore.util.IpSaltManager;
import dev.ed.edauth.storage.JdbcAccountRepository;
import dev.ed.edauth.storage.SecurityRepository;
import dev.ed.edauth.util.AuthConfig;
import dev.ed.edauth.util.ConfigLoader;
import dev.ed.edauth.util.MessageBundle;
import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.api.database.DatabaseManager;

import java.io.InputStream;
import java.nio.file.Path;

public final class EDAuthKernel {
    private final Path dataDir;
    private final ConfigLoader loader = new ConfigLoader();

    private AuthConfig config;
    private MessageBundle messages;
    private AuthService authService;

    public EDAuthKernel(Path dataDir) {
        this.dataDir = dataDir;
    }

    public void init(ClassLoader cl) throws Exception {
        try (InputStream cfg = cl.getResourceAsStream("config.yml");
             InputStream msg = cl.getResourceAsStream("messages.yml")) {
            this.config = loader.loadConfig(dataDir.resolve("config.yml"), cfg);
            this.messages = loader.loadMessages(dataDir.resolve("messages.yml"), msg);
        }
        try (InputStream common = cl.getResourceAsStream("common-passwords.txt")) {
            // Load dictionary (file by default, seeded from builtin if missing)
            var dict = this.config.dictionaryLoader.load(dataDir, common);
            this.config.passwordPolicy.loadBuiltinCommonPasswords(new java.io.ByteArrayInputStream(String.join("\n", dict).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        DatabaseManager db = EDCoreProvider.get().getDatabaseManager();
        db.configure(config.storage, config.jdbcUrl, config.user, config.password);
        JdbcAccountRepository repo = new JdbcAccountRepository(db);
        repo.init();
        SecurityRepository security = new SecurityRepository(db);
        security.init();
        byte[] ipSalt = new IpSaltManager(dataDir).loadOrCreate();
        this.authService = new AuthService(repo, security, config,
                new AuditLogger(config.auditLogEnabled, config.ipMaskingMode, dataDir, ipSalt, "edauth-audit.log"));
    }

    public void reload() throws Exception {
        init(getClass().getClassLoader());
    }

    public AuthConfig config() {
        return config;
    }

    public MessageBundle messages() {
        return messages;
    }

    public AuthService auth() {
        return authService;
    }

    public Path dataDir() {
        return dataDir;
    }
}
