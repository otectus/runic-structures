package com.otectus.runicstructures.event;

import com.otectus.runicstructures.RunicStructures;
import com.otectus.runicstructures.config.RSConfig;
import com.otectus.runicstructures.config.StructureProfile;
import com.otectus.runicstructures.config.StructureProfileLoader;
import com.otectus.runicstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RunicStructures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEffectHandler {

    private static final int PLAYER_CHECK_THROTTLE_TICKS = 20;
    private static final int EFFECT_DURATION_TICKS = 40;

    private static final int TOTAL_SOUND_WEIGHT = 100;
    private static final int CAVE_WEIGHT = 60;
    private static final int BASALT_WEIGHT = 80;

    // Ambient sound scheduling is only touched on the server thread.
    private static final Map<UUID, Integer> nextAmbientSoundTicks = new HashMap<>();

    public static void onServerStopped() {
        nextAmbientSoundTicks.clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        if (!RSConfig.enabled.get()) return;
        if (!(evt.player instanceof ServerPlayer player)) return;
        if (player.tickCount % PLAYER_CHECK_THROTTLE_TICKS != 0) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        ResourceLocation structureId = StructureDetection.getRunicStructureId(level, pos);
        if (structureId == null) return;

        long dayCount = level.getDayTime() / 24000L;
        StructureProfile profile = StructureProfileLoader.getProfile(structureId, dayCount);

        // Apply effects based on per-structure profile (falls through to global config if no override)
        if (profile.isDarknessEnabled()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS,
                    EFFECT_DURATION_TICKS,
                    RSConfig.darknessAmplifier.get(),
                    true, false, true
            ));
        }

        if (profile.isMiningFatigueEnabled()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN,
                    EFFECT_DURATION_TICKS,
                    RSConfig.miningFatigueAmplifier.get(),
                    true, false, true
            ));
        }

        if (profile.isSlownessEnabled()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    EFFECT_DURATION_TICKS,
                    RSConfig.slownessAmplifier.get(),
                    true, false, true
            ));
        }

        if (profile.isAmbientSoundsEnabled()) {
            int interval = RSConfig.ambientSoundInterval.get();
            int currentTick = level.getServer().getTickCount();
            int nextTick = nextAmbientSoundTicks.getOrDefault(player.getUUID(), currentTick);
            if (currentTick >= nextTick) {
                SoundEvent sound = pickAmbientSound(level);
                level.playSound(null, pos, sound,
                        SoundSource.AMBIENT, 0.7F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                nextAmbientSoundTicks.put(player.getUUID(), currentTick + interval);
            }
        }
    }

    private static SoundEvent pickAmbientSound(ServerLevel level) {
        int roll = level.getRandom().nextInt(TOTAL_SOUND_WEIGHT);
        if (roll < CAVE_WEIGHT) {
            return SoundEvents.AMBIENT_CAVE.value();
        } else if (roll < BASALT_WEIGHT) {
            return SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value();
        } else {
            return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value();
        }
    }
}
