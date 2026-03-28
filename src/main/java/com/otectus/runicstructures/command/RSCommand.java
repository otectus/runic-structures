package com.otectus.runicstructures.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.otectus.runicstructures.RunicStructures;
import com.otectus.runicstructures.config.RSConfig;
import com.otectus.runicstructures.config.StructureProfileLoader;
import com.otectus.runicstructures.event.TickSpawnHandler;
import com.otectus.runicstructures.util.StructureDetection;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RSCommand {

    private static final int INFO_SEARCH_HORIZONTAL_RADIUS = 64;
    private static final int INFO_SEARCH_VERTICAL_RADIUS = 32;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("runicstructures")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> showStatus(ctx.getSource())))
                .then(Commands.literal("info").executes(ctx -> showInfo(ctx.getSource())))
                .then(Commands.literal("list").executes(ctx -> listStructures(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reloadCaches(ctx.getSource())))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(ctx -> toggleDebug(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> toggleDebug(ctx.getSource(), false)))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> toggleDebug(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("spawns").executes(ctx -> showSpawnStats(ctx.getSource()))
                        .then(Commands.literal("detail").executes(ctx -> showSpawnStatsDetail(ctx.getSource()))))
                .then(Commands.literal("validate").executes(ctx -> validateConfig(ctx.getSource())))
        );

        dispatcher.register(Commands.literal("rs")
                .requires(source -> source.hasPermission(2))
                .redirect(dispatcher.getRoot().getChild("runicstructures"))
        );
    }

    private static int showStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.header").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.enabled", RSConfig.enabled.get()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.mode",
                Component.translatable(RSConfig.autoDetectStructures.get()
                        ? "command.runicstructures.status.mode.auto"
                        : "command.runicstructures.status.mode.manual")), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.villages", RSConfig.includeVillages.get()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.periodic", RSConfig.periodicSpawningEnabled.get()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.interval", RSConfig.periodicSpawnInterval.get()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.cap", RSConfig.periodicSpawnCap.get()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.difficulty", RSConfig.difficultyScalingEnabled.get()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.initial_pop",
                RSConfig.initialPopulationEnabled.get(), RSConfig.initialPopulationMin.get(), RSConfig.initialPopulationMax.get()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.debug", RSConfig.isDebugEnabled()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.status.dimensions", RSConfig.getAllowedDimensions().toString()), false);
        return 1;
    }

    private static int showInfo(CommandSourceStack source) {
        if (source.getEntity() == null) {
            source.sendFailure(Component.translatable("command.runicstructures.info.player_only"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        boolean inStructure = StructureDetection.isInRunicStructure(level, pos);
        source.sendSuccess(() -> Component.translatable(
                inStructure ? "command.runicstructures.info.inside" : "command.runicstructures.info.outside"
        ).withStyle(inStructure ? ChatFormatting.RED : ChatFormatting.GREEN), false);

        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Set<String> found = new LinkedHashSet<>();
        for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(pos), structure -> true)) {
            if (start == StructureStart.INVALID_START || !start.getBoundingBox().isInside(pos)) continue;

            ResourceLocationWithStatus info = toStructureStatus(registry, start.getStructure());
            if (info != null) {
                found.add(info.formatted());
            }
        }

        if (!found.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.runicstructures.info.structures_at",
                    String.join(", ", found)).withStyle(ChatFormatting.YELLOW), false);
        }

        int monsterCount = 0;
        var entities = level.getEntities(null, new net.minecraft.world.phys.AABB(
                pos.getX() - INFO_SEARCH_HORIZONTAL_RADIUS, pos.getY() - INFO_SEARCH_VERTICAL_RADIUS, pos.getZ() - INFO_SEARCH_HORIZONTAL_RADIUS,
                pos.getX() + INFO_SEARCH_HORIZONTAL_RADIUS, pos.getY() + INFO_SEARCH_VERTICAL_RADIUS, pos.getZ() + INFO_SEARCH_HORIZONTAL_RADIUS));
        for (var e : entities) {
            if (e.getType().getCategory() == MobCategory.MONSTER) monsterCount++;
        }

        int count = monsterCount;
        source.sendSuccess(() -> Component.translatable("command.runicstructures.info.nearby_monsters",
                INFO_SEARCH_HORIZONTAL_RADIUS, count).withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int listStructures(CommandSourceStack source) {
        if (source.getEntity() == null) {
            source.sendFailure(Component.translatable("command.runicstructures.info.player_only"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;
        int radius = RSConfig.chunkSearchRadius.get();

        Set<StructureStart> seenStarts = new LinkedHashSet<>();
        Set<String> found = new LinkedHashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(chunkX + dx, chunkZ + dz);
                var chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                if (chunk == null) continue;

                for (StructureStart start : level.structureManager().startsForStructure(chunkPos, structure -> true)) {
                    if (start == StructureStart.INVALID_START || !seenStarts.add(start)) continue;

                    BoundingBox bb = start.getBoundingBox();
                    ResourceLocationWithStatus info = toStructureStatus(registry, start.getStructure());
                    if (info == null) continue;

                    String desc = info.formatted() + " at "
                            + bb.getCenter().getX() + "," + bb.getCenter().getY() + "," + bb.getCenter().getZ();
                    found.add(desc);
                }
            }
        }

        source.sendSuccess(() -> Component.translatable("command.runicstructures.list.header").withStyle(ChatFormatting.GOLD), false);
        if (found.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.runicstructures.list.none").withStyle(ChatFormatting.GRAY), false);
        } else {
            for (String desc : found) {
                ChatFormatting color = desc.contains("[runic]") ? ChatFormatting.RED : ChatFormatting.GRAY;
                source.sendSuccess(() -> Component.literal("  " + desc).withStyle(color), false);
            }
        }

        return 1;
    }

    private static int reloadCaches(CommandSourceStack source) {
        StructureProfileLoader.reload();
        TickSpawnHandler.invalidateCaches();
        StructureDetection.invalidateCache();

        List<String> warnings = RunicStructures.runConfigValidation(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.runicstructures.reload.success").withStyle(ChatFormatting.GREEN), false);
        if (!warnings.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.runicstructures.validate.issues", warnings.size()).withStyle(ChatFormatting.YELLOW), false);
            for (String warning : warnings) {
                source.sendSuccess(() -> Component.literal("  - " + warning).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        return 1;
    }

    private static int toggleDebug(CommandSourceStack source, boolean enabled) {
        RSConfig.setRuntimeDebug(enabled);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.debug.toggled",
                Component.translatable(enabled
                        ? "command.runicstructures.debug.enabled"
                        : "command.runicstructures.debug.disabled"))
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int validateConfig(CommandSourceStack source) {
        List<String> warnings = RunicStructures.runConfigValidation(source.getServer());
        if (warnings.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.runicstructures.validate.passed").withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.runicstructures.validate.issues", warnings.size()).withStyle(ChatFormatting.YELLOW), false);
            for (String warning : warnings) {
                source.sendSuccess(() -> Component.literal("  - " + warning).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        return 1;
    }

    private static int showSpawnStats(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.header").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.attempts", TickSpawnHandler.getStatTotalAttempts()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.successful", TickSpawnHandler.getStatSuccessfulSpawns()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.cap_rejections", TickSpawnHandler.getStatCapRejects()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.position_failures", TickSpawnHandler.getStatPositionFailures()), false);
        source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.initial_populations", TickSpawnHandler.getStatInitialPopulations()), false);
        return 1;
    }

    private static int showSpawnStatsDetail(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.detail_header").withStyle(ChatFormatting.GOLD), false);

        var stats = TickSpawnHandler.getPerStructureStats();
        if (stats.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.detail_none").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        stats.entrySet().stream()
                .sorted(Comparator.comparingInt((java.util.Map.Entry<TickSpawnHandler.StructureInstanceKey, TickSpawnHandler.StructureSpawnStats> entry) ->
                        entry.getValue().spawns).reversed())
                .limit(10)
                .forEach(entry -> {
                    TickSpawnHandler.StructureInstanceKey key = entry.getKey();
                    var s = entry.getValue();
                    source.sendSuccess(() -> Component.translatable("command.runicstructures.spawns.detail_entry",
                            key.dimensionId(),
                            key.origin().getX(), key.origin().getY(), key.origin().getZ(),
                            s.spawns, s.attempts, s.capRejects).withStyle(ChatFormatting.GRAY), false);
                });

        return 1;
    }

    private static ResourceLocationWithStatus toStructureStatus(Registry<Structure> registry, Structure structure) {
        ResourceLocation id = registry.getKey(structure);
        if (id == null) return null;

        return new ResourceLocationWithStatus(id, StructureDetection.isStructureRunic(registry, structure));
    }

    private record ResourceLocationWithStatus(ResourceLocation id, boolean runic) {
        private String formatted() {
            return id + (runic ? " [runic]" : " [excluded]");
        }
    }
}
