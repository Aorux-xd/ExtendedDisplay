package dev.ed.edcore.api.gate;

import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.internal.EDCoreLifecycle;

/**
 * Обязательная проверка EDCore и лицензии для зависимых ED-плагинов.
 *
 * @param expectedEdCoreChecksum SHA-256 JAR EDCore (пусто — не проверять контрольную сумму)
 */
public final class EdCoreGate {

    private EdCoreGate() {
    }

    public static boolean verify(String expectedEdCoreChecksum) {
        if (!EDCoreProvider.isReady()) {
            return false;
        }
        if (!EDCoreProvider.isLicenseValid()) {
            return false;
        }
        if (expectedEdCoreChecksum == null || expectedEdCoreChecksum.isBlank()) {
            return true;
        }
        String actual = EDCoreLifecycle.getEdCoreChecksum();
        return expectedEdCoreChecksum.equalsIgnoreCase(actual);
    }
}
