package com.otectus.runicstructures.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Position-finding utilities for spawn placement within structures.
 */
public class SpawnPositionFinder {

    public static final int INITIAL_POPULATION_SCAN_ATTEMPTS = 50;
    public static final int NEARBY_FLOOR_MAX_RETRIES = 8;
    public static final int PACK_SEARCH_RADIUS = 4;

    /**
     * Find a valid floor position near the given origin, within the given radius.
     */
    public static BlockPos findNearbyFloor(ServerLevel level, BlockPos origin, RandomSource random, int radius) {
        for (int i = 0; i < NEARBY_FLOOR_MAX_RETRIES; i++) {
            int dx = random.nextIntBetweenInclusive(-radius, radius);
            int dz = random.nextIntBetweenInclusive(-radius, radius);
            BlockPos candidate = new BlockPos(origin.getX() + dx, origin.getY(), origin.getZ() + dz);

            // Check the Y level and one above/below
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos pos = candidate.offset(0, dy, 0);
                if (isValidSpawnFloor(level, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Shared floor validation used by periodic, initial, and pack placement.
     */
    public static boolean isValidSpawnFloor(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (level.getFluidState(below).is(FluidTags.LAVA)) return false;
        if (level.getFluidState(pos).is(FluidTags.WATER)) return false;
        return !level.canSeeSky(pos);
    }

    /**
     * Select well-distributed positions from the candidate pool.
     * Round-robins across structure pieces, picking the farthest candidate from
     * already-selected positions within each piece.
     */
    public static List<BlockPos> selectDistributedPositions(
            List<BlockPos> candidates, List<Integer> pieceIndices,
            int targetCount, RandomSource random) {

        if (candidates.size() <= targetCount) {
            return new ArrayList<>(candidates);
        }

        // Group candidate indices by piece
        Map<Integer, List<Integer>> byPiece = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            byPiece.computeIfAbsent(pieceIndices.get(i), k -> new ArrayList<>()).add(i);
        }

        List<BlockPos> selected = new ArrayList<>();
        List<Integer> pieceKeys = new ArrayList<>(byPiece.keySet());
        shuffle(pieceKeys, random);

        int piecePtr = 0;
        while (selected.size() < targetCount && !pieceKeys.isEmpty()) {
            int pieceKey = pieceKeys.get(piecePtr % pieceKeys.size());
            List<Integer> pieceCandidates = byPiece.get(pieceKey);

            if (pieceCandidates.isEmpty()) {
                pieceKeys.remove(piecePtr % pieceKeys.size());
                continue;
            }

            int bestIdx;
            if (selected.isEmpty()) {
                bestIdx = pieceCandidates.remove(random.nextInt(pieceCandidates.size()));
            } else {
                // Pick the candidate farthest from all already-selected positions
                int bestListIdx = 0;
                double bestMinDist = -1;
                for (int li = 0; li < pieceCandidates.size(); li++) {
                    BlockPos cand = candidates.get(pieceCandidates.get(li));
                    double minDist = Double.MAX_VALUE;
                    for (BlockPos sel : selected) {
                        double d = cand.distSqr(sel);
                        if (d < minDist) minDist = d;
                    }
                    if (minDist > bestMinDist) {
                        bestMinDist = minDist;
                        bestListIdx = li;
                    }
                }
                bestIdx = pieceCandidates.remove(bestListIdx);
            }

            selected.add(candidates.get(bestIdx));
            piecePtr++;
        }

        return selected;
    }

    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
