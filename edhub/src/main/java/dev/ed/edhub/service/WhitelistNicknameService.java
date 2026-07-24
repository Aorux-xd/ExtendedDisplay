package dev.ed.edhub.service;

import dev.ed.edhub.config.WhitelistEntry;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WhitelistNicknameService {
    private final Map<String, WhitelistEntry> byNickLower;

    public WhitelistNicknameService(List<WhitelistEntry> entries) {
        Map<String, WhitelistEntry> map = new HashMap<>();
        if (entries != null) {
            for (WhitelistEntry e : entries) {
                if (e == null || e.nick() == null) continue;
                String k = e.nick().trim().toLowerCase(Locale.ROOT);
                if (!k.isEmpty()) map.put(k, e);
            }
        }
        this.byNickLower = map;
    }

    public boolean isWhitelisted(Player player) {
        return entry(player) != null;
    }

    public boolean bypassesSpawnProtection(Player player) {
        WhitelistEntry e = entry(player);
        return e != null && Boolean.TRUE.equals(e.bypassSpawnProtection());
    }

    public Boolean allowBlockBreak(Player player) {
        WhitelistEntry e = entry(player);
        return e != null ? e.allowBlockBreak() : null;
    }

    public Boolean allowBlockPlace(Player player) {
        WhitelistEntry e = entry(player);
        return e != null ? e.allowBlockPlace() : null;
    }

    public Boolean allowPvp(Player player) {
        WhitelistEntry e = entry(player);
        return e != null ? e.allowPvp() : null;
    }

    public Boolean allowGamemodeChange(Player player) {
        WhitelistEntry e = entry(player);
        return e != null ? e.allowGamemodeChange() : null;
    }

    public boolean canChangeGamemode(Player player, GameMode target) {
        WhitelistEntry e = entry(player);
        if (e == null) return false;
        if (!Boolean.TRUE.equals(e.allowGamemodeChange())) return false;
        List<GameMode> allowed = e.allowedGamemodes();
        return allowed == null || allowed.isEmpty() || allowed.contains(target);
    }

    public boolean canFly(Player player) {
        WhitelistEntry e = entry(player);
        return e != null && Boolean.TRUE.equals(e.allowFly());
    }

    private WhitelistEntry entry(Player player) {
        if (player == null) return null;
        return byNickLower.get(player.getName().toLowerCase(Locale.ROOT));
    }
}
