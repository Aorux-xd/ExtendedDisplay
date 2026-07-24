package dev.ed.edchat.commands;

import dev.ed.edchat.ChatManager;
import dev.ed.edchat.ConfigManager;
import dev.ed.edcore.api.EDCoreProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MeCommand implements CommandExecutor {
    private final ConfigManager config;

    public MeCommand(ConfigManager config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        if (!sender.hasPermission("edchat.me")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/me <действие>");
            return true;
        }
        String action = String.join(" ", args);
        var component = EDCoreProvider.get().getMessageFormatter()
                .format(player, config.meFormat, ChatManager.basePlaceholders(player, action));
        Bukkit.getServer().broadcast(component);
        return true;
    }
}
