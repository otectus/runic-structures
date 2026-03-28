package com.otectus.runicstructures.util;

import com.otectus.runicstructures.api.RunicStructuresAPI;
import com.otectus.runicstructures.config.RSConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StructureDetection {

    public record RunicStructureRef(ResourceLocation id, Structure structure) {}

    private static final TagKey<Structure> VILLAGE_TAG =
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "village"));

    // Cached immutable runic structure list — invalidated on config reload, API updates, and server stop
    private static volatile List<RunicStructureRef> cachedRunicStructures = null;

    public static void invalidateCache() {
        cachedRunicStructures = null;
    }

    public static void onServerStopped() {
        cachedRunicStructures = null;
    }

    /**
     * Check if a structure should be excluded based on blacklist and village settings.
     */
    public static boolean isStructureExcluded(Registry<Structure> registry, Structure structure) {
        var keyOpt = registry.getResourceKey(structure);
        if (keyOpt.isEmpty()) return false;
        Holder<Structure> holder = registry.getHolderOrThrow(keyOpt.get());
        return isStructureExcluded(holder, keyOpt.get().location());
    }

    public static boolean isStructureRunic(Registry<Structure> registry, Structure structure) {
        var keyOpt = registry.getResourceKey(structure);
        if (keyOpt.isEmpty()) return false;
        Holder<Structure> holder = registry.getHolderOrThrow(keyOpt.get());
        return isStructureRunic(holder, keyOpt.get().location());
    }

    /**
     * Get the ResourceLocation of the runic structure at this position, or null if none.
     */
    @Nullable
    public static ResourceLocation getRunicStructureId(ServerLevelAccessor level, BlockPos pos) {
        if (!RSConfig.isDimensionAllowed(level.getLevel().dimension())) {
            return null;
        }

        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        StructureManager structureManager = level.getLevel().structureManager();
        int buffer = RSConfig.bufferRadius.get();

        for (RunicStructureRef ref : getRunicStructures(registry)) {
            if (isInsideStructure(structureManager, ref.structure(), pos, buffer)) {
                return ref.id();
            }
        }

        return null;
    }

    /** Boolean convenience — delegates to the identity-returning method. */
    public static boolean isInRunicStructure(ServerLevelAccessor level, BlockPos pos) {
        return getRunicStructureId(level, pos) != null;
    }

    /**
     * Get the cached list of all runic structures after applying inclusion and exclusion rules.
     */
    public static List<RunicStructureRef> getRunicStructures(Registry<Structure> registry) {
        List<RunicStructureRef> cached = cachedRunicStructures;
        if (cached != null) return cached;

        Map<ResourceLocation, Structure> building = new LinkedHashMap<>();
        if (RSConfig.autoDetectStructures.get()) {
            for (var entry : registry.entrySet()) {
                ResourceLocation structureId = entry.getKey().location();
                Holder<Structure> holder = registry.getHolderOrThrow(entry.getKey());
                if (isStructureRunic(holder, structureId)) {
                    building.put(structureId, entry.getValue());
                }
            }
        } else {
            for (ResourceLocation rl : RSConfig.getStructureWhitelist()) {
                Structure structure = registry.get(rl);
                if (structure != null && isStructureRunic(registry, structure)) {
                    building.put(rl, structure);
                }
            }

            for (ResourceLocation tagRL : RSConfig.getStructureTagWhitelist()) {
                TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagRL);
                for (Holder<Structure> holder : registry.getTagOrEmpty(tagKey)) {
                    ResourceLocation structureId = registry.getKey(holder.value());
                    if (structureId != null && isStructureRunic(holder, structureId)) {
                        building.put(structureId, holder.value());
                    }
                }
            }

            for (ResourceLocation rl : RunicStructuresAPI.getApiRegisteredStructures()) {
                Structure structure = registry.get(rl);
                if (structure != null && isStructureRunic(registry, structure)) {
                    building.put(rl, structure);
                }
            }
        }

        List<RunicStructureRef> result = new ArrayList<>(building.size());
        for (Map.Entry<ResourceLocation, Structure> entry : building.entrySet()) {
            result.add(new RunicStructureRef(entry.getKey(), entry.getValue()));
        }

        cached = List.copyOf(result);
        cachedRunicStructures = cached;
        return cached;
    }

    private static boolean isStructureRunic(Holder<Structure> holder, ResourceLocation structureId) {
        if (isStructureExcluded(holder, structureId)) {
            return false;
        }

        if (RSConfig.autoDetectStructures.get()) {
            return true;
        }

        if (RSConfig.getStructureWhitelist().contains(structureId)) {
            return true;
        }

        for (ResourceLocation tagRL : RSConfig.getStructureTagWhitelist()) {
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagRL);
            if (holder.is(tagKey)) {
                return true;
            }
        }

        return RunicStructuresAPI.getApiRegisteredStructures().contains(structureId);
    }

    private static boolean isStructureExcluded(Holder<Structure> holder, ResourceLocation structureId) {
        if (RSConfig.getStructureBlacklist().contains(structureId)) {
            return true;
        }

        for (ResourceLocation tagRL : RSConfig.getStructureTagBlacklist()) {
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagRL);
            if (holder.is(tagKey)) {
                return true;
            }
        }

        return RSConfig.autoDetectStructures.get() && !RSConfig.includeVillages.get() && holder.is(VILLAGE_TAG);
    }

    private static boolean isInsideStructure(StructureManager structureManager, Structure structure, BlockPos pos, int buffer) {
        if (buffer <= 0) {
            if (!structureManager.hasAnyStructureAt(pos)) {
                return false;
            }
            return structureManager.getStructureAt(pos, structure) != StructureStart.INVALID_START;
        }

        StructureStart direct = structureManager.getStructureAt(pos, structure);
        if (direct != StructureStart.INVALID_START) {
            return true;
        }

        int minChunkX = SectionPos.blockToSectionCoord(pos.getX() - buffer);
        int maxChunkX = SectionPos.blockToSectionCoord(pos.getX() + buffer);
        int minChunkZ = SectionPos.blockToSectionCoord(pos.getZ() - buffer);
        int maxChunkZ = SectionPos.blockToSectionCoord(pos.getZ() + buffer);

        Set<StructureStart> seen = new HashSet<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (StructureStart start : structureManager.startsForStructure(new ChunkPos(chunkX, chunkZ), candidate -> candidate == structure)) {
                    if (start == StructureStart.INVALID_START || !seen.add(start)) continue;

                    BoundingBox bb = start.getBoundingBox();
                    if (pos.getX() >= bb.minX() - buffer && pos.getX() <= bb.maxX() + buffer
                            && pos.getY() >= bb.minY() - buffer && pos.getY() <= bb.maxY() + buffer
                            && pos.getZ() >= bb.minZ() - buffer && pos.getZ() <= bb.maxZ() + buffer) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
