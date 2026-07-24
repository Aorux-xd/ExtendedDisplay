package dev.ed.edcore.internal.logging;

import dev.ed.edcore.api.logging.EDLogger;

public final class Slf4jLoggerAdapter implements EDLogger {

    private final org.slf4j.Logger delegate;

    public Slf4jLoggerAdapter(org.slf4j.Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void info(String message) {
        delegate.info(message);
    }

    @Override
    public void warn(String message) {
        delegate.warn(message);
    }

    @Override
    public void severe(String message) {
        delegate.error(message);
    }
}
