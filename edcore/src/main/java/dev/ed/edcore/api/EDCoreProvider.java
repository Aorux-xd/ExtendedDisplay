package dev.ed.edcore.api;

/**
 * Доступ к загруженному экземпляру EDCore.
 */
public final class EDCoreProvider {

    private static volatile EDCoreAPI instance;

    private EDCoreProvider() {
    }

    /**
     * @throws IllegalStateException если EDCore не включён или уже выгружен
     */
    public static EDCoreAPI get() {
        EDCoreAPI api = instance;
        if (api == null || api.isShutdown()) {
            throw new IllegalStateException("EDCore недоступен (не загружен или завершает работу).");
        }
        return api;
    }

    public static boolean isReady() {
        EDCoreAPI api = instance;
        return api != null && !api.isShutdown();
    }

    public static boolean isLicenseValid() {
        EDCoreAPI api = instance;
        return api != null && !api.isShutdown() && api.isLicenseValid();
    }

    public static String getEdCoreChecksum() {
        return dev.ed.edcore.internal.EDCoreLifecycle.getEdCoreChecksum();
    }

    public static void setInstance(EDCoreAPI api) {
        instance = api;
    }

    public static void clearInstance() {
        instance = null;
    }
}
