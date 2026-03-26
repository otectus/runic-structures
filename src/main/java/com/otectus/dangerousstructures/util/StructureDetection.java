package com.otectus.dangerousstructures.util;

import com.otectus.dangerousstructures.api.DangerousStructuresAPI;
import com.otectus.dangerousstructures.config.DSConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StructureDetection {

    private static final TagKey<Structure> VILLAGE_TAG =
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "village"));

    // Cache of non-excluded structures — invalidated on config reload and server stop
    private static volatile List<Structure> cachedNonExcludedStructures = null;

    public static void invalidateCache() {
        cachedNonExcludedStructures = null;
    }

    public static void onServerStopped() {
        cachedNonExcludedStructures = null;
    }

    /**
     * Check if a structure should be excluded based on blacklist and village settings.
     * Used by both StructureDetection and TickSpawnHandler.
     */
    public static boolean isStructureExcluded(Registry<Structure> registry, Structure structure) {
        var keyOpt = registry.getResourceKey(structure);
        if (keyOpt.isEmpty()) return false;

        ResourceLocation structureId = keyOpt.get().location();
        Holder<Structure> holder = registry.getHolderOrThrow(keyOpt.get());

        // Check structure ID blacklist
        if (DSConfig.getStructureBlacklist().contains(structureId)) {
            return true;
        }

        // Check structure tag blacklist
        for (ResourceLocation tagRL : DSConfig.getStructureTagBlacklist()) {
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagRL);
            if (holder.is(tagKey)) {
                return true;
            }
        }

        // Check village exclusion (only in auto-detect mode)
        if (DSConfig.autoDetectStructures.get() && !DSConfig.includeVillages.get()) {
            if (holder.is(VILLAGE_TAG)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isInDangerousStructure(ServerLevelAccessor level, BlockPos pos) {
        if (!DSConfig.isDimensionAllowed(level.getLevel().dimension())) {
            return false;
        }

        StructureManager structureManager = level.getLevel().structureManager();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        int buffer = DSConfig.bufferRadius.get();

        if (DSConfig.autoDetectStructures.get()) {
            return isInAnyStructure(structureManager, registry, pos, buffer);
        }

        // Manual mode: check whitelisted structures
        for (ResourceLocation rl : DSConfig.getStructureWhitelist()) {
            Structure structure = registry.get(rl);
            if (structure == null) continue;

            if (isInsideStructure(structureManager, structure, pos, buffer)) {
                return true;
            }
        }

        // Check whitelisted structure tags
        for (ResourceLocation tagRL : DSConfig.getStructureTagWhitelist()) {
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagRL);
            for (Holder<Structure> holder : registry.getTagOrEmpty(tagKey)) {
                if (isInsideStructure(structureManager, holder.value(), pos, buffer)) {
                    return true;
                }
            }
        }

        // Check API-registered structures
        for (ResourceLocation rl : DangerousStructuresAPI.getRegisteredStructuresInternal()) {
            Structure structure = registry.get(rl);
            if (structure == null) continue;
            if (isInsideStructure(structureManager, structure, pos, buffer)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the cached list of all non-excluded (dangerous) structures.
     * Used by both StructureDetection and TickSpawnHandler to share the same cache.
     */
    public static List<Structure> getDangerousStructures(Registry<Structure> registry) {
        List<Structure> structures = cachedNonExcludedStructures;
        if (structures == null) {
            List<Structure> building = new ArrayList<>();
            for (var entry : registry.entrySet()) {
                if (!isStructureExcluded(registry, entry.getValue())) {
                    building.add(entry.getValue());
                }
            }
            structures = List.copyOf(building);
            cachedNonExcludedStructures = structures;
        }
        return structures;
    }

    private static boolean isInAnyStructure(StructureManager structureManager,
            Registry<Structure> registry, BlockPos pos, int buffer) {
        List<Structure> structures = getDangerousStructures(registry);

        // Fast-reject: if no structure at all contains this position, skip per-structure iteration
        if (buffer == 0 && !structureManager.hasAnyStructureAt(pos)) {
            return false;
        }

        for (Structure structure : structures) {
            if (isInsideStructure(structureManager, structure, pos, buffer)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideStructure(StructureManager structureManager,
            Structure structure, BlockPos pos, int buffer) {
        if (buffer == 0) {
            StructureStart start = structureManager.getStructureAt(pos, structure);
            return start != StructureStart.INVALID_START;
        }

        // For buffer > 0, first try exact match (fast path)
        StructureStart start = structureManager.getStructureAt(pos, structure);
        if (start != StructureStart.INVALID_START) return true;

        // Check nearby structure starts with expanded bounding box
        SectionPos sectionPos = SectionPos.of(pos);
        for (StructureStart nearby : structureManager.startsForStructure(sectionPos, structure)) {
            if (nearby == StructureStart.INVALID_START) continue;
            BoundingBox bb = nearby.getBoundingBox();
            if (pos.getX() >= bb.minX() - buffer && pos.getX() <= bb.maxX() + buffer
                    && pos.getY() >= bb.minY() - buffer && pos.getY() <= bb.maxY() + buffer
                    && pos.getZ() >= bb.minZ() - buffer && pos.getZ() <= bb.maxZ() + buffer) {
                return true;
            }
        }
        return false;
    }
}
