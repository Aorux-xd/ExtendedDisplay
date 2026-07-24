package dev.ed.edcore.paper.command;

import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.api.license.LicenseStatus;
import dev.ed.edcore.internal.EDCoreLifecycle;
import dev.ed.edcore.internal.license.LicenseConfig;
import dev.ed.edcore.internal.license.UpdateManager;
import dev.ed.edcore.util.MessageFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EdCoreCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MessageFormatter formatter = new MessageFormatter();

    public EdCoreCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("edcore.admin")) {
            sender.sendMessage(Component.text("Нет прав: edcore.admin"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Использование: /edcore <status|update> ..."));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("status".equals(sub)) {
            return status(sender);
        }
        if ("update".equals(sub)) {
            return update(sender, args);
        }
        sender.sendMessage(Component.text("Неизвестная подкоманда. Доступно: status, update"));
        return true;
    }

    private boolean status(CommandSender sender) {
        var api = EDCoreProvider.get();
        LicenseStatus status = api.getLicenseStatus();
        var cfg = new LicenseConfig(api.getConfig());
        Component line = formatter.format(
                "&#00AAFFEDCore &#FFFFFF| статус: &#FFFF00" + status.name()
                        + " &#FFFFFF| владелец: &#00FF00" + api.getLicenseOwner()
                        + " &#FFFFFF| server-id: &#AAAAAA" + cfg.serverId());
        sender.sendMessage(line);
        return true;
    }

    private boolean update(CommandSender sender, String[] args) {
        UpdateManager manager = EDCoreLifecycle.getUpdateManager();
        if (manager == null) {
            sender.sendMessage(Component.text("Менеджер обновлений недоступен на этой платформе."));
            return true;
        }
        String target = args.length >= 2 ? args[1] : "all";
        sender.sendMessage(Component.text("Проверка и установка обновлений («" + target + "»)..."));
        EDCoreProvider.get().getScheduler().runAsync(() -> {
            int n = manager.installUpdates(target);
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("Установлено обновлений: " + n)));
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("edcore.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("status", "update"), args[0]);
        }
        if (args.length == 2 && "update".equalsIgnoreCase(args[0])) {
            return filter(List.of("all"), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
