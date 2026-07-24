package dev.ed.edhub.config;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EdHubConfigLoader {

    public EdHubConfig load(FileConfiguration cfg) {
        WorldConfig defaults = readWorldConfig(cfg, null);
        Map<String, WorldConfig> byWorld = new HashMap<>();
        ConfigurationSection worlds = cfg.getConfigurationSection("worlds");
        if (worlds != null) {
            for (String world : worlds.getKeys(false)) {
                ConfigurationSection sec = worlds.getConfigurationSection(world);
                if (sec == null) {
                    continue;
                }
                byWorld.put(world.toLowerCase(), readWorldConfig(cfg, sec));
            }
        }
        return new EdHubConfig(byWorld, defaults, readWhitelist(cfg));
    }

    private WorldConfig readWorldConfig(FileConfiguration root, ConfigurationSection override) {
        String worldName = readString(root, override, "world-name", "world");
        boolean applyAll = readBoolean(root, override, "apply-to-all-worlds", false);

        boolean infinityHp = readBoolean(root, override, "infinity-hp", true);
        boolean infinityHunger = readBoolean(root, override, "infinity-hunger", true);
        boolean fallDamage = readBoolean(root, override, "fall-damage", false);

        boolean weatherChange = readBoolean(root, override, "weather.change", false);
        boolean weatherFreeze = readBoolean(root, override, "weather.freeze", true);
        boolean timeCycle = readBoolean(root, override, "time.cycle", false);
        HubTimePreset fixedTime = HubTimePreset.parse(readString(root, override, "time.fixed", "DAY"));

        boolean monsters = readBoolean(root, override, "mob-spawn.monsters", false);
        boolean animals = readBoolean(root, override, "mob-spawn.animals", false);
        boolean patrols = readBoolean(root, override, "mob-spawn.patrols", false);
        boolean trader = readBoolean(root, override, "mob-spawn.trader", false);
        boolean freezeWater = readBoolean(root, override, "biome.freeze-water", false);
        boolean pvpEnabled = readBoolean(root, override, "pvp.enabled", false);
        boolean allowProjectiles = readBoolean(root, override, "pvp.allow-projectiles", false);

        GameMode gm = parseGamemode(readString(root, override, "default-gamemode", "ADVENTURE"));
        SpawnProtectionConfig protection = readSpawnProtection(root, override);
        HubLocation first = readLocation(root, override, "first-spawn", worldName);
        HubLocation hub = readLocation(root, override, "hub-spawn", worldName);
        boolean spawnEnabled = readBoolean(root, override, "commands.spawn-enabled", true);
        InventoryConfig inventory = readInventory(root, override);
        AntiVoidConfig antiVoid = readAntiVoid(root, override);
        boolean debug = readBoolean(root, override, "debug", false);

        return new WorldConfig(worldName, applyAll, infinityHp, infinityHunger, fallDamage,
                weatherChange, weatherFreeze, timeCycle, fixedTime,
                monsters, animals, patrols, trader, freezeWater, pvpEnabled, allowProjectiles, gm,
                protection, first, hub, spawnEnabled, inventory, antiVoid, debug);
    }

    private List<WhitelistEntry> readWhitelist(FileConfiguration root) {
        java.util.List<WhitelistEntry> out = new java.util.ArrayList<>();
        java.util.List<Map<?, ?>> list = root.getMapList("whitelist-nickname");
        for (Map<?, ?> raw : list) {
            if (raw == null) continue;
            Object nickRaw = raw.get("nick");
            String nick = nickRaw == null ? "" : String.valueOf(nickRaw).trim();
            if (nick.isBlank()) continue;
            Boolean allowBreak = boolOrNull(raw.get("allow-block-break"));
            Boolean allowPlace = boolOrNull(raw.get("allow-block-place"));
            Boolean allowPvp = boolOrNull(raw.get("allow-pvp"));
            Boolean allowGm = boolOrNull(raw.get("allow-gamemode-change"));
            Boolean bypass = boolOrNull(raw.get("bypass-spawn-protection"));
            Boolean fly = boolOrNull(raw.get("allow-fly"));

            java.util.List<GameMode> allowed = null;
            Object modes = raw.get("allowed-gamemodes");
            if (modes instanceof java.util.List<?> l && !l.isEmpty()) {
                java.util.List<GameMode> parsed = new java.util.ArrayList<>();
                for (Object o : l) {
                    try {
                        parsed.add(GameMode.valueOf(String.valueOf(o).trim().toUpperCase()));
                    } catch (Exception ignored) {
                    }
                }
                if (!parsed.isEmpty()) allowed = parsed;
            }
            out.add(new WhitelistEntry(nick, allowBreak, allowPlace, allowPvp, allowGm, allowed, bypass, fly));
        }
        return out;
    }

    private Boolean boolOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        return Boolean.parseBoolean(s);
    }

    private SpawnProtectionConfig readSpawnProtection(FileConfiguration root, ConfigurationSection override) {
        int radius = Math.max(0, readInt(root, override, "spawn-protection.radius", 256));
        HubLocation center = readLocation(root, override, "spawn-protection.center", readString(root, override, "world-name", "world"));
        return new SpawnProtectionConfig(
                radius,
                center,
                readBoolean(root, override, "spawn-protection.allow-block-break", false),
                readBoolean(root, override, "spawn-protection.allow-block-place", false),
                readBoolean(root, override, "spawn-protection.allow-pvp", false),
                readBoolean(root, override, "spawn-protection.allow-mob-spawning", false)
        );
    }

    private InventoryConfig readInventory(FileConfiguration root, ConfigurationSection override) {
        boolean clear = readBoolean(root, override, "inventory.clear-on-join", false);
        Set<Material> whitelist = new HashSet<>();
        java.util.List<String> items;
        if (override != null && override.isList("inventory.whitelist-items")) {
            items = override.getStringList("inventory.whitelist-items");
        } else {
            items = root.getStringList("inventory.whitelist-items");
        }
        for (String s : items) {
            if (s == null || s.isBlank()) {
                continue;
            }
            try {
                whitelist.add(Material.valueOf(s.trim().toUpperCase()));
            } catch (Exception ignored) {
            }
        }
        return new InventoryConfig(clear, whitelist);
    }

    private AntiVoidConfig readAntiVoid(FileConfiguration root, ConfigurationSection override) {
        return new AntiVoidConfig(
                readBoolean(root, override, "anti-void-y.enabled", false),
                readDouble(root, override, "anti-void-y.min-y", -64d),
                readDouble(root, override, "anti-void-y.max-y", 320d),
                readString(root, override, "anti-void-y.teleport-to", "hub-spawn"),
                readString(root, override, "anti-void-y.message", "§cВы телепортированы на спавн.")
        );
    }

    private HubLocation readLocation(FileConfiguration root, ConfigurationSection override, String path, String defaultWorld) {
        String world = readString(root, override, path + ".world", defaultWorld);
        double x = readDouble(root, override, path + ".x", -0.5d);
        double y = readDouble(root, override, path + ".y", 64d);
        double z = readDouble(root, override, path + ".z", 1.5d);
        float yaw = (float) readDouble(root, override, path + ".yaw", 0d);
        float pitch = (float) readDouble(root, override, path + ".pitch", 0d);
        return new HubLocation(world, x, y, z, yaw, pitch);
    }

    private String readString(FileConfiguration root, ConfigurationSection override, String key, String def) {
        if (override != null && override.isSet(key)) {
            return override.getString(key, def);
        }
        return root.getString(key, def);
    }

    private boolean readBoolean(FileConfiguration root, ConfigurationSection override, String key, boolean def) {
        if (override != null && override.isSet(key)) {
            return override.getBoolean(key, def);
        }
        return root.getBoolean(key, def);
    }

    private int readInt(FileConfiguration root, ConfigurationSection override, String key, int def) {
        if (override != null && override.isSet(key)) {
            return override.getInt(key, def);
        }
        return root.getInt(key, def);
    }

    private double readDouble(FileConfiguration root, ConfigurationSection override, String key, double def) {
        if (override != null && override.isSet(key)) {
            return override.getDouble(key, def);
        }
        return root.getDouble(key, def);
    }

    private GameMode parseGamemode(String raw) {
        if (raw == null || raw.isBlank()) {
            return GameMode.ADVENTURE;
        }
        try {
            return GameMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return GameMode.ADVENTURE;
        }
    }
}
