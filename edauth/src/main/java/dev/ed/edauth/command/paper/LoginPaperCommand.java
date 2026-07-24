package dev.ed.edauth.command.paper;

import dev.ed.edauth.bootstrap.EDAuthKernel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LoginPaperCommand implements CommandExecutor {
    private final EDAuthKernel kernel;

    public LoginPaperCommand(EDAuthKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length < 1) {
            p.sendMessage("§e/login <пароль>");
            return true;
        }
        try {
            String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "0.0.0.0";
            boolean ok = kernel.auth().login(p.getName(), args[0], ip);
            if (ok) {
                p.sendMessage(kernel.messages().get("login-success", "&aВы вошли!"));
            } else if (kernel.auth().isTotpPending(p.getName(), ip)) {
                p.sendMessage(kernel.messages().get("totp-required", "&eВведите /2fa verify <код> [trust]"));
            } else {
                p.sendMessage(kernel.messages().get("login-fail-generic", "&cНеверный ник или пароль."));
            }
        } catch (Exception e) {
            p.sendMessage(kernel.messages().get("login-fail-generic", "&cНеверный ник или пароль."));
        }
        return true;
    }
}
