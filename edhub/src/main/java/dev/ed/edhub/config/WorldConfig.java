package dev.ed.edhub.config;

import org.bukkit.GameMode;

public record WorldConfig(
        String worldName,
        boolean applyToAllWorlds,
        boolean infinityHp,
        boolean infinityHunger,
        boolean fallDamage,
        boolean weatherChange,
        boolean weatherFreeze,
        boolean timeCycle,
        HubTimePreset fixedTime,
        boolean monsters,
        boolean animals,
        boolean patrols,
        boolean trader,
        boolean freezeWater,
        boolean pvpEnabled,
        boolean allowProjectiles,
        GameMode defaultGamemode,
        SpawnProtectionConfig spawnProtection,
        HubLocation firstSpawn,
        HubLocation hubSpawn,
        boolean spawnCommandEnabled,
        InventoryConfig inventory,
        AntiVoidConfig antiVoid,
        boolean debug
) {
}
