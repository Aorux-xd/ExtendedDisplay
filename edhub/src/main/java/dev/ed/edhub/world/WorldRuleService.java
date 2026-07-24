package dev.ed.edhub.world;

import dev.ed.edhub.config.WorldConfig;
import org.bukkit.GameRule;
import org.bukkit.World;

public final class WorldRuleService {

    public void apply(World world, WorldConfig cfg) {
        if (world == null || cfg == null) {
            return;
        }
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, cfg.timeCycle());
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, cfg.weatherChange());
        world.setGameRule(GameRule.DO_MOB_SPAWNING, cfg.monsters() || cfg.animals());
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, cfg.patrols());
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, cfg.trader());
        if (!cfg.timeCycle()) {
            world.setTime(cfg.fixedTime().ticks());
        }
    }
}
