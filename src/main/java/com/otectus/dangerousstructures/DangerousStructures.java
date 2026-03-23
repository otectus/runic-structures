package com.otectus.dangerousstructures;

import com.mojang.logging.LogUtils;
import com.otectus.dangerousstructures.config.DSConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

@Mod(DangerousStructures.MOD_ID)
public class DangerousStructures {
    public static final String MOD_ID = "dangerousstructures";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DangerousStructures() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DSConfig.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ConfigEvents {
        @SubscribeEvent
        public static void onConfigLoad(ModConfigEvent.Loading evt) {
            if (evt.getConfig().getModId().equals(MOD_ID)) {
                DSConfig.refresh();
                LOGGER.info("[DangerousStructures] Config loaded");
            }
        }

        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading evt) {
            if (evt.getConfig().getModId().equals(MOD_ID)) {
                DSConfig.refresh();
                LOGGER.info("[DangerousStructures] Config reloaded");
            }
        }
    }
}
