package com.otectus.runicstructures.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.otectus.runicstructures.RunicStructures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads per-structure override profiles from a JSON file alongside the TOML config.
 * File location: {@code <world>/serverconfig/runicstructures-structures.json}.
 */
public class StructureProfileLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DEFAULT_RESOURCE_PATH = "/defaults/runicstructures-structures.json";

    private static volatile Map<ResourceLocation, StructureProfile> profiles = Collections.emptyMap();
    private static volatile List<String> validationWarnings = List.of();
    private static Path currentPath = null;

    private static final class RawStructureProfile {
        @Nullable Integer spawnCap;
        @Nullable Double eliteChance;
        @Nullable String eliteNamePrefix;
        @Nullable Boolean persistentMobs;
        @Nullable List<String> armorTiers;
        @Nullable List<String> weaponPool;
        @Nullable Double shieldChance;
        @Nullable List<String> shieldPool;
        @Nullable Integer enchantmentLevel;
        @Nullable Boolean darkness;
        @Nullable Boolean miningFatigue;
        @Nullable Boolean slowness;
        @Nullable Boolean ambientSounds;
        @Nullable String progressionTier;
    }

    /**
     * Get the profile for a structure, falling back to DEFAULT if none is defined.
     * If the profile has a progressionTier requirement that is not met, returns DEFAULT.
     */
    public static StructureProfile getProfile(@Nullable ResourceLocation structureId, long serverDayCount) {
        if (structureId == null) return StructureProfile.DEFAULT;
        StructureProfile profile = profiles.get(structureId);
        if (profile == null) return StructureProfile.DEFAULT;
        if (!profile.isProgressionMet(serverDayCount)) return StructureProfile.DEFAULT;
        return profile;
    }

    /** Get profile without progression check (for commands/display). */
    public static StructureProfile getProfileRaw(@Nullable ResourceLocation structureId) {
        if (structureId == null) return StructureProfile.DEFAULT;
        return profiles.getOrDefault(structureId, StructureProfile.DEFAULT);
    }

    public static Map<ResourceLocation, StructureProfile> getAll() {
        return profiles;
    }

    public static void load(MinecraftServer server) {
        Path serverConfigDir = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        currentPath = serverConfigDir.resolve("runicstructures-structures.json");
        reload();
    }

    public static void reload() {
        if (currentPath == null) return;

        if (!Files.exists(currentPath)) {
            generateDefaultFile(currentPath);
        }

        if (Files.exists(currentPath)) {
            reloadFromPath();
        } else {
            profiles = Collections.emptyMap();
            validationWarnings = List.of("Structure profile file '" + currentPath + "' could not be created");
        }
    }

    public static void clear() {
        profiles = Collections.emptyMap();
        validationWarnings = List.of();
        currentPath = null;
    }

    private static void reloadFromPath() {
        try (Reader reader = Files.newBufferedReader(currentPath)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                RunicStructures.LOGGER.warn("[RunicStructures] Structure profiles JSON is empty or malformed");
                profiles = Collections.emptyMap();
                validationWarnings = List.of("Structure profiles JSON is empty or malformed");
                return;
            }

            JsonObject structures = root.has("structures") ? root.getAsJsonObject("structures") : root;
            Map<ResourceLocation, StructureProfile> map = new LinkedHashMap<>();
            List<String> warnings = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : structures.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("_")) continue;

                ResourceLocation rl = ResourceLocation.tryParse(key);
                if (rl == null) {
                    warn(warnings, "Invalid structure ID '" + key + "' in profiles JSON, skipping");
                    continue;
                }

                try {
                    StructureProfile profile = parseProfile(rl, entry.getValue(), warnings);
                    if (profile != null) {
                        map.put(rl, profile);
                    }
                } catch (RuntimeException e) {
                    warn(warnings, "Failed to parse profile for '" + key + "': " + e.getMessage());
                }
            }

            profiles = Collections.unmodifiableMap(map);
            validationWarnings = List.copyOf(warnings);
            RunicStructures.LOGGER.info("[RunicStructures] Loaded {} structure profile(s) from JSON", map.size());

        } catch (IOException | JsonParseException e) {
            RunicStructures.LOGGER.error("[RunicStructures] Failed to read structure profiles from {}: {}", currentPath, e.getMessage());
            profiles = Collections.emptyMap();
            validationWarnings = List.of("Failed to read structure profiles from '" + currentPath + "': " + e.getMessage());
        }
    }

    private static void generateDefaultFile(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (var input = StructureProfileLoader.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
                if (input == null) {
                    throw new IOException("Missing bundled resource " + DEFAULT_RESOURCE_PATH);
                }
                Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
            }
            RunicStructures.LOGGER.info("[RunicStructures] Generated default structure profiles at {}", file);
        } catch (IOException e) {
            RunicStructures.LOGGER.error("[RunicStructures] Failed to generate default profiles: {}", e.getMessage());
        }
    }

    /** Validate profile structure IDs and load warnings. */
    public static List<String> validate(MinecraftServer server) {
        List<String> warnings = new ArrayList<>(validationWarnings);
        var registry = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (ResourceLocation rl : profiles.keySet()) {
            if (!registry.containsKey(rl)) {
                warnings.add("Structure profile references unknown structure '" + rl + "'");
            }
        }
        return warnings;
    }

    @Nullable
    private static StructureProfile parseProfile(ResourceLocation structureId, JsonElement element, List<String> warnings) {
        RawStructureProfile raw = GSON.fromJson(element, RawStructureProfile.class);
        if (raw == null) {
            warn(warnings, "Profile '" + structureId + "' is empty, skipping");
            return null;
        }

        List<Item[]> armorTiers = raw.armorTiers == null || raw.armorTiers.isEmpty()
                ? List.of()
                : EquipmentPools.resolveProfileArmorTiers(raw.armorTiers, structureId.toString(), warnings);
        List<Item> weaponPool = raw.weaponPool == null || raw.weaponPool.isEmpty()
                ? List.of()
                : EquipmentPools.resolveProfileWeaponPool(raw.weaponPool, structureId.toString(), warnings);
        List<Item> shieldPool = raw.shieldPool == null || raw.shieldPool.isEmpty()
                ? List.of()
                : EquipmentPools.resolveProfileShieldPool(raw.shieldPool, structureId.toString(), warnings);

        StructureProfile.ProgressionTier progressionTier = null;
        if (raw.progressionTier != null) {
            progressionTier = StructureProfile.ProgressionTier.fromString(raw.progressionTier);
            if (progressionTier == null) {
                warn(warnings, "Profile '" + structureId + "' has unknown progressionTier '" + raw.progressionTier + "'");
            }
        }

        return new StructureProfile(
                validateMin(raw.spawnCap, 1, structureId, "spawnCap", warnings),
                validateFraction(raw.eliteChance, structureId, "eliteChance", warnings),
                raw.eliteNamePrefix,
                raw.persistentMobs,
                armorTiers,
                weaponPool,
                validateFraction(raw.shieldChance, structureId, "shieldChance", warnings),
                shieldPool,
                validateRange(raw.enchantmentLevel, 0, 5, structureId, "enchantmentLevel", warnings),
                raw.darkness,
                raw.miningFatigue,
                raw.slowness,
                raw.ambientSounds,
                progressionTier
        );
    }

    @Nullable
    private static Integer validateMin(@Nullable Integer value, int min, ResourceLocation structureId,
            String field, List<String> warnings) {
        if (value == null) return null;
        if (value < min) {
            warn(warnings, "Profile '" + structureId + "' has invalid " + field + "=" + value + " (expected >= " + min + ")");
            return null;
        }
        return value;
    }

    @Nullable
    private static Integer validateRange(@Nullable Integer value, int min, int max, ResourceLocation structureId,
            String field, List<String> warnings) {
        if (value == null) return null;
        if (value < min || value > max) {
            warn(warnings, "Profile '" + structureId + "' has invalid " + field + "=" + value
                    + " (expected " + min + ".." + max + ")");
            return null;
        }
        return value;
    }

    @Nullable
    private static Double validateFraction(@Nullable Double value, ResourceLocation structureId,
            String field, List<String> warnings) {
        if (value == null) return null;
        if (value < 0.0 || value > 1.0) {
            warn(warnings, "Profile '" + structureId + "' has invalid " + field + "=" + value + " (expected 0.0..1.0)");
            return null;
        }
        return value;
    }

    private static void warn(List<String> warnings, String message) {
        warnings.add(message);
        RunicStructures.LOGGER.warn("[RunicStructures] {}", message);
    }
}
