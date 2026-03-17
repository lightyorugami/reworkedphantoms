package com.lightyorugami.reworkedphantoms;

import com.mojang.logging.LogUtils;
import com.lightyorugami.reworkedphantoms.event.PhantomSpawnHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;

public class ReworkedPhantomsMod implements ModInitializer {
    public static final String MODID = "reworkedphantoms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static GameRules.Key<GameRules.BooleanRule> RULE_DO_PHANTOM_SPAWNING;

    @Override
    public void onInitialize() {
        LOGGER.info("Reworked Phantoms Mod loaded!");

        // Enregistrement de la gamerule custom (équivalent de GameRules.register en Forge)
        RULE_DO_PHANTOM_SPAWNING = GameRuleRegistry.register(
            "doPhantomSpawning",
            GameRules.Category.SPAWNING,
            GameRuleFactory.createBooleanRule(true)
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                world.getGameRules().get(GameRules.DO_INSOMNIA).set(false, server);
                LOGGER.info("doInsomnia set to false in: " + world.getRegistryKey().getValue());
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(PhantomSpawnHandler::onWorldTick);
    }
}