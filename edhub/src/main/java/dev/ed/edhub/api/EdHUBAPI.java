package dev.ed.edhub.api;

import dev.ed.edhub.config.WorldConfig;

public interface EdHUBAPI {
    Location getSpawnLocation(String worldName);

    Location getFirstSpawnLocation(String worldName);

    boolean isSpawnProtected(org.bukkit.Location loc);

    WorldConfig getWorldConfig(String worldName);
}
