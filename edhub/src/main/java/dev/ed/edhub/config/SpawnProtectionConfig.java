package dev.ed.edhub.config;

public record SpawnProtectionConfig(
        int radius,
        HubLocation center,
        boolean allowBlockBreak,
        boolean allowBlockPlace,
        boolean allowPvp,
        boolean allowMobSpawning
) {
}
