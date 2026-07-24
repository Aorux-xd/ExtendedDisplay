package dev.ed.edcore.internal.config;

import dev.ed.edcore.api.config.EDConfig;
import dev.ed.edcore.api.logging.EDLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * config.yml на Velocity через зашитый SnakeYAML.
 */
public final class YamlConfigVelocity implements EDConfig {

    private final Path configFile;
    private final EDLogger log;
    private final Yaml yaml = new Yaml();
    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, Object> root = new LinkedHashMap<>();

    public YamlConfigVelocity(Path dataDirectory, EDLogger log) {
        this.configFile = dataDirectory.resolve("config.yml");
        this.log = log;
    }

    public void installDefaultFromResource(InputStream resource) throws IOException {
        Files.createDirectories(configFile.getParent());
        if (!Files.isRegularFile(configFile) && resource != null) {
            Files.copy(resource, configFile);
        }
        reload();
    }

    @Override
    public String getString(String path, String defaultValue) {
        lock.lock();
        try {
            Object v = navigate(path, false);
            return v instanceof String ? (String) v : defaultValue;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        lock.lock();
        try {
            String[] parts = path.split("\\.");
            Map<String, Object> cur = root;
            for (int i = 0; i < parts.length - 1; i++) {
                cur = (Map<String, Object>) cur.computeIfAbsent(parts[i], k -> new LinkedHashMap<String, Object>());
            }
            cur.put(parts[parts.length - 1], value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reload() {
        lock.lock();
        try {
            if (!Files.isRegularFile(configFile)) {
                root = new LinkedHashMap<>();
                return;
            }
            try (var in = Files.newInputStream(configFile)) {
                Object loaded = yaml.load(in);
                if (loaded instanceof Map) {
                    root = (Map<String, Object>) loaded;
                } else {
                    root = new LinkedHashMap<>();
                }
            }
        } catch (IOException e) {
            log.warn("config reload: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save() {
        lock.lock();
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer w = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                yaml.dump(root, w);
            }
        } catch (IOException e) {
            log.severe("config save: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private Object navigate(String path, boolean create) {
        String[] parts = path.split("\\.");
        Map<String, Object> cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = cur.get(parts[i]);
            if (!(next instanceof Map)) {
                if (!create) {
                    return null;
                }
                next = new LinkedHashMap<String, Object>();
                cur.put(parts[i], next);
            }
            cur = (Map<String, Object>) next;
        }
        return cur.get(parts[parts.length - 1]);
    }
}
