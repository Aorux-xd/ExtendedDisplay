package dev.ed.edchat.commands;

import dev.ed.edchat.ChatManager;
import dev.ed.edchat.ConfigManager;
import dev.ed.edcore.api.EDCoreProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReplyCommand implements CommandExecutor {
    private final ConfigManager config;
    private final ChatManager chatManager;
    private final MsgCommand msgCommand;

    public ReplyCommand(ConfigManager config, ChatManager chatManager, MsgCommand msgCommand) {
        this.config = config;
        this.chatManager = chatManager;
        this.msgCommand = msgCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player from)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        if (!sender.hasPermission("edchat.msg")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/reply <сообщение>");
            return true;
        }
        var partnerId = chatManager.getLastPartner(from.getUniqueId());
        if (partnerId == null) {
            sender.sendMessage("§cНекому отвечать.");
            return true;
        }
        Player target = Bukkit.getPlayer(partnerId);
        if (target == null) {
            sender.sendMessage("§cИгрок не в сети.");
            return true;
        }
        if (from.getUniqueId().equals(target.getUniqueId())) {
            from.sendMessage(EDCoreProvider.get().getMessageFormatter()
                    .format(from, config.msgCannotMsgSelf, java.util.Map.of()));
            return true;
        }
        String message = String.join(" ", args);
        msgCommand.sendPrivate(from, target, message);
        return true;
    }
}
