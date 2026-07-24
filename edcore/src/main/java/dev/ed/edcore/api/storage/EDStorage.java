package dev.ed.edcore.api.storage;

import java.util.Optional;

/**
 * Простое key-value хранилище в каталоге данных плагина (файл {@code storage.properties}).
 */
public interface EDStorage {

    Optional<String> get(String key);

    void put(String key, String value);

    void remove(String key);

    void save();
}
