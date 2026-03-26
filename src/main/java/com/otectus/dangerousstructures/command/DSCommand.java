package com.otectus.dangerousstructures.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.otectus.dangerousstructures.DangerousStructures;
import com.otectus.dangerousstructures.config.DSConfig;
import com.otectus.dangerousstructures.event.TickSpawnHandler;
import com.otectus.dangerousstructures.util.StructureDetection;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DSCommand {

    private static final int INFO_SEARCH_HORIZONTAL_RADIUS = 64;
    private static final int INFO_SEARCH_VERTICAL_RADIUS = 32;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dangerousstructures")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> showStatus(ctx.getSource())))
                .then(Commands.literal("info").executes(ctx -> showInfo(ctx.getSource())))
                .then(Commands.literal("list").executes(ctx -> listStructures(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reloadCaches(ctx.getSource())))
                .then(Commands.literal("debug")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> toggleDebug(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("spawns").executes(ctx -> showSpawnStats(ctx.getSource())))
                .then(Commands.literal("validate").executes(ctx -> validateConfig(ctx.getSource())))
        );

        // Alias
        dispatcher.register(Commands.literal("ds")
                .requires(source -> source.hasPermission(2))
                .redirect(dispatcher.getRoot().getChild("dangerousstructures"))
        );
    }

    private static int showStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Dangerous Structures Status ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Enabled: " + DSConfig.enabled.get()), false);
        source.sendSuccess(() -> Component.literal("Mode: " + (DSConfig.autoDetectStructures.get() ? "Auto-detect" : "Manual whitelist")), false);
        source.sendSuccess(() -> Component.literal("Villages included: " + DSConfig.includeVillages.get()), false);
        source.sendSuccess(() -> Component.literal("Periodic spawning: " + DSConfig.periodicSpawningEnabled.get()), false);
        source.sendSuccess(() -> Component.literal("Spawn interval: " + DSConfig.periodicSpawnInterval.get() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("Base spawn cap: " + DSConfig.periodicSpawnCap.get()), false);
        source.sendSuccess(() -> Component.literal("Difficulty scaling: " + DSConfig.difficultyScalingEnabled.get()), false);
        source.sendSuccess(() -> Component.literal("Initial population: " + DSConfig.initialPopulationEnabled.get()
                + " (" + DSConfig.initialPopulationMin.get() + "-" + DSConfig.initialPopulationMax.get() + " mobs)"), false);
        source.sendSuccess(() -> Component.literal("Debug logging: " + DSConfig.isDebugEnabled()), false);
        source.sendSuccess(() -> Component.literal("Dimensions: " + DSConfig.allowedDimensions.get()), false);
        return 1;
    }

    private static int showInfo(CommandSourceStack source) {
        if (source.getEntity() == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        boolean inStructure = StructureDetection.isInDangerousStructure(level, pos);

        if (inStructure) {
            source.sendSuccess(() -> Component.literal("You ARE inside a dangerous structure!").withStyle(ChatFormatting.RED), false);
        } else {
            source.sendSuccess(() -> Component.literal("You are NOT inside a dangerous structure.").withStyle(ChatFormatting.GREEN), false);
        }

        // Show which structures contain this position
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        SectionPos sectionPos = SectionPos.of(pos);
        List<String> found = new ArrayList<>();

        for (var entry : registry.entrySet()) {
            Structure structure = entry.getValue();
            for (StructureStart start : level.structureManager().startsForStructure(sectionPos, structure)) {
                if (start == StructureStart.INVALID_START) continue;
                if (start.getBoundingBox().isInside(pos)) {
                    found.add(entry.getKey().location().toString());
                }
            }
        }

        if (!found.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Structures at position: " + String.join(", ", found)).withStyle(ChatFormatting.YELLOW), false);
        }

        // Count nearby monsters
        int monsterCount = 0;
        var entities = level.getEntities(null, new net.minecraft.world.phys.AABB(
                pos.getX() - INFO_SEARCH_HORIZONTAL_RADIUS, pos.getY() - INFO_SEARCH_VERTICAL_RADIUS, pos.getZ() - INFO_SEARCH_HORIZONTAL_RADIUS,
                pos.getX() + INFO_SEARCH_HORIZONTAL_RADIUS, pos.getY() + INFO_SEARCH_VERTICAL_RADIUS, pos.getZ() + INFO_SEARCH_HORIZONTAL_RADIUS));
        for (var e : entities) {
            if (e.getType().getCategory() == MobCategory.MONSTER) monsterCount++;
        }
        int count = monsterCount;
        source.sendSuccess(() -> Component.literal("Nearby monsters (" + INFO_SEARCH_HORIZONTAL_RADIUS + " block radius): " + count).withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int listStructures(CommandSourceStack source) {
        if (source.getEntity() == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;
        int radius = DSConfig.chunkSearchRadius.get();

        Set<String> found = new LinkedHashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) continue;
                SectionPos sectionPos = SectionPos.bottomOf(chunk);

                for (var entry : registry.entrySet()) {
                    for (StructureStart start : level.structureManager().startsForStructure(sectionPos, entry.getValue())) {
                        if (start == StructureStart.INVALID_START) continue;
                        BoundingBox bb = start.getBoundingBox();
                        boolean excluded = StructureDetection.isStructureExcluded(registry, entry.getValue());
                        String status = excluded ? " [excluded]" : " [dangerous]";
                        String desc = entry.getKey().location() + status + " at "
                                + bb.getCenter().getX() + "," + bb.getCenter().getY() + "," + bb.getCenter().getZ();
                        found.add(desc);
                    }
                }
            }
        }

        source.sendSuccess(() -> Component.literal("=== Nearby Structures ===").withStyle(ChatFormatting.GOLD), false);
        if (found.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No structures found nearby.").withStyle(ChatFormatting.GRAY), false);
        } else {
            for (String desc : found) {
                ChatFormatting color = desc.contains("[dangerous]") ? ChatFormatting.RED : ChatFormatting.GRAY;
                source.sendSuccess(() -> Component.literal("  " + desc).withStyle(color), false);
            }
        }

        return 1;
    }

    private static int reloadCaches(CommandSourceStack source) {
        TickSpawnHandler.invalidateCaches();
        StructureDetection.invalidateCache();
        source.sendSuccess(() -> Component.literal("Dangerous Structures caches invalidated.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int toggleDebug(CommandSourceStack source, boolean enabled) {
        DSConfig.setRuntimeDebug(enabled);
        source.sendSuccess(() -> Component.literal("Debug logging " + (enabled ? "enabled" : "disabled")
                + " (runtime only, survives config reload)").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int validateConfig(CommandSourceStack source) {
        List<String> warnings = DangerousStructures.runConfigValidation(source.getServer());
        if (warnings.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Config validation passed — no issues found.").withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendSuccess(() -> Component.literal("Config validation found " + warnings.size() + " issue(s):").withStyle(ChatFormatting.YELLOW), false);
            for (String warning : warnings) {
                source.sendSuccess(() -> Component.literal("  - " + warning).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        return 1;
    }

    private static int showSpawnStats(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Spawn Statistics ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Total spawn attempts: " + TickSpawnHandler.getStatTotalAttempts()), false);
        source.sendSuccess(() -> Component.literal("Successful spawns: " + TickSpawnHandler.getStatSuccessfulSpawns()), false);
        source.sendSuccess(() -> Component.literal("Cap rejections: " + TickSpawnHandler.getStatCapRejects()), false);
        source.sendSuccess(() -> Component.literal("Position failures: " + TickSpawnHandler.getStatPositionFailures()), false);
        source.sendSuccess(() -> Component.literal("Initial populations: " + TickSpawnHandler.getStatInitialPopulations()), false);
        return 1;
    }
}
