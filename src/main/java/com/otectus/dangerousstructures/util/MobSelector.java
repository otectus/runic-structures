package com.otectus.dangerousstructures.util;

import com.otectus.dangerousstructures.config.DSConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mob type selection utilities for periodic and initial population spawning.
 */
public class MobSelector {

    private static volatile List<EntityType<?>> cachedSpawnableMobs = null;

    public static void invalidateCache() {
        cachedSpawnableMobs = null;
    }

    /**
     * Pick a biome-specific spawner data entry, respecting vanilla spawn weights.
     */
    public static MobSpawnSettings.SpawnerData pickBiomeSpecificSpawnerData(
            ServerLevel level, BlockPos pos, RandomSource random) {
        List<MobSpawnSettings.SpawnerData> spawners = level.getBiome(pos).value()
                .getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        if (spawners.isEmpty()) return null;

        List<MobSpawnSettings.SpawnerData> eligible = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData data : spawners) {
            if (DSConfig.isEntityAllowed(data.type)) {
                eligible.add(data);
            }
        }
        if (eligible.isEmpty()) return null;

        if (DSConfig.useVanillaSpawnWeights.get()) {
            // Weighted random selection
            int totalWeight = 0;
            for (MobSpawnSettings.SpawnerData data : eligible) {
                totalWeight += data.getWeight().asInt();
            }
            if (totalWeight <= 0) return eligible.get(random.nextInt(eligible.size()));

            int roll = random.nextInt(totalWeight);
            for (MobSpawnSettings.SpawnerData data : eligible) {
                roll -= data.getWeight().asInt();
                if (roll < 0) return data;
            }
            return eligible.get(eligible.size() - 1);
        } else {
            return eligible.get(random.nextInt(eligible.size()));
        }
    }

    public static EntityType<?> pickRandomHostileMob(RandomSource random) {
        List<EntityType<?>> mobs = getSpawnableMobs();
        if (mobs.isEmpty()) return null;
        return mobs.get(random.nextInt(mobs.size()));
    }

    private static List<EntityType<?>> getSpawnableMobs() {
        List<EntityType<?>> cached = cachedSpawnableMobs;
        if (cached != null) return cached;

        Set<ResourceLocation> whitelist = DSConfig.getMobWhitelist();
        Set<ResourceLocation> blacklist = DSConfig.getMobBlacklist();

        List<EntityType<?>> result;
        if (!whitelist.isEmpty()) {
            List<EntityType<?>> list = new ArrayList<>();
            for (ResourceLocation rl : whitelist) {
                EntityType<?> et = ForgeRegistries.ENTITY_TYPES.getValue(rl);
                if (et != null) list.add(et);
            }
            result = List.copyOf(list);
        } else {
            List<EntityType<?>> list = new ArrayList<>();
            for (EntityType<?> et : ForgeRegistries.ENTITY_TYPES.getValues()) {
                if (et.getCategory() != MobCategory.MONSTER) continue;
                if (!blacklist.isEmpty() && blacklist.contains(EntityType.getKey(et))) continue;
                list.add(et);
            }
            result = List.copyOf(list);
        }

        cachedSpawnableMobs = result;
        return result;
    }
}
