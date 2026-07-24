package dev.ed.edchat;

import dev.ed.edchat.util.BlockedWordsFilter;
import dev.ed.edcore.api.EDCoreProvider;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ChatListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final ChatManager chatManager;
    private final AntiSpamFilter antiSpam;

    public ChatListener(JavaPlugin plugin, ConfigManager config, ChatManager chatManager,
                        AntiSpamFilter antiSpam) {
        this.plugin = plugin;
        this.config = config;
        this.chatManager = chatManager;
        this.antiSpam = antiSpam;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player sender = event.getPlayer();
        var formatter = EDCoreProvider.get().getMessageFormatter();

        if (!chatManager.isChatEnabled() && !sender.hasPermission("edchat.admin")) {
            return;
        }
        if (chatManager.isMuted(sender.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (config.muteActionbarNotify) {
                    sender.sendActionBar(Component.text(config.muteActionbarBlocked));
                }
            });
            return;
        }
        if (!antiSpam.allow(sender)) {
            if (config.spamMessage != null && !config.spamMessage.isBlank()) {
                sender.sendMessage(formatter.format(sender, config.spamMessage,
                        ChatManager.basePlaceholders(sender, "")));
            }
            return;
        }

        String raw = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message());
        if (raw == null) {
            raw = "";
        }
        raw = filterBlockedWords(sender, raw);

        boolean global = resolveGlobal(raw);
        String message = stripPrefix(raw, global);
        if (message.isBlank()) {
            return;
        }

        Map<String, String> basePh = ChatManager.basePlaceholders(sender, message);

        List<Player> recipients = global ? new ArrayList<>(Bukkit.getOnlinePlayers())
                : localRecipients(sender);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player recipient : recipients) {
                String format = global ? config.globalFormat : config.localFormat;
                if (global && config.enableGlobalWorldSeparator
                        && !recipient.getWorld().equals(sender.getWorld())) {
                    format = insertWorldSeparator(format);
                }
                Component out = formatter.format(sender, format, basePh);
                recipient.sendMessage(out);
            }
        });
    }

    private String insertWorldSeparator(String format) {
        String sep = config.globalWorldSeparator;
        if (sep == null || sep.isBlank()) {
            return format;
        }
        if (format.contains("%player_name%")) {
            return format.replace("%player_name%", "%player_name%" + sep);
        }
        return format + sep;
    }

    private boolean resolveGlobal(String raw) {
        if (config.defaultMode == ConfigManager.DefaultMode.GLOBAL) {
            if (raw.startsWith(config.localPrefix)) {
                return false;
            }
            return true;
        }
        if (raw.startsWith(config.globalPrefix)) {
            return true;
        }
        return false;
    }

    private String stripPrefix(String raw, boolean global) {
        if (config.defaultMode == ConfigManager.DefaultMode.GLOBAL) {
            if (!global && raw.startsWith(config.localPrefix)) {
                return raw.substring(config.localPrefix.length()).trim();
            }
            return raw.trim();
        }
        if (global && raw.startsWith(config.globalPrefix)) {
            return raw.substring(config.globalPrefix.length()).trim();
        }
        return raw.trim();
    }

    private List<Player> localRecipients(Player sender) {
        List<Player> out = new ArrayList<>();
        double r2 = (double) config.localRadius * config.localRadius;
        var world = sender.getWorld();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(world)) {
                continue;
            }
            if (p.getLocation().distanceSquared(sender.getLocation()) <= r2) {
                out.add(p);
            }
        }
        return out;
    }

    private String filterBlockedWords(Player sender, String message) {
        if (!config.blockedWordsEnabled || config.blockedWords.isEmpty()) {
            return message;
        }
        List<String> matched = BlockedWordsFilter.findMatched(message, config.blockedWords);
        if (!matched.isEmpty() && config.blockedNotifyStaff) {
            notifyStaffBlockedWords(sender, matched);
        }
        return BlockedWordsFilter.filter(message, config.blockedWords, config.blockedReplaceWith);
    }

    private void notifyStaffBlockedWords(Player sender, List<String> matchedWords) {
        var formatter = EDCoreProvider.get().getMessageFormatter();
        String words = matchedWords.stream().distinct().collect(Collectors.joining(", "));
        Map<String, String> ph = Map.of(
                "%player%", sender.getName(),
                "%player_name%", sender.getName(),
                "%words%", words
        );
        Component notify = formatter.format(sender, config.blockedNotifyMessage, ph);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("edchat.notify")) {
                staff.sendMessage(notify);
            }
        }
    }
}
