package com.otectus.runicstructures.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StructureProfileTest {

    @Test
    void progressionTierParsing_isCaseInsensitive() {
        assertEquals(StructureProfile.ProgressionTier.MID, StructureProfile.ProgressionTier.fromString("MiD"));
        assertEquals(StructureProfile.ProgressionTier.VETERAN, StructureProfile.ProgressionTier.fromString("veteran"));
    }

    @Test
    void progressionTierParsing_rejectsUnknownValues() {
        assertNull(StructureProfile.ProgressionTier.fromString("legendary"));
        assertNull(StructureProfile.ProgressionTier.fromString(null));
    }
}
