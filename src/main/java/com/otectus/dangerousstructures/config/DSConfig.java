package com.otectus.dangerousstructures.config;

import com.otectus.dangerousstructures.event.TickSpawnHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DSConfig {

    public static final ForgeConfigSpec SPEC;

    public static final BooleanValue enabled;
    public static final BooleanValue autoDetectStructures;
    public static final BooleanValue includeVillages;
    public static final ConfigValue<List<? extends String>> structureWhitelist;
    public static final ConfigValue<List<? extends String>> structureTagWhitelist;
    public static final ConfigValue<List<? extends String>> structureBlacklist;
    public static final ConfigValue<List<? extends String>> structureTagBlacklist;
    public static final ConfigValue<List<? extends String>> mobBlacklist;
    public static final ConfigValue<List<? extends String>> mobWhitelist;
    public static final ConfigValue<List<? extends String>> allowedDimensions;
    public static final IntValue bufferRadius;
    public static final BooleanValue debugLogging;
    public static final BooleanValue periodicSpawningEnabled;
    public static final IntValue periodicSpawnInterval;
    public static final IntValue periodicSpawnCap;

    // Parsed caches — rebuilt on config load/reload
    private static volatile Set<ResourceLocation> parsedStructureWhitelist = Collections.emptySet();
    private static volatile Set<ResourceLocation> parsedStructureTagWhitelist = Collections.emptySet();
    private static volatile Set<ResourceLocation> parsedStructureBlacklist = Collections.emptySet();
    private static volatile Set<ResourceLocation> parsedStructureTagBlacklist = Collections.emptySet();
    private static volatile Set<ResourceLocation> parsedMobBlacklist = Collections.emptySet();
    private static volatile Set<ResourceLocation> parsedMobWhitelist = Collections.emptySet();
    private static volatile Set<ResourceLocation> parsedDimensions = Collections.emptySet();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Dangerous Structures Configuration").push("general");

        enabled = builder
                .comment("Master enable/disable for the mod")
                .define("enabled", true);

        autoDetectStructures = builder
                .comment("Automatically treat ALL registered structures as dangerous",
                        "When enabled, the structureWhitelist and structureTagWhitelist lists are ignored",
                        "Modded structures are included automatically",
                        "Use structureBlacklist/structureTagBlacklist to exclude specific structures")
                .define("autoDetectStructures", true);

        includeVillages = builder
                .comment("When autoDetectStructures is enabled, include village structures",
                        "When false, structures tagged as minecraft:village are excluded",
                        "Has no effect when autoDetectStructures is disabled")
                .define("includeVillages", false);

        builder.pop();

        builder.comment("Structure Filtering").push("structures");

        structureWhitelist = builder
                .comment("Structure IDs to make dangerous",
                        "Only used when autoDetectStructures is disabled",
                        "Example: [\"minecraft:fortress\", \"minecraft:stronghold\", \"minecraft:monument\"]")
                .defineListAllowEmpty(List.of("structureWhitelist"),
                        () -> List.of("minecraft:fortress", "minecraft:stronghold", "minecraft:monument"),
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        structureTagWhitelist = builder
                .comment("Structure tags to make dangerous (all structures in these tags are included)",
                        "Only used when autoDetectStructures is disabled",
                        "Example: [\"minecraft:village\"]")
                .defineListAllowEmpty(List.of("structureTagWhitelist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        structureBlacklist = builder
                .comment("Structure IDs to NEVER make dangerous",
                        "Applied in all modes (overrides auto-detection)",
                        "Example: [\"minecraft:igloo\", \"minecraft:trail_ruins\"]")
                .defineListAllowEmpty(List.of("structureBlacklist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        structureTagBlacklist = builder
                .comment("Structure tags to NEVER make dangerous",
                        "All structures in these tags are excluded",
                        "Applied in all modes (overrides auto-detection)",
                        "Example: [\"minecraft:ocean_ruin\"]")
                .defineListAllowEmpty(List.of("structureTagBlacklist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        builder.pop();

        builder.comment("Mob Filtering").push("mobs");

        mobBlacklist = builder
                .comment("Mob IDs to exclude from forced spawning",
                        "Example: [\"minecraft:creeper\", \"minecraft:phantom\"]")
                .defineListAllowEmpty(List.of("mobBlacklist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        mobWhitelist = builder
                .comment("If non-empty, ONLY these mob IDs will be force-spawned",
                        "Takes priority over blacklist")
                .defineListAllowEmpty(List.of("mobWhitelist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        builder.pop();

        builder.comment("Other Settings").push("other");

        allowedDimensions = builder
                .comment("Dimensions where the mod is active",
                        "Example: [\"minecraft:overworld\", \"minecraft:the_nether\", \"minecraft:the_end\"]")
                .defineListAllowEmpty(List.of("allowedDimensions"),
                        () -> List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"),
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        bufferRadius = builder
                .comment("Extra blocks around structure bounding box to consider 'inside'",
                        "0 = strict bounding box only")
                .defineInRange("bufferRadius", 0, 0, 16);

        debugLogging = builder
                .comment("Log each spawn override to the server console")
                .define("debugLogging", false);

        builder.pop();

        builder.comment("Periodic Spawning (for daytime surface spawns)").push("periodicSpawning");

        periodicSpawningEnabled = builder
                .comment("Enable periodic forced spawning inside dangerous structures",
                        "This allows hostile mobs to spawn even during daytime when vanilla wouldn't attempt spawns")
                .define("enabled", false);

        periodicSpawnInterval = builder
                .comment("Ticks between periodic spawn attempts (20 ticks = 1 second)")
                .defineInRange("spawnInterval", 200, 20, 6000);

        periodicSpawnCap = builder
                .comment("Maximum hostile mobs the periodic spawner will maintain per structure")
                .defineInRange("spawnCapPerStructure", 8, 1, 64);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Rebuild parsed caches from config values. Called on config load/reload.
     */
    public static void refresh() {
        parsedStructureWhitelist = structureWhitelist.get().stream()
                .map(s -> new ResourceLocation((String) s))
                .collect(Collectors.toUnmodifiableSet());
        parsedStructureTagWhitelist = structureTagWhitelist.get().stream()
                .map(s -> new ResourceLocation((String) s))
                .collect(Collectors.toUnmodifiableSet());
        parsedStructureBlacklist = structureBlacklist.get().stream()
                .map(s -> new ResourceLocation((String) s))
                .collect(Collectors.toUnmodifiableSet());
        parsedStructureTagBlacklist = structureTagBlacklist.get().stream()
                .map(s -> new ResourceLocation((String) s))
                .collect(Collectors.toUnmodifiableSet());
        parsedMobBlacklist = mobBlacklist.get().stream()
                .map(s -> new ResourceLocation((String) s))
                .collect(Collectors.toUnmodifiableSet());
        parsedMobWhitelist = mobWhitelist.get().stream()
                .map(s -> new ResourceLocation((String) s))
                .collect(Collectors.toUnmodifiableSet());
        parsedDimensions = allowedDimensions.get().stream()
                .map(s -> new ResourceLocation((String) s))
                .collect(Collectors.toUnmodifiableSet());

        TickSpawnHandler.invalidateMobCache();
    }

    public static Set<ResourceLocation> getStructureWhitelist() {
        return parsedStructureWhitelist;
    }

    public static Set<ResourceLocation> getStructureTagWhitelist() {
        return parsedStructureTagWhitelist;
    }

    public static Set<ResourceLocation> getStructureBlacklist() {
        return parsedStructureBlacklist;
    }

    public static Set<ResourceLocation> getStructureTagBlacklist() {
        return parsedStructureTagBlacklist;
    }

    public static Set<ResourceLocation> getMobWhitelist() {
        return parsedMobWhitelist;
    }

    public static Set<ResourceLocation> getMobBlacklist() {
        return parsedMobBlacklist;
    }

    public static boolean isEntityAllowed(EntityType<?> type) {
        ResourceLocation id = EntityType.getKey(type);
        if (!parsedMobWhitelist.isEmpty()) {
            return parsedMobWhitelist.contains(id);
        }
        if (!parsedMobBlacklist.isEmpty()) {
            return !parsedMobBlacklist.contains(id);
        }
        return true;
    }

    public static boolean isDimensionAllowed(ResourceKey<Level> dimension) {
        return parsedDimensions.contains(dimension.location());
    }
}
