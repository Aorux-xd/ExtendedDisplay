package dev.ed.edcore.internal.config;

import dev.ed.edcore.api.config.EDConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitConfigAdapter implements EDConfig {

    private final JavaPlugin plugin;

    public BukkitConfigAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    @Override
    public String getString(String path, String defaultValue) {
        return cfg().getString(path, defaultValue);
    }

    @Override
    public void set(String path, Object value) {
        cfg().set(path, value);
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
    }

    @Override
    public void save() {
        plugin.saveConfig();
    }
}
