package com.lightyorugami.reworkedphantoms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ReworkedPhantomsMod.MODID)
public class ReworkedPhantomsMod {
    public static final String MODID = "reworkedphantoms";
    public static final Logger LOGGER = LogManager.getLogger();

    public static boolean doPhantomSpawning = true;
    private static final ForgeConfigSpec.BooleanValue CONFIG_DO_SPAWN;
    private static final ForgeConfigSpec COMMON_CONFIG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG_DO_SPAWN = builder
            .comment("doPhantomSpawning (default: true)")
            .define("doPhantomSpawning", true);
        COMMON_CONFIG = builder.build();
    }

    public ReworkedPhantomsMod() {
        LOGGER.info("Reworked Phantoms Mod loaded!");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);
    }

    private void onConfigLoad(net.minecraftforge.fml.config.ModConfig.ModConfigEvent event) {
        doPhantomSpawning = CONFIG_DO_SPAWN.get();
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Load event) {
            if (event.getWorld() instanceof ServerWorld) {
                ServerWorld level = (ServerWorld) event.getWorld();
                MinecraftServer server = level.getServer();
                level.getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
                LOGGER.info("doInsomnia has been set to false in level: " + level.dimension().location());
            }
        }
    }
}