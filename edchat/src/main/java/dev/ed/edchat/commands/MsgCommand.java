package dev.ed.edchat.commands;

import dev.ed.edchat.ChatManager;
import dev.ed.edchat.ConfigManager;
import dev.ed.edchat.util.PlayerLookup;
import dev.ed.edcore.api.EDCoreProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MsgCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager config;
    private final ChatManager chatManager;

    public MsgCommand(ConfigManager config, ChatManager chatManager) {
        this.config = config;
        this.chatManager = chatManager;
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
        if (args.length < 2) {
            sender.sendMessage("§e/msg <игрок> <сообщение>");
            return true;
        }
        Player target = PlayerLookup.findOnline(args[0]);
        if (target == null) {
            sender.sendMessage("§cИгрок не в сети.");
            return true;
        }
        if (from.getUniqueId().equals(target.getUniqueId())) {
            from.sendMessage(EDCoreProvider.get().getMessageFormatter()
                    .format(from, config.msgCannotMsgSelf, Map.of()));
            return true;
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        sendPrivate(from, target, message);
        return true;
    }

    public void sendPrivate(Player from, Player target, String message) {
        if (from.getUniqueId().equals(target.getUniqueId())) {
            from.sendMessage(EDCoreProvider.get().getMessageFormatter()
                    .format(from, config.msgCannotMsgSelf, Map.of()));
            return;
        }
        var formatter = EDCoreProvider.get().getMessageFormatter();
        Map<String, String> sent = ChatManager.basePlaceholders(from, message);
        sent.put("%target%", target.getName());
        from.sendMessage(formatter.format(from, config.pmSentFormat, sent));

        Map<String, String> recv = ChatManager.basePlaceholders(from, message);
        target.sendMessage(formatter.format(from, config.pmReceiveFormat, recv));

        chatManager.setLastPartner(from.getUniqueId(), target.getUniqueId());
        chatManager.setLastPartner(target.getUniqueId(), from.getUniqueId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player self) {
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(self.getUniqueId()))
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
