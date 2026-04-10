package com.extendeddisplay.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

public record EDProxyConfig(int cacheTtlSeconds, boolean debug, Set<String> blacklistedServers) {
    public static EDProxyConfig load(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        Path configPath = dataDir.resolve("config.yml");
        if (Files.notExists(configPath)) {
            try (InputStream input = EDProxyConfig.class.getClassLoader().getResourceAsStream("config.yml")) {
                if (input != null) {
                    Files.copy(input, configPath);
                }
            }
        }

        if (Files.notExists(configPath)) {
            return new EDProxyConfig(1, false, Collections.emptySet());
        }

        Yaml yaml = new Yaml();
        try (InputStream input = Files.newInputStream(configPath)) {
            Map<String, Object> root = yaml.load(input);
            if (root == null) {
                return new EDProxyConfig(1, false, Collections.emptySet());
            }
            int ttl = asInt(root.get("cache-ttl"), 1);
            boolean debug = asBoolean(root.get("debug"), false);
            Set<String> blacklist = new HashSet<>();
            Object rawList = root.get("blacklisted-servers");
            if (rawList instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        blacklist.add(item.toString().toLowerCase());
                    }
                }
            }
            return new EDProxyConfig(Math.max(1, ttl), debug, blacklist);
        }
    }

    public void save(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        Path configPath = dataDir.resolve("config.yml");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cache-ttl", cacheTtlSeconds);
        root.put("debug", debug);
        root.put("blacklisted-servers", new ArrayList<>(blacklistedServers));
        String dumped = new Yaml().dump(root);
        Files.writeString(configPath, dumped);
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return fallback;
    }
}
