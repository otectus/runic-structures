package com.otectus.dangerousstructures.util;

import com.otectus.dangerousstructures.config.DSConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Mob enhancement utilities: armor, elite buffs, fire resistance.
 */
public class MobEnhancer {

    /**
     * Apply elite enhancements to a mob: boosted stats, glow, custom name.
     */
    public static void applyEliteEnhancements(Mob mob) {
        // Boost health
        AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * DSConfig.eliteHealthMultiplier.get();
            healthAttr.setBaseValue(newHealth);
            mob.setHealth((float) newHealth);
        }

        // Boost damage
        AttributeInstance damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * DSConfig.eliteDamageMultiplier.get());
        }

        // Glowing effect
        if (DSConfig.eliteGlowingEffect.get()) {
            mob.setGlowingTag(true);
        }

        // Custom name
        String prefix = DSConfig.eliteNamePrefix.get();
        if (prefix != null && !prefix.isEmpty()) {
            String mobName = mob.getType().getDescription().getString();
            mob.setCustomName(Component.literal(prefix + " " + mobName));
            mob.setCustomNameVisible(true);
        }

        // Persistence (respects config)
        if (DSConfig.persistentMobs.get()) {
            mob.setPersistenceRequired();
        }

        // Tag the mob for bonus XP identification
        mob.addTag("ds_elite");
    }

    /**
     * Equip a mob with a full suit of randomized armor from the configured tier pool.
     * Each armor slot gets an independently randomized material tier.
     */
    public static void applyRandomArmor(Mob mob, RandomSource random) {
        // Build the pool of enabled armor tiers: [helmet, chestplate, leggings, boots]
        List<Item[]> tiers = new ArrayList<>();
        if (DSConfig.allowLeather.get())    tiers.add(new Item[]{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS});
        if (DSConfig.allowChainmail.get())  tiers.add(new Item[]{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS});
        if (DSConfig.allowIron.get())       tiers.add(new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS});
        if (DSConfig.allowGold.get())       tiers.add(new Item[]{Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS});
        if (DSConfig.allowDiamond.get())    tiers.add(new Item[]{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS});
        if (DSConfig.allowNetherite.get())  tiers.add(new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS});
        if (tiers.isEmpty()) return;

        float dropChance = DSConfig.armorDropChance.get().floatValue();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        for (int i = 0; i < slots.length; i++) {
            Item[] tier = tiers.get(random.nextInt(tiers.size()));
            mob.setItemSlot(slots[i], new ItemStack(tier[i]));
            mob.setDropChance(slots[i], dropChance);
        }
    }

    /**
     * Apply permanent Fire Resistance to a mob.
     */
    public static void applyFireResistance(Mob mob) {
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                Integer.MAX_VALUE, 0, true, false, false));
    }
}
