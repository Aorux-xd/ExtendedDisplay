package dev.ed.edchat.commands;

import dev.ed.edchat.ChatManager;
import dev.ed.edchat.ConfigManager;
import dev.ed.edcore.api.EDCoreProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ChatCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager config;
    private final ChatManager chatManager;

    public ChatCommand(ConfigManager config, ChatManager chatManager) {
        this.config = config;
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("edchat.admin")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(usageHint());
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "toggle" -> handleToggle(sender);
            case "clear" -> handleClear(sender);
            case "mute" -> {
                if (!config.muteEnableCommand) {
                    sender.sendMessage("§cКоманда /chat mute отключена в конфиге.");
                    return true;
                }
                handleMute(sender, args);
            }
            case "unmute" -> {
                if (!config.muteEnableCommand) {
                    sender.sendMessage("§cКоманда /chat unmute отключена в конфиге.");
                    return true;
                }
                handleUnmute(sender, args);
            }
            default -> sender.sendMessage(usageHint());
        }
        return true;
    }

    private void handleToggle(CommandSender sender) {
        chatManager.setChatEnabled(!chatManager.isChatEnabled());
        String template = chatManager.isChatEnabled() ? config.msgChatEnabled : config.msgChatDisabled;
        broadcastAdmin(sender, template);
        sender.sendMessage(chatManager.isChatEnabled() ? "§aЧат включён." : "§cЧат выключен.");
    }

    private void handleClear(CommandSender sender) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                p.sendMessage(" ");
            }
        }
        broadcastAdmin(sender, config.msgChatCleared);
        sender.sendMessage("§aЧат очищен.");
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/chat mute <игрок>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cИгрок не в сети.");
            return;
        }
        chatManager.adminMute(target.getUniqueId());
        if (config.muteActionbarNotify) {
            target.sendActionBar(Component.text(config.muteActionbarMuted));
        }
        sender.sendMessage("§aИгрок " + target.getName() + " замьючен.");
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/chat unmute <игрок>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cИгрок не в сети.");
            return;
        }
        chatManager.unmute(target.getUniqueId());
        if (config.muteActionbarNotify) {
            target.sendActionBar(Component.text(config.muteActionbarUnmuted));
        }
        sender.sendMessage("§aИгрок " + target.getName() + " размьючен.");
    }

    private void broadcastAdmin(CommandSender admin, String template) {
        if (template == null || template.isBlank()) {
            return;
        }
        var formatter = EDCoreProvider.get().getMessageFormatter();
        Object playerCtx = admin instanceof Player p ? p : null;
        Map<String, String> ph = Map.of("%admin%", admin.getName());
        Bukkit.getServer().broadcast(formatter.format(playerCtx, template, ph));
    }

    private String usageHint() {
        if (config.muteEnableCommand) {
            return "§e/chat toggle|clear|mute|unmute";
        }
        return "§e/chat toggle|clear";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("toggle", "clear"));
            if (config.muteEnableCommand) {
                subs.add("mute");
                subs.add("unmute");
            }
            return subs;
        }
        if (config.muteEnableCommand && args.length == 2
                && (args[0].equalsIgnoreCase("mute") || args[0].equalsIgnoreCase("unmute"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
