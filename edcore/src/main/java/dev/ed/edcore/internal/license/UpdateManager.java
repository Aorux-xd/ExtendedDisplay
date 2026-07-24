package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.logging.EDLogger;
import dev.ed.edcore.api.scheduler.EDScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Проверка и установка обновлений ED-плагинов (кроме EDCore).
 */
public final class UpdateManager {

    private static final Pattern UPDATE_ENTRY = Pattern.compile(
            "\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"version\"\\s*:\\s*\"([^\"]+)\"");

    private final JavaPlugin host;
    private final EdCoreApiClient api;
    private final LicenseConfig licenseConfig;
    private final LicenseState licenseState;
    private final EDLogger log;
    private final EDScheduler scheduler;

    public UpdateManager(JavaPlugin host, EdCoreApiClient api, LicenseConfig licenseConfig,
                         LicenseState licenseState, EDLogger log, EDScheduler scheduler) {
        this.host = host;
        this.api = api;
        this.licenseConfig = licenseConfig;
        this.licenseState = licenseState;
        this.log = log;
        this.scheduler = scheduler;
    }

    public void scheduleAutoCheck() {
        if (!licenseConfig.autoCheckUpdates() || !licenseState.isValid()) {
            return;
        }
        long hours = licenseConfig.updateCheckIntervalHours();
        long periodMs = Math.max(1, hours) * 3_600_000L;
        scheduler.runAsync(() -> checkForUpdates(false));
        scheduler.runLater(periodMs, () -> {
            checkForUpdates(false);
            scheduleAutoCheck();
        });
    }

    public void checkForUpdates(boolean fromCommand) {
        if (!licenseState.isValid()) {
            return;
        }
        List<String> plugins = listUpdatablePlugins();
        if (plugins.isEmpty()) {
            return;
        }
        api.fetchUpdates(licenseConfig.licenseKey(), licenseConfig.serverId(), plugins).ifPresent(body -> {
            for (UpdateEntry entry : parseUpdates(body)) {
                String msg = licenseConfig.message("messages.update-available",
                                "Доступно обновление: %plugin% v%version%")
                        .replace("%plugin%", entry.name())
                        .replace("%version%", entry.version());
                log.info(stripColor(msg));
            }
            if (fromCommand) {
                log.info("EDCore: проверка обновлений завершена.");
            }
        });
    }

    public int installUpdates(String target) {
        if (!licenseState.isValid()) {
            log.warn("EDCore: обновления доступны только при активной лицензии.");
            return 0;
        }
        List<String> plugins = listUpdatablePlugins();
        var response = api.fetchUpdates(licenseConfig.licenseKey(), licenseConfig.serverId(), plugins);
        if (response.isEmpty()) {
            return 0;
        }
        int installed = 0;
        for (UpdateEntry entry : parseUpdates(response.get())) {
            if (!matchesTarget(target, entry.name())) {
                continue;
            }
            if (installOne(entry)) {
                installed++;
            }
        }
        return installed;
    }

    private boolean installOne(UpdateEntry entry) {
        var bytes = api.downloadPlugin(
                licenseConfig.licenseKey(),
                licenseConfig.serverId(),
                entry.name(),
                entry.version());
        if (bytes.isEmpty()) {
            log.warn("EDCore: не удалось скачать " + entry.name() + " v" + entry.version());
            return false;
        }
        Path pluginsDir = host.getServer().getPluginsFolder().toPath();
        Path target = pluginsDir.resolve(entry.name() + "-" + entry.version() + ".jar");
        try {
            Files.write(target, bytes.get());
            disableOldJar(pluginsDir, entry.name());
            String msg = licenseConfig.message("messages.update-installed",
                            "%plugin% обновлён до v%version%. Перезагрузите сервер.")
                    .replace("%plugin%", entry.name())
                    .replace("%version%", entry.version());
            log.info(stripColor(msg));
            return true;
        } catch (IOException e) {
            log.warn("EDCore: ошибка записи JAR " + entry.name() + ": " + e.getMessage());
            return false;
        }
    }

    private void disableOldJar(Path pluginsDir, String pluginName) throws IOException {
        String prefix = pluginName.toLowerCase(Locale.ROOT);
        try (var stream = Files.list(pluginsDir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).startsWith(prefix))
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .forEach(p -> {
                        try {
                            Files.move(p, p.resolveSibling(p.getFileName() + ".old"),
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private List<String> listUpdatablePlugins() {
        List<String> names = new ArrayList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            String name = plugin.getName();
            if ("EDCore".equalsIgnoreCase(name)) {
                continue;
            }
            if (name.toLowerCase(Locale.ROOT).startsWith("ed")) {
                names.add(name);
            }
        }
        return names;
    }

    private static boolean matchesTarget(String target, String pluginName) {
        if (target == null || target.equalsIgnoreCase("all")) {
            return true;
        }
        return target.equalsIgnoreCase(pluginName);
    }

    static List<UpdateEntry> parseUpdates(String json) {
        List<UpdateEntry> list = new ArrayList<>();
        Matcher m = UPDATE_ENTRY.matcher(json);
        while (m.find()) {
            list.add(new UpdateEntry(m.group(1), m.group(2)));
        }
        return list;
    }

    private static String stripColor(String msg) {
        return msg == null ? "" : msg.replaceAll("&#[0-9A-Fa-f]{6}", "");
    }

    record UpdateEntry(String name, String version) {
    }
}
