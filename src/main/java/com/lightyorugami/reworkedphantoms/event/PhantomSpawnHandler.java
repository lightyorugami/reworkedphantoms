package com.lightyorugami.reworkedphantoms.event;

import com.lightyorugami.reworkedphantoms.ReworkedPhantomsMod;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;

import java.util.*;

public class PhantomSpawnHandler {

    private static final double SPAWN_CHANCE = 1.0;
    private static final int RESPAWN_DELAY_TICKS = 200;
    private static final Set<UUID> processedPlayers = new HashSet<>();
    private static final Set<UUID> playersWithSalve = new HashSet<>();
    private static final Map<UUID, RespawnTracker> respawnTrackers = new HashMap<>();
    private static long lastSalveDay = -1;

    public static void onWorldTick(ServerWorld world) {
        long dayTime = world.getTimeOfDay();
        long currentDay = dayTime / 24000L;
        long currentTick = dayTime % 24000L;

        if (currentTick == 18000L && currentDay != lastSalveDay) {
            playersWithSalve.clear();
            respawnTrackers.clear();
            lastSalveDay = currentDay;
        }

        if (
            currentTick == 18000L &&
            world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING) &&
            world.getGameRules().getBoolean(ReworkedPhantomsMod.RULE_DO_PHANTOM_SPAWNING) &&
            world.getDifficulty() != Difficulty.PEACEFUL &&
            world.getDimension().hasSkyLight() &&
            world.isNight() &&
            world.getMoonPhase() == 0
        ) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID uuid = player.getUuid();
                if (processedPlayers.contains(uuid)) continue;
                processedPlayers.add(uuid);

                if (playersWithSalve.contains(uuid)) continue;

                BlockPos pos = player.getBlockPos();
                if (
                    pos.getY() >= 64 &&
                    world.isSkyVisible(pos) &&
                    hasAcquireHardware(player) &&
                    world.getRandom().nextDouble() < SPAWN_CHANCE
                ) {
                    List<UUID> spawned = trySpawnPhantomGroup(player, world);
                    playersWithSalve.add(uuid);
                    respawnTrackers.put(uuid, new RespawnTracker(spawned));
                }
            }
        }

        if (currentTick > 18000L && currentTick <= 23000L &&
            world.getGameRules().getBoolean(ReworkedPhantomsMod.RULE_DO_PHANTOM_SPAWNING)) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID uuid = player.getUuid();
                RespawnTracker tracker = respawnTrackers.get(uuid);
                if (tracker == null) continue;

                boolean allDead = tracker.spawnedPhantoms.stream()
                        .noneMatch(id -> world.getEntity(id) instanceof PhantomEntity);

                if (allDead) {
                    if (tracker.phantomsDeathTick == -1L) {
                        tracker.phantomsDeathTick = world.getTime();
                    } else if (world.getTime() - tracker.phantomsDeathTick >= RESPAWN_DELAY_TICKS) {
                        List<UUID> newSpawned = trySpawnPhantomGroup(player, world);
                        tracker.spawnedPhantoms = newSpawned;
                        tracker.phantomsDeathTick = -1L;
                    }
                }
            }
        }

        if (currentTick != 18000L) {
            processedPlayers.clear();
        }
    }

    private static boolean hasAcquireHardware(ServerPlayerEntity player) {
        
        AdvancementEntry adv = player.getServer().getAdvancementLoader()
            .get(Identifier.of("minecraft", "story/smelt_iron"));
        return adv != null && player.getAdvancementTracker().getProgress(adv).isDone();
    }

    private static List<UUID> trySpawnPhantomGroup(ServerPlayerEntity player, ServerWorld world) {
        List<UUID> phantomIds = new ArrayList<>();
        int spawnCount = switch (world.getDifficulty()) {
            case EASY -> 1 + world.getRandom().nextInt(2);
            case NORMAL -> 2 + world.getRandom().nextInt(2);
            case HARD -> 2 + world.getRandom().nextInt(3);
            default -> 1;
        };

        for (int i = 0; i < spawnCount; i++) {
            int dx = world.getRandom().nextInt(21) - 10;
            int dz = world.getRandom().nextInt(21) - 10;
            int dy = 20 + world.getRandom().nextInt(15);

            BlockPos basePos = player.getBlockPos().add(dx, dy, dz);
            BlockPos finalPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, basePos);
            if (finalPos.getY() < basePos.getY()) finalPos = basePos;

            PhantomEntity phantom = EntityType.PHANTOM.spawn(
                world,
                null,               
                finalPos,
                SpawnReason.NATURAL,
                false,
                false
            );
            if (phantom != null) {
                NbtCompound persistentData = new NbtCompound();
                phantom.writeNbt(persistentData);
                persistentData.putUuid("OwnerUUID", player.getUuid());
                phantom.readNbt(persistentData);
                phantomIds.add(phantom.getUuid());
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