package com.otectus.dangerousstructures.event;

import com.otectus.dangerousstructures.DangerousStructures;
import com.otectus.dangerousstructures.config.DSConfig;
import com.otectus.dangerousstructures.util.StructureDetection;
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
        if (evt.getEntityType().getCategory() != MobCategory.MONSTER) return;
        if (evt.getSpawnType() != MobSpawnType.NATURAL) return;
        if (!DSConfig.isEntityAllowed(evt.getEntityType())) return;

        if (StructureDetection.isInDangerousStructure(evt.getLevel(), evt.getPos())) {
            evt.setResult(Event.Result.ALLOW);

            if (DSConfig.debugLogging.get()) {
                DangerousStructures.LOGGER.info("[DangerousStructures] Forced spawn of {} at {}",
                        evt.getEntityType().getDescriptionId(), evt.getPos());
            }
        }
    }
}
