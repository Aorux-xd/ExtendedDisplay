package dev.ed.edauth.bootstrap;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.ed.edauth.util.ConfirmableActionStore;
import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.api.support.EDPluginSupportVelocity;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "edauth", name = "EDAuth", version = "2.0.0", authors = {"ED"},
        dependencies = {@Dependency(id = "edcore")})
public final class EDAuthVelocityBootstrap {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDir;
    private final PluginContainer container;
    private final ConfirmableActionStore confirmations = new ConfirmableActionStore(15_000L);
    private EDAuthKernel kernel;

    @Inject
    public EDAuthVelocityBootstrap(ProxyServer server, Logger logger, @DataDirectory Path dataDir, PluginContainer container) {
        this.server = server;
        this.logger = logger;
        this.dataDir = dataDir;
        this.container = container;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        if (!EDPluginSupportVelocity.requireEDCore(server.getPluginManager(), "edcore", logger, "edauth")) return;
        kernel = new EDAuthKernel(dataDir);
        try {
            kernel.init(getClass().getClassLoader());
            EDCoreProvider.get().setAuthApi(kernel.auth());
        } catch (Exception e) {
            logger.error("EDAuth init failed", e);
            return;
        }
        register("register", this::onRegister, "reg");
        register("login", this::onLogin, "log", "l");
        register("logout", this::onLogout);
        register("premium", this::onPremium);
        register("crack", this::onCrack);
        register("2fa", this::on2fa);
        register("auth", this::onAuth);
    }

