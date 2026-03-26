package com.otectus.dangerousstructures;

import com.mojang.logging.LogUtils;
import com.otectus.dangerousstructures.command.DSCommand;
import com.otectus.dangerousstructures.config.DSConfig;
import com.otectus.dangerousstructures.event.TickSpawnHandler;
import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod(DangerousStructures.MOD_ID)
public class DangerousStructures {
    public static final String MOD_ID = "dangerousstructures";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DangerousStructures() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DSConfig.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent evt) {
            TickSpawnHandler.onServerStopped();
            StructureDetection.onServerStopped();
            LOGGER.info("[DangerousStructures] Server stopped, caches cleared");
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent evt) {
            if (evt.getPlayer() == null) {
                TickSpawnHandler.invalidateCaches();
                StructureDetection.invalidateCache();
                LOGGER.info("[DangerousStructures] Datapack reload detected, caches invalidated");
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent evt) {
            DSCommand.register(evt.getDispatcher());
        }

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent evt) {
            // Config validation against registries
            validateConfig(evt);
        }

        @SubscribeEvent
        public static void onEliteMobDeath(LivingDeathEvent evt) {
            // Grant bonus XP when an elite mob is killed by a player
            if (!DSConfig.eliteSpawnsEnabled.get()) return;
            if (!(evt.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
            if (!mob.getTags().contains("ds_elite")) return;

            int bonusXP = DSConfig.eliteBonusXP.get();
            if (bonusXP <= 0) return;

            if (evt.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                player.giveExperiencePoints(bonusXP);
            }
        }

        private static void validateConfig(ServerStartedEvent evt) {
            var registryAccess = evt.getServer().registryAccess();

            // Validate structure IDs
            var structureRegistry = registryAccess.registryOrThrow(Registries.STRUCTURE);
            for (ResourceLocation rl : DSConfig.getStructureWhitelist()) {
                if (!structureRegistry.containsKey(rl)) {
                    LOGGER.warn("[DangerousStructures] Unknown structure '{}' in structureWhitelist", rl);
                }
            }
            for (ResourceLocation rl : DSConfig.getStructureBlacklist()) {
                if (!structureRegistry.containsKey(rl)) {
                    LOGGER.warn("[DangerousStructures] Unknown structure '{}' in structureBlacklist", rl);
                }
            }

            // Validate mob IDs
            for (ResourceLocation rl : DSConfig.getMobWhitelist()) {
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
                    LOGGER.warn("[DangerousStructures] Unknown entity '{}' in mobWhitelist", rl);
                }
            }
            for (ResourceLocation rl : DSConfig.getMobBlacklist()) {
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
                    LOGGER.warn("[DangerousStructures] Unknown entity '{}' in mobBlacklist", rl);
                }
            }

            // Warn about contradictory config
            if (!DSConfig.autoDetectStructures.get()
                    && DSConfig.getStructureWhitelist().isEmpty()
                    && DSConfig.getStructureTagWhitelist().isEmpty()) {
                LOGGER.warn("[DangerousStructures] autoDetectStructures is disabled but both whitelists are empty — mod will have no effect");
            }
        }
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
