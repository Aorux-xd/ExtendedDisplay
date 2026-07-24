package dev.ed.edhub.service;

import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edhub.api.EdHUBAPI;
import dev.ed.edhub.api.Location;
import dev.ed.edhub.config.*;
import dev.ed.edhub.protection.SpawnProtectionService;
import dev.ed.edhub.world.WorldRuleService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class HubManager implements EdHUBAPI {
    private final JavaPlugin plugin;
    private final WorldRuleService worldRuleService = new WorldRuleService();
    private final SpawnProtectionService protection = new SpawnProtectionService();
    private volatile EdHubConfig config;
    private volatile boolean debug;
    private volatile WhitelistNicknameService whitelist;

    public HubManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        reload();
        applyAllWorlds();
    }

    public void reload() {
        plugin.reloadConfig();

        var fc = plugin.getConfig();
        if (!fc.isSet("infinity-hunger") && fc.isSet("disable-hunger")) {
            boolean v = fc.getBoolean("disable-hunger", true);
            fc.set("infinity-hunger", v);
            plugin.getLogger().warning("EdHUB: параметр `disable-hunger` устарел и будет удалён в будущем. Используйте `infinity-hunger` вместо него.");
        }
        if (fc.isSet("disable-hunger")) {
            fc.set("disable-hunger", null);
            plugin.saveConfig();
        }

        this.config = new EdHubConfigLoader().load(plugin.getConfig());
        this.debug = config.defaults().debug();
        this.whitelist = new WhitelistNicknameService(config.whitelist());
    }

    public boolean debug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void applyAllWorlds() {
        for (World world : Bukkit.getWorlds()) {
            WorldConfig cfg = getWorldConfig(world.getName());
            if (matchesWorld(world, cfg)) {
                worldRuleService.apply(world, cfg);
            }
        }
    }

    public boolean matchesWorld(World world, WorldConfig cfg) {
        return cfg.applyToAllWorlds() || cfg.worldName().equalsIgnoreCase(world.getName());
    }

    public CompletableFuture<Boolean> teleportToConfiguredSpawn(Player player, boolean firstSpawn) {
        WorldConfig cfg = getWorldConfig(player.getWorld().getName());
        HubLocation src = firstSpawn ? cfg.firstSpawn() : cfg.hubSpawn();
        org.bukkit.Location location = toBukkit(src);
        if (location == null) {
            plugin.getLogger().warning("Мир точки спавна не загружен: " + src.world());
            return CompletableFuture.completedFuture(false);
        }
        return player.teleportAsync(location).exceptionally(ex -> false);
    }

    public boolean isFirstJoin(Player player) {
        try {
            var storage = EDCoreProvider.get().getStorage();
            String key = "edhub.joined." + player.getUniqueId().toString().toLowerCase(Locale.ROOT);
            boolean first = storage.get(key).isEmpty();
            if (first) {
                storage.put(key, "1");
                storage.save();
            }
            return first;
        } catch (Exception e) {
            return !player.hasPlayedBefore();
        }
    }

    public void clearInventoryWithWhitelist(Player player, WorldConfig cfg) {
        if (!cfg.inventory().clearOnJoin()) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        List<ItemStack> keep = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (cfg.inventory().whitelistItems().contains(item.getType())) {
                keep.add(item.clone());
            }
        }
        player.getInventory().clear();
        for (ItemStack item : keep) {
            player.getInventory().addItem(item);
        }
    }

    public org.bukkit.Location resolveAntiVoidTarget(Player player, WorldConfig cfg) {
        String target = cfg.antiVoid().teleportTo();
        boolean first = "first-spawn".equalsIgnoreCase(target);
        HubLocation loc = first ? cfg.firstSpawn() : cfg.hubSpawn();
        return toBukkit(loc);
    }

    public org.bukkit.Location toBukkit(HubLocation in) {
        World world = Bukkit.getWorld(in.world());
        if (world == null) {
            return null;
        }
        return new org.bukkit.Location(world, in.x(), in.y(), in.z(), in.yaw(), in.pitch());
    }

    @Override
    public Location getSpawnLocation(String worldName) {
        HubLocation l = getWorldConfig(worldName).hubSpawn();
        return new Location(l.world(), l.x(), l.y(), l.z(), l.yaw(), l.pitch());
    }

    @Override
    public Location getFirstSpawnLocation(String worldName) {
        HubLocation l = getWorldConfig(worldName).firstSpawn();
        return new Location(l.world(), l.x(), l.y(), l.z(), l.yaw(), l.pitch());
    }

    @Override
    public boolean isSpawnProtected(org.bukkit.Location loc) {
        WorldConfig cfg = getWorldConfig(loc.getWorld() != null ? loc.getWorld().getName() : null);
        return protection.isProtected(loc, cfg.spawnProtection());
    }

    @Override
    public WorldConfig getWorldConfig(String worldName) {
        return config.resolve(worldName);
    }

    public WhitelistNicknameService whitelist() {
        return whitelist;
    }

    public void logDebug(String message) {
        if (debug) {
            plugin.getLogger().info("[debug] " + message);
        }
    }
}
