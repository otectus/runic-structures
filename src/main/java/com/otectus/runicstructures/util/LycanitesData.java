package com.otectus.runicstructures.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.otectus.runicstructures.RunicStructures;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Static reference data for Lycanites Mobs creatures (element + creature type),
 * loaded once from a bundled classpath resource. Used for element-aware guardian
 * theming. Load failure is non-fatal: all lookups report "not Lycanites".
 */
public final class LycanitesData {

    private static final String RESOURCE = "/defaults/runicstructures-lycanites-elements.json";

    private record Entry(@Nullable String element, @Nullable String type) {}

    private static volatile Map<ResourceLocation, Entry> data = Collections.emptyMap();
    private static volatile boolean loaded = false;

    private static final Map<String, String> ELEMENT_ADJECTIVES = buildAdjectives();

    private LycanitesData() {}

    public static void load() {
        Map<ResourceLocation, Entry> map = new HashMap<>();
        try (Reader r = new InputStreamReader(
                LycanitesData.class.getResourceAsStream(RESOURCE), StandardCharsets.UTF_8)) {
            JsonObject root = new Gson().fromJson(r, JsonObject.class);
            for (var e : root.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(e.getKey());
                if (id == null || !e.getValue().isJsonObject()) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                String element = o.has("element") && !o.get("element").isJsonNull()
                        ? o.get("element").getAsString() : null;
                String type = o.has("type") && !o.get("type").isJsonNull()
                        ? o.get("type").getAsString() : null;
                map.put(id, new Entry(element, type));
            }
            data = Map.copyOf(map);
            loaded = true;
            RunicStructures.LOGGER.info("[RunicStructures] Loaded {} Lycanites creature entries", map.size());
        } catch (Exception ex) {
            data = Collections.emptyMap();
            loaded = false;
            RunicStructures.LOGGER.warn("[RunicStructures] Could not load Lycanites element data: {}", ex.toString());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static int size() {
        return data.size();
    }

    public static boolean isLycanites(ResourceLocation id) {
        return data.containsKey(id);
    }

    @Nullable
    public static String getElement(ResourceLocation id) {
        Entry e = data.get(id);
        return e == null ? null : e.element();
    }

    /** Element-themed adjective, or null if the element is unknown/unmapped. */
    @Nullable
    public static String elementAdjective(@Nullable String element) {
        if (element == null) return null;
        return ELEMENT_ADJECTIVES.get(element.toLowerCase(Locale.ROOT));
    }

    private static Map<String, String> buildAdjectives() {
        Map<String, String> m = new HashMap<>();
        m.put("fire", "Infernal");
        m.put("lava", "Molten");
        m.put("frost", "Frozen");
        m.put("water", "Tidal");
        m.put("air", "Gale");
        m.put("earth", "Stoneborn");
        m.put("quake", "Tremorous");
        m.put("lightning", "Storm");
        m.put("shadow", "Shadowed");
        m.put("nether", "Hellborn");
        m.put("arcane", "Arcane");
        m.put("order", "Hallowed");
        m.put("chaos", "Chaotic");
        m.put("light", "Radiant");
        m.put("arbour", "Thornclad");
        m.put("acid", "Caustic");
        m.put("aether", "Aetherial");
        m.put("poison", "Venomous");
        m.put("fae", "Feywild");
        m.put("phase", "Phantom");
        m.put("void", "Voidtouched");
        return Map.copyOf(m);
    }
}
