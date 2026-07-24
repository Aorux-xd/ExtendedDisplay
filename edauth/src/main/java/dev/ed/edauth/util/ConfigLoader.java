package dev.ed.edauth.util;

import dev.ed.edcore.api.crypto.CryptoManager;
import dev.ed.edcore.api.database.DatabaseManager;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ConfigLoader {
    private final Yaml yaml = new Yaml();

    @SuppressWarnings("unchecked")
    public AuthConfig loadConfig(Path configFile, InputStream defaultStream) throws IOException {
        ensure(configFile, defaultStream);
        AuthConfig out = new AuthConfig();
        Map<String, Object> root = yaml.load(Files.readString(configFile, StandardCharsets.UTF_8));
        if (root == null) {
            return out;
        }
        Map<String, Object> auth = map(root, "auth");
        String storage = str(auth, "storage", "sqlite");
        out.storage = switch (storage.toLowerCase()) {
            case "mysql" -> DatabaseManager.Type.MYSQL;
            case "postgresql", "postgres" -> DatabaseManager.Type.POSTGRESQL;
            case "h2" -> DatabaseManager.Type.H2;
            default -> DatabaseManager.Type.SQLITE;
        };
        Map<String, Object> mysql = map(auth, "mysql");
        out.user = str(mysql, "user", "");
        out.password = str(mysql, "password", "");
        String host = str(mysql, "host", "localhost");
        int port = intval(mysql, "port", 3306);
        String db = str(mysql, "database", "edauth");
        out.jdbcUrl = switch (out.storage) {
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + db;
            case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + port + "/" + db;
            case H2 -> "jdbc:h2:file:" + configFile.getParent().resolve("edauth-h2").toAbsolutePath();
            case SQLITE -> "jdbc:sqlite:" + configFile.getParent().resolve("edauth.db").toAbsolutePath();
        };
        Map<String, Object> session = map(auth, "session");
        out.sessionEnabled = bool(session, "enabled", true);
        out.sessionTimeoutSec = intval(session, "timeout", 3600);
        out.sessionStrictIpBinding = bool(session, "strict-ip-binding", true);
        out.sessionIpMask = intval(session, "ip-mask", 32);
        Map<String, Object> premium = map(auth, "premium");
        out.premiumEnabled = bool(premium, "enabled", true);
        Map<String, Object> brute = map(auth, "brute-force");
        out.bruteMaxAttempts = intval(brute, "max-attempts", 5);
        out.bruteDelaySec = intval(brute, "delay", 300);
        Map<String, Object> virtual = map(auth, "virtual-server");
        out.virtualServer = bool(virtual, "enabled", false);
        String algo = str(auth, "algorithm", "bcrypt");
        out.algorithm = switch (algo.toLowerCase()) {
            case "sha-512", "sha512" -> CryptoManager.Algorithm.SHA512;
            case "pbkdf2" -> CryptoManager.Algorithm.PBKDF2;
            default -> CryptoManager.Algorithm.BCRYPT;
        };
        Object allowed = root.get("allowed-commands");
        if (allowed instanceof List<?> l) {
            out.allowedCommands = l.stream().map(String::valueOf).map(String::toLowerCase).toList();
        }

        Map<String, Object> security = map(root, "security");
        out.bcryptMaxPerSecond = intval(security, "bcrypt-max-per-second", 20);
        Map<String, Object> pp = map(security, "password-policy");
        out.passwordPolicy.minLength = intval(pp, "min-length", 6);
        out.passwordPolicy.requireUppercase = bool(pp, "require-uppercase", false);
        out.passwordPolicy.requireDigit = bool(pp, "require-digit", false);
        out.passwordPolicy.requireSpecial = bool(pp, "require-special-char", false);
        out.passwordPolicy.blockCommonPasswords = bool(pp, "block-common-passwords", true);
        String src = str(pp, "common-passwords-source", "file");
        out.dictionaryLoader.source = switch (src.toLowerCase()) {
            case "builtin" -> dev.ed.edcore.security.PasswordDictionaryLoader.Source.BUILTIN;
            case "url" -> dev.ed.edcore.security.PasswordDictionaryLoader.Source.URL;
            default -> dev.ed.edcore.security.PasswordDictionaryLoader.Source.FILE;
        };
        out.dictionaryLoader.url = str(pp, "common-passwords-url", "");
        Object wl = pp.get("common-passwords-url-whitelist");
        if (wl instanceof java.util.List<?> l) {
            out.dictionaryLoader.urlWhitelist = l.stream().map(String::valueOf).toList();
        } else {
            out.dictionaryLoader.urlWhitelist = java.util.List.of();
        }
        out.dictionaryLoader.urlMaxSizeMb = intval(pp, "common-passwords-url-max-size-mb", 5);

        Map<String, Object> rl = map(security, "rate-limit");
        out.rateLimiter.enabled = bool(rl, "enabled", true);
        out.rateLimiter.ipPerMinute = intval(rl, "ip-per-minute", 5);
        out.rateLimiter.nickPerMinute = intval(rl, "nick-per-minute", 10);
        out.rateLimiter.globalIpThreshold = intval(rl, "global-ip-threshold", 15);
        out.rateLimiter.globalIpBlockMinutes = intval(rl, "global-ip-block-minutes", 10);
        out.rateLimiter.aggregateSubnet = intval(rl, "aggregate-subnet", 0);

        Map<String, Object> logging = map(root, "logging");
        out.ipMaskingMode = str(logging, "ip-masking", "hash");
        out.auditLogEnabled = bool(logging, "audit-log", true);

        Map<String, Object> premiumProtection = map(security, "premium-protection");
        out.premiumAllowInOffline = bool(premiumProtection, "allow-in-offline", false);
        return out;
    }

    @SuppressWarnings("unchecked")
    public MessageBundle loadMessages(Path messagesFile, InputStream defaultStream) throws IOException {
        ensure(messagesFile, defaultStream);
        MessageBundle out = new MessageBundle();
        Map<String, Object> root = yaml.load(Files.readString(messagesFile, StandardCharsets.UTF_8));
        if (root == null) {
            return out;
        }
        for (Map.Entry<String, Object> e : root.entrySet()) {
            out.put(e.getKey(), String.valueOf(e.getValue()));
        }
        return out;
    }

    private void ensure(Path file, InputStream defaults) throws IOException {
        if (Files.exists(file)) return;
        Files.createDirectories(file.getParent());
        if (defaults != null) {
            Files.copy(defaults, file);
        } else {
            Files.writeString(file, "");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> in, String key) {
        Object o = in != null ? in.get(key) : null;
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private String str(Map<String, Object> in, String key, String def) {
        Object o = in.get(key);
        return o == null ? def : String.valueOf(o);
    }

    private int intval(Map<String, Object> in, String key, int def) {
        Object o = in.get(key);
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private boolean bool(Map<String, Object> in, String key, boolean def) {
        Object o = in.get(key);
        if (o instanceof Boolean b) return b;
        if (o == null) return def;
        return Boolean.parseBoolean(String.valueOf(o));
    }
}
