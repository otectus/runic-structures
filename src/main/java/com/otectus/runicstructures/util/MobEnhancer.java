package com.otectus.runicstructures.util;

import com.otectus.runicstructures.config.EquipmentPools;
import com.otectus.runicstructures.config.RSConfig;
import com.otectus.runicstructures.config.StructureProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/**
 * Mob enhancement utilities: armor, weapons, shields, enchantments, elite buffs, fire resistance.
 * All equipment methods accept a {@link StructureProfile} for per-structure customization.
 */
public class MobEnhancer {

    // Armor enchantment pool with weights (total 100)
    private static final Object[][] ARMOR_ENCHANT_POOL = {
            {Enchantments.ALL_DAMAGE_PROTECTION, 55},
            {Enchantments.BLAST_PROTECTION, 15},
            {Enchantments.PROJECTILE_PROTECTION, 15},
            {Enchantments.THORNS, 15},
    };
    private static final int ARMOR_ENCHANT_TOTAL_WEIGHT = 100;

    // Weapon enchantment pool with weights (total 100)
    private static final Object[][] MELEE_ENCHANT_POOL = {
            {Enchantments.SHARPNESS, 50},
            {Enchantments.KNOCKBACK, 25},
            {Enchantments.FIRE_ASPECT, 25},
    };
    private static final int MELEE_ENCHANT_TOTAL_WEIGHT = 100;

    /**
     * Apply elite enhancements to a mob: boosted stats, glow, custom name.
     */
    public static void applyEliteEnhancements(Mob mob, StructureProfile profile) {
        // Anti-stacking: skip if mob is already buffed beyond threshold
        double threshold = RSConfig.eliteMaxBaseHealth.get();
        if (threshold > 0) {
            AttributeInstance checkHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (checkHealth != null && checkHealth.getBaseValue() > threshold) {
                return;
            }
        }

        AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * RSConfig.eliteHealthMultiplier.get();
            healthAttr.setBaseValue(newHealth);
            mob.setHealth((float) newHealth);
        }

        AttributeInstance damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * RSConfig.eliteDamageMultiplier.get());
        }

        if (RSConfig.eliteGlowingEffect.get()) {
            mob.setGlowingTag(true);
        }

        String prefix = profile.getEliteNamePrefix();
        if (prefix != null && !prefix.isEmpty()) {
            mob.setCustomName(Component.translatable("entity.runicstructures.elite_prefix",
                    prefix, mob.getType().getDescription()));
            mob.setCustomNameVisible(true);
        }

        if (profile.isPersistentMobs()) {
            mob.setPersistenceRequired();
        }

        mob.addTag("rs_elite");
    }

    /**
     * Equip a mob with randomized armor from the profile's tier pool (or global config).
     * Uses {@link EquipmentPools} for tier resolution including custom modded sets.
     */
    public static void applyRandomArmor(Mob mob, RandomSource random, StructureProfile profile) {
        if (RSConfig.respectExistingArmor.get()) {
            if (!mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                    || !mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    || !mob.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                    || !mob.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
                return;
            }
        }

        List<Item[]> tiers = EquipmentPools.resolveArmorTiers(profile);
        if (tiers.isEmpty()) return;

        float baseDropChance = RSConfig.armorDropChance.get().floatValue();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        Item[] selectedTier = tiers.get(random.nextInt(tiers.size()));

        for (int i = 0; i < slots.length; i++) {
            mob.setItemSlot(slots[i], new ItemStack(selectedTier[i]));
            mob.setDropChance(slots[i], baseDropChance * EquipmentPools.getDropMultiplierForTier(selectedTier));
        }
    }

    /**
     * Equip a mob with a random weapon from the profile's pool (or global config).
     * Skips mobs with ranged weapons to preserve their AI.
     */
    public static void applyRandomWeapon(Mob mob, RandomSource random, StructureProfile profile) {
        if (mob instanceof AbstractSkeleton) return;
        ItemStack currentMainhand = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (currentMainhand.getItem() instanceof ProjectileWeaponItem) return;

        if (RSConfig.respectExistingWeapon.get()) {
            if (!currentMainhand.isEmpty()) {
                return;
            }
        }

        List<Item> weapons = EquipmentPools.resolveWeaponPool(profile);
        if (weapons.isEmpty()) return;

        Item weapon = weapons.get(random.nextInt(weapons.size()));
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        mob.setDropChance(EquipmentSlot.MAINHAND, RSConfig.weaponDropChance.get().floatValue());
    }

    /**
     * Optionally equip a melee mob with a shield in the offhand slot.
     * Skips ranged mobs and mobs that already have an offhand item.
     */
    public static void applyRandomShield(Mob mob, RandomSource random, StructureProfile profile) {
        if (mob instanceof AbstractSkeleton) return;
        if (mob.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof ProjectileWeaponItem) return;
        if (!mob.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) return;

        double chance = profile.getShieldChance();
        if (chance <= 0 || random.nextDouble() >= chance) return;

        List<Item> shields = EquipmentPools.resolveShieldPool(profile);
        if (shields.isEmpty()) return;

        Item shield = shields.get(random.nextInt(shields.size()));
        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(shield));
        mob.setDropChance(EquipmentSlot.OFFHAND, RSConfig.armorDropChance.get().floatValue());
    }

    /**
     * Apply random enchantments to equipment already on the mob.
     * Uses per-structure enchantment level override.
     */
    public static void applyRandomEnchantments(Mob mob, RandomSource random, StructureProfile profile) {
        int maxLevel = profile.getEnchantmentLevel();
        if (maxLevel <= 0) return;

        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (!stack.isEmpty() && !stack.isEnchanted()) {
                Enchantment enchant = pickWeightedEnchantment(ARMOR_ENCHANT_POOL, ARMOR_ENCHANT_TOTAL_WEIGHT, random);
                stack.enchant(enchant, random.nextIntBetweenInclusive(1, maxLevel));
            }
        }

        ItemStack mainhand = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainhand.isEmpty() && !mainhand.isEnchanted()) {
            if (mainhand.getItem() instanceof ProjectileWeaponItem) {
                mainhand.enchant(Enchantments.POWER_ARROWS, random.nextIntBetweenInclusive(1, maxLevel));
            } else {
                Enchantment enchant = pickWeightedEnchantment(MELEE_ENCHANT_POOL, MELEE_ENCHANT_TOTAL_WEIGHT, random);
                mainhand.enchant(enchant, random.nextIntBetweenInclusive(1, maxLevel));
            }
        }
    }

    /**
     * Apply permanent Fire Resistance to a mob.
     */
    public static void applyFireResistance(Mob mob) {
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                Integer.MAX_VALUE, 0, true, false, false));
    }

    private static Enchantment pickWeightedEnchantment(Object[][] pool, int totalWeight, RandomSource random) {
        int roll = random.nextInt(totalWeight);
        for (Object[] entry : pool) {
            roll -= (int) entry[1];
            if (roll < 0) return (Enchantment) entry[0];
        }
        return (Enchantment) pool[0][0];
    }
}
