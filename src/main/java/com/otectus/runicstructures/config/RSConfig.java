package com.otectus.runicstructures.config;

import com.otectus.runicstructures.RunicStructures;
import com.otectus.runicstructures.event.TickSpawnHandler;
import com.otectus.runicstructures.util.StructureDetection;
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

public class RSConfig {

    public static final ForgeConfigSpec SPEC;

    // Config fields — assigned once during class initialization via builder methods
    public static BooleanValue enabled;
    public static BooleanValue autoDetectStructures;
    public static BooleanValue includeVillages;
    public static ConfigValue<List<? extends String>> structureWhitelist;
    public static ConfigValue<List<? extends String>> structureTagWhitelist;
    public static ConfigValue<List<? extends String>> structureBlacklist;
    public static ConfigValue<List<? extends String>> structureTagBlacklist;
    public static ConfigValue<List<? extends String>> mobBlacklist;
    public static ConfigValue<List<? extends String>> mobWhitelist;
    public static BooleanValue dimensionSpecificMobs;
    public static ConfigValue<List<? extends String>> allowedDimensions;
    public static IntValue bufferRadius;
    public static BooleanValue debugLogging;
    public static BooleanValue periodicSpawningEnabled;
    public static IntValue periodicSpawnInterval;
    public static IntValue periodicSpawnCap;
    public static BooleanValue useStructurePieces;
    public static BooleanValue weightPiecesByArea;
    public static BooleanValue useVanillaSpawnWeights;
    public static IntValue minPlayerDistance;
    public static BooleanValue packSpawning;
    public static IntValue maxPackSize;
    public static BooleanValue randomizeInterval;
    public static IntValue chunkSearchRadius;
    public static IntValue maxPositionAttempts;
    public static BooleanValue scaleCapByStructureSize;
    public static IntValue mobsPerChunkArea;
    public static IntValue maxScaledCap;

    // Environmental effects
    public static BooleanValue applyDarknessEffect;
    public static IntValue darknessAmplifier;
    public static BooleanValue playAmbientSounds;
    public static IntValue ambientSoundInterval;

    // Elite spawns
    public static BooleanValue eliteSpawnsEnabled;
    public static DoubleValue eliteSpawnChance;
    public static DoubleValue eliteHealthMultiplier;
    public static DoubleValue eliteDamageMultiplier;
    public static BooleanValue eliteGlowingEffect;
    public static ConfigValue<String> eliteNamePrefix;
    public static IntValue eliteBonusXP;
    public static DoubleValue eliteMaxBaseHealth;

    // Armor
    public static BooleanValue armorEnabled;
    public static DoubleValue armorDropChance;
    public static BooleanValue allowLeather;
    public static BooleanValue allowChainmail;
    public static BooleanValue allowIron;
    public static BooleanValue allowGold;
    public static BooleanValue allowDiamond;
    public static BooleanValue allowNetherite;
    public static BooleanValue respectExistingArmor;

    // Mob behavior
    public static BooleanValue fireImmunity;
    public static BooleanValue persistentMobs;

    // Night spawn boost
    public static DoubleValue nightSpawnMultiplier;

    // Initial population
    public static BooleanValue initialPopulationEnabled;
    public static IntValue initialPopulationMin;
    public static IntValue initialPopulationMax;

    // Weapons
    public static BooleanValue weaponsEnabled;
    public static DoubleValue weaponDropChance;
    public static BooleanValue respectExistingWeapon;

    // Enchantments
    public static BooleanValue enchantmentsEnabled;
    public static IntValue maxEnchantmentLevel;

    // Additional environmental effects
    public static BooleanValue applyMiningFatigue;
    public static IntValue miningFatigueAmplifier;
    public static BooleanValue applySlowness;
    public static IntValue slownessAmplifier;

    // Armor drop tier multipliers
    public static DoubleValue diamondDropMultiplier;
    public static DoubleValue netheriteDropMultiplier;

    // Equipment pools
    public static ConfigValue<List<? extends String>> weaponPool;
    public static ConfigValue<List<? extends String>> customArmorSets;
    public static BooleanValue shieldsEnabled;
    public static ConfigValue<List<? extends String>> shieldPool;
    public static DoubleValue shieldChance;

