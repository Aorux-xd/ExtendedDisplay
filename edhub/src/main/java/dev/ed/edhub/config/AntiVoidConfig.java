package dev.ed.edhub.config;

public record AntiVoidConfig(
        boolean enabled,
        double minY,
        double maxY,
        String teleportTo,
        String message
) {
}
