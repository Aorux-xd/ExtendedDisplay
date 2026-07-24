package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.config.EDConfig;

import java.util.UUID;

/**
 * Чтение/запись license.* и updates.* в config.yml.
 */
public final class LicenseConfig {

    private final EDConfig config;

    public LicenseConfig(EDConfig config) {
        this.config = config;
    }

    public String apiBaseUrl() {
        return config.getString("license.api-base-url", "https://edcore.vercel.app");
    }

    public String licenseKey() {
        return config.getString("license.key", "").trim();
    }

    public String serverId() {
        return config.getString("license.server-id", "").trim();
    }

    public String hardwareId() {
        return config.getString("license.hardware-id", "").trim();
    }

    public void ensureServerIdentity() {
        boolean changed = false;
        if (serverId().isBlank()) {
            config.set("license.server-id", UUID.randomUUID().toString());
            changed = true;
        }
        if (hardwareId().isBlank()) {
            config.set("license.hardware-id", HardwareIdGenerator.generate());
            changed = true;
        }
        if (changed) {
            config.save();
            config.reload();
        }
    }

    public boolean autoCheckUpdates() {
        return Boolean.parseBoolean(config.getString("updates.auto-check", "true"));
    }

    public int updateCheckIntervalHours() {
        try {
            return Integer.parseInt(config.getString("updates.check-interval-hours", "24"));
        } catch (NumberFormatException e) {
            return 24;
        }
    }

    public String message(String path, String def) {
        return config.getString(path, def);
    }
}
