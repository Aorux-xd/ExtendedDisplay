package dev.ed.edcore.api.session;

public interface SessionManager {
    void put(String key, long expiresAtEpochSeconds);

    boolean isValid(String key, long nowEpochSeconds);

    void remove(String key);

    void clearExpired(long nowEpochSeconds);
}
