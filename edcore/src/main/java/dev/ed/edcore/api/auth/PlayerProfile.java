package dev.ed.edcore.api.auth;

public record PlayerProfile(
        String username,
        boolean premium,
        String firstIp,
        String lastIp,
        long lastLogin,
        long registeredAt
) {
}
