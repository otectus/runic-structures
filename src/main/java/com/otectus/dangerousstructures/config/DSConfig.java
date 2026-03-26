package com.otectus.dangerousstructures.config;

import com.otectus.dangerousstructures.DangerousStructures;
import com.otectus.dangerousstructures.event.TickSpawnHandler;
import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
    public static final BooleanValue dimensionSpecificMobs;
    public static final ConfigValue<List<? extends String>> allowedDimensions;
    public static final IntValue bufferRadius;
    public static final BooleanValue debugLogging;
    public static final BooleanValue periodicSpawningEnabled;
    public static final IntValue periodicSpawnInterval;
    public static final IntValue periodicSpawnCap;
    public static final BooleanValue useStructurePieces;
    public static final BooleanValue weightPiecesByArea;
    public static final BooleanValue useVanillaSpawnWeights;
    public static final IntValue minPlayerDistance;
    public static final BooleanValue packSpawning;
    public static final IntValue maxPackSize;
    public static final BooleanValue randomizeInterval;
    public static final IntValue chunkSearchRadius;
    public static final IntValue maxPositionAttempts;
    public static final BooleanValue scaleCapByStructureSize;
    public static final IntValue mobsPerChunkArea;
    public static final IntValue maxScaledCap;

    // Environmental effects
    public static final BooleanValue applyDarknessEffect;
    public static final IntValue darknessAmplifier;
    public static final BooleanValue playAmbientSounds;
    public static final IntValue ambientSoundInterval;

    // Elite spawns
    public static final BooleanValue eliteSpawnsEnabled;
    public static final DoubleValue eliteSpawnChance;
    public static final DoubleValue eliteHealthMultiplier;
    public static final DoubleValue eliteDamageMultiplier;
    public static final BooleanValue eliteGlowingEffect;
    public static final ConfigValue<String> eliteNamePrefix;
    public static final IntValue eliteBonusXP;

    // Armor
    public static final BooleanValue armorEnabled;
    public static final DoubleValue armorDropChance;
    public static final BooleanValue allowLeather;
    public static final BooleanValue allowChainmail;
    public static final BooleanValue allowIron;
    public static final BooleanValue allowGold;
    public static final BooleanValue allowDiamond;
    public static final BooleanValue allowNetherite;

    // Mob behavior
    public static final BooleanValue sunlightImmunity;
    public static final BooleanValue persistentMobs;

    // Night spawn boost
    public static final DoubleValue nightSpawnMultiplier;

    // Initial population
    public static final BooleanValue initialPopulationEnabled;
    public static final IntValue initialPopulationMin;
    public static final IntValue initialPopulationMax;

    // Difficulty scaling
    public static final BooleanValue difficultyScalingEnabled;
    public static final DoubleValue easyCapMultiplier;
    public static final DoubleValue normalCapMultiplier;
    public static final DoubleValue hardCapMultiplier;
    public static final DoubleValue easyIntervalMultiplier;
    public static final DoubleValue normalIntervalMultiplier;
    public static final DoubleValue hardIntervalMultiplier;

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
                        "Boss mobs are blacklisted by default",
                        "Example: [\"minecraft:creeper\", \"minecraft:phantom\"]")
                .defineListAllowEmpty(List.of("mobBlacklist"),
                        () -> List.of("minecraft:ender_dragon", "minecraft:wither"),
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        mobWhitelist = builder
                .comment("If non-empty, ONLY these mob IDs will be force-spawned",
                        "Takes priority over blacklist")
                .defineListAllowEmpty(List.of("mobWhitelist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        dimensionSpecificMobs = builder
                .comment("When enabled, periodic spawning only picks mobs that naturally spawn",
                        "in the current dimension's biomes (e.g. blazes only in nether structures)",
                        "Only affects periodic spawning; event-based spawning already uses vanilla's biome rules")
                .define("dimensionSpecificMobs", true);

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
                .define("enabled", true);

        periodicSpawnInterval = builder
                .comment("Ticks between periodic spawn attempts (20 ticks = 1 second)")
                .defineInRange("spawnInterval", 200, 20, 6000);

        periodicSpawnCap = builder
                .comment("Maximum hostile mobs the periodic spawner will maintain per structure")
                .defineInRange("spawnCapPerStructure", 8, 1, 64);

        maxPositionAttempts = builder
                .comment("Maximum random position attempts per structure per spawn cycle")
                .defineInRange("maxPositionAttempts", 10, 1, 20);

        minPlayerDistance = builder
                .comment("Minimum distance (blocks) from any player for periodic spawns",
                        "Prevents mobs from spawning directly on top of players",
                        "0 = no minimum distance")
                .defineInRange("minPlayerDistance", 6, 0, 48);

        chunkSearchRadius = builder
                .comment("Chunk radius around each player to search for structures",
                        "Higher values find more distant structures but cost more performance")
                .defineInRange("chunkSearchRadius", 4, 2, 8);

        randomizeInterval = builder
                .comment("Add +/- 25% jitter to spawn interval to prevent synchronized waves")
                .define("randomizeInterval", true);

        useStructurePieces = builder
                .comment("Spawn within individual structure pieces instead of the full bounding box",
                        "Greatly improves spawn accuracy for sprawling structures like fortresses")
                .define("useStructurePieces", true);

        weightPiecesByArea = builder
                .comment("When useStructurePieces is enabled, prefer larger pieces (rooms) over smaller ones (corridors)")
                .define("weightPiecesByArea", true);

        useVanillaSpawnWeights = builder
                .comment("Respect vanilla biome spawn weights when picking mob types",
                        "When false, all eligible mobs are equally likely")
                .define("useVanillaSpawnWeights", true);

        packSpawning = builder
                .comment("Spawn mobs in packs (groups) matching vanilla pack sizes",
                        "Uses minCount/maxCount from biome spawn data when dimensionSpecificMobs is enabled")
                .define("packSpawning", true);

        maxPackSize = builder
                .comment("Maximum pack size regardless of biome data")
                .defineInRange("maxPackSize", 4, 1, 8);

        scaleCapByStructureSize = builder
                .comment("Scale the spawn cap based on structure footprint area",
                        "Larger structures get higher caps; smaller structures use the base cap")
                .define("scaleCapByStructureSize", false);

        mobsPerChunkArea = builder
                .comment("When scaleCapByStructureSize is enabled, mobs per 16x16 footprint area")
                .defineInRange("mobsPerChunkArea", 3, 1, 16);

        maxScaledCap = builder
                .comment("When scaleCapByStructureSize is enabled, absolute maximum cap")
                .defineInRange("maxScaledCap", 32, 1, 128);

        sunlightImmunity = builder
                .comment("Make mod-spawned mobs immune to burning in sunlight",
                        "Grants permanent Fire Resistance (also prevents lava/fire damage)")
                .define("sunlightImmunity", true);

        persistentMobs = builder
                .comment("Make periodic-spawned mobs persistent (never despawn)",
                        "WARNING: Can cause entity accumulation if mobs are never killed")
                .define("persistentMobs", false);

        nightSpawnMultiplier = builder
                .comment("Spawn frequency multiplier at night (divides the interval)",
                        "1.0 = same rate day and night, 2.0 = twice as fast at night")
                .defineInRange("nightSpawnMultiplier", 1.0, 1.0, 4.0);

        initialPopulationEnabled = builder
                .comment("Spawn a group of mobs when a structure is first discovered each session",
                        "Guarantees structures are populated even when periodic spawning is unreliable")
                .define("initialPopulationEnabled", true);

        initialPopulationMin = builder
                .comment("Minimum mobs to spawn during initial population")
                .defineInRange("initialPopulationMin", 3, 1, 16);

        initialPopulationMax = builder
                .comment("Maximum mobs to spawn during initial population")
                .defineInRange("initialPopulationMax", 6, 1, 32);

        builder.pop();

        builder.comment("Armor",
                "Equip hostile mobs spawned by this mod with randomized armor",
                "Applies to both periodic spawns and event-intercepted spawns").push("armor");

        armorEnabled = builder
                .comment("Equip all hostile mobs spawned by this mod with a full suit of randomized armor")
                .define("armorEnabled", true);

        armorDropChance = builder
                .comment("Chance (0.0 to 1.0) for each armor piece to drop on death",
                        "Vanilla default is ~0.085 (8.5%). Set to 0.0 to prevent drops.")
                .defineInRange("armorDropChance", 0.085, 0.0, 1.0);

        allowLeather = builder
                .comment("Include leather armor in the random pool")
                .define("allowLeather", true);

        allowChainmail = builder
                .comment("Include chainmail armor in the random pool")
                .define("allowChainmail", true);

        allowIron = builder
                .comment("Include iron armor in the random pool")
                .define("allowIron", true);

        allowGold = builder
                .comment("Include gold armor in the random pool")
                .define("allowGold", true);

        allowDiamond = builder
                .comment("Include diamond armor in the random pool")
                .define("allowDiamond", true);

        allowNetherite = builder
                .comment("Include netherite armor in the random pool")
                .define("allowNetherite", false);

        builder.pop();

        builder.comment("Environmental Effects",
                "Atmospheric effects applied to players inside dangerous structures").push("environmentalEffects");

        applyDarknessEffect = builder
                .comment("Apply the Darkness effect (like the Warden) to players inside dangerous structures")
                .define("applyDarknessEffect", false);

        darknessAmplifier = builder
                .comment("Amplifier level for the Darkness effect (0 = level I, 1 = level II, etc.)")
                .defineInRange("darknessAmplifier", 0, 0, 4);

        playAmbientSounds = builder
                .comment("Play ominous ambient sounds to players inside dangerous structures")
                .define("playAmbientSounds", false);

        ambientSoundInterval = builder
                .comment("Ticks between ambient sound plays (20 ticks = 1 second)")
                .defineInRange("ambientSoundInterval", 400, 40, 2400);

        builder.pop();

        builder.comment("Elite Mob Spawns",
                "Rare enhanced mobs with boosted stats").push("eliteSpawns");

        eliteSpawnsEnabled = builder
                .comment("Enable rare elite mob spawns with enhanced stats")
                .define("enabled", false);

        eliteSpawnChance = builder
                .comment("Chance (0.0 to 1.0) for each periodic spawn to be an elite")
                .defineInRange("spawnChance", 0.02, 0.0, 1.0);

        eliteHealthMultiplier = builder
                .comment("Health multiplier for elite mobs")
                .defineInRange("healthMultiplier", 2.0, 1.0, 10.0);

        eliteDamageMultiplier = builder
                .comment("Attack damage multiplier for elite mobs")
                .defineInRange("damageMultiplier", 1.5, 1.0, 10.0);

        eliteGlowingEffect = builder
                .comment("Give elite mobs the glowing effect (visible through walls)")
                .define("glowingEffect", true);

        eliteNamePrefix = builder
                .comment("Custom name prefix for elite mobs (empty = no custom name)")
                .define("namePrefix", "Elite");

        eliteBonusXP = builder
                .comment("Bonus XP dropped by elite mobs when killed by a player")
                .defineInRange("bonusXP", 20, 0, 500);

        builder.pop();

        builder.comment("Difficulty Scaling",
                "Scale spawn rates and caps based on game difficulty").push("difficultyScaling");

        difficultyScalingEnabled = builder
                .comment("Enable difficulty-based scaling of spawn cap and interval")
                .define("enabled", true);

        easyCapMultiplier = builder
                .comment("Spawn cap multiplier on Easy difficulty")
                .defineInRange("easyCapMultiplier", 0.5, 0.1, 4.0);

        normalCapMultiplier = builder
                .comment("Spawn cap multiplier on Normal difficulty")
                .defineInRange("normalCapMultiplier", 1.0, 0.1, 4.0);

        hardCapMultiplier = builder
                .comment("Spawn cap multiplier on Hard difficulty")
                .defineInRange("hardCapMultiplier", 1.5, 0.1, 4.0);

        easyIntervalMultiplier = builder
                .comment("Spawn interval multiplier on Easy difficulty (higher = slower spawns)")
                .defineInRange("easyIntervalMultiplier", 1.5, 0.25, 4.0);

        normalIntervalMultiplier = builder
                .comment("Spawn interval multiplier on Normal difficulty")
                .defineInRange("normalIntervalMultiplier", 1.0, 0.25, 4.0);

        hardIntervalMultiplier = builder
                .comment("Spawn interval multiplier on Hard difficulty (lower = faster spawns)")
                .defineInRange("hardIntervalMultiplier", 0.75, 0.25, 4.0);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Rebuild parsed caches from config values. Called on config load/reload.
     */
    public static void refresh() {
        parsedStructureWhitelist = parseResourceLocations(structureWhitelist.get(), "structureWhitelist");
        parsedStructureTagWhitelist = parseResourceLocations(structureTagWhitelist.get(), "structureTagWhitelist");
        parsedStructureBlacklist = parseResourceLocations(structureBlacklist.get(), "structureBlacklist");
        parsedStructureTagBlacklist = parseResourceLocations(structureTagBlacklist.get(), "structureTagBlacklist");
        parsedMobBlacklist = parseResourceLocations(mobBlacklist.get(), "mobBlacklist");
        parsedMobWhitelist = parseResourceLocations(mobWhitelist.get(), "mobWhitelist");
        parsedDimensions = parseResourceLocations(allowedDimensions.get(), "allowedDimensions");

        TickSpawnHandler.invalidateCaches();
        StructureDetection.invalidateCache();
    }

    private static Set<ResourceLocation> parseResourceLocations(List<? extends String> list, String configKey) {
        return list.stream()
                .map(s -> {
                    ResourceLocation rl = ResourceLocation.tryParse((String) s);
                    if (rl == null) {
                        DangerousStructures.LOGGER.warn("[DangerousStructures] Invalid ResourceLocation '{}' in config key '{}', skipping", s, configKey);
                    }
                    return rl;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
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
