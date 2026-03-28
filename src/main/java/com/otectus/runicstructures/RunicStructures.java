package com.otectus.runicstructures;

import com.mojang.logging.LogUtils;
import com.otectus.runicstructures.command.RSCommand;
import com.otectus.runicstructures.config.EquipmentPools;
import com.otectus.runicstructures.config.RSConfig;
import com.otectus.runicstructures.config.StructureProfileLoader;
import com.otectus.runicstructures.event.PlayerEffectHandler;
import com.otectus.runicstructures.event.TickSpawnHandler;
import com.otectus.runicstructures.util.StructureDetection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
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

import java.util.ArrayList;
import java.util.List;

@Mod(RunicStructures.MOD_ID)
public class RunicStructures {
    public static final String MOD_ID = "runicstructures";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RunicStructures() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RSConfig.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent evt) {
            TickSpawnHandler.onServerStopped();
            PlayerEffectHandler.onServerStopped();
            StructureDetection.onServerStopped();
            StructureProfileLoader.clear();
            LOGGER.info("[RunicStructures] Server stopped, caches cleared");
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent evt) {
            if (evt.getPlayer() == null) {
                TickSpawnHandler.invalidateCaches();
                StructureDetection.invalidateCache();
                LOGGER.info("[RunicStructures] Datapack reload detected, caches invalidated");
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent evt) {
            RSCommand.register(evt.getDispatcher());
        }

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent evt) {
            StructureProfileLoader.load(evt.getServer());
            validateConfig(evt);
        }

        @SubscribeEvent
        public static void onEliteMobDeath(LivingDeathEvent evt) {
            // Grant bonus XP when an elite mob is killed by a player
            if (!RSConfig.eliteSpawnsEnabled.get()) return;
            if (!(evt.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
            if (!mob.getTags().contains("rs_elite")) return;

            int bonusXP = RSConfig.eliteBonusXP.get();
            if (bonusXP <= 0) return;

            if (evt.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level
                    && evt.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
                ExperienceOrb.award(level, Vec3.atCenterOf(evt.getEntity().blockPosition()), bonusXP);
            }
        }

        private static void validateConfig(ServerStartedEvent evt) {
            List<String> warnings = runConfigValidation(evt.getServer());
            for (String warning : warnings) {
                LOGGER.warn("[RunicStructures] {}", warning);
            }
        }
    }

    /**
     * Run config validation against registries and return a list of warning messages.
     * Used by both server startup validation and the /rs validate command.
     */
    public static List<String> runConfigValidation(MinecraftServer server) {
        List<String> warnings = new ArrayList<>();
        var registryAccess = server.registryAccess();

        // Validate structure IDs
        var structureRegistry = registryAccess.registryOrThrow(Registries.STRUCTURE);
        for (ResourceLocation rl : RSConfig.getStructureWhitelist()) {
            if (!structureRegistry.containsKey(rl)) {
                warnings.add("Unknown structure '" + rl + "' in structureWhitelist");
            }
        }
        for (ResourceLocation rl : RSConfig.getStructureBlacklist()) {
            if (!structureRegistry.containsKey(rl)) {
                warnings.add("Unknown structure '" + rl + "' in structureBlacklist");
            }
        }
        var structureTagNames = structureRegistry.getTagNames()
                .map(tagKey -> tagKey.location())
                .collect(java.util.stream.Collectors.toSet());
        for (ResourceLocation rl : RSConfig.getStructureTagWhitelist()) {
            if (!structureTagNames.contains(rl)) {
                warnings.add("Unknown structure tag '" + rl + "' in structureTagWhitelist");
            }
        }
        for (ResourceLocation rl : RSConfig.getStructureTagBlacklist()) {
            if (!structureTagNames.contains(rl)) {
                warnings.add("Unknown structure tag '" + rl + "' in structureTagBlacklist");
            }
        }

        // Validate mob IDs
        for (ResourceLocation rl : RSConfig.getMobWhitelist()) {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
                warnings.add("Unknown entity '" + rl + "' in mobWhitelist");
            }
        }
        for (ResourceLocation rl : RSConfig.getMobBlacklist()) {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
                warnings.add("Unknown entity '" + rl + "' in mobBlacklist");
            }
        }

        var loadedDimensions = server.levelKeys().stream()
                .map(key -> key.location())
                .collect(java.util.stream.Collectors.toSet());
        for (ResourceLocation rl : RSConfig.getAllowedDimensions()) {
            if (!loadedDimensions.contains(rl)) {
                warnings.add("Configured dimension '" + rl + "' in allowedDimensions is not present on this server");
            }
        }

        // Warn about contradictory config
        if (!RSConfig.autoDetectStructures.get()
                && RSConfig.getStructureWhitelist().isEmpty()
                && RSConfig.getStructureTagWhitelist().isEmpty()) {
            warnings.add("autoDetectStructures is disabled but both whitelists are empty — mod will have no effect");
        }

        // Cross-option dependency warnings
        if (RSConfig.useVanillaSpawnWeights.get() && !RSConfig.dimensionSpecificMobs.get()) {
            warnings.add("useVanillaSpawnWeights has no effect when dimensionSpecificMobs is disabled");
        }

        warnings.addAll(EquipmentPools.getValidationWarnings());
        // Validate structure profiles
        warnings.addAll(StructureProfileLoader.validate(server));

        return warnings;
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ConfigEvents {
        @SubscribeEvent
        public static void onConfigLoad(ModConfigEvent.Loading evt) {
            if (evt.getConfig().getModId().equals(MOD_ID)) {
                RSConfig.refresh();
                LOGGER.info("[RunicStructures] Config loaded");
            }
        }

        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading evt) {
            if (evt.getConfig().getModId().equals(MOD_ID)) {
                RSConfig.refresh();
                LOGGER.info("[RunicStructures] Config reloaded");
            }
        }
    }
}
