package dev.ed.edhub.command;

import dev.ed.edhub.service.HubManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class HubCommand implements CommandExecutor {
    private final HubManager manager;

    public HubCommand(HubManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("edhub.admin")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/hub reload | /hub debug");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.reload();
                manager.applyAllWorlds();
                sender.sendMessage("§aКонфигурация перезагружена!");
            }
            case "debug" -> {
                boolean newValue = !manager.debug();
                manager.setDebug(newValue);
                sender.sendMessage(newValue ? "§eОтладка включена" : "§eОтладка выключена");
            }
            default -> sender.sendMessage("§e/hub reload | /hub debug");
        }
        return true;
    }
}
