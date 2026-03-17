package com.lightyorugami.reworkedphantoms;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ReworkedPhantomsMod.MODID)
public class ReworkedPhantomsMod {
    public static final String MODID = "reworkedphantoms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static GameRules.Key<GameRules.BooleanValue> RULE_DO_PHANTOM_SPAWNING;

    public ReworkedPhantomsMod() {
        LOGGER.info("Reworked Phantoms Mod loaded!");
        RULE_DO_PHANTOM_SPAWNING = GameRules.register(
            "doPhantomSpawning",
            GameRules.Category.SPAWNING,
            GameRules.BooleanValue.create(true)
        );
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            MinecraftServer server = event.getServer();
            for (ServerLevel level : server.getAllLevels()) {
                level.getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
                LOGGER.info("doInsomnia has been set to false in level: " + level.dimension().location());
            }
            LOGGER.info("Custom gamerule 'doPhantomSpawning' registered (default: true)");
        }
    }
}
