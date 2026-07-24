package dev.ed.edauth.storage;

public record AccountRecord(
        String username,
        String passwordHash,
        boolean premium,
        String firstIp,
        String lastIp,
        long lastLogin,
        long registered
) {
}
