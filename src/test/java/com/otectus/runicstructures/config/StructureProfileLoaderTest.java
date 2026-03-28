package com.otectus.runicstructures.config;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructureProfileLoaderTest {

    @AfterEach
    void tearDown() throws Exception {
        StructureProfileLoader.clear();
        setCurrentPath(null);
    }

    @Test
    void reload_handlesMalformedJsonWithoutThrowing() throws Exception {
        Path tempFile = Files.createTempFile("runicstructures-invalid", ".json");
        Files.writeString(tempFile, "{ not valid json");
        setCurrentPath(tempFile);

        assertDoesNotThrow(StructureProfileLoader::reload);
        assertTrue(StructureProfileLoader.getAll().isEmpty());
        assertFalse(getValidationWarnings().isEmpty());
    }

    @Test
    void reload_recordsWarningForUnknownProgressionTier() throws Exception {
        Path tempFile = Files.createTempFile("runicstructures-profile", ".json");
        Files.writeString(tempFile, """
                {
                  "structures": {
                    "minecraft:stronghold": {
                      "spawnCap": 4,
                      "progressionTier": "legendary"
                    }
                  }
                }
                """);
        setCurrentPath(tempFile);

        StructureProfileLoader.reload();

        StructureProfile profile = StructureProfileLoader.getProfileRaw(new ResourceLocation("minecraft", "stronghold"));
        assertEquals(4, profile.getSpawnCap());
        assertNull(profile.getProgressionTier());
        assertTrue(getValidationWarnings().stream().anyMatch(message -> message.contains("unknown progressionTier")));
    }

    private static void setCurrentPath(Path path) throws Exception {
        Field field = StructureProfileLoader.class.getDeclaredField("currentPath");
        field.setAccessible(true);
        field.set(null, path);
    }

    @SuppressWarnings("unchecked")
    private static List<String> getValidationWarnings() throws Exception {
        Field field = StructureProfileLoader.class.getDeclaredField("validationWarnings");
        field.setAccessible(true);
        return (List<String>) field.get(null);
    }
}
