package dev.ed.edcore.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigUtils {
    private ConfigUtils() {}

    public static void reloadConfig(JavaPlugin plugin) {
        plugin.reloadConfig();
    }

    public static void saveConfig(JavaPlugin plugin) {
        plugin.saveConfig();
    }

    public static void migrateKey(FileConfiguration config, String oldKey, String newKey) {
        if (config.contains(newKey) || !config.contains(oldKey)) {
            return;
        }
        config.set(newKey, config.get(oldKey));
        config.set(oldKey, null);
    }

    public static void ensureDefaultFile(Path target, InputStream defaults) throws IOException {
        if (Files.exists(target)) {
            return;
        }
        if (defaults == null) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(defaults, target);
    }
}
