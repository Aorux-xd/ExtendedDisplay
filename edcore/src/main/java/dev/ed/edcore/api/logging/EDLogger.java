package dev.ed.edcore.api.logging;

/**
 * Логгер без привязки к SLF4J / java.util.logging.
 */
public interface EDLogger {

    void info(String message);

    void warn(String message);

    void severe(String message);
}
