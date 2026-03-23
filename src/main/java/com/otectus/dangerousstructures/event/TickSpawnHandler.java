package com.otectus.dangerousstructures.event;

import com.otectus.dangerousstructures.DangerousStructures;
import com.otectus.dangerousstructures.config.DSConfig;
import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = DangerousStructures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickSpawnHandler {

    private static int tickCounter = 0;

    // Dynamic mob list cache — invalidated on config reload
    private static volatile List<EntityType<?>> cachedSpawnableMobs = null;

    public static void invalidateMobCache() {
        cachedSpawnableMobs = null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        if (!DSConfig.enabled.get()) return;
        if (!DSConfig.periodicSpawningEnabled.get()) return;

        tickCounter++;
        if (tickCounter < DSConfig.periodicSpawnInterval.get()) return;
        tickCounter = 0;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (!DSConfig.isDimensionAllowed(level.dimension())) continue;

            Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            List<Structure> dangerousStructuresList = resolveDangerousStructures(registry);
            if (dangerousStructuresList.isEmpty()) continue;

            Set<StructureStart> processedStarts = new HashSet<>();

            for (LevelChunk chunk : getLoadedChunks(level)) {
                SectionPos sectionPos = SectionPos.bottomOf(chunk);
                for (Structure structure : dangerousStructuresList) {
                    for (StructureStart start : level.structureManager().startsForStructure(sectionPos, structure)) {
                        if (start == StructureStart.INVALID_START) continue;
                        if (!processedStarts.add(start)) continue;
                        tryPeriodicSpawn(level, start);
                    }
                }
            }
        }
    }

    private static List<Structure> resolveDangerousStructures(Registry<Structure> registry) {
        if (DSConfig.autoDetectStructures.get()) {
            List<Structure> result = new ArrayList<>();
            for (var entry : registry.entrySet()) {
                Structure structure = entry.getValue();
                if (StructureDetection.isStructureExcluded(registry, structure)) continue;
                result.add(structure);
            }
            return result;
        }

        // Manual mode: use whitelist
        List<Structure> result = new ArrayList<>();
        for (ResourceLocation rl : DSConfig.getStructureWhitelist()) {
            Structure s = registry.get(rl);
            if (s != null) result.add(s);
        }
        for (ResourceLocation tagRL : DSConfig.getStructureTagWhitelist()) {
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagRL);
            for (Holder<Structure> holder : registry.getTagOrEmpty(tagKey)) {
                result.add(holder.value());
            }
        }
        return result;
    }

    private static void tryPeriodicSpawn(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        int cap = DSConfig.periodicSpawnCap.get();

        // Count existing monsters inside the bounding box
        long existingMonsters = level.getEntities(null, new net.minecraft.world.phys.AABB(
                bb.minX(), bb.minY(), bb.minZ(),
                bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1
        )).stream().filter(e -> e.getType().getCategory() == MobCategory.MONSTER).count();

        if (existingMonsters >= cap) return;

        RandomSource random = level.getRandom();

        // Pick a random position inside the bounding box
        int x = random.nextIntBetweenInclusive(bb.minX(), bb.maxX());
        int z = random.nextIntBetweenInclusive(bb.minZ(), bb.maxZ());
        int y = random.nextIntBetweenInclusive(bb.minY(), bb.maxY());
        BlockPos spawnPos = new BlockPos(x, y, z);

        // Validate: solid block below, air at spawn pos and above
        if (!level.getBlockState(spawnPos.below()).isSolid()) return;
        if (!level.getBlockState(spawnPos).isAir()) return;
        if (!level.getBlockState(spawnPos.above()).isAir()) return;

        // Pick a random hostile mob type from the dynamic list
        EntityType<?> mobType = pickRandomHostileMob(random);
        if (mobType == null) return;

        // Spawn the entity
        var entity = mobType.create(level);
        if (entity == null) return;

        entity.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);

        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.NATURAL, null, null);
        }

        if (level.addFreshEntity(entity)) {
            if (DSConfig.debugLogging.get()) {
                DangerousStructures.LOGGER.info("[DangerousStructures] Periodic spawn of {} at {}",
                        mobType.getDescriptionId(), spawnPos);
            }
        }
    }

    private static EntityType<?> pickRandomHostileMob(RandomSource random) {
        List<EntityType<?>> mobs = getSpawnableMobs();
        if (mobs.isEmpty()) return null;
        return mobs.get(random.nextInt(mobs.size()));
    }

    private static List<EntityType<?>> getSpawnableMobs() {
        List<EntityType<?>> cached = cachedSpawnableMobs;
        if (cached != null) return cached;

        Set<ResourceLocation> whitelist = DSConfig.getMobWhitelist();
        Set<ResourceLocation> blacklist = DSConfig.getMobBlacklist();

        List<EntityType<?>> result;
        if (!whitelist.isEmpty()) {
            List<EntityType<?>> list = new ArrayList<>();
            for (ResourceLocation rl : whitelist) {
                EntityType<?> et = ForgeRegistries.ENTITY_TYPES.getValue(rl);
                if (et != null) list.add(et);
            }
            result = List.copyOf(list);
        } else {
            List<EntityType<?>> list = new ArrayList<>();
            for (EntityType<?> et : ForgeRegistries.ENTITY_TYPES.getValues()) {
                if (et.getCategory() != MobCategory.MONSTER) continue;
                if (!blacklist.isEmpty() && blacklist.contains(EntityType.getKey(et))) continue;
                list.add(et);
            }
            result = List.copyOf(list);
        }

        cachedSpawnableMobs = result;
        return result;
    }

    private static Iterable<LevelChunk> getLoadedChunks(ServerLevel level) {
        List<LevelChunk> chunks = new ArrayList<>();
        Set<ChunkPos> seen = new HashSet<>();
        level.players().forEach(player -> {
            BlockPos playerPos = player.blockPosition();
            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;
            int radius = 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = chunkX + dx;
                    int cz = chunkZ + dz;
                    if (level.hasChunk(cx, cz)) {
                        if (seen.add(new ChunkPos(cx, cz))) {
                            chunks.add(level.getChunk(cx, cz));
                        }
                    }
                }
            }
        });
        return chunks;
    }
}
