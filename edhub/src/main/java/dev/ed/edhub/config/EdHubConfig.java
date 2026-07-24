package dev.ed.edhub.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EdHubConfig {
    private final Map<String, WorldConfig> worlds;
    private final WorldConfig defaults;
    private final List<WhitelistEntry> whitelist;

    public EdHubConfig(Map<String, WorldConfig> worlds, WorldConfig defaults, List<WhitelistEntry> whitelist) {
        this.worlds = new HashMap<>(worlds);
        this.defaults = defaults;
        this.whitelist = whitelist != null ? List.copyOf(whitelist) : List.of();
    }

    public WorldConfig defaults() {
        return defaults;
    }

    public Map<String, WorldConfig> worlds() {
        return Collections.unmodifiableMap(worlds);
    }

    public List<WhitelistEntry> whitelist() {
        return whitelist;
    }

    public WorldConfig resolve(String worldName) {
        if (worldName == null) {
            return defaults;
        }
        return worlds.getOrDefault(worldName.toLowerCase(), defaults);
    }
}
