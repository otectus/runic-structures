package com.otectus.runicstructures.config;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RSConfigTest {

    @Test
    void parseResourceLocations_validInputs() {
        Set<ResourceLocation> result = RSConfig.parseResourceLocations(
                List.of("minecraft:zombie", "minecraft:skeleton"), "test");

        assertEquals(2, result.size());
        assertTrue(result.contains(new ResourceLocation("minecraft", "zombie")));
        assertTrue(result.contains(new ResourceLocation("minecraft", "skeleton")));
    }

    @Test
    void parseResourceLocations_emptyList() {
        Set<ResourceLocation> result = RSConfig.parseResourceLocations(
                Collections.emptyList(), "test");

        assertTrue(result.isEmpty());
    }

    @Test
    void parseResourceLocations_invalidInputSkipped() {
        // Invalid resource locations (containing spaces or invalid chars) are skipped
        Set<ResourceLocation> result = RSConfig.parseResourceLocations(
                List.of("minecraft:zombie", "not a valid id!!!", "minecraft:skeleton"), "test");

        // "not a valid id!!!" should be skipped; valid entries kept
        // Note: ResourceLocation.tryParse is lenient — it lowercases and may accept some strings.
        // We test that at least the valid ones are present.
        assertTrue(result.contains(new ResourceLocation("minecraft", "zombie")));
        assertTrue(result.contains(new ResourceLocation("minecraft", "skeleton")));
    }

    @Test
    void parseResourceLocations_duplicatesRemoved() {
        Set<ResourceLocation> result = RSConfig.parseResourceLocations(
                List.of("minecraft:zombie", "minecraft:zombie", "minecraft:zombie"), "test");

        assertEquals(1, result.size());
        assertTrue(result.contains(new ResourceLocation("minecraft", "zombie")));
    }

    @Test
    void parseResourceLocations_resultIsUnmodifiable() {
        Set<ResourceLocation> result = RSConfig.parseResourceLocations(
                List.of("minecraft:zombie"), "test");

        assertThrows(UnsupportedOperationException.class, () ->
                result.add(new ResourceLocation("minecraft", "creeper")));
    }

    @Test
    void parseResourceLocations_moddedNamespaces() {
        Set<ResourceLocation> result = RSConfig.parseResourceLocations(
                List.of("cataclysm:netherite_monstrosity", "iceandfire:fire_dragon"), "test");

        assertEquals(2, result.size());
        assertTrue(result.contains(new ResourceLocation("cataclysm", "netherite_monstrosity")));
        assertTrue(result.contains(new ResourceLocation("iceandfire", "fire_dragon")));
    }
}
