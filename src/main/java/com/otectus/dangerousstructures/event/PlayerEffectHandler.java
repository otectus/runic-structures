package com.otectus.dangerousstructures.event;

import com.otectus.dangerousstructures.DangerousStructures;
import com.otectus.dangerousstructures.config.DSConfig;
import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DangerousStructures.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEffectHandler {

    private static final int PLAYER_CHECK_THROTTLE_TICKS = 20;
    private static final int DARKNESS_EFFECT_DURATION_TICKS = 40;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        if (!DSConfig.enabled.get()) return;
        if (!(evt.player instanceof ServerPlayer player)) return;

        boolean darknessEnabled = DSConfig.applyDarknessEffect.get();
        boolean soundsEnabled = DSConfig.playAmbientSounds.get();
        if (!darknessEnabled && !soundsEnabled) return;

        // Throttle structure checks to every 20 ticks (1 second) per player
        if (player.tickCount % PLAYER_CHECK_THROTTLE_TICKS != 0) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        if (!StructureDetection.isInDangerousStructure(level, pos)) return;

        // Apply darkness effect
        if (darknessEnabled) {
            // Short duration (2 seconds), refreshed every second
            player.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS,
                    DARKNESS_EFFECT_DURATION_TICKS,
                    DSConfig.darknessAmplifier.get(),
                    true,   // ambient (no particles icon flash)
                    false,  // no particles
                    true    // show icon
            ));
        }

        // Play ambient sounds — use player.tickCount for per-player timing
        if (soundsEnabled) {
            int interval = DSConfig.ambientSoundInterval.get();
            if (player.tickCount % interval == 0) {
                level.playSound(null, pos, SoundEvents.AMBIENT_CAVE.value(),
                        SoundSource.AMBIENT, 0.7F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            }
        }
    }
}
