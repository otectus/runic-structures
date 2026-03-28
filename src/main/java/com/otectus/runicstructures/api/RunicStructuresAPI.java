package com.otectus.runicstructures.api;

import com.otectus.runicstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API for other mods to interact with Runic Structures.
 * <p>
 * Use these methods to programmatically register structures as runic,
 * or to query whether a position is in a runic structure.
 */
public class RunicStructuresAPI {

    private static final Set<ResourceLocation> apiRegisteredStructures = ConcurrentHashMap.newKeySet();

    /**
     * Register a structure as runic via the API.
     * This is checked alongside config-based structure lists.
     *
     * @param structureId the registry ID of the structure (e.g., "mymod:my_dungeon")
     */
    public static void registerRunicStructure(ResourceLocation structureId) {
        apiRegisteredStructures.add(structureId);
        StructureDetection.invalidateCache();
    }

    /**
     * Unregister a structure that was previously registered via the API.
     *
     * @param structureId the registry ID of the structure
     */
    public static void unregisterRunicStructure(ResourceLocation structureId) {
        apiRegisteredStructures.remove(structureId);
        StructureDetection.invalidateCache();
    }

    /**
     * Check if a position is inside a runic structure.
     *
     * @param level the server level
     * @param pos   the block position to check
     * @return true if the position is inside a runic structure
     */
    public static boolean isPositionRunic(ServerLevel level, BlockPos pos) {
        return StructureDetection.isInRunicStructure(level, pos);
    }

    /**
     * Get all API-registered runic structure IDs (unmodifiable view).
     */
    public static Set<ResourceLocation> getApiRegisteredStructures() {
        return Collections.unmodifiableSet(apiRegisteredStructures);
    }
}
