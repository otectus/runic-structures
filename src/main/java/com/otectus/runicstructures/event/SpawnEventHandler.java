package com.otectus.runicstructures.event;

import com.otectus.runicstructures.RunicStructures;
import com.otectus.runicstructures.config.RSConfig;
import com.otectus.runicstructures.config.StructureProfile;
import com.otectus.runicstructures.config.StructureProfileLoader;
import com.otectus.runicstructures.util.MobEnhancer;
import com.otectus.runicstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RunicStructures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpawnEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck evt) {
        if (!RSConfig.enabled.get()) return;
        if (evt.getLevel().getLevel().getDifficulty() == Difficulty.PEACEFUL) return;
        if (evt.getEntityType().getCategory() != MobCategory.MONSTER) return;
        if (evt.getSpawnType() != MobSpawnType.NATURAL) return;
        if (!RSConfig.isEntityAllowed(evt.getEntityType())) return;
        if (evt.getDefaultResult()) return;

        // Only override bright-environment placement failures so obstruction and other
        // non-light checks still run through the normal position validation path.
        if (evt.getLevel().getMaxLocalRawBrightness(evt.getPos()) > 0
                && StructureDetection.isInRunicStructure(evt.getLevel(), evt.getPos())) {
            evt.setResult(Event.Result.ALLOW);

            if (RSConfig.isDebugEnabled()) {
                RunicStructures.LOGGER.info("[RunicStructures] Forced placement of {} at {}",
                        evt.getEntityType().getDescriptionId(), evt.getPos());
            }
        }
    }

    /**
     * Apply enhancements to hostile mobs spawned inside runic structures.
     * Uses per-structure profiles for equipment customization.
     * Only fires for event-intercepted spawns; periodic/initial spawns are tagged and skipped.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn evt) {
        if (!RSConfig.enabled.get()) return;
        if (evt.getLevel().getLevel().getDifficulty() == Difficulty.PEACEFUL) return;
        if (evt.getEntity().getType().getCategory() != MobCategory.MONSTER) return;
        if (evt.getSpawnType() != MobSpawnType.NATURAL) return;
        if (!RSConfig.isEntityAllowed(evt.getEntity().getType())) return;

        // Skip mobs already being enhanced by the periodic/initial spawner
        if (evt.getEntity().getTags().contains("rs_periodic")) return;

        BlockPos pos = evt.getEntity().blockPosition();
        ResourceLocation structureId = StructureDetection.getRunicStructureId(evt.getLevel(), pos);
        if (structureId != null) {
            long dayCount = evt.getLevel().getLevel().getDayTime() / 24000L;
            StructureProfile profile = StructureProfileLoader.getProfile(structureId, dayCount);
            Mob mob = evt.getEntity();

            if (RSConfig.eliteSpawnsEnabled.get()
                    && evt.getLevel().getRandom().nextDouble() < profile.getEliteChance()) {
                MobEnhancer.applyEliteEnhancements(mob, profile);
            }

            if (RSConfig.armorEnabled.get()) {
                MobEnhancer.applyRandomArmor(mob, evt.getLevel().getRandom(), profile);
            }

            if (RSConfig.weaponsEnabled.get()) {
                MobEnhancer.applyRandomWeapon(mob, evt.getLevel().getRandom(), profile);
            }

            if (RSConfig.shieldsEnabled.get()) {
                MobEnhancer.applyRandomShield(mob, evt.getLevel().getRandom(), profile);
            }

            if (RSConfig.enchantmentsEnabled.get()) {
                MobEnhancer.applyRandomEnchantments(mob, evt.getLevel().getRandom(), profile);
            }

            if (RSConfig.fireImmunity.get()) {
                MobEnhancer.applyFireResistance(mob);
            }
        }
    }
}
