package com.extendeddisplay.purpur;

import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public record EDExpansionConfig(
        int updateIntervalSeconds,
        int proxyTimeoutMillis,
        int slpTimeoutMillis,
        Map<String, String> serverAddresses
) {
    public static EDExpansionConfig from(FileConfiguration config) {
        int updateInterval = Math.max(1, config.getInt("update-interval", 2));
        int proxyTimeout = Math.max(100, config.getInt("proxy-timeout", 1000));
        int slpTimeout = Math.max(100, config.getInt("slp-timeout", 1000));
        Map<String, String> addresses = config.getConfigurationSection("server-addresses") == null
                ? Map.of()
                : config.getConfigurationSection("server-addresses").getValues(false).entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

        return new EDExpansionConfig(updateInterval, proxyTimeout, slpTimeout, addresses);
    }
}
