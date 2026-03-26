package com.otectus.dangerousstructures.api;

import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API for other mods to interact with Dangerous Structures.
 * <p>
 * Use these methods to programmatically register structures as dangerous,
 * or to query whether a position is in a dangerous structure.
 */
public class DangerousStructuresAPI {

    private static final Set<ResourceLocation> apiRegisteredStructures = ConcurrentHashMap.newKeySet();

    /**
     * Register a structure as dangerous via the API.
     * This is checked alongside config-based structure lists.
     *
     * @param structureId the registry ID of the structure (e.g., "mymod:my_dungeon")
     */
    public static void registerDangerousStructure(ResourceLocation structureId) {
        apiRegisteredStructures.add(structureId);
        StructureDetection.invalidateCache();
    }

    /**
     * Unregister a structure that was previously registered via the API.
     *
     * @param structureId the registry ID of the structure
     */
    public static void unregisterDangerousStructure(ResourceLocation structureId) {
        apiRegisteredStructures.remove(structureId);
        StructureDetection.invalidateCache();
    }

    /**
     * Check if a position is inside a dangerous structure.
     *
     * @param level the server level
     * @param pos   the block position to check
     * @return true if the position is inside a dangerous structure
     */
    public static boolean isPositionDangerous(ServerLevel level, BlockPos pos) {
        return StructureDetection.isInDangerousStructure(level, pos);
    }

    /**
     * Get all API-registered dangerous structure IDs (unmodifiable view).
     */
    public static Set<ResourceLocation> getApiRegisteredStructures() {
        return Collections.unmodifiableSet(apiRegisteredStructures);
    }

    /**
     * Internal: used by StructureDetection to include API-registered structures.
     */
    public static Set<ResourceLocation> getRegisteredStructuresInternal() {
        return apiRegisteredStructures;
    }
}
