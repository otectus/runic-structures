package com.otectus.dangerousstructures.event;

import com.otectus.dangerousstructures.DangerousStructures;
import com.otectus.dangerousstructures.config.DSConfig;
import com.otectus.dangerousstructures.util.MobEnhancer;
import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DangerousStructures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpawnEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck evt) {
        if (!DSConfig.enabled.get()) return;
        if (evt.getLevel().getLevel().getDifficulty() == Difficulty.PEACEFUL) return;
        if (evt.getEntityType().getCategory() != MobCategory.MONSTER) return;
        if (evt.getSpawnType() != MobSpawnType.NATURAL) return;
        if (!DSConfig.isEntityAllowed(evt.getEntityType())) return;

        if (StructureDetection.isInDangerousStructure(evt.getLevel(), evt.getPos())) {
            evt.setResult(Event.Result.ALLOW);

            if (DSConfig.isDebugEnabled()) {
                DangerousStructures.LOGGER.info("[DangerousStructures] Forced placement of {} at {}",
                        evt.getEntityType().getDescriptionId(), evt.getPos());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPositionCheck(MobSpawnEvent.PositionCheck evt) {
        if (!DSConfig.enabled.get()) return;
        if (evt.getLevel().getLevel().getDifficulty() == Difficulty.PEACEFUL) return;
        if (evt.getEntity().getType().getCategory() != MobCategory.MONSTER) return;
        if (evt.getSpawnType() != MobSpawnType.NATURAL) return;
        if (!DSConfig.isEntityAllowed(evt.getEntity().getType())) return;

        BlockPos pos = BlockPos.containing(evt.getX(), evt.getY(), evt.getZ());
        if (StructureDetection.isInDangerousStructure(evt.getLevel(), pos)) {
            evt.setResult(Event.Result.ALLOW);

            if (DSConfig.isDebugEnabled()) {
                DangerousStructures.LOGGER.info("[DangerousStructures] Forced spawn of {} at {}",
                        evt.getEntity().getType().getDescriptionId(), pos);
            }
        }
    }

    /**
     * Apply enhancements (armor, fire immunity) to hostile mobs spawned inside dangerous structures.
     * Only fires for event-intercepted spawns; periodic/initial spawns are tagged and skipped.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn evt) {
        if (!DSConfig.enabled.get()) return;
        if (evt.getLevel().getLevel().getDifficulty() == Difficulty.PEACEFUL) return;
        if (evt.getEntity().getType().getCategory() != MobCategory.MONSTER) return;
        if (evt.getSpawnType() != MobSpawnType.NATURAL) return;
        if (!DSConfig.isEntityAllowed(evt.getEntity().getType())) return;

        // Skip mobs already being enhanced by the periodic/initial spawner
        if (evt.getEntity().getTags().contains("ds_periodic")) return;

        BlockPos pos = evt.getEntity().blockPosition();
        if (StructureDetection.isInDangerousStructure(evt.getLevel(), pos)) {
            Mob mob = evt.getEntity();

            if (DSConfig.armorEnabled.get()) {
                MobEnhancer.applyRandomArmor(mob, evt.getLevel().getRandom());
            }

            if (DSConfig.fireImmunity.get()) {
                MobEnhancer.applyFireResistance(mob);
            }
        }
    }
}
