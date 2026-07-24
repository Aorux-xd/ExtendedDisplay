package dev.ed.edcore.security;

import java.util.concurrent.atomic.AtomicInteger;

public final class BcryptThrottle {
    private final int maxPerSecond;
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile long second = System.currentTimeMillis() / 1000L;

    public BcryptThrottle(int maxPerSecond) {
        this.maxPerSecond = maxPerSecond;
    }

    public boolean allow() {
        if (maxPerSecond <= 0) {
            return true;
        }
        long now = System.currentTimeMillis() / 1000L;
        if (now != second) {
            synchronized (this) {
                if (now != second) {
                    second = now;
                    counter.set(0);
                }
            }
        }
        return counter.incrementAndGet() <= maxPerSecond;
    }
}
