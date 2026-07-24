package dev.ed.edcore.util;

/**
 * Мост к PlaceholderAPI без жёсткой зависимости от Bukkit в статических импортах (безопасно для Velocity JAR).
 */
public final class PlaceholderHook {

    private PlaceholderHook() {}

    public static boolean isAvailable() {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object plugin = pm.getClass().getMethod("getPlugin", String.class).invoke(pm, "PlaceholderAPI");
            if (plugin == null) {
                return false;
            }
            return (boolean) plugin.getClass().getMethod("isEnabled").invoke(plugin);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * @param player объект {@code org.bukkit.entity.Player} на Paper; иначе возвращает {@code text} без изменений
     */
    public static String setPlaceholders(Object player, String text) {
        if (player == null || text == null || !isAvailable()) {
            return text;
        }
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
            if (!playerClass.isInstance(player)) {
                return text;
            }
            return (String) papi.getMethod("setPlaceholders", playerClass, String.class)
                    .invoke(null, player, text);
        } catch (ReflectiveOperationException e) {
            return text;
        }
    }
}
