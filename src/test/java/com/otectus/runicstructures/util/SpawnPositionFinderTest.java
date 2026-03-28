package com.otectus.runicstructures.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpawnPositionFinderTest {

    @Test
    void selectDistributedPositions_returnsAllCandidates_whenFewerThanTarget() {
        List<BlockPos> candidates = List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(10, 64, 10)
        );
        List<Integer> pieceIndices = List.of(0, 0);
        RandomSource random = RandomSource.create(42L);

        List<BlockPos> result = SpawnPositionFinder.selectDistributedPositions(candidates, pieceIndices, 5, random);

        assertEquals(2, result.size());
        assertTrue(result.containsAll(candidates));
    }

    @Test
    void selectDistributedPositions_returnsExactTargetCount() {
        List<BlockPos> candidates = new ArrayList<>();
        List<Integer> pieceIndices = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            candidates.add(new BlockPos(i * 5, 64, i * 5));
            pieceIndices.add(i % 3);
        }
        RandomSource random = RandomSource.create(42L);

        List<BlockPos> result = SpawnPositionFinder.selectDistributedPositions(candidates, pieceIndices, 6, random);

        assertEquals(6, result.size());
    }

    @Test
    void selectDistributedPositions_selectsFromMultiplePieces() {
        List<BlockPos> candidates = new ArrayList<>();
        List<Integer> pieceIndices = new ArrayList<>();

        // 5 candidates in piece 0, 5 in piece 1
        for (int i = 0; i < 5; i++) {
            candidates.add(new BlockPos(i * 10, 64, 0));
            pieceIndices.add(0);
        }
        for (int i = 0; i < 5; i++) {
            candidates.add(new BlockPos(0, 64, i * 10));
            pieceIndices.add(1);
        }
        RandomSource random = RandomSource.create(42L);

        List<BlockPos> result = SpawnPositionFinder.selectDistributedPositions(candidates, pieceIndices, 4, random);

        assertEquals(4, result.size());

        // Verify round-robin: should have candidates from both pieces
        Set<Integer> selectedPieces = new HashSet<>();
        for (BlockPos pos : result) {
            int idx = candidates.indexOf(pos);
            selectedPieces.add(pieceIndices.get(idx));
        }
        assertEquals(2, selectedPieces.size(), "Should select from both pieces");
    }

    @Test
    void selectDistributedPositions_producesNoDuplicates() {
        List<BlockPos> candidates = new ArrayList<>();
        List<Integer> pieceIndices = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            candidates.add(new BlockPos(i, 64, i));
            pieceIndices.add(0);
        }
        RandomSource random = RandomSource.create(42L);

        List<BlockPos> result = SpawnPositionFinder.selectDistributedPositions(candidates, pieceIndices, 10, random);

        assertEquals(10, result.size());
        assertEquals(new HashSet<>(result).size(), result.size(), "No duplicates expected");
    }

    @Test
    void selectDistributedPositions_spreadsPositionsApart() {
        List<BlockPos> candidates = new ArrayList<>();
        List<Integer> pieceIndices = new ArrayList<>();

        // Create a spread of candidates along a line
        for (int i = 0; i < 100; i++) {
            candidates.add(new BlockPos(i, 64, 0));
            pieceIndices.add(0);
        }
        RandomSource random = RandomSource.create(42L);

        List<BlockPos> result = SpawnPositionFinder.selectDistributedPositions(candidates, pieceIndices, 5, random);

        assertEquals(5, result.size());

        // Verify minimum distance between selected positions is > 1
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                double dist = Math.sqrt(result.get(i).distSqr(result.get(j)));
                if (dist < minDist) minDist = dist;
            }
        }
        assertTrue(minDist > 5, "Farthest-point heuristic should spread positions apart, but min distance was " + minDist);
    }
}
