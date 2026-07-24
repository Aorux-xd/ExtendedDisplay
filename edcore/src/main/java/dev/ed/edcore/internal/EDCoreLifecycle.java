package dev.ed.edcore.internal;

import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.internal.license.UpdateManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Общее состояние EDCore без зависимостей от Bukkit или Velocity API (безопасно грузить на любой платформе).
 */
public final class EDCoreLifecycle {

    public static final String VERSION = "2.0.0";

    private static final AtomicBoolean started = new AtomicBoolean(false);
    private static EDCoreImpl instance;
    private static volatile String edCoreChecksum = "";
    private static volatile UpdateManager updateManager;

    private EDCoreLifecycle() {
    }

    public static boolean tryBeginStart() {
        return started.compareAndSet(false, true);
    }

    public static void abortStart() {
        started.set(false);
        updateManager = null;
        edCoreChecksum = "";
    }

    public static void activate(EDCoreImpl impl) {
        instance = impl;
        EDCoreProvider.setInstance(instance);
    }

    public static void shutdown() {
        if (instance != null) {
            instance.markShutdown();
        }
        EDCoreProvider.clearInstance();
        instance = null;
        started.set(false);
        updateManager = null;
        edCoreChecksum = "";
    }

    public static String getEdCoreChecksum() {
        return edCoreChecksum;
    }

    public static void setEdCoreChecksum(String checksum) {
        edCoreChecksum = checksum == null ? "" : checksum;
    }

    public static UpdateManager getUpdateManager() {
        return updateManager;
    }

    public static void setUpdateManager(UpdateManager manager) {
        updateManager = manager;
    }
}
