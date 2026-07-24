package dev.ed.edauth.command.paper;

import dev.ed.edauth.bootstrap.EDAuthKernel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RegisterPaperCommand implements CommandExecutor {
    private final EDAuthKernel kernel;

    public RegisterPaperCommand(EDAuthKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length < 2 || !args[0].equals(args[1])) {
            p.sendMessage("§e/register <пароль> <пароль>");
            return true;
        }
        if (!kernel.config().passwordPolicy.validate(p.getName(), args[0])) {
            p.sendMessage(kernel.messages().get("password-policy-fail",
                    "&cПароль не соответствует требованиям безопасности."));
            return true;
        }
        try {
            String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "0.0.0.0";
            boolean ok = kernel.auth().register(p.getName(), args[0], ip);
            if (ok) {
                kernel.auth().forceLogin(p.getName(), ip);
                p.sendMessage(kernel.messages().get("register-success", "&aУспешная регистрация!"));
            } else {
                p.sendMessage(kernel.messages().get("already-registered", "&cАккаунт уже существует."));
            }
        } catch (Exception e) {
            p.sendMessage("§cОшибка регистрации.");
        }
        return true;
    }
}
