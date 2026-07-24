package dev.ed.edauth.bootstrap;

import dev.ed.edauth.command.paper.AuthAdminPaperCommand;
import dev.ed.edauth.command.paper.LoginPaperCommand;
import dev.ed.edauth.command.paper.RegisterPaperCommand;
import dev.ed.edauth.command.paper.SimplePaperCommand;
import dev.ed.edauth.command.paper.TwoFactorPaperCommand;
import dev.ed.edauth.listener.paper.PreAuthPaperListener;
import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.api.support.EDPluginSupportPaper;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class EDAuthPaperBootstrap extends JavaPlugin {
    private EDAuthKernel kernel;

    @Override
    public void onEnable() {
        if (!EDPluginSupportPaper.requireEDCore(this)) {
            return;
        }
        this.kernel = new EDAuthKernel(getDataFolder().toPath());
        try {
            kernel.init(getClassLoader());
            EDCoreProvider.get().setAuthApi(kernel.auth());
        } catch (Exception e) {
            getLogger().severe("EDAuth init failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(new PreAuthPaperListener(kernel), this);
        bind("register", new RegisterPaperCommand(kernel));
        bind("reg", new RegisterPaperCommand(kernel));
        bind("login", new LoginPaperCommand(kernel));
        bind("log", new LoginPaperCommand(kernel));
        bind("l", new LoginPaperCommand(kernel));
        bind("logout", new SimplePaperCommand((s, a) -> {
            kernel.auth().logout(s.getName());
            s.sendMessage(kernel.messages().get("logout-success", "&eВы вышли."));
            return true;
        }));
        bind("changepassword", new SimplePaperCommand((s, a) -> {
            if (a.length < 2) return false;
            if (!kernel.auth().verifyPassword(s.getName(), a[0])) {
                s.sendMessage(kernel.messages().get("password-wrong", "&cСтарый пароль неверен."));
                return true;
            }
            kernel.auth().changePassword(s.getName(), a[1]);
            s.sendMessage(kernel.messages().get("password-changed", "&aПароль изменен."));
            return true;
        }));
        bind("unregister", new SimplePaperCommand((s, a) -> {
            if (a.length < 1) return false;
            if (!kernel.auth().verifyPassword(s.getName(), a[0])) {
                s.sendMessage(kernel.messages().get("password-wrong", "&cПароль неверен."));
                return true;
            }
            kernel.auth().unregister(s.getName());
            s.sendMessage(kernel.messages().get("unregister-success", "&aАккаунт удален."));
            return true;
        }));
        bind("premium", new SimplePaperCommand((s, a) -> {
            if (!getServer().getOnlineMode() && !kernel.config().premiumAllowInOffline) {
                s.sendMessage(kernel.messages().get("premium-offline-disabled", "&cПремиум-режим недоступен на данном сервере."));
                return true;
            }
            kernel.auth().setPremium(s.getName(), true);
            s.sendMessage(kernel.messages().get("premium-enabled", "&6Премиум-режим активирован."));
            return true;
        }));
        bind("crack", new SimplePaperCommand((s, a) -> {
            kernel.auth().setPremium(s.getName(), false);
            s.sendMessage(kernel.messages().get("premium-disabled", "&6Премиум-режим отключён."));
            return true;
        }));
        bind("2fa", new TwoFactorPaperCommand(kernel));
        bind("auth", new AuthAdminPaperCommand(kernel));
    }

    @Override
    public void onDisable() {
        if (EDCoreProvider.isReady()) {
            EDCoreProvider.get().clearAuthApi();
        }
    }

    private void bind(String name, org.bukkit.command.CommandExecutor ex) {
        PluginCommand c = getCommand(name);
        if (c != null) c.setExecutor(ex);
    }
}
