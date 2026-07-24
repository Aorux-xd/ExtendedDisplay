package dev.ed.edhub.config;

import org.bukkit.Material;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class InventoryConfig {
    private final boolean clearOnJoin;
    private final Set<Material> whitelistItems;

    public InventoryConfig(boolean clearOnJoin, Set<Material> whitelistItems) {
        this.clearOnJoin = clearOnJoin;
        this.whitelistItems = new HashSet<>(whitelistItems);
    }

    public boolean clearOnJoin() {
        return clearOnJoin;
    }

    public Set<Material> whitelistItems() {
        return Collections.unmodifiableSet(whitelistItems);
    }
}
