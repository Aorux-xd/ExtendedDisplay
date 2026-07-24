package dev.ed.edauth.command.paper;

import dev.ed.edauth.bootstrap.EDAuthKernel;
import dev.ed.edauth.util.ConfirmableActionStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class AuthAdminPaperCommand implements CommandExecutor {
    private final EDAuthKernel kernel;
    private final ConfirmableActionStore confirmations = new ConfirmableActionStore(15_000L);

    public AuthAdminPaperCommand(EDAuthKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        try {
            switch (args[0].toLowerCase()) {
                case "token" -> {
                    if (!sender.hasPermission("edauth.token")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (!(sender instanceof org.bukkit.entity.Player p)) {
                        sender.sendMessage("§cТолько для игроков.");
                        return true;
                    }
                    if (args.length < 2) return false;
                    if ("create".equalsIgnoreCase(args[1])) {
                        String token = kernel.auth().createLoginToken(p.getName());
                        sender.sendMessage("§aОдноразовый токен: §f" + token);
                    } else if ("revoke".equalsIgnoreCase(args[1])) {
                        kernel.auth().revokeLoginTokens(p.getName());
                        sender.sendMessage("§aТокены отозваны.");
                    } else return false;
                }
                case "reload" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    kernel.reload();
                    sender.sendMessage("§aReloaded.");
                }
                case "changepass" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 3) return false;
                    kernel.auth().changePassword(args[1], args[2]);
                    sender.sendMessage("§aOK");
                }
                case "delete" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 2) return false;
                    String action = "delete:" + args[1].toLowerCase();
                    if (!confirmations.confirmNow(sender.getName(), action)) {
                        sender.sendMessage("§eПодтвердите действие: повторите ту же команду в течение 15 секунд.");
                        return true;
                    }
                    kernel.auth().unregister(args[1]);
                    sender.sendMessage("§aOK");
                }
                case "forcelogin" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 2) return false;
                    kernel.auth().forceLogin(args[1], "0.0.0.0");
                    sender.sendMessage("§aOK");
                }
                case "forceregister" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 3) return false;
                    kernel.auth().forceRegister(args[1], args[2], "0.0.0.0");
                    sender.sendMessage("§aOK");
                }
                case "forcepremium" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 2) return false;
                    String action = "forcepremium:" + args[1].toLowerCase();
                    if (!confirmations.confirmNow(sender.getName(), action)) {
                        sender.sendMessage("§eПодтвердите действие: повторите ту же команду в течение 15 секунд.");
                        return true;
                    }
                    if (!org.bukkit.Bukkit.getOnlineMode() && !kernel.config().premiumAllowInOffline) {
                        sender.sendMessage("§cPremium disabled in offline-mode by security policy.");
                        return true;
                    }
                    kernel.auth().setPremium(args[1], true);
                    sender.sendMessage("§aOK");
                }
                case "resettotp" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 2) return false;
                    kernel.auth().forceReset2fa(args[1]);
                    sender.sendMessage("§a2FA reset requested for " + args[1]);
                }
                case "check" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    String nick = args.length >= 2 ? args[1] : sender.getName();
                    var p = kernel.auth().getProfile(nick);
                    sender.sendMessage("§eCheck " + nick + ": registered=" + (p != null)
                            + ", premium=" + kernel.auth().isPremium(nick)
                            + ", 2fa=" + kernel.auth().is2faEnabled(nick));
                }
                case "audit" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 2) return false;
                    java.nio.file.Path log = kernel.dataDir().resolve("edauth-audit.log");
                    if (!java.nio.file.Files.exists(log)) {
                        sender.sendMessage("§eAudit log empty.");
                        return true;
                    }
                    String target = "\"" + args[1].toLowerCase() + "\"";
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(log);
                    int shown = 0;
                    for (int i = lines.size() - 1; i >= 0 && shown < 10; i--) {
                        String line = lines.get(i);
                        if (line.toLowerCase().contains(target)) {
                            sender.sendMessage("§7" + line);
                            shown++;
                        }
                    }
                    if (shown == 0) sender.sendMessage("§eNo audit events.");
                }
                case "migrate" -> {
                    if (!sender.hasPermission("edauth.admin")) {
                        sender.sendMessage("§cНет прав.");
                        return true;
                    }
                    if (args.length < 3) return false;
                    int n = dev.ed.edcore.api.EDCoreProvider.get().getMigrationManager().migrate(args[1], args[2]);
                    sender.sendMessage("§aMigrated: " + n);
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§cError: " + e.getMessage());
        }
        return true;
    }
}
