package com.lightyorugami.reworkedphantoms.event;

import com.lightyorugami.reworkedphantoms.ReworkedPhantomsMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;

@EventBusSubscriber(modid = ReworkedPhantomsMod.MODID)
public class PhantomSpawnHandler {

    private static final double SPAWN_CHANCE = 1.0;
    private static final int RESPAWN_DELAY_TICKS = 200;
    private static final Set<UUID> processedPlayers = new HashSet<>();
    private static final Set<UUID> playersWithSalve = new HashSet<>();
    private static final Map<UUID, RespawnTracker> respawnTrackers = new HashMap<>();
    private static long lastSalveDay = -1;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) return;

        long dayTime = level.getDayTime();
        long currentDay = dayTime / 24000L;
        long currentTick = dayTime % 24000L;
        boolean isNight = currentTick >= 13000L && currentTick <= 23000L;

        if (currentTick == 18000L && currentDay != lastSalveDay) {
            playersWithSalve.clear();
            respawnTrackers.clear();
            lastSalveDay = currentDay;
        }

        if (
            currentTick == 18000L &&
            level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) &&
            level.getGameRules().getBoolean(ReworkedPhantomsMod.RULE_DO_PHANTOM_SPAWNING) &&
            level.getDifficulty() != Difficulty.PEACEFUL &&
            level.dimensionType().hasSkyLight() &&
            isNight &&
            level.getMoonPhase() == 0
        ) {
            for (ServerPlayer player : level.players()) {
                UUID uuid = player.getUUID();
                if (processedPlayers.contains(uuid)) continue;
                processedPlayers.add(uuid);

                if (playersWithSalve.contains(uuid)) continue;

                BlockPos pos = player.blockPosition();
                if (
                    pos.getY() >= 64 &&
                    level.canSeeSky(pos) &&
                    hasAcquireHardware(player) &&
                    level.getRandom().nextDouble() < SPAWN_CHANCE
                ) {
                    List<UUID> spawned = trySpawnPhantomGroup(player, level);
                    playersWithSalve.add(uuid);
                    respawnTrackers.put(uuid, new RespawnTracker(spawned));
                }
            }
        }

        if (currentTick > 18000L && currentTick <= 23000L) {
            for (ServerPlayer player : level.players()) {
                UUID uuid = player.getUUID();
                RespawnTracker tracker = respawnTrackers.get(uuid);
                if (tracker == null) continue;

                boolean allDead = tracker.spawnedPhantoms.stream()
                        .noneMatch(id -> level.getEntity(id) instanceof Phantom);

                if (allDead) {
                    if (tracker.phantomsDeathTick == -1L) {
                        tracker.phantomsDeathTick = level.getDayTime();
                    } else if (level.getDayTime() - tracker.phantomsDeathTick >= RESPAWN_DELAY_TICKS) {
                        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) &&
                            level.getGameRules().getBoolean(ReworkedPhantomsMod.RULE_DO_PHANTOM_SPAWNING)) {
                            List<UUID> newSpawned = trySpawnPhantomGroup(player, level);
                            tracker.spawnedPhantoms = newSpawned;
                            tracker.phantomsDeathTick = -1L;
                        } else {
                            respawnTrackers.remove(uuid);
                        }
                    }
                }
            }
        }

        if (currentTick != 18000L) {
            processedPlayers.clear();
        }
    }

    private static boolean hasAcquireHardware(ServerPlayer player) {
        AdvancementHolder adv = player.server.getAdvancements().get(ResourceLocation.tryParse("minecraft:story/smelt_iron"));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static List<UUID> trySpawnPhantomGroup(ServerPlayer player, ServerLevel level) {
        List<UUID> phantomIds = new ArrayList<>();
        int spawnCount = switch (level.getDifficulty()) {
            case EASY -> 1 + level.getRandom().nextInt(2);
            case NORMAL -> 2 + level.getRandom().nextInt(2);
            case HARD -> 2 + level.getRandom().nextInt(3);
            default -> 1;
        };

        for (int i = 0; i < spawnCount; i++) {
            int dx = level.getRandom().nextInt(21) - 10;
            int dz = level.getRandom().nextInt(21) - 10;
            int dy = 20 + level.getRandom().nextInt(15);

            BlockPos basePos = player.blockPosition().offset(dx, dy, dz);
            BlockPos finalPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, basePos);
            if (finalPos.getY() < basePos.getY()) finalPos = basePos;

            Phantom phantom = EntityType.PHANTOM.create(level, EntitySpawnReason.NATURAL);
            if (phantom != null) {
                phantom.getPersistentData().putString("OwnerUUID", player.getUUID().toString());
                phantom.setPos(finalPos.getX() + 0.5, (double) finalPos.getY(), finalPos.getZ() + 0.5);
                phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(finalPos), EntitySpawnReason.NATURAL, null);
                level.addFreshEntity(phantom);
                phantomIds.add(phantom.getUUID());
            }
        }
        return phantomIds;
    }

    private static class RespawnTracker {
        long phantomsDeathTick = -1L;
        List<UUID> spawnedPhantoms;

        RespawnTracker(List<UUID> spawned) {
            this.spawnedPhantoms = spawned;
        }
    }
}