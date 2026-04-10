package com.extendeddisplay.purpur;

import com.extendeddisplay.protocol.ServerStatusDto;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class EDPlaceholderExpansion extends PlaceholderExpansion {
    private final EDExpansionPlugin plugin;
    private final StatusCacheService cacheService;

    public EDPlaceholderExpansion(EDExpansionPlugin plugin, StatusCacheService cacheService) {
        this.plugin = plugin;
        this.cacheService = cacheService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ed";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ExtendedDisplay";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.split("_", 2);
        if (parts.length < 2) {
            return "";
        }
        String metric = parts[0].toLowerCase();
        String server = parts[1];
        cacheService.trackServer(server);

        ServerStatusDto status = cacheService.get(server);
        return switch (metric) {
            case "status" -> status.online() ? "Online" : "Offline";
            case "players" -> Integer.toString(status.players());
            case "maxplayers" -> Integer.toString(status.max());
            default -> "";
        };
    }
}
