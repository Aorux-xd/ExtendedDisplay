package dev.ed.edcustomjoin;

import dev.ed.edcore.api.support.EDPluginSupportPaper;
import org.bukkit.plugin.java.JavaPlugin;

public final class EDCustomJoinPlugin extends JavaPlugin {
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        if (!EDPluginSupportPaper.requireEDCore(this)) {
            return;
        }
        configManager = new ConfigManager(this);
        configManager.load();
        getServer().getPluginManager().registerEvents(new JoinListener(this, configManager), this);
        var cmd = getCommand("customjoin");
        if (cmd != null) {
            ReloadCommand reload = new ReloadCommand(configManager);
            cmd.setExecutor(reload);
            cmd.setTabCompleter(reload);
        }
        getLogger().info("EDCustomJoin v1.0.3 включён");
    }

    public ConfigManager configManager() {
        return configManager;
    }
}
