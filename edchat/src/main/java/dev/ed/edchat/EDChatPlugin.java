package dev.ed.edchat;

import dev.ed.edchat.commands.ChatCommand;
import dev.ed.edchat.commands.MeCommand;
import dev.ed.edchat.commands.MsgCommand;
import dev.ed.edchat.commands.ReloadCommand;
import dev.ed.edchat.commands.ReplyCommand;
import dev.ed.edcore.api.support.EDPluginSupportPaper;
import org.bukkit.plugin.java.JavaPlugin;

public final class EDChatPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        if (!EDPluginSupportPaper.requireEDCore(this)) {
            return;
        }
        configManager = new ConfigManager(this);
        configManager.load();
        chatManager = new ChatManager();

        AntiSpamFilter antiSpam = new AntiSpamFilter(configManager, chatManager);
        getServer().getPluginManager().registerEvents(
                new ChatListener(this, configManager, chatManager, antiSpam), this);

        MsgCommand msgCommand = new MsgCommand(configManager, chatManager);

        var edchat = getCommand("edchat");
        if (edchat != null) {
            ReloadCommand reload = new ReloadCommand(configManager);
            edchat.setExecutor(reload);
            edchat.setTabCompleter(reload);
        }
        var chat = getCommand("chat");
        if (chat != null) {
            ChatCommand chatCmd = new ChatCommand(configManager, chatManager);
            chat.setExecutor(chatCmd);
            chat.setTabCompleter(chatCmd);
        }
        var msg = getCommand("msg");
        if (msg != null) {
            msg.setExecutor(msgCommand);
            msg.setTabCompleter(msgCommand);
        }
        var reply = getCommand("reply");
        if (reply != null) {
            reply.setExecutor(new ReplyCommand(configManager, chatManager, msgCommand));
        }
        var me = getCommand("me");
        if (me != null) {
            me.setExecutor(new MeCommand(configManager));
        }

        getLogger().info("EDChat v2.0.0 включён");
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public ChatManager chatManager() {
        return chatManager;
    }
}
