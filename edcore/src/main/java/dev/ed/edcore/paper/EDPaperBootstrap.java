package dev.ed.edcore.paper;

import dev.ed.edcore.internal.EDCoreLifecycle;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Точка входа Paper/Purpur: тот же JAR, что и для Velocity.
 */
public final class EDPaperBootstrap extends JavaPlugin {

    @Override
    public void onEnable() {
        EDCoreKernelPaper.start(this);
    }

    @Override
    public void onDisable() {
        EDCoreLifecycle.shutdown();
    }
}
