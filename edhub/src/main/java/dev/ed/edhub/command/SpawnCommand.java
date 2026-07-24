package dev.ed.edhub.command;

import dev.ed.edhub.config.WorldConfig;
import dev.ed.edhub.service.HubManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor {
    private final HubManager manager;

    public SpawnCommand(HubManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (!p.hasPermission("edhub.spawn")) {
            p.sendMessage("§cНет прав.");
            return true;
        }
        WorldConfig cfg = manager.getWorldConfig(p.getWorld().getName());
        if (!cfg.spawnCommandEnabled()) {
            p.sendMessage("§cКоманда отключена.");
            return true;
        }
        manager.teleportToConfiguredSpawn(p, false).thenAccept(ok ->
                p.sendMessage(ok ? "§aТелепортация на спавн..." : "§cНе удалось найти точку спавна."));
        return true;
    }
}
