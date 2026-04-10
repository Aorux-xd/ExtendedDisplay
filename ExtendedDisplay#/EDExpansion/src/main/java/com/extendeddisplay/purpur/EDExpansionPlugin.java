package com.extendeddisplay.purpur;

import com.extendeddisplay.protocol.EdProtocolConstants;
import com.extendeddisplay.protocol.ServerStatusDto;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EDExpansionPlugin extends JavaPlugin {
    private StatusCacheService cacheService;
    private PluginMessageTransport transport;
    private DirectSlpPinger slpPinger;
    private EDExpansionConfig config;
    private volatile boolean debugEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = EDExpansionConfig.from(getConfig());
        this.debugEnabled = getConfig().getBoolean("debug", false);
        this.cacheService = new StatusCacheService(this);
        this.cacheService.loadKnownAddresses(config.serverAddresses());
        this.transport = new PluginMessageTransport(this);
        this.slpPinger = new DirectSlpPinger();

        Bukkit.getMessenger().registerIncomingPluginChannel(this, EdProtocolConstants.CHANNEL_ID, transport);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, EdProtocolConstants.CHANNEL_ID);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new EDPlaceholderExpansion(this, cacheService).register();
            getLogger().info("Registered PlaceholderAPI expansion: %ed_<metric>_<server>%");
        } else {
            getLogger().warning("PlaceholderAPI not found; expansion not registered.");
        }

        long ticks = config.updateIntervalSeconds() * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::refreshCache, ticks, ticks);
    }

    @Override
    public void onDisable() {
        getConfig().set("server-addresses", cacheService.getKnownAddresses());
        saveConfig();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, EdProtocolConstants.CHANNEL_ID, transport);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, EdProtocolConstants.CHANNEL_ID);
    }

    private void refreshCache() {
        logDebug("Starting refreshCache cycle");
        Set<String> targets = cacheService.getKnownServers();
        if (!transport.hasCarrierPlayer()) {
            logDebug("No players online -> using direct SLP mode, targets=" + targets);
            for (String server : targets) {
                String address = cacheService.getKnownAddress(server);
                if (address == null || address.isBlank()) {
                    address = config.serverAddresses().get(server);
                }
                if (address == null || address.isBlank()) {
                    logDebugWarn("No known address for " + server + ". Waiting for list_all sync from Velocity.");
                    cacheService.put(server, ServerStatusDto.fallbackOffline());
                    continue;
                }
                ServerStatusDto status = slpPinger.ping(address, config.slpTimeoutMillis());
                cacheService.put(server, status);
            }
            return;
        }

        transport.requestAll()
                .orTimeout(config.proxyTimeoutMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable == null && result != null && !result.isEmpty()) {
                        logDebug("Received list_all response, entries=" + result.size());
                        cacheService.putAll(result);
                        return;
                    }
                    if (throwable != null) {
                        logDebugWarn("list_all failed: " + throwable.getClass().getSimpleName()
                                + " - " + throwable.getMessage());
                    } else {
                        logDebugWarn("list_all returned empty/null, fallback to per-server requests");
                    }
                    logDebug("Fallback targets size=" + targets.size() + " -> " + targets);
                    for (String server : targets) {
                        logDebug("Sending fallback get request for " + server);
                        transport.requestOne(server)
                                .orTimeout(config.proxyTimeoutMillis(), TimeUnit.MILLISECONDS)
                                .whenComplete((status, error) -> cacheService.put(server, error == null && status != null
                                        ? status
                                        : ServerStatusDto.fallbackOffline()));
                    }
                });
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void logDebug(String message) {
        if (debugEnabled) {
            getLogger().info("[DEBUG][EDExpansion] " + message);
        }
    }

    public void logDebugWarn(String message) {
        if (debugEnabled) {
            getLogger().warning("[DEBUG][EDExpansion] " + message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("eddebug")) {
            return false;
        }
        if (args.length != 1 || (!"true".equalsIgnoreCase(args[0]) && !"false".equalsIgnoreCase(args[0]))) {
            sender.sendMessage(ChatColor.RED + "Usage: /eddebug <true|false>");
            return true;
        }
        this.debugEnabled = Boolean.parseBoolean(args[0]);
        getConfig().set("debug", this.debugEnabled);
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "EDExpansion debug mode: " + this.debugEnabled);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("eddebug")) {
            return java.util.Collections.emptyList();
        }
        if (args.length == 1) {
            return Arrays.asList("true", "false");
        }
        return java.util.Collections.emptyList();
    }
}
