package com.otectus.dangerousstructures.event;

import com.otectus.dangerousstructures.DangerousStructures;
import com.otectus.dangerousstructures.config.DSConfig;
import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = DangerousStructures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickSpawnHandler {

    private static int tickCounter = 0;

    // Spawn statistics
    private static int statTotalAttempts = 0;
    private static int statSuccessfulSpawns = 0;
    private static int statCapRejects = 0;
    private static int statPositionFailures = 0;
    private static int statInitialPopulations = 0;

    // Initial population tracking — keyed by structure BB origin, cleared each session
    private static final Set<BlockPos> populatedStructures = new HashSet<>();

    // Caches — invalidated on config reload and server stop
    private static volatile List<EntityType<?>> cachedSpawnableMobs = null;
    private static volatile List<Structure> cachedDangerousStructures = null;

    public static int getStatTotalAttempts() { return statTotalAttempts; }
    public static int getStatSuccessfulSpawns() { return statSuccessfulSpawns; }
    public static int getStatCapRejects() { return statCapRejects; }
    public static int getStatPositionFailures() { return statPositionFailures; }
    public static int getStatInitialPopulations() { return statInitialPopulations; }

    public static void invalidateCaches() {
        cachedSpawnableMobs = null;
        cachedDangerousStructures = null;
    }

    public static void onServerStopped() {
        tickCounter = 0;
        statTotalAttempts = 0;
        statSuccessfulSpawns = 0;
        statCapRejects = 0;
        statPositionFailures = 0;
        statInitialPopulations = 0;
        populatedStructures.clear();
        cachedSpawnableMobs = null;
        cachedDangerousStructures = null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        if (!DSConfig.enabled.get()) return;

        boolean periodicEnabled = DSConfig.periodicSpawningEnabled.get();
        boolean initialPopEnabled = DSConfig.initialPopulationEnabled.get();
        if (!periodicEnabled && !initialPopEnabled) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (server.getWorldData().getDifficulty() == Difficulty.PEACEFUL) return;

        // Tick counter logic only applies to periodic spawning
        boolean doPeriodicSpawn = false;
        if (periodicEnabled) {
            int interval = getEffectiveInterval(server.getWorldData().getDifficulty());

            // Night spawn boost: reduce interval at night
            double nightMult = DSConfig.nightSpawnMultiplier.get();
            if (nightMult > 1.0 && server.overworld().isNight()) {
                interval = Math.max(20, (int) (interval / nightMult));
            }

            tickCounter++;
            if (tickCounter >= interval) {
                // Apply jitter: reset counter to a random offset instead of 0
                if (DSConfig.randomizeInterval.get()) {
                    RandomSource random = server.overworld().getRandom();
                    tickCounter = -(random.nextInt(Math.max(1, interval / 4)));
                } else {
                    tickCounter = 0;
                }
                doPeriodicSpawn = true;
            }
        }

        // Skip structure iteration if nothing needs to run this tick
        if (!doPeriodicSpawn && !initialPopEnabled) return;

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

                        // Initial population: one-time spawn when structure is first seen this session
                        if (initialPopEnabled) {
                            BlockPos originKey = new BlockPos(
                                    start.getBoundingBox().minX(),
                                    start.getBoundingBox().minY(),
                                    start.getBoundingBox().minZ());
                            if (populatedStructures.add(originKey)) {
                                tryInitialPopulation(level, start);
                            }
                        }

                        if (doPeriodicSpawn) {
                            tryPeriodicSpawn(level, start);
                        }
                    }
                }
            }
        }
    }

    /**
     * Compute the effective spawn interval after difficulty scaling.
     */
    private static int getEffectiveInterval(Difficulty difficulty) {
        int base = DSConfig.periodicSpawnInterval.get();
        if (!DSConfig.difficultyScalingEnabled.get()) return base;

        double multiplier = switch (difficulty) {
            case EASY -> DSConfig.easyIntervalMultiplier.get();
            case NORMAL -> DSConfig.normalIntervalMultiplier.get();
            case HARD -> DSConfig.hardIntervalMultiplier.get();
            default -> 1.0;
        };
        return Math.max(20, (int) (base * multiplier));
    }

    /**
     * Compute the effective spawn cap after difficulty and structure-size scaling.
     */
    private static int getEffectiveCap(Difficulty difficulty, BoundingBox bb) {
        int base = DSConfig.periodicSpawnCap.get();

        // Difficulty scaling
        if (DSConfig.difficultyScalingEnabled.get()) {
            double multiplier = switch (difficulty) {
                case EASY -> DSConfig.easyCapMultiplier.get();
                case NORMAL -> DSConfig.normalCapMultiplier.get();
                case HARD -> DSConfig.hardCapMultiplier.get();
                default -> 1.0;
            };
            base = Math.max(1, (int) (base * multiplier));
        }

        // Structure size scaling
        if (DSConfig.scaleCapByStructureSize.get()) {
            int chunkArea = (bb.getXSpan() / 16 + 1) * (bb.getZSpan() / 16 + 1);
            int scaledCap = chunkArea * DSConfig.mobsPerChunkArea.get();
            base = Math.min(DSConfig.maxScaledCap.get(), Math.max(base, scaledCap));
        }

        return base;
    }

    private static List<Structure> resolveDangerousStructures(Registry<Structure> registry) {
        List<Structure> cached = cachedDangerousStructures;
        if (cached != null) return cached;

        List<Structure> result;
        if (DSConfig.autoDetectStructures.get()) {
            // Use shared cache from StructureDetection
            result = StructureDetection.getDangerousStructures(registry);
        } else {
            // Manual mode: use whitelist + API-registered structures
            List<Structure> building = new ArrayList<>();
            for (ResourceLocation rl : DSConfig.getStructureWhitelist()) {
                Structure s = registry.get(rl);
                if (s != null) building.add(s);
            }
            for (ResourceLocation tagRL : DSConfig.getStructureTagWhitelist()) {
                TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagRL);
                for (Holder<Structure> holder : registry.getTagOrEmpty(tagKey)) {
                    building.add(holder.value());
                }
            }
            for (ResourceLocation rl : com.otectus.dangerousstructures.api.DangerousStructuresAPI.getRegisteredStructuresInternal()) {
                Structure s = registry.get(rl);
                if (s != null) building.add(s);
            }
            result = List.copyOf(building);
        }

        cachedDangerousStructures = result;
        return result;
    }

    private static void tryPeriodicSpawn(ServerLevel level, StructureStart start) {
        BoundingBox structureBB = start.getBoundingBox();
        Difficulty difficulty = level.getServer().getWorldData().getDifficulty();
        int cap = getEffectiveCap(difficulty, structureBB);

        // Count existing monsters inside the bounding box (early-exit when cap reached)
        var entities = level.getEntities(null, new net.minecraft.world.phys.AABB(
                structureBB.minX(), structureBB.minY(), structureBB.minZ(),
                structureBB.maxX() + 1, structureBB.maxY() + 1, structureBB.maxZ() + 1
        ));
        int existingMonsters = 0;
        for (var e : entities) {
            if (e.getType().getCategory() == MobCategory.MONSTER) {
                if (++existingMonsters >= cap) {
                    statCapRejects++;
                    return;
                }
            }
        }

        statTotalAttempts++;
        RandomSource random = level.getRandom();
        int maxAttempts = DSConfig.maxPositionAttempts.get();
        int minPlayerDistSq = DSConfig.minPlayerDistance.get() * DSConfig.minPlayerDistance.get();

        // Select the bounding box to spawn within (structure pieces or full BB)
        List<BoundingBox> spawnBoxes = null;
        List<Integer> spawnBoxAreas = null;
        int totalArea = 0;

        if (DSConfig.useStructurePieces.get() && !start.getPieces().isEmpty()) {
            spawnBoxes = new ArrayList<>();
            spawnBoxAreas = new ArrayList<>();
            for (StructurePiece piece : start.getPieces()) {
                BoundingBox pieceBB = piece.getBoundingBox();
                int area = pieceBB.getXSpan() * pieceBB.getZSpan();
                spawnBoxes.add(pieceBB);
                spawnBoxAreas.add(area);
                totalArea += area;
            }
        }

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Select which bounding box to use for this attempt
            BoundingBox bb;
            if (spawnBoxes != null && DSConfig.weightPiecesByArea.get() && totalArea > 0) {
                // Weighted random selection by floor area
                int roll = random.nextInt(totalArea);
                int idx = 0;
                for (int i = 0; i < spawnBoxAreas.size(); i++) {
                    roll -= spawnBoxAreas.get(i);
                    if (roll < 0) { idx = i; break; }
                }
                bb = spawnBoxes.get(idx);
            } else if (spawnBoxes != null) {
                bb = spawnBoxes.get(random.nextInt(spawnBoxes.size()));
            } else {
                bb = structureBB;
            }

            // Column-based floor finding: pick random (x,z), scan downward for valid floors
            int x = random.nextIntBetweenInclusive(bb.minX(), bb.maxX());
            int z = random.nextIntBetweenInclusive(bb.minZ(), bb.maxZ());

            List<BlockPos> validFloors = new ArrayList<>();
            for (int y = bb.maxY(); y >= bb.minY(); y--) {
                BlockPos candidate = new BlockPos(x, y, z);
                BlockPos below = candidate.below();

                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (!level.getBlockState(candidate).isAir()) continue;
                if (!level.getBlockState(candidate.above()).isAir()) continue;

                // Hazard avoidance: skip lava floors
                FluidState fluidBelow = level.getFluidState(below);
                if (fluidBelow.is(FluidTags.LAVA)) continue;

                // Hazard avoidance: skip positions in water
                if (level.getFluidState(candidate).is(FluidTags.WATER)) continue;

                // Skip sky-exposed positions to prevent rooftop spawning
                if (level.canSeeSky(candidate)) continue;

                validFloors.add(candidate);
            }
            if (validFloors.isEmpty()) continue;

            // Pick a random floor from the column (distributes across multi-story structures)
            BlockPos spawnPos = validFloors.get(random.nextInt(validFloors.size()));

            // Minimum player distance check
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

            // Pick mob type
            MobSpawnSettings.SpawnerData selectedSpawnerData = null;
            EntityType<?> mobType;
            if (DSConfig.dimensionSpecificMobs.get()) {
                selectedSpawnerData = pickBiomeSpecificSpawnerData(level, spawnPos, random);
                mobType = selectedSpawnerData != null ? selectedSpawnerData.type : null;
            } else {
                mobType = pickRandomHostileMob(random);
            }
            if (mobType == null) continue;

            // Determine pack size
            int packSize = 1;
            if (DSConfig.packSpawning.get()) {
                int maxPack = DSConfig.maxPackSize.get();
                if (selectedSpawnerData != null) {
                    packSize = random.nextIntBetweenInclusive(
                            selectedSpawnerData.minCount,
                            Math.min(selectedSpawnerData.maxCount, maxPack));
                } else {
                    packSize = random.nextIntBetweenInclusive(1, Math.min(2, maxPack));
                }
            }

            // Spawn the pack
            int spawned = 0;
            for (int p = 0; p < packSize && (existingMonsters + spawned) < cap; p++) {
                // Pack members spawn near the first position (offset 0-3 blocks)
                BlockPos packPos;
                if (p == 0) {
                    packPos = spawnPos;
                } else {
                    packPos = findNearbyFloor(level, spawnPos, random, 4);
                    if (packPos == null) continue;
                }

                var entity = mobType.create(level);
                if (entity == null) continue;

                entity.moveTo(packPos.getX() + 0.5, packPos.getY(), packPos.getZ() + 0.5,
                        random.nextFloat() * 360.0F, 0.0F);

                if (entity instanceof Mob mob) {
                    var spawnResult = ForgeEventFactory.onFinalizeSpawn(mob, level,
                            level.getCurrentDifficultyAt(packPos), MobSpawnType.NATURAL, null, null);
                    if (spawnResult == null) {
                        entity.discard();
                        continue;
                    }

                    // Persistence
                    if (DSConfig.persistentMobs.get()) {
                        mob.setPersistenceRequired();
                    }

                    // Elite mob enhancement
                    if (DSConfig.eliteSpawnsEnabled.get() && random.nextDouble() < DSConfig.eliteSpawnChance.get()) {
                        applyEliteEnhancements(mob);
                    }

                    // Direct armor & sunlight immunity — periodic spawner knows the mob
                    // is inside a structure, so apply without re-checking detection
                    if (DSConfig.armorEnabled.get()) {
                        applyRandomArmor(mob, random);
                    }
                    if (DSConfig.sunlightImmunity.get()) {
                        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                                Integer.MAX_VALUE, 0, true, false, false));
                    }
                }

                if (level.addFreshEntity(entity)) {
                    spawned++;
                    statSuccessfulSpawns++;
                    if (DSConfig.debugLogging.get()) {
                        DangerousStructures.LOGGER.info("[DangerousStructures] Periodic spawn of {} at {}",
                                mobType.getDescriptionId(), packPos);
                    }
                } else {
                    entity.discard();
                }
            }
            if (spawned > 0) return; // Successfully spawned, done with this structure
        }
        statPositionFailures++;
    }

    /**
     * Spawn an initial group of mobs throughout a structure the first time it's discovered.
     * Uses more aggressive position-finding than the periodic spawner and skips
     * minPlayerDistance to guarantee mob presence.
     */
    private static void tryInitialPopulation(ServerLevel level, StructureStart start) {
        BoundingBox structureBB = start.getBoundingBox();
        RandomSource random = level.getRandom();

        int min = DSConfig.initialPopulationMin.get();
        int max = DSConfig.initialPopulationMax.get();
        if (min > max) { int tmp = min; min = max; max = tmp; }
        int targetCount = random.nextIntBetweenInclusive(min, max);

        List<StructurePiece> pieces = start.getPieces();
        boolean usePieces = DSConfig.useStructurePieces.get() && !pieces.isEmpty();

        // Phase 1: Collect valid floor positions across many scan attempts
        List<BlockPos> candidatePositions = new ArrayList<>();
        List<Integer> candidatePieceIndices = new ArrayList<>();
        int totalArea = 0;

        if (usePieces) {
            for (StructurePiece piece : pieces) {
                BoundingBox pbb = piece.getBoundingBox();
                totalArea += pbb.getXSpan() * pbb.getZSpan();
            }
        }

        for (int attempt = 0; attempt < 50; attempt++) {
            BoundingBox bb;
            int pieceIndex = -1;
            if (usePieces) {
                if (DSConfig.weightPiecesByArea.get() && totalArea > 0) {
                    int roll = random.nextInt(totalArea);
                    pieceIndex = 0;
                    for (int i = 0; i < pieces.size(); i++) {
                        BoundingBox pbb = pieces.get(i).getBoundingBox();
                        roll -= pbb.getXSpan() * pbb.getZSpan();
                        if (roll < 0) { pieceIndex = i; break; }
                    }
                } else {
                    pieceIndex = random.nextInt(pieces.size());
                }
                bb = pieces.get(pieceIndex).getBoundingBox();
            } else {
                bb = structureBB;
            }

            int x = random.nextIntBetweenInclusive(bb.minX(), bb.maxX());
            int z = random.nextIntBetweenInclusive(bb.minZ(), bb.maxZ());

            for (int y = bb.maxY(); y >= bb.minY(); y--) {
                BlockPos candidate = new BlockPos(x, y, z);
                BlockPos below = candidate.below();

                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (!level.getBlockState(candidate).isAir()) continue;
                if (!level.getBlockState(candidate.above()).isAir()) continue;
                if (level.getFluidState(below).is(FluidTags.LAVA)) continue;
                if (level.getFluidState(candidate).is(FluidTags.WATER)) continue;
                if (level.canSeeSky(candidate)) continue;

                candidatePositions.add(candidate);
                candidatePieceIndices.add(pieceIndex);
            }
        }

        if (candidatePositions.isEmpty()) {
            if (DSConfig.debugLogging.get()) {
                DangerousStructures.LOGGER.warn("[DangerousStructures] Initial population failed: no valid positions for structure at [{}, {}, {}]",
                        structureBB.minX(), structureBB.minY(), structureBB.minZ());
            }
            return;
        }

        // Phase 2: Select well-distributed positions
        List<BlockPos> selectedPositions = selectDistributedPositions(
                candidatePositions, candidatePieceIndices, targetCount, random);

        // Phase 3: Spawn mobs at selected positions
        int spawned = 0;
        for (BlockPos spawnPos : selectedPositions) {
            MobSpawnSettings.SpawnerData selectedSpawnerData = null;
            EntityType<?> mobType;
            if (DSConfig.dimensionSpecificMobs.get()) {
                selectedSpawnerData = pickBiomeSpecificSpawnerData(level, spawnPos, random);
                mobType = selectedSpawnerData != null ? selectedSpawnerData.type : null;
            } else {
                mobType = pickRandomHostileMob(random);
            }
            if (mobType == null) continue;

            var entity = mobType.create(level);
            if (entity == null) continue;

            entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);

            if (entity instanceof Mob mob) {
                var spawnResult = ForgeEventFactory.onFinalizeSpawn(mob, level,
                        level.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null, null);
                if (spawnResult == null) {
                    entity.discard();
                    continue;
                }

                if (DSConfig.persistentMobs.get()) {
                    mob.setPersistenceRequired();
                }

                if (DSConfig.eliteSpawnsEnabled.get() && random.nextDouble() < DSConfig.eliteSpawnChance.get()) {
                    applyEliteEnhancements(mob);
                }

                if (DSConfig.armorEnabled.get()) {
                    applyRandomArmor(mob, random);
                }
                if (DSConfig.sunlightImmunity.get()) {
                    mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                            Integer.MAX_VALUE, 0, true, false, false));
                }
            }

            if (level.addFreshEntity(entity)) {
                spawned++;
                statSuccessfulSpawns++;
            } else {
                entity.discard();
            }
        }

        if (spawned > 0) {
            statInitialPopulations++;
            if (DSConfig.debugLogging.get()) {
                DangerousStructures.LOGGER.info("[DangerousStructures] Initial population: spawned {} mobs in structure at [{}, {}, {}]",
                        spawned, structureBB.minX(), structureBB.minY(), structureBB.minZ());
            }
        }
    }

    /**
     * Select well-distributed positions from the candidate pool.
     * Round-robins across structure pieces, picking the farthest candidate from
     * already-selected positions within each piece.
     */
    private static List<BlockPos> selectDistributedPositions(
            List<BlockPos> candidates, List<Integer> pieceIndices,
            int targetCount, RandomSource random) {

        if (candidates.size() <= targetCount) {
            return new ArrayList<>(candidates);
        }

        // Group candidate indices by piece
        Map<Integer, List<Integer>> byPiece = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            byPiece.computeIfAbsent(pieceIndices.get(i), k -> new ArrayList<>()).add(i);
        }

        List<BlockPos> selected = new ArrayList<>();
        List<Integer> pieceKeys = new ArrayList<>(byPiece.keySet());
        Collections.shuffle(pieceKeys, new java.util.Random(random.nextLong()));

        int piecePtr = 0;
        while (selected.size() < targetCount && !pieceKeys.isEmpty()) {
            int pieceKey = pieceKeys.get(piecePtr % pieceKeys.size());
            List<Integer> pieceCandidates = byPiece.get(pieceKey);

            if (pieceCandidates.isEmpty()) {
                pieceKeys.remove(piecePtr % pieceKeys.size());
                continue;
            }

            int bestIdx;
            if (selected.isEmpty()) {
                bestIdx = pieceCandidates.remove(random.nextInt(pieceCandidates.size()));
            } else {
                // Pick the candidate farthest from all already-selected positions
                int bestListIdx = 0;
                double bestMinDist = -1;
                for (int li = 0; li < pieceCandidates.size(); li++) {
                    BlockPos cand = candidates.get(pieceCandidates.get(li));
                    double minDist = Double.MAX_VALUE;
                    for (BlockPos sel : selected) {
                        double d = cand.distSqr(sel);
                        if (d < minDist) minDist = d;
                    }
                    if (minDist > bestMinDist) {
                        bestMinDist = minDist;
                        bestListIdx = li;
                    }
                }
                bestIdx = pieceCandidates.remove(bestListIdx);
            }

            selected.add(candidates.get(bestIdx));
            piecePtr++;
        }

        return selected;
    }

    /**
     * Apply elite enhancements to a mob: boosted stats, glow, custom name.
     */
    private static void applyEliteEnhancements(Mob mob) {
        // Boost health
        AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * DSConfig.eliteHealthMultiplier.get();
            healthAttr.setBaseValue(newHealth);
            mob.setHealth((float) newHealth);
        }

        // Boost damage
        AttributeInstance damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * DSConfig.eliteDamageMultiplier.get());
        }

        // Glowing effect
        if (DSConfig.eliteGlowingEffect.get()) {
            mob.setGlowingTag(true);
        }

        // Custom name
        String prefix = DSConfig.eliteNamePrefix.get();
        if (prefix != null && !prefix.isEmpty()) {
            String mobName = mob.getType().getDescription().getString();
            mob.setCustomName(Component.literal(prefix + " " + mobName));
            mob.setCustomNameVisible(true);
        }

        // Persistence (elites should always persist)
        mob.setPersistenceRequired();

        // Tag the mob for bonus XP identification
        mob.addTag("ds_elite");
    }

    /**
     * Equip a mob with a full suit of randomized armor from the configured tier pool.
     * Each armor slot gets an independently randomized material tier.
     */
    public static void applyRandomArmor(Mob mob, RandomSource random) {
        // Build the pool of enabled armor tiers: [helmet, chestplate, leggings, boots]
        List<Item[]> tiers = new ArrayList<>();
        if (DSConfig.allowLeather.get())    tiers.add(new Item[]{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS});
        if (DSConfig.allowChainmail.get())  tiers.add(new Item[]{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS});
        if (DSConfig.allowIron.get())       tiers.add(new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS});
        if (DSConfig.allowGold.get())       tiers.add(new Item[]{Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS});
        if (DSConfig.allowDiamond.get())    tiers.add(new Item[]{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS});
        if (DSConfig.allowNetherite.get())  tiers.add(new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS});
        if (tiers.isEmpty()) return;

        float dropChance = DSConfig.armorDropChance.get().floatValue();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        for (int i = 0; i < slots.length; i++) {
            Item[] tier = tiers.get(random.nextInt(tiers.size()));
            mob.setItemSlot(slots[i], new ItemStack(tier[i]));
            mob.setDropChance(slots[i], dropChance);
        }
    }

    /**
     * Find a valid floor position near the given origin, within the given radius.
     */
    private static BlockPos findNearbyFloor(ServerLevel level, BlockPos origin, RandomSource random, int radius) {
        for (int i = 0; i < 4; i++) {
            int dx = random.nextIntBetweenInclusive(-radius, radius);
            int dz = random.nextIntBetweenInclusive(-radius, radius);
            BlockPos candidate = new BlockPos(origin.getX() + dx, origin.getY(), origin.getZ() + dz);

            // Check the Y level and one above/below
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos pos = candidate.offset(0, dy, 0);
                BlockPos below = pos.below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                        && level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.above()).isAir()
                        && !level.getFluidState(below).is(FluidTags.LAVA)
                        && !level.getFluidState(pos).is(FluidTags.WATER)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Pick a biome-specific spawner data entry, respecting vanilla spawn weights.
     */
    private static MobSpawnSettings.SpawnerData pickBiomeSpecificSpawnerData(
            ServerLevel level, BlockPos pos, RandomSource random) {
        List<MobSpawnSettings.SpawnerData> spawners = level.getBiome(pos).value()
                .getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        if (spawners.isEmpty()) return null;

        List<MobSpawnSettings.SpawnerData> eligible = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData data : spawners) {
            if (DSConfig.isEntityAllowed(data.type)) {
                eligible.add(data);
            }
        }
        if (eligible.isEmpty()) return null;

        if (DSConfig.useVanillaSpawnWeights.get()) {
            // Weighted random selection
            int totalWeight = 0;
            for (MobSpawnSettings.SpawnerData data : eligible) {
                totalWeight += data.getWeight().asInt();
            }
            if (totalWeight <= 0) return eligible.get(random.nextInt(eligible.size()));

            int roll = random.nextInt(totalWeight);
            for (MobSpawnSettings.SpawnerData data : eligible) {
                roll -= data.getWeight().asInt();
                if (roll < 0) return data;
            }
            return eligible.get(eligible.size() - 1);
        } else {
            return eligible.get(random.nextInt(eligible.size()));
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
        int radius = DSConfig.chunkSearchRadius.get();
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
