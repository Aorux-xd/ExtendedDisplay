package dev.ed.edcore.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

public final class AuditLogger {
    private final boolean enabled;
    private final String mode;
    private final Path file;
    private final byte[] salt;

    public AuditLogger(boolean enabled, String mode, Path dataDir, byte[] salt) {
        this(enabled, mode, dataDir, salt, "ed-audit.log");
    }

    public AuditLogger(boolean enabled, String mode, Path dataDir, byte[] salt, String logFileName) {
        this.enabled = enabled;
        this.mode = mode == null ? "salted-hash" : mode;
        this.file = dataDir.resolve(logFileName == null ? "ed-audit.log" : logFileName);
        this.salt = salt != null ? salt : new byte[0];
    }

    public void log(String action, String username, String ip, String reason) {
        if (!enabled) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            String line = "{\"ts\":" + Instant.now().getEpochSecond()
                    + ",\"action\":\"" + esc(action) + "\""
                    + ",\"user\":\"" + esc(username) + "\""
                    + ",\"ip\":\"" + esc(maskIp(ip)) + "\""
                    + ",\"reason\":\"" + esc(reason) + "\"}\n";
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    public void log(String event, Map<String, String> data) {
        if (!enabled || data == null) {
            return;
        }
        log(event,
                data.getOrDefault("user", data.getOrDefault("username", "")),
                data.getOrDefault("ip", ""),
                data.getOrDefault("reason", ""));
    }

    private String maskIp(String ip) {
        if (ip == null) {
            return "unknown";
        }
        if ("plain".equalsIgnoreCase(mode)) {
            return ip;
        }
        if ("mask".equalsIgnoreCase(mode)) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".x";
            }
            return "masked";
        }
        return hmacSha256(ip);
    }

    private String hmacSha256(String v) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            byte[] out = mac.doFinal(v.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            return "hasherr";
        }
    }

    private static String esc(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
