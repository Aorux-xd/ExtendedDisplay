package dev.ed.edauth.listener.paper;

import dev.ed.edauth.bootstrap.EDAuthKernel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public final class PreAuthPaperListener implements Listener {
    private final EDAuthKernel kernel;

    public PreAuthPaperListener(EDAuthKernel kernel) {
        this.kernel = kernel;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String ip = p.getAddress() != null && p.getAddress().getAddress() != null
                ? p.getAddress().getAddress().getHostAddress()
                : "0.0.0.0";
        if (kernel.auth().canBySession(p.getName(), ip)) {
            kernel.auth().forceLogin(p.getName(), ip);
            p.sendMessage(kernel.messages().get("session-restored", "&aСессия восстановлена."));
            return;
        }
        if (kernel.auth().isPremium(p.getName()) && kernel.config().premiumEnabled) {
            kernel.auth().forceLogin(p.getName(), "0.0.0.0");
            p.sendMessage(kernel.messages().get("premium-bypass", "&6Премиум вход."));
            return;
        }
        p.sendMessage(kernel.messages().get("login-required", "&cПожалуйста, войдите: /login <пароль>"));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (kernel.auth().isAuthenticated(p.getName())) return;
        String cmd = event.getMessage().split(" ")[0].toLowerCase();
        boolean allowed = kernel.config().allowedCommands.stream().anyMatch(cmd::startsWith);
        if (!allowed) {
            event.setCancelled(true);
            p.sendMessage(kernel.messages().get("login-required", "&cПожалуйста, войдите: /login <пароль>"));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!kernel.auth().isAuthenticated(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (!kernel.auth().isAuthenticated(event.getPlayer().getName())) {
            if (event.getFrom().distanceSquared(event.getTo()) > 0.01d) {
                event.setTo(event.getFrom());
            }
        }
    }
}
