package dev.ed.edhub;

import dev.ed.edcore.api.support.EDPluginSupportPaper;
import dev.ed.edhub.command.HubCommand;
import dev.ed.edhub.command.SpawnCommand;
import dev.ed.edcore.api.gate.EdCoreGate;
import dev.ed.edhub.listener.*;
import dev.ed.edhub.service.HubManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EdHUBPlugin extends JavaPlugin {
    private HubManager hubManager;

    @Override
    public void onEnable() {
        if (!EDPluginSupportPaper.requireEDCore(this)) {
            return;
        }
        if (!EdCoreGate.verify("")) {
            getLogger().severe("EdHUB: проверка EDCore не пройдена.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        hubManager = new HubManager(this);
        hubManager.init();

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this, hubManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new EntityDamageListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new GodModeListener(this, hubManager), this);
        Bukkit.getPluginManager().registerEvents(new FallDamageListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new InfinityHungerListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new WeatherChangeListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new TimeChangeListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new CreatureSpawnListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new EntityChangeBlockListener(hubManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerGameModeChangeListener(hubManager), this);

        var spawnCmd = getCommand("spawn");
        if (spawnCmd != null) {
            spawnCmd.setExecutor(new SpawnCommand(hubManager));
        }
        var hubCmd = getCommand("hub");
        if (hubCmd != null) {
            hubCmd.setExecutor(new HubCommand(hubManager));
        }
        getLogger().info("EdHUB успешно запущен");
    }
}
