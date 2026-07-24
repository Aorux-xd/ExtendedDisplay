package dev.ed.edcustomjoin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private boolean enableJoin = true;
    private boolean enableQuit = true;
    private boolean enableFirstJoin = true;
    private String joinMessage;
    private String quitMessage;
    private String firstJoinMessage;
    private boolean hideDefault;
    private int delayTicks;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        enableJoin = c.getBoolean("enable-join", true);
        enableQuit = c.getBoolean("enable-quit", true);
        enableFirstJoin = c.getBoolean("enable-first-join", true);
        joinMessage = c.getString("join-message", "");
        quitMessage = c.getString("quit-message", "");
        firstJoinMessage = c.getString("first-join-message", "");
        hideDefault = c.getBoolean("hide-default", true);
        delayTicks = Math.max(0, c.getInt("delay-ticks", 0));
    }

    public boolean enableJoin() {
        return enableJoin;
    }

    public boolean enableQuit() {
        return enableQuit;
    }

    public boolean enableFirstJoin() {
        return enableFirstJoin;
    }

    public String joinMessage() {
        return joinMessage;
    }

    public String quitMessage() {
        return quitMessage;
    }

    public String firstJoinMessage() {
        return firstJoinMessage;
    }

    public boolean hideDefault() {
        return hideDefault;
    }

    public int delayTicks() {
        return delayTicks;
    }
}
