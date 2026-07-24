package dev.ed.edcore.internal.license;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * SHA-256 от серийника материнской платы и ID процессора (с запасным офлайн-снимком).
 */
public final class HardwareIdGenerator {

    private HardwareIdGenerator() {
    }

    public static String generate() {
        StringBuilder raw = new StringBuilder();
        raw.append(readWmic("baseboard", "serialnumber"));
        raw.append('|');
        raw.append(readWmic("cpu", "processorid"));
        if (raw.toString().replace("|", "").isBlank()) {
            raw.append(fallbackFingerprint());
        }
        return sha256Hex(raw.toString());
    }

    private static String fallbackFingerprint() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(System.getProperty("os.name", "").getBytes(StandardCharsets.UTF_8));
            md.update(System.getProperty("os.arch", "").getBytes(StandardCharsets.UTF_8));
            md.update(System.getenv().getOrDefault("COMPUTERNAME",
                    System.getenv().getOrDefault("HOSTNAME", "")).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "fallback";
        }
    }

    private static String readWmic(String alias, String field) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return "";
        }
        Process process = null;
        try {
            process = new ProcessBuilder("wmic", alias, "get", field)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.equalsIgnoreCase(field)) {
                        continue;
                    }
                    if (!line.equalsIgnoreCase("to be filled by o.e.m.")
                            && !line.equalsIgnoreCase("default string")
                            && !line.equalsIgnoreCase("none")) {
                        return line;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return "";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return HexFormat.of().formatHex("error".getBytes(StandardCharsets.UTF_8));
        }
    }
}
