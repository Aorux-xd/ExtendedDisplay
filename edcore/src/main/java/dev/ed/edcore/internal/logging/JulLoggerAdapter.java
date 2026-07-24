package dev.ed.edcore.internal.logging;

import dev.ed.edcore.api.logging.EDLogger;

import java.util.logging.Level;

public final class JulLoggerAdapter implements EDLogger {

    private final java.util.logging.Logger delegate;

    public JulLoggerAdapter(java.util.logging.Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void info(String message) {
        delegate.info(message);
    }

    @Override
    public void warn(String message) {
        delegate.warning(message);
    }

    @Override
    public void severe(String message) {
        delegate.severe(message);
    }
}
