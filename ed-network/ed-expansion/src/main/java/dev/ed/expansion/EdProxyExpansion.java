package dev.ed.expansion;

import dev.ed.network.proxy.ServerSnapshot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EdProxyExpansion extends PlaceholderExpansion {

    private static final Pattern PARAM = Pattern.compile("^(.*)_(online|max|motd|ping|version|status)$",
            Pattern.CASE_INSENSITIVE);

    private final EdExpansionPlugin plugin;

    EdProxyExpansion(EdExpansionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "edproxy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ED";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(org.bukkit.entity.Player player, @NotNull String params) {
        Matcher m = PARAM.matcher(params);
        if (!m.matches()) {
            return "";
        }
        String serverRaw = m.group(1);
        String field = m.group(2).toLowerCase(Locale.ROOT);
        String serverKey = serverRaw.toLowerCase(Locale.ROOT);

        FileConfiguration cfg = plugin.getConfig();
        long ttlMs = Math.max(1, cfg.getLong("cache-ttl-seconds", 5)) * 1000L;
        long now = System.currentTimeMillis();
        long last = plugin.cache().lastUpdateMs();
        boolean fresh = last > 0 && now - last <= ttlMs;

        Optional<ServerSnapshot> snap = fresh ? plugin.cache().get(serverKey) : Optional.empty();
        if (snap.isEmpty()) {
            snap = plugin.fallbackSlp(serverKey, serverRaw);
        }
        if (snap.isEmpty()) {
            return field.equals("status") ? "unknown" : "";
        }
        ServerSnapshot s = snap.get();
        return switch (field) {
            case "online" -> Integer.toString(s.online());
            case "max" -> Integer.toString(s.max());
            case "motd" -> s.motd();
            case "ping" -> Long.toString(s.pingMs());
            case "version" -> s.version();
            case "status" -> s.statusToken();
            default -> "";
        };
    }
}
