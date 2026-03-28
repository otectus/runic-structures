package com.otectus.runicstructures.config;

import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Immutable per-structure override profile.
 *
 * Null scalar fields mean "fall back to RSConfig".
 * Collection fields are eagerly validated and resolved at load time.
 */
public final class StructureProfile {

    public enum ProgressionTier {
        EARLY("early", 0),
        MID("mid", 10),
        LATE("late", 30),
        VETERAN("veteran", 50);

        private final String id;
        private final int requiredDay;

        ProgressionTier(String id, int requiredDay) {
            this.id = id;
            this.requiredDay = requiredDay;
        }

        public int requiredDay() {
            return requiredDay;
        }

        public String id() {
            return id;
        }

        @Nullable
        public static ProgressionTier fromString(@Nullable String value) {
            if (value == null) return null;

            return switch (value.toLowerCase(Locale.ROOT)) {
                case "early" -> EARLY;
                case "mid" -> MID;
                case "late" -> LATE;
                case "veteran" -> VETERAN;
                default -> null;
            };
        }
    }

    public static final StructureProfile DEFAULT = new StructureProfile(
            null, null, null, null,
            List.of(), List.of(),
            null, List.of(),
            null,
            null, null, null, null,
            null
    );

    @Nullable private final Integer spawnCap;
    @Nullable private final Double eliteChance;
    @Nullable private final String eliteNamePrefix;
    @Nullable private final Boolean persistentMobs;

    private final List<Item[]> armorTiers;
    private final List<Item> weaponPool;

    @Nullable private final Double shieldChance;
    private final List<Item> shieldPool;

    @Nullable private final Integer enchantmentLevel;

    @Nullable private final Boolean darkness;
    @Nullable private final Boolean miningFatigue;
    @Nullable private final Boolean slowness;
    @Nullable private final Boolean ambientSounds;

    @Nullable private final ProgressionTier progressionTier;

    public StructureProfile(
            @Nullable Integer spawnCap,
            @Nullable Double eliteChance,
            @Nullable String eliteNamePrefix,
            @Nullable Boolean persistentMobs,
            @Nullable List<Item[]> armorTiers,
            @Nullable List<Item> weaponPool,
            @Nullable Double shieldChance,
            @Nullable List<Item> shieldPool,
            @Nullable Integer enchantmentLevel,
            @Nullable Boolean darkness,
            @Nullable Boolean miningFatigue,
            @Nullable Boolean slowness,
            @Nullable Boolean ambientSounds,
            @Nullable ProgressionTier progressionTier
    ) {
        this.spawnCap = spawnCap;
        this.eliteChance = eliteChance;
        this.eliteNamePrefix = eliteNamePrefix;
        this.persistentMobs = persistentMobs;
        this.armorTiers = armorTiers == null ? List.of() : List.copyOf(armorTiers);
        this.weaponPool = weaponPool == null ? List.of() : List.copyOf(weaponPool);
        this.shieldChance = shieldChance;
        this.shieldPool = shieldPool == null ? List.of() : List.copyOf(shieldPool);
        this.enchantmentLevel = enchantmentLevel;
        this.darkness = darkness;
        this.miningFatigue = miningFatigue;
        this.slowness = slowness;
        this.ambientSounds = ambientSounds;
        this.progressionTier = progressionTier;
    }

    public int getSpawnCap() {
        return spawnCap != null ? spawnCap : RSConfig.periodicSpawnCap.get();
    }

    public double getEliteChance() {
        return eliteChance != null ? eliteChance : RSConfig.eliteSpawnChance.get();
    }

    public String getEliteNamePrefix() {
        return eliteNamePrefix != null ? eliteNamePrefix : RSConfig.eliteNamePrefix.get();
    }

    public boolean isPersistentMobs() {
        return persistentMobs != null ? persistentMobs : RSConfig.persistentMobs.get();
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel != null ? enchantmentLevel : RSConfig.maxEnchantmentLevel.get();
    }

    public boolean isDarknessEnabled() {
        return darkness != null ? darkness : RSConfig.applyDarknessEffect.get();
    }

    public boolean isMiningFatigueEnabled() {
        return miningFatigue != null ? miningFatigue : RSConfig.applyMiningFatigue.get();
    }

    public boolean isSlownessEnabled() {
        return slowness != null ? slowness : RSConfig.applySlowness.get();
    }

    public boolean isAmbientSoundsEnabled() {
        return ambientSounds != null ? ambientSounds : RSConfig.playAmbientSounds.get();
    }

    public double getShieldChance() {
        return shieldChance != null ? shieldChance : RSConfig.shieldChance.get();
    }

    public boolean hasArmorTierOverride() {
        return !armorTiers.isEmpty();
    }

    public List<Item[]> getArmorTiers() {
        return armorTiers;
    }

    public boolean hasWeaponPoolOverride() {
        return !weaponPool.isEmpty();
    }

    public List<Item> getWeaponPool() {
        return weaponPool;
    }

    public boolean hasShieldPoolOverride() {
        return !shieldPool.isEmpty();
    }

    public List<Item> getShieldPool() {
        return shieldPool;
    }

    @Nullable
    public ProgressionTier getProgressionTier() {
        return progressionTier;
    }

    /**
     * Check if this profile's progression tier requirement is met by the current server day.
     * Returns true if no progression tier is set, or if the server day meets/exceeds the tier.
     */
    public boolean isProgressionMet(long serverDayCount) {
        if (progressionTier == null || !RSConfig.progressionScalingEnabled.get()) return true;
        return serverDayCount >= progressionTier.requiredDay();
    }
}
