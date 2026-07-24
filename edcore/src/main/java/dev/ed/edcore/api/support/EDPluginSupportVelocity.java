package dev.ed.edcore.api.support;

import com.velocitypowered.api.plugin.PluginManager;
import dev.ed.edcore.api.EDCoreProvider;

/**
 * Проверка наличия EDCore на Velocity перед инициализацией зависимого плагина.
 */
public final class EDPluginSupportVelocity {

    private EDPluginSupportVelocity() {
    }

    /**
     * @param edcorePluginId обычно {@code edcore}
     * @return {@code false}, если EDCore не загружен
     */
    public static boolean requireEDCore(PluginManager pluginManager, String edcorePluginId,
                                        org.slf4j.Logger logger, String dependentName) {
        var opt = pluginManager.getPlugin(edcorePluginId);
        if (opt.isEmpty()) {
            logger.error("Требуется плагин EDCore (id: {}). Отключаю зависимость {}.", edcorePluginId, dependentName);
            return false;
        }
        if (!EDCoreProvider.isReady()) {
            logger.error("EDCore ещё не инициализирован. Проверьте порядок загрузки (depend).");
            return false;
        }
        if (!EDCoreProvider.isLicenseValid()) {
            logger.error("EDCore: лицензия не активна. Привяжите ключ на edcore.vercel.app.");
            return false;
        }
        return true;
    }
}
