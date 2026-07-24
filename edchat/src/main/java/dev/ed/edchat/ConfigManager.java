package dev.ed.edchat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {
    public enum DefaultMode { GLOBAL, LOCAL }

    private final JavaPlugin plugin;

    public DefaultMode defaultMode = DefaultMode.LOCAL;
    public int localRadius = 100;
    public String globalFormat = "";
    public String localFormat = "";
    public String globalPrefix = "!";
    public String localPrefix = "?";
    public String meFormat = "";
    public boolean enableGlobalWorldSeparator = false;
    public String globalWorldSeparator = "";
    public String pmSentFormat = "";
    public String pmReceiveFormat = "";

    public boolean antiSpamEnabled = true;
    public int maxMessagesPerSecond = 2;
    public boolean muteOnSpam = true;
    public int muteDurationSeconds = 30;
    public String spamMessage = "";

    public boolean blockedWordsEnabled = false;
    public List<String> blockedWords = new ArrayList<>();
    public String blockedReplaceWith = "***";
    public boolean blockedNotifyStaff = true;
    public String blockedNotifyMessage =
            "&#FF4444⚠ &#FFFFFF%player% &#FF4444попытался написать запрещённое слово: &#FFFF55%words%";

    public boolean muteEnableCommand = true;
    public boolean muteActionbarNotify = true;
    public String muteActionbarMuted = "🔇 Вы замьючены администратором";
    public String muteActionbarUnmuted = "🔊 Вас размьютили";
    public String muteActionbarBlocked = "🔇 Вы не можете писать в чат (вы замьючены)";

    public String msgChatCleared = "&#FF4444🗑 Чат очищен администратором %admin%";
    public String msgChatDisabled = "&#FF4444🔇 Чат временно отключён администратором %admin%";
    public String msgChatEnabled = "&#00FF00🔊 Чат включён администратором %admin%";
    public String msgCannotMsgSelf = "&#FF4444Нельзя писать в ЛС самому себе";

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        String mode = c.getString("default-mode", "LOCAL");
        defaultMode = "GLOBAL".equalsIgnoreCase(mode) ? DefaultMode.GLOBAL : DefaultMode.LOCAL;
        localRadius = Math.max(1, c.getInt("local-radius", 100));
        globalFormat = c.getString("global-format", globalFormat);
        localFormat = c.getString("local-format", localFormat);
        globalPrefix = c.getString("global-prefix", "!");
        localPrefix = c.getString("local-prefix", "?");
        meFormat = c.getString("me-format", meFormat);
        enableGlobalWorldSeparator = c.getBoolean("enable-global-world-separator", false);
        globalWorldSeparator = c.getString("global-world-separator", " &#AAAAAA[%world%]");
        pmSentFormat = c.getString("pm-sent-format", pmSentFormat);
        pmReceiveFormat = c.getString("pm-receive-format", pmReceiveFormat);

        var as = c.getConfigurationSection("anti-spam");
        if (as != null) {
            antiSpamEnabled = as.getBoolean("enabled", true);
            maxMessagesPerSecond = Math.max(1, as.getInt("max-messages-per-second", 2));
            muteOnSpam = as.getBoolean("mute-on-spam", true);
            muteDurationSeconds = Math.max(1, as.getInt("mute-duration-seconds", 30));
            spamMessage = as.getString("spam-message", spamMessage);
        }

        var bw = c.getConfigurationSection("blocked-words");
        if (bw != null) {
            blockedWordsEnabled = bw.getBoolean("enabled", false);
            blockedWords = bw.getStringList("list");
            blockedReplaceWith = bw.getString("replace-with", "***");
            blockedNotifyStaff = bw.getBoolean("notify-staff", true);
            blockedNotifyMessage = bw.getString("notify-message", blockedNotifyMessage);
        }

        var mute = c.getConfigurationSection("mute");
        if (mute != null) {
            muteEnableCommand = mute.getBoolean("enable-command", true);
            muteActionbarNotify = mute.getBoolean("actionbar-notify", true);
            muteActionbarMuted = mute.getString("muted-actionbar", muteActionbarMuted);
            muteActionbarUnmuted = mute.getString("unmuted-actionbar", muteActionbarUnmuted);
            muteActionbarBlocked = mute.getString("chat-blocked-actionbar", muteActionbarBlocked);
        }

        var messages = c.getConfigurationSection("messages");
        if (messages != null) {
            msgChatCleared = messages.getString("chat-cleared", msgChatCleared);
            msgChatDisabled = messages.getString("chat-disabled", msgChatDisabled);
            msgChatEnabled = messages.getString("chat-enabled", msgChatEnabled);
            msgCannotMsgSelf = messages.getString("cannot-msg-self", msgCannotMsgSelf);
        }
    }
}