    private void register(String name, SimpleCommand command, String... aliases) {
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder(name).plugin(container).aliases(aliases).build(),
                command
        );
    }

    private void onRegister(SimpleCommand.Invocation inv) {
        if (!(inv.source() instanceof Player p)) return;
        String[] a = inv.arguments();
        if (a.length < 2 || !a[0].equals(a[1])) {
            p.sendMessage(net.kyori.adventure.text.Component.text("/register <pass> <pass>"));
            return;
        }
        if (!kernel.config().passwordPolicy.validate(p.getUsername(), a[0])) {
            p.sendMessage(net.kyori.adventure.text.Component.text(
                    kernel.messages().get("password-policy-fail", "Password does not meet policy.")
            ));
            return;
        }
        try {
            boolean ok = kernel.auth().register(p.getUsername(), a[0], p.getRemoteAddress().getAddress().getHostAddress());
            p.sendMessage(net.kyori.adventure.text.Component.text(ok
                    ? kernel.messages().get("register-success", "Registered.")
                    : kernel.messages().get("already-registered", "Already registered.")));
        } catch (Exception e) {
            p.sendMessage(net.kyori.adventure.text.Component.text("Error."));
        }
    }

    private void onLogin(SimpleCommand.Invocation inv) {
        if (!(inv.source() instanceof Player p)) return;
        String[] a = inv.arguments();
        if (a.length < 1) return;
        try {
            String ip = p.getRemoteAddress().getAddress().getHostAddress();
            boolean ok = kernel.auth().login(p.getUsername(), a[0], ip);
            if (ok) {
                p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("login-success", "Logged in.")));
            } else if (kernel.auth().isTotpPending(p.getUsername(), ip)) {
                p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("totp-required", "Use /2fa verify <code> [trust]")));
            } else {
                p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("login-fail-generic", "Invalid username or password.")));
            }
        } catch (Exception e) {
            p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("login-fail-generic", "Invalid username or password.")));
        }
    }

    private void onLogout(SimpleCommand.Invocation inv) {
        if (!(inv.source() instanceof Player p)) return;
        kernel.auth().logout(p.getUsername());
        p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("logout-success", "Logged out.")));
    }

    private void onPremium(SimpleCommand.Invocation inv) {
        if (!(inv.source() instanceof Player p)) return;
        if (!server.getConfiguration().isOnlineMode() && !kernel.config().premiumAllowInOffline) {
            p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("premium-offline-disabled", "Premium mode unavailable on this server.")));
            return;
        }
        kernel.auth().setPremium(p.getUsername(), true);
        p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("premium-enabled", "Premium on")));
    }

    private void onCrack(SimpleCommand.Invocation inv) {
        if (!(inv.source() instanceof Player p)) return;
        kernel.auth().setPremium(p.getUsername(), false);
        p.sendMessage(net.kyori.adventure.text.Component.text(kernel.messages().get("premium-disabled", "Premium off")));
    }

    private void onAuth(SimpleCommand.Invocation inv) {
        CommandSource src = inv.source();
        String[] a = inv.arguments();
        if (a.length == 0) return;
        try {
            switch (a[0].toLowerCase()) {
                case "token" -> {
                    if (!src.hasPermission("edauth.token") || !(src instanceof Player p) || a.length < 2) return;
                    if ("create".equalsIgnoreCase(a[1])) {
                        String token = kernel.auth().createLoginToken(p.getUsername());
                        src.sendMessage(net.kyori.adventure.text.Component.text("token: " + token));
                    } else if ("revoke".equalsIgnoreCase(a[1])) {
                        kernel.auth().revokeLoginTokens(p.getUsername());
                        src.sendMessage(net.kyori.adventure.text.Component.text("tokens revoked"));
                    }
                }
                case "reload" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    kernel.reload();
                    src.sendMessage(net.kyori.adventure.text.Component.text("reloaded"));
                }
                case "changepass" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length >= 3) kernel.auth().changePassword(a[1], a[2]);
                }
                case "forcelogin" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length >= 2) kernel.auth().forceLogin(a[1], "0.0.0.0");
                }
                case "forceregister" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length >= 3) kernel.auth().forceRegister(a[1], a[2], "0.0.0.0");
                }
                case "delete" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length >= 2) {
                        String action = "delete:" + a[1].toLowerCase();
                        if (!confirmations.confirmNow(src.toString(), action)) {
                            src.sendMessage(net.kyori.adventure.text.Component.text("Repeat same command within 15s to confirm."));
                            return;
                        }
                        kernel.auth().unregister(a[1]);
                    }
                }
                case "forcepremium" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length >= 2) {
                        if (!server.getConfiguration().isOnlineMode() && !kernel.config().premiumAllowInOffline) {
                            src.sendMessage(net.kyori.adventure.text.Component.text("premium disabled in offline-mode"));
                            return;
                        }
                        String action = "forcepremium:" + a[1].toLowerCase();
                        if (!confirmations.confirmNow(src.toString(), action)) {
                            src.sendMessage(net.kyori.adventure.text.Component.text("Repeat same command within 15s to confirm."));
                            return;
                        }
                        kernel.auth().setPremium(a[1], true);
                    }
                }
                case "resettotp" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length >= 2) {
                        kernel.auth().forceReset2fa(a[1]);
                        src.sendMessage(net.kyori.adventure.text.Component.text("totp reset: " + a[1]));
                    }
                }
                case "check" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    String nick = a.length >= 2 ? a[1] : (src instanceof Player p ? p.getUsername() : "");
                    var pr = kernel.auth().getProfile(nick);
                    src.sendMessage(net.kyori.adventure.text.Component.text("check " + nick
                            + " reg=" + (pr != null)
                            + " premium=" + kernel.auth().isPremium(nick)
                            + " 2fa=" + kernel.auth().is2faEnabled(nick)));
                }
                case "audit" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length < 2) return;
                    java.nio.file.Path log = kernel.dataDir().resolve("edauth-audit.log");
                    if (!java.nio.file.Files.exists(log)) {
                        src.sendMessage(net.kyori.adventure.text.Component.text("audit empty"));
                        return;
                    }
                    String target = "\"" + a[1].toLowerCase() + "\"";
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(log);
                    int shown = 0;
                    for (int i = lines.size() - 1; i >= 0 && shown < 10; i--) {
                        if (lines.get(i).toLowerCase().contains(target)) {
                            src.sendMessage(net.kyori.adventure.text.Component.text(lines.get(i)));
                            shown++;
                        }
                    }
                }
                case "migrate" -> {
                    if (!src.hasPermission("edauth.admin")) return;
                    if (a.length >= 3) {
                        int n = dev.ed.edcore.api.EDCoreProvider.get().getMigrationManager().migrate(a[1], a[2]);
                        src.sendMessage(net.kyori.adventure.text.Component.text("migrated " + n));
                    }
                }
            }
        } catch (Exception e) {
            src.sendMessage(net.kyori.adventure.text.Component.text("error"));
        }
    }

    private void on2fa(SimpleCommand.Invocation inv) {
        if (!(inv.source() instanceof Player p)) return;
        if (!p.hasPermission("edauth.2fa")) return;
        String[] a = inv.arguments();
        if (a.length == 0) {
            p.sendMessage(net.kyori.adventure.text.Component.text("/2fa enable|disable <code>|recovery <code>"));
            return;
        }
        try {
            switch (a[0].toLowerCase()) {
                case "enable" -> {
                    String secret = kernel.auth().enable2fa(p.getUsername());
                    var backup = kernel.auth().regenerateRecoveryCodes(p.getUsername());
                    p.sendMessage(net.kyori.adventure.text.Component.text("2fa secret: " + secret));
                    p.sendMessage(net.kyori.adventure.text.Component.text("recovery: " + String.join(", ", backup)));
                }
                case "disable" -> {
                    if (a.length < 2) return;
                    boolean ok = kernel.auth().disable2fa(p.getUsername(), Integer.parseInt(a[1]));
                    p.sendMessage(net.kyori.adventure.text.Component.text(ok ? "2fa disabled" : "invalid code"));
                }
                case "recovery" -> {
                    if (a.length < 2) return;
                    boolean ok = kernel.auth().recover2fa(p.getUsername(), a[1]);
                    p.sendMessage(net.kyori.adventure.text.Component.text(ok ? "2fa reset" : "invalid recovery code"));
                }
                case "verify" -> {
                    if (a.length < 2) return;
                    boolean trust = a.length >= 3 && "trust".equalsIgnoreCase(a[2]);
                    String ip = p.getRemoteAddress().getAddress().getHostAddress();
                    boolean ok = kernel.auth().verifyTotpLogin(p.getUsername(), ip, Integer.parseInt(a[1]), trust);
                    p.sendMessage(net.kyori.adventure.text.Component.text(ok ? "2fa login success" : "invalid 2fa code"));
                }
            }
        } catch (Exception e) {
            p.sendMessage(net.kyori.adventure.text.Component.text("2fa error"));
        }
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player p = event.getPlayer();
        if (!kernel.auth().isAuthenticated(p.getUsername())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }

    @Subscribe
    public void onCmd(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player p)) return;
        if (kernel.auth().isAuthenticated(p.getUsername())) return;
        String cmd = "/" + event.getCommand().split(" ")[0].toLowerCase();
        boolean allowed = kernel.config().allowedCommands.stream().anyMatch(cmd::startsWith);
        if (!allowed) event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (EDCoreProvider.isReady()) {
            EDCoreProvider.get().clearAuthApi();
        }
    }
}
