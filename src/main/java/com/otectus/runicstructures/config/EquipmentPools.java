package com.otectus.runicstructures.config;

import com.otectus.runicstructures.RunicStructures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Resolves config-driven equipment pools (weapons, armor sets, shields) from
 * ResourceLocation strings to actual Item instances. Rebuilt on config reload.
 */
public class EquipmentPools {

    private static final Predicate<Item> SHIELD_VALIDATOR = item -> item instanceof ShieldItem;

    // Vanilla armor tier registry: name -> [helmet, chestplate, leggings, boots]
    private static final Map<String, Item[]> VANILLA_ARMOR_TIERS;
    static {
        Map<String, Item[]> m = new HashMap<>();
        m.put("leather", new Item[]{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS});
        m.put("chainmail", new Item[]{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS});
        m.put("iron", new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS});
        m.put("gold", new Item[]{Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS});
        m.put("diamond", new Item[]{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS});
        m.put("netherite", new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS});
        VANILLA_ARMOR_TIERS = Collections.unmodifiableMap(m);
    }

    // Resolved from TOML config at refresh time
    private static volatile List<Item> globalWeaponPool = List.of();
    private static volatile List<Item> globalShieldPool = List.of();
    private static volatile Map<String, Item[]> customArmorTiers = Map.of();
    private static volatile List<String> validationWarnings = List.of();

    /**
     * Rebuild pools from current RSConfig values. Called from RSConfig.refresh().
     */
    public static void refresh() {
        List<String> warnings = new ArrayList<>();
        globalWeaponPool = resolveItemList(RSConfig.weaponPool.get(), "weaponPool", warnings, null, null);
        globalShieldPool = resolveItemList(RSConfig.shieldPool.get(), "shieldPool", warnings, SHIELD_VALIDATOR, "shield");
        customArmorTiers = parseCustomArmorSets(RSConfig.customArmorSets.get(), warnings);
        validationWarnings = List.copyOf(warnings);
    }

    public static List<String> getValidationWarnings() {
        return validationWarnings;
    }

    /**
     * Resolve the weapon pool for a given profile. Returns the profile's custom pool
     * if set, otherwise the global TOML pool supplemented by the hardcoded fallback.
     */
    public static List<Item> resolveWeaponPool(StructureProfile profile) {
        if (profile.hasWeaponPoolOverride()) {
            return profile.getWeaponPool();
        }
        if (!globalWeaponPool.isEmpty()) return globalWeaponPool;
        // Hardcoded fallback: stone/iron/diamond swords
        return List.of(Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD);
    }

    /**
     * Resolve armor tiers for a given profile. Returns the profile's tier subset
     * if set, otherwise the global TOML-enabled tiers.
     */
    public static List<Item[]> resolveArmorTiers(StructureProfile profile) {
        if (profile.hasArmorTierOverride()) {
            return profile.getArmorTiers();
        }
        return resolveGlobalArmorTiers();
    }

    /**
     * Resolve shield pool for a given profile.
     */
    public static List<Item> resolveShieldPool(StructureProfile profile) {
        if (profile.hasShieldPoolOverride()) {
            return profile.getShieldPool();
        }
        if (!globalShieldPool.isEmpty()) return globalShieldPool;
        return List.of(Items.SHIELD);
    }

    /**
     * Look up an armor tier by name (vanilla or custom).
     */
    @Nullable
    public static Item[] lookupArmorTier(String name) {
        String key = name.trim().toLowerCase();
        Item[] tier = VANILLA_ARMOR_TIERS.get(key);
        if (tier != null) return tier;
        return customArmorTiers.get(key);
    }

    public static List<Item[]> resolveProfileArmorTiers(@Nullable List<String> tierNames, String profileId, List<String> warnings) {
        if (tierNames == null || tierNames.isEmpty()) return List.of();

        List<Item[]> result = new ArrayList<>();
        for (String tierName : tierNames) {
            Item[] tier = lookupArmorTier(tierName);
            if (tier == null) {
                warn(warnings, "Profile '" + profileId + "' references unknown armor tier '" + tierName + "'");
                continue;
            }
            result.add(tier);
        }
        return List.copyOf(result);
    }

    public static List<Item> resolveProfileWeaponPool(@Nullable List<String> items, String profileId, List<String> warnings) {
        if (items == null || items.isEmpty()) return List.of();
        return resolveItemList(items, "profile '" + profileId + "' weaponPool", warnings, null, null);
    }

    public static List<Item> resolveProfileShieldPool(@Nullable List<String> items, String profileId, List<String> warnings) {
        if (items == null || items.isEmpty()) return List.of();
        return resolveItemList(items, "profile '" + profileId + "' shieldPool", warnings, SHIELD_VALIDATOR, "shield");
    }

    // --- Internals ---

    private static List<Item[]> resolveGlobalArmorTiers() {
        List<Item[]> tiers = new ArrayList<>();
        if (RSConfig.allowLeather.get())    tiers.add(VANILLA_ARMOR_TIERS.get("leather"));
        if (RSConfig.allowChainmail.get())  tiers.add(VANILLA_ARMOR_TIERS.get("chainmail"));
        if (RSConfig.allowIron.get())       tiers.add(VANILLA_ARMOR_TIERS.get("iron"));
        if (RSConfig.allowGold.get())       tiers.add(VANILLA_ARMOR_TIERS.get("gold"));
        if (RSConfig.allowDiamond.get())    tiers.add(VANILLA_ARMOR_TIERS.get("diamond"));
        if (RSConfig.allowNetherite.get())  tiers.add(VANILLA_ARMOR_TIERS.get("netherite"));
        return tiers;
    }

    private static List<Item> resolveItemList(
            List<? extends String> list,
            String configKey,
            List<String> warnings,
            @Nullable Predicate<Item> validator,
            @Nullable String expectedType
    ) {
        List<Item> result = new ArrayList<>();
        for (String s : list) {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                warn(warnings, "Invalid item '" + s + "' in " + configKey + ", skipping");
                continue;
            }

            Item item = resolveItem(rl, warnings, configKey);
            if (item == null) continue;

            if (validator != null && !validator.test(item)) {
                warn(warnings, "Item '" + rl + "' in " + configKey + " is not a valid " + expectedType + ", skipping");
                continue;
            }

            result.add(item);
        }
        return List.copyOf(result);
    }

    /**
     * Parse custom armor set definitions from config.
     * Format: "name=namespace:helmet,namespace:chest,namespace:legs,namespace:boots"
     */
    private static Map<String, Item[]> parseCustomArmorSets(List<? extends String> list, List<String> warnings) {
        Map<String, Item[]> result = new HashMap<>();
        for (String s : list) {
            int eqIdx = s.indexOf('=');
            if (eqIdx < 0) {
                warn(warnings, "Invalid custom armor set format '" + s + "', expected 'name=helmet,chest,legs,boots'");
                continue;
            }

            String name = s.substring(0, eqIdx).trim().toLowerCase();
            String[] parts = s.substring(eqIdx + 1).split(",");
            if (parts.length != 4) {
                warn(warnings, "Custom armor set '" + name + "' needs exactly 4 items (helmet,chest,legs,boots), got " + parts.length);
                continue;
            }

            Item[] items = new Item[4];
            boolean valid = true;
            for (int i = 0; i < 4; i++) {
                String itemId = parts[i].trim();
                ResourceLocation rl = ResourceLocation.tryParse(itemId);
                if (rl == null) {
                    warn(warnings, "Invalid item '" + itemId + "' in custom armor set '" + name + "'");
                    valid = false;
                    break;
                }

                Item item = resolveItem(rl, warnings, "custom armor set '" + name + "'");
                if (item == null) {
                    valid = false;
                    break;
                }
                items[i] = item;
            }

            if (valid) {
                result.put(name, items);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Nullable
    static Item resolveItem(ResourceLocation rl, List<String> warnings, String configKey) {
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            warn(warnings, "Unknown item '" + rl + "' in " + configKey + ", skipping");
            return null;
        }
        return item;
    }

    private static void warn(List<String> warnings, String message) {
        warnings.add(message);
        RunicStructures.LOGGER.warn("[RunicStructures] {}", message);
    }

    /** Get a tier's drop chance multiplier. Diamond=0.5x, Netherite=0.25x, everything else=1.0x. */
    public static float getDropMultiplierForTier(Item[] tier) {
        if (tier == VANILLA_ARMOR_TIERS.get("diamond")) {
            return RSConfig.diamondDropMultiplier.get().floatValue();
        } else if (tier == VANILLA_ARMOR_TIERS.get("netherite")) {
            return RSConfig.netheriteDropMultiplier.get().floatValue();
        }
        return 1.0f;
    }
}