    // Progression
    public static BooleanValue progressionScalingEnabled;

    // Difficulty scaling
    public static BooleanValue difficultyScalingEnabled;
    public static DoubleValue easyCapMultiplier;
    public static DoubleValue normalCapMultiplier;
    public static DoubleValue hardCapMultiplier;
    public static DoubleValue easyIntervalMultiplier;
    public static DoubleValue normalIntervalMultiplier;
    public static DoubleValue hardIntervalMultiplier;

    // Runtime debug toggle — set by /rs debug, not reset by config reload
    private static volatile boolean runtimeDebug = false;

    public static boolean isDebugEnabled() {
        return runtimeDebug || debugLogging.get();
    }

    public static void setRuntimeDebug(boolean enabled) {
        runtimeDebug = enabled;
    }

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
        buildGeneralConfig(builder);
        buildStructureConfig(builder);
        buildMobConfig(builder);
        buildOtherConfig(builder);
        buildPeriodicSpawningConfig(builder);
        buildArmorConfig(builder);
        buildWeaponConfig(builder);
        buildEnchantmentConfig(builder);
        buildEquipmentPoolConfig(builder);
        buildEnvironmentalConfig(builder);
        buildEliteConfig(builder);
        buildDifficultyConfig(builder);
        SPEC = builder.build();
    }

    private static void buildGeneralConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Runic Structures Configuration",
                "Defaults tuned for the Runecraft modpack (Forge 1.20.1)",
                "Complements InControl, majruszsdifficulty, and 40+ structure mods").push("general");

        enabled = builder
                .comment("Master enable/disable for the mod")
                .define("enabled", true);

        autoDetectStructures = builder
                .comment("Automatically treat ALL registered structures as runic",
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
    }

    private static void buildStructureConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Structure Filtering").push("structures");

        structureWhitelist = builder
                .comment("Structure IDs to make runic",
                        "Only used when autoDetectStructures is disabled",
                        "Example: [\"minecraft:fortress\", \"minecraft:stronghold\", \"minecraft:monument\"]")
                .defineListAllowEmpty(List.of("structureWhitelist"),
                        () -> List.of("minecraft:fortress", "minecraft:stronghold", "minecraft:monument"),
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        structureTagWhitelist = builder
                .comment("Structure tags to make runic (all structures in these tags are included)",
                        "Only used when autoDetectStructures is disabled",
                        "Example: [\"minecraft:village\"]")
                .defineListAllowEmpty(List.of("structureTagWhitelist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        structureBlacklist = builder
                .comment("Structure IDs to NEVER make runic",
                        "Applied in all modes (overrides auto-detection)",
                        "Example: [\"minecraft:igloo\", \"minecraft:trail_ruins\"]")
                .defineListAllowEmpty(List.of("structureBlacklist"),
                        () -> List.of(
                                // Cataclysm boss arenas — scripted encounters
                                "cataclysm:burning_arena", "cataclysm:cursed_pyramid",
                                "cataclysm:ruined_citadel", "cataclysm:soul_black_smith",
                                "cataclysm:sunken_city", "cataclysm:ancient_factory",
                                "cataclysm:frosted_prison", "cataclysm:acropolis",
                                // Stalwart Dungeons boss arenas — scripted encounters
                                "stalwart_dungeons:awful_dungeon",
                                "stalwart_dungeons:nether_dungeon",
                                "stalwart_dungeons:end_dungeon",
                                // Bosses of Mass Destruction arenas — scripted encounters
                                "bosses_of_mass_destruction:lich_tower",
                                "bosses_of_mass_destruction:void_blossom",
                                "bosses_of_mass_destruction:gauntlet_arena",
                                "bosses_of_mass_destruction:obsidilith_arena",
                                // Mowzie's Mobs boss rooms — scripted encounters
                                "mowziesmobs:wroughtnaut_room",
                                "mowziesmobs:frostmaw_spawn",
                                // Decorative/tiny structures — no meaningful interior
                                "minecraft:trail_ruins", "minecraft:igloo",
                                "minecraft:desert_well",
                                "taxtg:giant_spruce_tree", "taxtg:giant_oak_tree",
                                "taxtg:giant_jungle_tree", "taxtg:giant_cherry_tree",
                                "hopo:underwater/underwater_fossils"),
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        structureTagBlacklist = builder
                .comment("Structure tags to NEVER make runic",
                        "All structures in these tags are excluded",
                        "Applied in all modes (overrides auto-detection)",
                        "Example: [\"minecraft:ocean_ruin\"]")
                .defineListAllowEmpty(List.of("structureTagBlacklist"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        builder.pop();
    }

    private static void buildMobConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Mob Filtering").push("mobs");

        mobBlacklist = builder
                .comment("Mob IDs to exclude from forced spawning",
                        "Boss mobs are blacklisted by default",
                        "Example: [\"minecraft:creeper\", \"minecraft:phantom\"]")
                .defineListAllowEmpty(List.of("mobBlacklist"),
                        () -> List.of(
                                // Vanilla bosses
                                "minecraft:ender_dragon", "minecraft:wither",
                                "minecraft:elder_guardian", "minecraft:warden",
                                // Cataclysm bosses
                                "cataclysm:netherite_monstrosity", "cataclysm:ender_golem",
                                "cataclysm:ignis", "cataclysm:the_harbinger",
                                "cataclysm:the_leviathan", "cataclysm:ancient_remnant",
                                "cataclysm:maledictus", "cataclysm:ender_guardian",
                                "cataclysm:scylla", "cataclysm:coral_golem", "cataclysm:coralssus",
                                // Bosses of Mass Destruction
                                "bosses_of_mass_destruction:void_blossom",
                                "bosses_of_mass_destruction:lich",
                                "bosses_of_mass_destruction:gauntlet",
                                "bosses_of_mass_destruction:obsidilith",
                                // Mowzie's Mobs (boss-tier)
                                "mowziesmobs:ferrous_wroughtnaut", "mowziesmobs:frostmaw",
                                "mowziesmobs:umvuthi",
                                // Stalwart Dungeons bosses
                                "stalwart_dungeons:awful_ghast", "stalwart_dungeons:nether_keeper",
                                "stalwart_dungeons:shelterer",
                                // Saints Dragons
                                "saintsdragons:cindervane", "saintsdragons:ignivorus",
                                "saintsdragons:nulljaw", "saintsdragons:raevyx",
                                "saintsdragons:stegonaut",
                                // Ice and Fire boss-tier
                                "iceandfire:fire_dragon", "iceandfire:ice_dragon",
                                "iceandfire:lightning_dragon", "iceandfire:hydra",
                                "iceandfire:gorgon", "iceandfire:cyclops",
                                "iceandfire:sea_serpent", "iceandfire:dread_lich",
                                "iceandfire:myrmex_queen",
                                // Irons Spellbooks
                                "irons_spellbooks:dead_king",
                                // Ars Nouveau
                                "ars_nouveau:wilden_boss",
                                // RealmRPG
                                "realmrpg_demons:demon_lord", "realmrpg_demons:ancient_demon_lord",
                                // Galosphere
                                "galosphere:berserker",
                                // Dark Doppelganger
                                "dark_doppelganger:dark_doppelganger",
                                // Majrusz Difficulty
                                "majruszsdifficulty:cerberus", "majruszsdifficulty:giant",
                                "majruszsdifficulty:tank"),
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
    }

    private static void buildOtherConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Other Settings").push("other");

        allowedDimensions = builder
                .comment("Dimensions where the mod is active",
                        "Example: [\"minecraft:overworld\", \"minecraft:the_nether\", \"minecraft:the_end\"]")
                .defineListAllowEmpty(List.of("allowedDimensions"),
                        () -> List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
                                "mining_dimension:mining", "dungeon_realm:dungeon_realm"),
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        bufferRadius = builder
                .comment("Extra blocks around structure bounding box to consider 'inside'",
                        "0 = strict bounding box only")
                .defineInRange("bufferRadius", 0, 0, 16);

        debugLogging = builder
                .comment("Log each spawn override to the server console")
                .define("debugLogging", false);

        builder.pop();
    }

    private static void buildPeriodicSpawningConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Periodic Spawning (for daytime surface spawns)").push("periodicSpawning");

        periodicSpawningEnabled = builder
                .comment("Enable periodic forced spawning inside runic structures",
                        "This allows hostile mobs to spawn even during daytime when vanilla wouldn't attempt spawns")
                .define("enabled", true);

        periodicSpawnInterval = builder
                .comment("Ticks between periodic spawn attempts (20 ticks = 1 second)")
                .defineInRange("spawnInterval", 300, 20, 6000);

        periodicSpawnCap = builder
                .comment("Maximum hostile mobs the periodic spawner will maintain per structure")
                .defineInRange("spawnCapPerStructure", 6, 1, 64);

        maxPositionAttempts = builder
                .comment("Maximum random position attempts per structure per spawn cycle")
                .defineInRange("maxPositionAttempts", 10, 1, 20);

        minPlayerDistance = builder
                .comment("Minimum distance (blocks) from any player for periodic spawns",
                        "Prevents mobs from spawning directly on top of players",
                        "0 = no minimum distance",
                        "Note: Initial population ignores this setting to guarantee structures are populated on discovery")
                .defineInRange("minPlayerDistance", 6, 0, 48);

        chunkSearchRadius = builder
                .comment("Chunk radius around each player to search for structures",
                        "Higher values find more distant structures but cost more performance")
                .defineInRange("chunkSearchRadius", 3, 2, 8);

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
                        "When false, all eligible mobs are equally likely",
                        "Note: Only applies when dimensionSpecificMobs is enabled")
                .define("useVanillaSpawnWeights", true);

        packSpawning = builder
                .comment("Spawn mobs in packs (groups) matching vanilla pack sizes",
                        "Uses minCount/maxCount from biome spawn data when dimensionSpecificMobs is enabled")
                .define("packSpawning", true);

        maxPackSize = builder
                .comment("Maximum pack size regardless of biome data")
                .defineInRange("maxPackSize", 3, 1, 8);

        scaleCapByStructureSize = builder
                .comment("Scale the spawn cap based on structure footprint area",
                        "Larger structures get higher caps; smaller structures use the base cap")
                .define("scaleCapByStructureSize", true);

        mobsPerChunkArea = builder
                .comment("When scaleCapByStructureSize is enabled, mobs per 16x16 footprint area")
                .defineInRange("mobsPerChunkArea", 2, 1, 16);

        maxScaledCap = builder
                .comment("When scaleCapByStructureSize is enabled, absolute maximum cap")
                .defineInRange("maxScaledCap", 20, 1, 128);

        fireImmunity = builder
                .comment("Grant mod-spawned mobs permanent Fire Resistance",
                        "Prevents undead from burning in sunlight AND makes all mobs immune to fire, lava, and Fire Aspect")
                .define("fireImmunity", true);

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
                        "Guarantees structures are populated even when periodic spawning is unreliable",
                        "Note: Ignores minPlayerDistance to ensure mobs are present when players arrive")
                .define("initialPopulationEnabled", true);

        initialPopulationMin = builder
                .comment("Minimum mobs to spawn during initial population")
                .defineInRange("initialPopulationMin", 2, 1, 16);

        initialPopulationMax = builder
                .comment("Maximum mobs to spawn during initial population")
                .defineInRange("initialPopulationMax", 5, 1, 32);

        builder.pop();
    }

    private static void buildArmorConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Armor",
                "Equip hostile mobs spawned by this mod with randomized armor",
                "Applies to both periodic spawns and event-intercepted spawns").push("armor");

        armorEnabled = builder
                .comment("Equip all hostile mobs spawned by this mod with a full suit of randomized armor")
                .define("armorEnabled", true);

        armorDropChance = builder
                .comment("Chance (0.0 to 1.0) for each armor piece to drop on death",
                        "Vanilla default is ~0.085 (8.5%). Set to 0.0 to prevent drops.")
                .defineInRange("armorDropChance", 0.05, 0.0, 1.0);

        allowLeather = builder
                .comment("Include leather armor in the random pool")
                .define("allowLeather", false);

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

        respectExistingArmor = builder
                .comment("Skip armor application if the mob already has equipment",
                        "Prevents overwriting armor from InControl, Majrusz's Difficulty, or vanilla spawns",
                        "Recommended when running alongside other mob enhancement mods")
                .define("respectExistingArmor", true);

        diamondDropMultiplier = builder
                .comment("Drop chance multiplier for diamond armor (applied on top of base armorDropChance)",
                        "1.0 = same as base, 0.5 = half the base chance")
                .defineInRange("diamondDropMultiplier", 0.5, 0.0, 2.0);

        netheriteDropMultiplier = builder
                .comment("Drop chance multiplier for netherite armor (applied on top of base armorDropChance)",
                        "1.0 = same as base, 0.25 = quarter the base chance")
                .defineInRange("netheriteDropMultiplier", 0.25, 0.0, 2.0);

        builder.pop();
    }

    private static void buildWeaponConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Weapons",
                "Equip hostile mobs with randomized melee weapons",
                "Skeletons are excluded to preserve ranged AI").push("weapons");

        weaponsEnabled = builder
                .comment("Equip melee mobs with a random sword")
                .define("weaponsEnabled", false);

        weaponDropChance = builder
                .comment("Chance (0.0 to 1.0) for the weapon to drop on death")
                .defineInRange("weaponDropChance", 0.05, 0.0, 1.0);

        respectExistingWeapon = builder
                .comment("Skip weapon application if the mob already has a mainhand item",
                        "Prevents overwriting weapons from other mods or vanilla spawns")
                .define("respectExistingWeapon", true);

        builder.pop();
    }

    private static void buildEnchantmentConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Enchantments",
                "Apply random enchantments to mod-applied equipment").push("enchantments");

        enchantmentsEnabled = builder
                .comment("Apply random enchantments to armor and weapons given by this mod")
                .define("enchantmentsEnabled", false);

        maxEnchantmentLevel = builder
                .comment("Maximum enchantment level (e.g., Protection III, Sharpness III)")
                .defineInRange("maxEnchantmentLevel", 2, 1, 5);

        builder.pop();
    }

    private static void buildEquipmentPoolConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Equipment Pools",
                "Config-driven equipment pools for modded weapon/armor/shield support",
                "Per-structure overrides are in runicstructures-structures.json").push("equipmentPools");

        weaponPool = builder
                .comment("Additional weapons beyond the hardcoded stone/iron/diamond swords",
                        "If non-empty, these REPLACE the vanilla sword pool",
                        "Format: list of item ResourceLocations",
                        "Example: [\"spartanweaponry:greatsword_iron\", \"minecraft:iron_axe\"]")
                .defineListAllowEmpty(List.of("weaponPool"),
                        Collections::emptyList,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        customArmorSets = builder
                .comment("Custom armor sets beyond vanilla tiers",
                        "Format: \"name=namespace:helmet,namespace:chest,namespace:legs,namespace:boots\"",
                        "The name can then be used in per-structure JSON profiles",
                        "Example: [\"fantasy_dragonslayer=fantasy_armor:dragonslayer_helmet,fantasy_armor:dragonslayer_chestplate,fantasy_armor:dragonslayer_leggings,fantasy_armor:dragonslayer_boots\"]")
                .defineListAllowEmpty(List.of("customArmorSets"),
                        Collections::emptyList,
                        o -> o instanceof String);

        shieldsEnabled = builder
                .comment("Enable shield/offhand equipment for melee mobs",
                        "Skeletons and other ranged mobs are excluded")
                .define("shieldsEnabled", false);

        shieldPool = builder
                .comment("Shield items available for offhand equipping",
                        "Format: list of item ResourceLocations")
                .defineListAllowEmpty(List.of("shieldPool"),
                        () -> List.of("minecraft:shield"),
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

        shieldChance = builder
                .comment("Chance (0.0 to 1.0) for a melee mob to receive a shield")
                .defineInRange("shieldChance", 0.2, 0.0, 1.0);

        progressionScalingEnabled = builder
                .comment("Enable progression-based equipment scaling",
                        "When enabled, per-structure profiles with progressionTier set will only",
                        "activate when the server day count reaches the specified tier",
                        "Tiers: early (day 0+), mid (day 10+), late (day 30+), veteran (day 50+)")
                .define("progressionScalingEnabled", false);

        builder.pop();
    }

    private static void buildEnvironmentalConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Environmental Effects",
                "Atmospheric effects applied to players inside runic structures").push("environmentalEffects");

        applyDarknessEffect = builder
                .comment("Apply the Darkness effect (like the Warden) to players inside runic structures")
                .define("applyDarknessEffect", false);

        darknessAmplifier = builder
                .comment("Amplifier level for the Darkness effect (0 = level I, 1 = level II, etc.)")
                .defineInRange("darknessAmplifier", 0, 0, 4);

        applyMiningFatigue = builder
                .comment("Apply Mining Fatigue to players inside runic structures",
                        "Slows block-breaking, discouraging cheese strategies")
                .define("applyMiningFatigue", false);

        miningFatigueAmplifier = builder
                .comment("Amplifier level for Mining Fatigue (0 = level I)")
                .defineInRange("miningFatigueAmplifier", 0, 0, 4);

        applySlowness = builder
                .comment("Apply Slowness to players inside runic structures",
                        "Subtle movement penalty that increases danger")
                .define("applySlowness", false);

        slownessAmplifier = builder
                .comment("Amplifier level for Slowness (0 = level I)")
                .defineInRange("slownessAmplifier", 0, 0, 4);

        playAmbientSounds = builder
                .comment("Play ominous ambient sounds to players inside runic structures")
                .define("playAmbientSounds", false);

        ambientSoundInterval = builder
                .comment("Ticks between ambient sound plays (20 ticks = 1 second)")
                .defineInRange("ambientSoundInterval", 400, 40, 2400);

        builder.pop();
    }

    private static void buildEliteConfig(ForgeConfigSpec.Builder builder) {
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
                .defineInRange("healthMultiplier", 2.5, 1.0, 10.0);

        eliteDamageMultiplier = builder
                .comment("Attack damage multiplier for elite mobs")
                .defineInRange("damageMultiplier", 1.75, 1.0, 10.0);

        eliteGlowingEffect = builder
                .comment("Give elite mobs the glowing effect (visible through walls)")
                .define("glowingEffect", true);

        eliteNamePrefix = builder
                .comment("Custom name prefix for elite mobs (empty = no custom name)")
                .define("namePrefix", "Runic");

        eliteBonusXP = builder
                .comment("Bonus XP dropped by elite mobs when killed by a player")
                .defineInRange("bonusXP", 30, 0, 500);

        eliteMaxBaseHealth = builder
                .comment("Skip elite enhancement if the mob's base health already exceeds this value",
                        "Prevents stat-stacking with InControl elites and Majrusz's difficulty mobs",
                        "Set to 0 to disable this check")
                .defineInRange("maxBaseHealth", 40.0, 0.0, 1000.0);

        builder.pop();
    }

    private static void buildDifficultyConfig(ForgeConfigSpec.Builder builder) {
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
                .defineInRange("normalCapMultiplier", 0.8, 0.1, 4.0);

        hardCapMultiplier = builder
                .comment("Spawn cap multiplier on Hard difficulty")
                .defineInRange("hardCapMultiplier", 1.2, 0.1, 4.0);

        easyIntervalMultiplier = builder
                .comment("Spawn interval multiplier on Easy difficulty (higher = slower spawns)")
                .defineInRange("easyIntervalMultiplier", 1.5, 0.25, 4.0);

        normalIntervalMultiplier = builder
                .comment("Spawn interval multiplier on Normal difficulty")
                .defineInRange("normalIntervalMultiplier", 1.1, 0.25, 4.0);

        hardIntervalMultiplier = builder
                .comment("Spawn interval multiplier on Hard difficulty (lower = faster spawns)")
                .defineInRange("hardIntervalMultiplier", 0.85, 0.25, 4.0);

        builder.pop();
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

        EquipmentPools.refresh();
        StructureProfileLoader.reload();
        TickSpawnHandler.invalidateCaches();
        StructureDetection.invalidateCache();
    }

    static Set<ResourceLocation> parseResourceLocations(List<? extends String> list, String configKey) {
        return list.stream()
                .map(s -> {
                    ResourceLocation rl = ResourceLocation.tryParse((String) s);
                    if (rl == null) {
                        RunicStructures.LOGGER.warn("[RunicStructures] Invalid ResourceLocation '{}' in config key '{}', skipping", s, configKey);
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

    public static Set<ResourceLocation> getAllowedDimensions() {
        return parsedDimensions;
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
