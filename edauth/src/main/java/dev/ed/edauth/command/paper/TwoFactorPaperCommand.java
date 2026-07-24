package dev.ed.edauth.command.paper;

import dev.ed.edauth.bootstrap.EDAuthKernel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class TwoFactorPaperCommand implements CommandExecutor {
    private final EDAuthKernel kernel;

    public TwoFactorPaperCommand(EDAuthKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!sender.hasPermission("edauth.2fa")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/2fa enable | /2fa disable <code> | /2fa recovery <code> | /2fa verify <code> [trust]");
            return true;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "enable" -> {
                    String secret = kernel.auth().enable2fa(p.getName());
                    List<String> backup = kernel.auth().regenerateRecoveryCodes(p.getName());
                    sender.sendMessage("§a2FA включена. Secret: §f" + secret);
                    sender.sendMessage("§eRecovery codes (show once): §f" + String.join(", ", backup));
                }
                case "disable" -> {
                    if (args.length < 2) return false;
                    int code = Integer.parseInt(args[1]);
                    boolean ok = kernel.auth().disable2fa(p.getName(), code);
                    sender.sendMessage(ok ? "§a2FA отключена." : "§cНеверный код.");
                }
                case "recovery" -> {
                    if (args.length < 2) return false;
                    boolean ok = kernel.auth().recover2fa(p.getName(), args[1]);
                    sender.sendMessage(ok ? "§a2FA сброшена recovery-кодом." : "§cНеверный recovery-код.");
                }
                case "verify" -> {
                    if (args.length < 2) return false;
                    boolean trust = args.length >= 3 && "trust".equalsIgnoreCase(args[2]);
                    String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "0.0.0.0";
                    boolean ok = kernel.auth().verifyTotpLogin(p.getName(), ip, Integer.parseInt(args[1]), trust);
                    sender.sendMessage(ok ? "§a2FA подтверждена, вход выполнен." : "§cНеверный код 2FA.");
                }
                default -> sender.sendMessage("§e/2fa enable | /2fa disable <code> | /2fa recovery <code> | /2fa verify <code> [trust]");
            }
        } catch (Exception e) {
            sender.sendMessage("§cОшибка: " + e.getMessage());
        }
        return true;
    }
}
