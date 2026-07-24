package dev.ed.edhub.config;

import org.bukkit.GameMode;

import java.util.List;

public record WhitelistEntry(
        String nick,
        Boolean allowBlockBreak,
        Boolean allowBlockPlace,
        Boolean allowPvp,
        Boolean allowGamemodeChange,
        List<GameMode> allowedGamemodes,
        Boolean bypassSpawnProtection,
        Boolean allowFly
) {
}
