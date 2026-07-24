package dev.ed.edchat.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlayerLookup {
    private PlayerLookup() {}

    public static Player findOnline(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null && exact.isOnline()) {
            return exact;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }
}
