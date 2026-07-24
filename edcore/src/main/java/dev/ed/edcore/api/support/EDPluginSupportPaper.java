package dev.ed.edcore.api.support;

import dev.ed.edcore.api.EDCoreProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Проверка наличия EDCore на Paper/Purpur перед включением зависимого плагина.
 */
public final class EDPluginSupportPaper {

    private EDPluginSupportPaper() {
    }

    /**
     * @return {@code false}, если EDCore не найден и текущий плагин отключён
     */
    public static boolean requireEDCore(JavaPlugin plugin) {
        var pm = Bukkit.getPluginManager();
        if (pm.getPlugin("EDCore") == null || !pm.isPluginEnabled("EDCore")) {
            plugin.getLogger().severe("Требуется плагин EDCore. Отключаю " + plugin.getName() + ".");
            pm.disablePlugin(plugin);
            return false;
        }
        if (!EDCoreProvider.isReady()) {
            plugin.getLogger().severe("EDCore ещё не инициализирован. Отключаю " + plugin.getName() + ".");
            pm.disablePlugin(plugin);
            return false;
        }
        if (!EDCoreProvider.isLicenseValid()) {
            plugin.getLogger().severe("EDCore: лицензия не активна. Привяжите ключ на edcore.vercel.app. Отключаю "
                    + plugin.getName() + ".");
            pm.disablePlugin(plugin);
            return false;
        }
        return true;
    }
}
