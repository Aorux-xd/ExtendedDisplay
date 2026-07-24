package dev.ed.edcore.internal.storage;

import dev.ed.edcore.api.logging.EDLogger;
import dev.ed.edcore.api.storage.EDStorage;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

public final class FileStorage implements EDStorage {

    private final Path file;
    private final EDLogger log;
    private final Properties props = new Properties();
    private final ReentrantLock lock = new ReentrantLock();

    public FileStorage(Path dataDirectory, EDLogger log) {
        this.file = dataDirectory.resolve("storage.properties");
        this.log = log;
        load();
    }

    private void load() {
        lock.lock();
        try {
            props.clear();
            if (Files.isRegularFile(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    props.load(r);
                } catch (IOException e) {
                    log.warn("Не удалось прочитать storage: " + e.getMessage());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<String> get(String key) {
        lock.lock();
        try {
            return Optional.ofNullable(props.getProperty(key));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(String key, String value) {
        lock.lock();
        try {
            props.setProperty(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String key) {
        lock.lock();
        try {
            props.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save() {
        lock.lock();
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(w, "EDCore storage");
            }
        } catch (IOException e) {
            log.severe("Не удалось сохранить storage: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
