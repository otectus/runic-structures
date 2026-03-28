package com.otectus.runicstructures.event;

import com.otectus.runicstructures.RunicStructures;
import com.otectus.runicstructures.config.RSConfig;
import com.otectus.runicstructures.config.StructureProfile;
import com.otectus.runicstructures.config.StructureProfileLoader;
import com.otectus.runicstructures.util.MobEnhancer;
import com.otectus.runicstructures.util.MobSelector;
import com.otectus.runicstructures.util.SpawnPositionFinder;
import com.otectus.runicstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = RunicStructures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickSpawnHandler {

    private static final int INITIAL_POPULATION_SCAN_INTERVAL_TICKS = 20;

    public record StructureInstanceKey(ResourceLocation dimensionId, BlockPos origin) {}

    private static int tickCounter = 0;

    // Spawn statistics — all reads and writes occur on the server thread.
    private static int statTotalAttempts = 0;
    private static int statSuccessfulSpawns = 0;
    private static int statCapRejects = 0;
    private static int statPositionFailures = 0;
    private static int statInitialPopulations = 0;

    // Initial population tracking — keyed by dimension + structure BB origin, cleared each session.
    private static final Set<StructureInstanceKey> populatedStructures = new HashSet<>();

    // Per-structure spawn tracking — keyed by dimension + structure BB origin
    private static final Map<StructureInstanceKey, StructureSpawnStats> perStructureStats = new HashMap<>();

    public static int getStatTotalAttempts() { return statTotalAttempts; }
    public static int getStatSuccessfulSpawns() { return statSuccessfulSpawns; }
    public static int getStatCapRejects() { return statCapRejects; }
    public static int getStatPositionFailures() { return statPositionFailures; }
    public static int getStatInitialPopulations() { return statInitialPopulations; }
    public static Map<StructureInstanceKey, StructureSpawnStats> getPerStructureStats() {
        return Collections.unmodifiableMap(perStructureStats);
    }

    public static class StructureSpawnStats {
        public int spawns = 0;
        public int attempts = 0;
        public int capRejects = 0;
    }

    public static void invalidateCaches() {
        MobSelector.invalidateCache();
    }

    public static void onServerStopped() {
        tickCounter = 0;
        statTotalAttempts = 0;
        statSuccessfulSpawns = 0;
        statCapRejects = 0;
        statPositionFailures = 0;
        statInitialPopulations = 0;
        populatedStructures.clear();
        perStructureStats.clear();
        MobSelector.invalidateCache();
        RSConfig.setRuntimeDebug(false);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        if (!RSConfig.enabled.get()) return;

        boolean periodicEnabled = RSConfig.periodicSpawningEnabled.get();
        boolean initialPopEnabled = RSConfig.initialPopulationEnabled.get();
        if (!periodicEnabled && !initialPopEnabled) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (server.getWorldData().getDifficulty() == Difficulty.PEACEFUL) return;

        boolean doPeriodicSpawn = false;
        if (periodicEnabled) {
            int interval = getEffectiveInterval(server.getWorldData().getDifficulty());

            // Intentionally uses overworld time globally — the Nether and End have no day/night cycle.
            double nightMult = RSConfig.nightSpawnMultiplier.get();
            if (nightMult > 1.0 && server.overworld().isNight()) {
                interval = Math.max(20, (int) (interval / nightMult));
            }

            tickCounter++;
            if (tickCounter >= interval) {
                if (RSConfig.randomizeInterval.get()) {
                    RandomSource random = server.overworld().getRandom();
                    tickCounter = -(random.nextInt(Math.max(1, interval / 4)));
                } else {
                    tickCounter = 0;
                }
                doPeriodicSpawn = true;
            }
        }

        boolean doInitialPopulationScan = initialPopEnabled
                && server.getTickCount() % INITIAL_POPULATION_SCAN_INTERVAL_TICKS == 0;

        if (!doPeriodicSpawn && !doInitialPopulationScan) return;

        for (ServerLevel level : server.getAllLevels()) {
            if (!RSConfig.isDimensionAllowed(level.dimension())) continue;

            Registry<net.minecraft.world.level.levelgen.structure.Structure> registry =
                    level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            List<StructureDetection.RunicStructureRef> runicStructures = StructureDetection.getRunicStructures(registry);
            if (runicStructures.isEmpty()) continue;

            Set<net.minecraft.world.level.levelgen.structure.Structure> runicStructureSet = new LinkedHashSet<>();
            for (StructureDetection.RunicStructureRef ref : runicStructures) {
                runicStructureSet.add(ref.structure());
            }

            Set<StructureStart> processedStarts = new HashSet<>();
            for (LevelChunk chunk : getLoadedChunks(level)) {
                for (StructureStart start : level.structureManager().startsForStructure(chunk.getPos(), runicStructureSet::contains)) {
                    if (start == StructureStart.INVALID_START || !processedStarts.add(start)) continue;

                    ResourceLocation structureId = registry.getKey(start.getStructure());
                    StructureInstanceKey key = structureKey(level, start.getBoundingBox());

                    if (doInitialPopulationScan && populatedStructures.add(key)) {
                        tryInitialPopulation(level, start, structureId);
                    }

                    if (doPeriodicSpawn) {
                        tryPeriodicSpawn(level, start, structureId);
                    }
                }
            }
        }
    }

    /**
     * Compute the effective spawn interval after difficulty scaling.
     */
    static int getEffectiveInterval(Difficulty difficulty) {
        int base = RSConfig.periodicSpawnInterval.get();
        if (!RSConfig.difficultyScalingEnabled.get()) return base;

        double multiplier = switch (difficulty) {
            case EASY -> RSConfig.easyIntervalMultiplier.get();
            case NORMAL -> RSConfig.normalIntervalMultiplier.get();
            case HARD -> RSConfig.hardIntervalMultiplier.get();
            default -> 1.0;
        };
        return Math.max(20, (int) (base * multiplier));
    }

    /**
     * Compute the effective spawn cap after difficulty and structure-size scaling.
     */
    static int getEffectiveCap(Difficulty difficulty, BoundingBox bb) {
        return getEffectiveCap(difficulty, bb, StructureProfile.DEFAULT);
    }

    static int getEffectiveCap(Difficulty difficulty, BoundingBox bb, StructureProfile profile) {
        int base = profile.getSpawnCap();

        if (RSConfig.difficultyScalingEnabled.get()) {
            double multiplier = switch (difficulty) {
                case EASY -> RSConfig.easyCapMultiplier.get();
                case NORMAL -> RSConfig.normalCapMultiplier.get();
                case HARD -> RSConfig.hardCapMultiplier.get();
                default -> 1.0;
            };
            base = Math.max(1, (int) (base * multiplier));
        }

        if (RSConfig.scaleCapByStructureSize.get()) {
            int minChunkX = SectionPos.blockToSectionCoord(bb.minX());
            int maxChunkX = SectionPos.blockToSectionCoord(bb.maxX());
            int minChunkZ = SectionPos.blockToSectionCoord(bb.minZ());
            int maxChunkZ = SectionPos.blockToSectionCoord(bb.maxZ());
            int chunkArea = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            int scaledCap = chunkArea * RSConfig.mobsPerChunkArea.get();
            base = Math.min(RSConfig.maxScaledCap.get(), Math.max(base, scaledCap));
        }

        return base;
    }

    /**
     * Shared spawn pipeline for periodic and initial population spawns.
     *
     * @return true if the entity was successfully added to the world
     */
    private static boolean spawnEnhancedMob(ServerLevel level, EntityType<?> mobType,
            BlockPos pos, RandomSource random, StructureProfile profile) {
        var entity = mobType.create(level);
        if (entity == null) return false;

        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);

        if (entity instanceof Mob mob) {
            mob.addTag("rs_periodic");
            var spawnResult = ForgeEventFactory.onFinalizeSpawn(mob, level,
                    level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            if (spawnResult == null) {
                entity.discard();
                return false;
            }

            if (profile.isPersistentMobs()) {
                mob.setPersistenceRequired();
            }

            if (RSConfig.eliteSpawnsEnabled.get() && random.nextDouble() < profile.getEliteChance()) {
                MobEnhancer.applyEliteEnhancements(mob, profile);
            }

            if (RSConfig.armorEnabled.get()) {
                MobEnhancer.applyRandomArmor(mob, random, profile);
            }
            if (RSConfig.weaponsEnabled.get()) {
                MobEnhancer.applyRandomWeapon(mob, random, profile);
            }
            if (RSConfig.shieldsEnabled.get()) {
                MobEnhancer.applyRandomShield(mob, random, profile);
            }
            if (RSConfig.enchantmentsEnabled.get()) {
                MobEnhancer.applyRandomEnchantments(mob, random, profile);
            }
            if (RSConfig.fireImmunity.get()) {
                MobEnhancer.applyFireResistance(mob);
            }
        }

        if (level.addFreshEntity(entity)) {
            statSuccessfulSpawns++;
            return true;
        }

        entity.discard();
        return false;
    }

    private static void tryPeriodicSpawn(ServerLevel level, StructureStart start,
            @javax.annotation.Nullable ResourceLocation structureId) {
        BoundingBox structureBB = start.getBoundingBox();
        StructureInstanceKey key = structureKey(level, structureBB);
        StructureSpawnStats structStats = perStructureStats.computeIfAbsent(key, ignored -> new StructureSpawnStats());

        long dayCount = level.getDayTime() / 24000L;
        StructureProfile profile = StructureProfileLoader.getProfile(structureId, dayCount);

        Difficulty difficulty = level.getServer().getWorldData().getDifficulty();
        int cap = getEffectiveCap(difficulty, structureBB, profile);

        int existingMonsters = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(
                structureBB.minX(), structureBB.minY(), structureBB.minZ(),
                structureBB.maxX() + 1, structureBB.maxY() + 1, structureBB.maxZ() + 1))) {
            if (mob.getType().getCategory() == MobCategory.MONSTER) {
                if (++existingMonsters >= cap) {
                    statCapRejects++;
                    structStats.capRejects++;
                    return;
                }
            }
        }

        statTotalAttempts++;
        structStats.attempts++;
        RandomSource random = level.getRandom();
        int maxAttempts = RSConfig.maxPositionAttempts.get();
        int minPlayerDistSq = RSConfig.minPlayerDistance.get() * RSConfig.minPlayerDistance.get();

        List<BoundingBox> spawnBoxes = null;
        List<Integer> spawnBoxAreas = null;
        int totalArea = 0;

        if (RSConfig.useStructurePieces.get() && !start.getPieces().isEmpty()) {
            spawnBoxes = new ArrayList<>();
            spawnBoxAreas = new ArrayList<>();
            for (var piece : start.getPieces()) {
                BoundingBox pieceBB = piece.getBoundingBox();
                spawnBoxes.add(pieceBB);
                spawnBoxAreas.add(pieceBB.getXSpan() * pieceBB.getZSpan());
            }
            for (int area : spawnBoxAreas) {
                totalArea += area;
            }
        }

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            BoundingBox bb;
            if (spawnBoxes != null && RSConfig.weightPiecesByArea.get() && totalArea > 0) {
                int roll = random.nextInt(totalArea);
                int idx = 0;
                for (int i = 0; i < spawnBoxAreas.size(); i++) {
                    roll -= spawnBoxAreas.get(i);
                    if (roll < 0) {
                        idx = i;
                        break;
                    }
                }
                bb = spawnBoxes.get(idx);
            } else if (spawnBoxes != null) {
                bb = spawnBoxes.get(random.nextInt(spawnBoxes.size()));
            } else {
                bb = structureBB;
            }

            int x = bb.minX() == bb.maxX() ? bb.minX() : random.nextIntBetweenInclusive(bb.minX(), bb.maxX());
            int z = bb.minZ() == bb.maxZ() ? bb.minZ() : random.nextIntBetweenInclusive(bb.minZ(), bb.maxZ());

            List<BlockPos> validFloors = new ArrayList<>();
            for (int y = bb.maxY(); y >= bb.minY(); y--) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (SpawnPositionFinder.isValidSpawnFloor(level, candidate)) {
                    validFloors.add(candidate);
                }
            }
            if (validFloors.isEmpty()) continue;

            BlockPos spawnPos = validFloors.get(random.nextInt(validFloors.size()));

            if (minPlayerDistSq > 0) {
                boolean tooClose = false;
                for (ServerPlayer player : level.players()) {
                    if (player.blockPosition().distSqr(spawnPos) < minPlayerDistSq) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;
            }

            MobSpawnSettings.SpawnerData selectedSpawnerData = null;
            EntityType<?> mobType;
            if (profile.hasMobPoolOverride()) {
                mobType = MobSelector.pickFromMobPool(profile.getMobPool(), random);
            } else if (RSConfig.dimensionSpecificMobs.get()) {
                selectedSpawnerData = MobSelector.pickBiomeSpecificSpawnerData(level, spawnPos, random);
                mobType = selectedSpawnerData != null ? selectedSpawnerData.type : null;
            } else {
                mobType = MobSelector.pickRandomHostileMob(random);
            }
            if (mobType == null) continue;

            int packSize = 1;
            if (RSConfig.packSpawning.get()) {
                int maxPack = Math.max(1, RSConfig.maxPackSize.get());
                if (selectedSpawnerData != null) {
                    int effectiveMax = Math.max(1, Math.min(selectedSpawnerData.maxCount, maxPack));
                    int effectiveMin = Math.min(Math.max(1, selectedSpawnerData.minCount), effectiveMax);
                    packSize = effectiveMin == effectiveMax ? effectiveMin : random.nextIntBetweenInclusive(effectiveMin, effectiveMax);
                } else {
                    packSize = random.nextIntBetweenInclusive(1, Math.max(1, Math.min(2, maxPack)));
                }
            }

            int spawned = 0;
            for (int p = 0; p < packSize && (existingMonsters + spawned) < cap; p++) {
                BlockPos packPos;
                if (p == 0) {
                    packPos = spawnPos;
                } else {
                    packPos = SpawnPositionFinder.findNearbyFloor(level, spawnPos, random, SpawnPositionFinder.PACK_SEARCH_RADIUS);
                    if (packPos == null) continue;
                }

                if (spawnEnhancedMob(level, mobType, packPos, random, profile)) {
                    spawned++;
                    if (RSConfig.isDebugEnabled()) {
                        RunicStructures.LOGGER.info("[RunicStructures] Periodic spawn of {} at {}",
                                mobType.getDescriptionId(), packPos);
                    }
                }
            }

            if (spawned > 0) {
                structStats.spawns += spawned;
                return;
            }
        }

        if (RSConfig.isDebugEnabled()) {
            RunicStructures.LOGGER.info(
                    "[RunicStructures] Periodic spawn position finding failed after {} attempts for structure at [{}, {}, {}]",
                    maxAttempts, structureBB.minX(), structureBB.minY(), structureBB.minZ());
        }
        statPositionFailures++;
    }

    /**
     * Spawn an initial group of mobs throughout a structure the first time it's discovered each session.
     */
    private static void tryInitialPopulation(ServerLevel level, StructureStart start,
            @javax.annotation.Nullable ResourceLocation structureId) {
        BoundingBox structureBB = start.getBoundingBox();
        RandomSource random = level.getRandom();
        long dayCount = level.getDayTime() / 24000L;
        StructureProfile profile = StructureProfileLoader.getProfile(structureId, dayCount);

        int min = RSConfig.initialPopulationMin.get();
        int max = RSConfig.initialPopulationMax.get();
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        int targetCount = min == max ? min : random.nextIntBetweenInclusive(Math.max(1, min), Math.max(1, max));

        List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces = start.getPieces();
        boolean usePieces = RSConfig.useStructurePieces.get() && !pieces.isEmpty();

        Map<BlockPos, Integer> candidateMap = new LinkedHashMap<>();
        int totalArea = 0;

        if (usePieces) {
            for (net.minecraft.world.level.levelgen.structure.StructurePiece piece : pieces) {
                BoundingBox pbb = piece.getBoundingBox();
                totalArea += pbb.getXSpan() * pbb.getZSpan();
            }
        }

        for (int attempt = 0; attempt < SpawnPositionFinder.INITIAL_POPULATION_SCAN_ATTEMPTS; attempt++) {
            BoundingBox bb;
            int pieceIndex = -1;
            if (usePieces) {
                if (RSConfig.weightPiecesByArea.get() && totalArea > 0) {
                    int roll = random.nextInt(totalArea);
                    pieceIndex = 0;
                    for (int i = 0; i < pieces.size(); i++) {
                        BoundingBox pbb = pieces.get(i).getBoundingBox();
                        roll -= pbb.getXSpan() * pbb.getZSpan();
                        if (roll < 0) {
                            pieceIndex = i;
                            break;
                        }
                    }
                } else {
                    pieceIndex = random.nextInt(pieces.size());
                }
                bb = pieces.get(pieceIndex).getBoundingBox();
            } else {
                bb = structureBB;
            }

            int x = bb.minX() == bb.maxX() ? bb.minX() : random.nextIntBetweenInclusive(bb.minX(), bb.maxX());
            int z = bb.minZ() == bb.maxZ() ? bb.minZ() : random.nextIntBetweenInclusive(bb.minZ(), bb.maxZ());

            for (int y = bb.maxY(); y >= bb.minY(); y--) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (SpawnPositionFinder.isValidSpawnFloor(level, candidate)) {
                    candidateMap.putIfAbsent(candidate, pieceIndex);
                }
            }
        }

        if (candidateMap.isEmpty()) {
            if (RSConfig.isDebugEnabled()) {
                RunicStructures.LOGGER.warn("[RunicStructures] Initial population failed: no valid positions for structure at [{}, {}, {}]",
                        structureBB.minX(), structureBB.minY(), structureBB.minZ());
            }
            return;
        }

        List<BlockPos> candidatePositions = new ArrayList<>(candidateMap.keySet());
        List<Integer> candidatePieceIndices = new ArrayList<>(candidatePositions.size());
        for (BlockPos pos : candidatePositions) {
            candidatePieceIndices.add(candidateMap.get(pos));
        }

        List<BlockPos> selectedPositions = SpawnPositionFinder.selectDistributedPositions(
                candidatePositions, candidatePieceIndices, targetCount, random);

        int spawned = 0;
        for (BlockPos spawnPos : selectedPositions) {
            EntityType<?> mobType;
            if (profile.hasMobPoolOverride()) {
                mobType = MobSelector.pickFromMobPool(profile.getMobPool(), random);
            } else if (RSConfig.dimensionSpecificMobs.get()) {
                MobSpawnSettings.SpawnerData data = MobSelector.pickBiomeSpecificSpawnerData(level, spawnPos, random);
                mobType = data != null ? data.type : null;
            } else {
                mobType = MobSelector.pickRandomHostileMob(random);
            }
            if (mobType == null) continue;

            if (spawnEnhancedMob(level, mobType, spawnPos, random, profile)) {
                spawned++;
            }
        }

        if (spawned > 0) {
            statInitialPopulations++;
            if (RSConfig.isDebugEnabled()) {
                RunicStructures.LOGGER.info("[RunicStructures] Initial population: spawned {} mobs in structure at [{}, {}, {}]",
                        spawned, structureBB.minX(), structureBB.minY(), structureBB.minZ());
            }
        }
    }

    private static StructureInstanceKey structureKey(ServerLevel level, BoundingBox bb) {
        return new StructureInstanceKey(level.dimension().location(), new BlockPos(bb.minX(), bb.minY(), bb.minZ()));
    }

    private static Iterable<LevelChunk> getLoadedChunks(ServerLevel level) {
        int radius = RSConfig.chunkSearchRadius.get();
        List<LevelChunk> chunks = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        level.players().forEach(player -> {
            BlockPos playerPos = player.blockPosition();
            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = chunkX + dx;
                    int cz = chunkZ + dz;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk != null && seen.add(((long) cx << 32) | (cz & 0xFFFFFFFFL))) {
                        chunks.add(chunk);
                    }
                }
            }
        });
        return chunks;
    }
}
