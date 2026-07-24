package dev.ed.edcore.api.config;

/**
 * Доступ к config.yml EDCore (общие настройки ядра).
 */
public interface EDConfig {

    String getString(String path, String defaultValue);

    void set(String path, Object value);

    void reload();

    void save();
}
