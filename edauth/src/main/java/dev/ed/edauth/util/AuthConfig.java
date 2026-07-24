package dev.ed.edauth.util;

import dev.ed.edcore.api.crypto.CryptoManager;
import dev.ed.edcore.api.database.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

public final class AuthConfig {
    public DatabaseManager.Type storage = DatabaseManager.Type.SQLITE;
    public String jdbcUrl = "";
    public String user = "";
    public String password = "";
    public boolean sessionEnabled = true;
    public int sessionTimeoutSec = 3600;
    public boolean sessionStrictIpBinding = true;
    public int sessionIpMask = 32;
    public boolean premiumEnabled = true;
    public int bruteMaxAttempts = 5;
    public int bruteDelaySec = 300;
    public boolean virtualServer = false;
    public CryptoManager.Algorithm algorithm = CryptoManager.Algorithm.BCRYPT;
    public List<String> allowedCommands = new ArrayList<>();

    public final dev.ed.edcore.security.PasswordPolicy passwordPolicy = new dev.ed.edcore.security.PasswordPolicy();
    public final dev.ed.edcore.security.RateLimiter rateLimiter = new dev.ed.edcore.security.RateLimiter();
    public final dev.ed.edcore.security.PasswordDictionaryLoader dictionaryLoader = new dev.ed.edcore.security.PasswordDictionaryLoader();
    public String ipMaskingMode = "hash";
    public boolean auditLogEnabled = true;
    public int bcryptMaxPerSecond = 20;
    public boolean premiumAllowInOffline = false;
}
