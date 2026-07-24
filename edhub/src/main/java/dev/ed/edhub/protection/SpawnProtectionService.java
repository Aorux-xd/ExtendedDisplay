package dev.ed.edhub.protection;

import dev.ed.edhub.config.HubLocation;
import dev.ed.edhub.config.SpawnProtectionConfig;
import org.bukkit.Location;

public final class SpawnProtectionService {

    public boolean isProtected(Location target, SpawnProtectionConfig cfg) {
        if (target == null || cfg == null || cfg.center() == null) {
            return false;
        }
        HubLocation c = cfg.center();
        if (target.getWorld() == null || !target.getWorld().getName().equalsIgnoreCase(c.world())) {
            return false;
        }
        double dx = target.getX() - c.x();
        double dz = target.getZ() - c.z();
        double distSq = dx * dx + dz * dz;
        int r = Math.max(0, cfg.radius());
        return distSq <= (double) r * (double) r;
    }
}
