package dev.ed.edauth.command.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SimplePaperCommand implements CommandExecutor {
    @FunctionalInterface
    public interface Exec {
        boolean run(Player sender, String[] args) throws Exception;
    }

    private final Exec exec;

    public SimplePaperCommand(Exec exec) {
        this.exec = exec;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        try {
            return exec.run(p, args);
        } catch (Exception e) {
            sender.sendMessage("§cError.");
            return true;
        }
    }
}
