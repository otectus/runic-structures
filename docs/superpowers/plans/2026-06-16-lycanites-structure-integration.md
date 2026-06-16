# Lycanites Structure-Territory Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Runic Structures spawn element-matched Lycanites mobs as ecological structure guardians, with element-themed elite naming and a `rs_guardian` tag.

**Architecture:** Two modules. (1) A one-shot Python generator regenerates the bundled structure-profile JSON, appending tier-scaled Lycanites IDs to each structure's existing `mobPool` based on an archetype signature, and emits a bundled element-map resource. (2) A small `LycanitesData` classpath lookup feeds element-aware theming applied to elite Lycanites mobs in both spawn paths.

**Tech Stack:** Java 17, Forge 1.20.1, Gson, ForgeConfigSpec; Python 3 for the offline generator. No Java unit-test framework exists in this repo (removed in commit 053c9c4); verification is via the generator's self-checks, `./gradlew build`, and a manual runtime checklist.

---

## Task 1: Generator script + element map

**Files:**
- Create: `tools/lycanites_integration.py`
- Create (generated): `src/main/resources/defaults/runicstructures-lycanites-elements.json`
- Modify (generated): `src/main/resources/defaults/runicstructures-structures.json`

The Lycanites source clone is at `/home/joshua/Documents/GitHub/LycanitesMobs`. The generator reads its creature JSONs only to emit the element map; the mod ships the generated map, never the source.

- [ ] **Step 1: Write the generator script**

Create `tools/lycanites_integration.py`:

```python
#!/usr/bin/env python3
"""One-shot generator for the Lycanites structure-territory integration.

Run from the repo root:
    python3 tools/lycanites_integration.py

Outputs:
  - Rewrites src/main/resources/defaults/runicstructures-structures.json
    (appends tier-scaled Lycanites IDs to each structure's mobPool by archetype)
  - Writes  src/main/resources/defaults/runicstructures-lycanites-elements.json
    (entityId -> {element, type}) from the Lycanites source clone.
"""
import json
import os
import sys
import glob

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STRUCT_JSON = os.path.join(REPO, "src/main/resources/defaults/runicstructures-structures.json")
ELEM_JSON = os.path.join(REPO, "src/main/resources/defaults/runicstructures-lycanites-elements.json")
LYC_CREATURES = "/home/joshua/Documents/GitHub/LycanitesMobs/src/main/resources/common/lycanitesmobs/creatures"

# Archetype detection: (archetype, signature mob). First match wins; order matters
# because ancient_city and cave both contain creature_compendium:moleman.
SIGNATURES = [
    ("nether", "minecraft:blaze"),
    ("ocean", "cataclysm:deepling"),
    ("end", "cataclysm:endermaptera"),
    ("crypt", "cataclysm:draugr"),
    ("ancient_city", "minecraft:phantom"),
    ("illager", "irons_spellbooks:magehunter_vindicator"),
    ("cave", "creature_compendium:moleman"),
    ("early", "variantsandventures:gelid"),
]

TIER_ORDER = ["early", "mid", "late", "veteran"]
DEFAULT_TIER = {
    "nether": "mid", "ocean": "mid", "cave": "mid", "crypt": "late",
    "ancient_city": "late", "end": "late", "early": "early", "illager": "early",
}

# Lycanites additions per archetype. Tiered archetypes accumulate cumulatively by
# tier index; "_all" archetypes apply their single set regardless of tier.
ADDITIONS = {
    "nether": {
        "mid": ["afrit", "cinder", "belphegor", "salamander", "trite"],
        "late": ["cherufe", "behemophet", "wraith", "malwrath", "volcan", "khalk"],
    },
    "ocean": {
        "early": ["skylus", "herma", "abtu"],
        "mid": ["jengu", "ioray", "lacedon", "roa", "thresher"],
        "late": ["abaia"],
    },
    "cave": {
        "early": ["darkling", "gnekk", "gorgomite", "eyewig"],
        "mid": ["grue", "troll", "maug", "geonach", "shade", "chupacabra"],
        "late": ["wraamon", "tremor"],
    },
    "crypt": {"_all": ["ghoul", "geist", "cryptkeeper", "necrovore", "banshee", "reaper", "shade", "grue"]},
    "ancient_city": {"_all": ["grue", "shade", "fear", "spectre", "banshee", "epion", "darkling"]},
    "end": {"_all": ["argus", "astaroth", "grell", "spectre", "trite", "naxiris"]},
    "early": {"_all": ["conba", "lycosa", "ghoul"]},
    "illager": {},
}

BOSSES = {"rahovart", "asmodeus", "amalgalich"}


def classify(pool):
    s = set(pool)
    for arch, sig in SIGNATURES:
        if sig in s:
            return arch
    return None


def additions_for(archetype, tier):
    spec = ADDITIONS.get(archetype, {})
    if "_all" in spec:
        return list(spec["_all"])
    if tier not in TIER_ORDER:
        tier = DEFAULT_TIER[archetype]
    idx = TIER_ORDER.index(tier)
    result = []
    for t in TIER_ORDER:  # deterministic early->late ordering
        if t in spec and TIER_ORDER.index(t) <= idx:
            result += spec[t]
    return result


def regen_structures():
    with open(STRUCT_JSON, encoding="utf-8") as f:
        root = json.load(f)
    structures = root["structures"] if "structures" in root else root

    matched = {}
    unmatched = []
    for key, entry in structures.items():
        if key.startswith("_") or not isinstance(entry, dict):
            continue
        pool = entry.get("mobPool")
        if not pool:
            continue
        arch = classify(pool)
        if arch is None:
            unmatched.append(key)
            continue
        matched[arch] = matched.get(arch, 0) + 1
        tier = entry.get("progressionTier") or DEFAULT_TIER[arch]
        existing = set(pool)
        for name in additions_for(arch, tier):
            assert name not in BOSSES, f"boss {name} must never be added to a pool"
            rid = "lycanitesmobs:" + name
            if rid not in existing:
                pool.append(rid)
                existing.add(rid)

    with open(STRUCT_JSON, "w", encoding="utf-8") as f:
        json.dump(root, f, separators=(",", ":"), ensure_ascii=False)

    # Self-check: output re-parses, and no boss leaked into any pool.
    with open(STRUCT_JSON, encoding="utf-8") as f:
        check = json.load(f)
    cs = check["structures"] if "structures" in check else check
    for key, entry in cs.items():
        if key.startswith("_") or not isinstance(entry, dict):
            continue
        for m in entry.get("mobPool", []):
            assert not any(m == "lycanitesmobs:" + b for b in BOSSES), f"{key} contains boss {m}"

    print(f"structures: matched {matched}, unmatched ({len(unmatched)}): {unmatched}")


def regen_elements():
    out = {}
    for path in sorted(glob.glob(os.path.join(LYC_CREATURES, "*.json"))):
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        name = os.path.basename(path)[:-5]
        rec = {}
        elems = d.get("elements") or []
        if elems:
            rec["element"] = elems[0]
        ctype = d.get("creatureType")
        if ctype:
            rec["type"] = ctype
        out["lycanitesmobs:" + name] = rec
    with open(ELEM_JSON, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=1, ensure_ascii=False, sort_keys=True)
    print(f"elements: wrote {len(out)} creatures")


if __name__ == "__main__":
    if not os.path.isdir(LYC_CREATURES):
        sys.exit(f"Lycanites source not found at {LYC_CREATURES}")
    regen_elements()
    regen_structures()
    print("done")
```

- [ ] **Step 2: Run the generator**

Run: `cd /home/joshua/Documents/GitHub/runic-structures && python3 tools/lycanites_integration.py`
Expected output: `elements: wrote 122 creatures`, then `structures: matched {'crypt': 21, 'nether': 54, 'ocean': 14, 'illager': 13, 'ancient_city': 1, 'early': 63, 'end': 8, 'cave': 41, 'mage': 13}, unmatched (10): [...goblins/barako/ba_bt/mtr...]`, then `done`. The 10 unmatched are intentional (coherent factions or signature-ambiguous). The script asserts no boss ID leaked into any pool and that the output re-parses.

Note: the committed `tools/lycanites_integration.py` is the source of truth for the exact `SIGNATURES` / `ADDITIONS` / `DEFAULT_TIER` tables (it includes the crypt-variant + nether-camp signatures and the `mage` archetype documented in the spec). The code block above shows the structure; defer to the committed file.

- [ ] **Step 3: Verify the generated files**

Run:
```bash
python3 -c "import json; d=json.load(open('src/main/resources/defaults/runicstructures-lycanites-elements.json')); print(len(d), d['lycanitesmobs:afrit'])"
python3 -c "import json; r=json.load(open('src/main/resources/defaults/runicstructures-structures.json'))['structures']; import itertools; print([m for m in r['minecraft:fortress']['mobPool'] if m.startswith('lycanitesmobs')]); print([m for m in r['minecraft:monument']['mobPool'] if m.startswith('lycanitesmobs')]); print([m for m in r['minecraft:ancient_city']['mobPool'] if m.startswith('lycanitesmobs')])"
grep -c "lycanitesmobs:rahovart\|lycanitesmobs:asmodeus\|lycanitesmobs:amalgalich" src/main/resources/defaults/runicstructures-structures.json
```
Expected: element map has 122 entries and `{'element': 'fire', 'type': 'imp'}` for afrit; fortress shows nether mobs (`afrit, cinder, belphegor, salamander, trite` since fortress is `mid`); monument shows ocean mobs; ancient_city shows `grue, shade, fear, spectre, banshee, epion, darkling`; the boss grep prints `0`.

- [ ] **Step 4: Commit**

```bash
git add tools/lycanites_integration.py src/main/resources/defaults/runicstructures-lycanites-elements.json src/main/resources/defaults/runicstructures-structures.json
git commit -m "feat: generate Lycanites territory pools and element map"
```

---

## Task 2: LycanitesData lookup

**Files:**
- Create: `src/main/java/com/otectus/runicstructures/util/LycanitesData.java`

- [ ] **Step 1: Write LycanitesData**

Create the file:

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/otectus/runicstructures/util/LycanitesData.java
git commit -m "feat: add LycanitesData element lookup"
```

---

## Task 3: Guardian theming method + config + boss blacklist

**Files:**
- Modify: `src/main/java/com/otectus/runicstructures/util/MobEnhancer.java`
- Modify: `src/main/java/com/otectus/runicstructures/config/RSConfig.java`

- [ ] **Step 1: Add the theming method to MobEnhancer**

In `MobEnhancer.java`, add this import near the other imports:

```java
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
```

Then add this method after `applyEliteEnhancements`:

```java
    /**
     * Apply element-aware guardian theming to a Lycanites elite mob.
     * Tags it as a structure guardian ({@code rs_guardian}) and, when the creature
     * has a mapped element, overrides the elite name with an element-themed
     * adjective (e.g. "Infernal Afrit"). No-op for non-Lycanites mobs; mobs whose
     * element is unknown keep the generic elite name from
     * {@link #applyEliteEnhancements}. Call AFTER {@code applyEliteEnhancements}.
     */
    public static void applyLycanitesGuardianTheming(Mob mob, StructureProfile profile) {
        ResourceLocation id = EntityType.getKey(mob.getType());
        if (!LycanitesData.isLycanites(id)) return;

        mob.addTag("rs_guardian");

        String adjective = LycanitesData.elementAdjective(LycanitesData.getElement(id));
        if (adjective == null) return;

        mob.setCustomName(Component.translatable("entity.runicstructures.elite_prefix",
                adjective, mob.getType().getDescription()));
        mob.setCustomNameVisible(true);
    }
```

Note: `Component` and `Mob` are already imported in this file; `StructureProfile` is too. `LycanitesData` is in the same `util` package (no import needed).

- [ ] **Step 2: Add the boss IDs to the default mob blacklist**

In `RSConfig.java`, inside `buildMobConfig`, the `mobBlacklist` default list ends with the Majrusz entries:

```java
                                "majruszsdifficulty:cerberus", "majruszsdifficulty:giant",
                                "majruszsdifficulty:tank"),
```

Change the last line to append the Lycanites bosses:

```java
                                "majruszsdifficulty:cerberus", "majruszsdifficulty:giant",
                                "majruszsdifficulty:tank",
                                // Lycanites Mobs bosses
                                "lycanitesmobs:rahovart", "lycanitesmobs:asmodeus",
                                "lycanitesmobs:amalgalich"),
```

- [ ] **Step 3: Add the lycanites config section**

In `RSConfig.java`, add these two fields after the difficulty-scaling fields (near the other `public static` config fields, e.g. just before the `runtimeDebug` block):

```java
    // Lycanites integration
    public static BooleanValue lycanitesEliteTheming;
    public static IntValue guardianBonusXP;
```

In the static initializer block, add the call after `buildDifficultyConfig(builder);`:

```java
        buildDifficultyConfig(builder);
        buildLycanitesConfig(builder);
```

Add the builder method (place it after `buildDifficultyConfig`):

```java
    private static void buildLycanitesConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Lycanites Mobs Integration",
                "Element-aware theming for Lycanites mobs spawned as elites in runic structures").push("lycanites");

        lycanitesEliteTheming = builder
                .comment("Give Lycanites elite mobs element-themed names (e.g. \"Infernal Afrit\")",
                        "and tag them as structure guardians (rs_guardian tag)")
                .define("eliteTheming", true);

        guardianBonusXP = builder
                .comment("Extra XP dropped by Lycanites structure guardians when killed by a player",
                        "Added on top of the elite bonus XP")
                .defineInRange("guardianBonusXP", 50, 0, 500);

        builder.pop();
    }
```

- [ ] **Step 4: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`. (First run downloads Forge/MCP mappings; allow several minutes.)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/otectus/runicstructures/util/MobEnhancer.java src/main/java/com/otectus/runicstructures/config/RSConfig.java
git commit -m "feat: add guardian theming method, lycanites config, boss blacklist"
```

---

## Task 4: Wire theming into both spawn paths, XP, load, and validation

**Files:**
- Modify: `src/main/java/com/otectus/runicstructures/event/SpawnEventHandler.java`
- Modify: `src/main/java/com/otectus/runicstructures/event/TickSpawnHandler.java`
- Modify: `src/main/java/com/otectus/runicstructures/RunicStructures.java`

- [ ] **Step 1: Hook theming in SpawnEventHandler.onFinalizeSpawn**

In `SpawnEventHandler.java`, the elite block reads:

```java
            if (RSConfig.eliteSpawnsEnabled.get()
                    && evt.getLevel().getRandom().nextDouble() < profile.getEliteChance()) {
                MobEnhancer.applyEliteEnhancements(mob, profile);
            }
```

Replace it with:

```java
            if (RSConfig.eliteSpawnsEnabled.get()
                    && evt.getLevel().getRandom().nextDouble() < profile.getEliteChance()) {
                MobEnhancer.applyEliteEnhancements(mob, profile);
                if (RSConfig.lycanitesEliteTheming.get()) {
                    MobEnhancer.applyLycanitesGuardianTheming(mob, profile);
                }
            }
```

- [ ] **Step 2: Hook theming in TickSpawnHandler periodic elite branch**

In `TickSpawnHandler.java`, the block at lines ~248-250 reads:

```java
            if (RSConfig.eliteSpawnsEnabled.get() && random.nextDouble() < profile.getEliteChance()) {
                MobEnhancer.applyEliteEnhancements(mob, profile);
            }
```

Replace it with:

```java
            if (RSConfig.eliteSpawnsEnabled.get() && random.nextDouble() < profile.getEliteChance()) {
                MobEnhancer.applyEliteEnhancements(mob, profile);
                if (RSConfig.lycanitesEliteTheming.get()) {
                    MobEnhancer.applyLycanitesGuardianTheming(mob, profile);
                }
            }
```

- [ ] **Step 3: Award guardian bonus XP in onEliteMobDeath**

In `RunicStructures.java`, `onEliteMobDeath` currently reads:

```java
            int bonusXP = RSConfig.eliteBonusXP.get();
            if (bonusXP <= 0) return;
```

Replace with:

```java
            int bonusXP = RSConfig.eliteBonusXP.get();
            if (mob.getTags().contains("rs_guardian")) {
                bonusXP += RSConfig.guardianBonusXP.get();
            }
            if (bonusXP <= 0) return;
```

(`mob` is the `Mob` already bound earlier in the method via `evt.getEntity() instanceof ... Mob mob`.)

- [ ] **Step 4: Load LycanitesData at startup**

In `RunicStructures.java`, add the import:

```java
import com.otectus.runicstructures.util.LycanitesData;
```

In the constructor `public RunicStructures()`, add a load call:

```java
    public RunicStructures() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RSConfig.SPEC);
        LycanitesData.load();
    }
```

- [ ] **Step 5: Add validation warning**

In `RunicStructures.runConfigValidation`, just before `return warnings;`, add:

```java
        if (!LycanitesData.isLoaded()) {
            warnings.add("Lycanites element data failed to load; guardian theming will be inactive");
        }
```

- [ ] **Step 6: Full build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. Confirms all wiring compiles and the jar assembles with the new bundled resource.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/otectus/runicstructures/event/SpawnEventHandler.java src/main/java/com/otectus/runicstructures/event/TickSpawnHandler.java src/main/java/com/otectus/runicstructures/RunicStructures.java
git commit -m "feat: wire Lycanites guardian theming, bonus XP, data load, validation"
```

---

## Task 5: Changelog + final verification

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add a changelog entry**

Open `CHANGELOG.md`, and under the top (newest) section add a bullet group. Match the existing heading style in the file; insert at the top of the entries:

```markdown
### Added
- Lycanites Mobs structure-territory integration: element-matched Lycanites creatures are blended into per-structure mob pools (nether, ocean, cave, crypt/stronghold, ancient city, end, and ruins archetypes), scaled by progression tier.
- Element-aware guardian theming: Lycanites elites spawned in runic structures gain element-themed names (e.g. "Infernal Afrit") and an `rs_guardian` tag, with configurable bonus XP (`lycanites.guardianBonusXP`).
- Lycanites bosses (Rahovart, Asmodeus, Amalgalich) added to the default mob blacklist.
```

- [ ] **Step 2: Final full build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual runtime checklist (record results, do not skip)**

Document outcomes in the final report. In a dev instance (`./gradlew runServer` or the dev client) with Lycanites + the referenced mods present:
1. `/rs validate` reports no new errors and does NOT warn about Lycanites element data failing to load.
2. Travel to a nether fortress / ocean monument / cave dungeon / crypt and confirm Lycanites types from the matching archetype appear among forced spawns.
3. Temporarily set `eliteSpawns.enabled=true` and `eliteSpawns.spawnChance=1.0`; confirm a Lycanites spawn in a structure gets an element-themed name and, on player kill, drops elite+guardian XP.
4. In a pack WITHOUT Lycanites, confirm no errors: missing IDs are skipped by `pickFromMobPool` and theming is a no-op.

- [ ] **Step 4: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: changelog for Lycanites structure integration"
```

---

## Notes for the implementer

- Do not reintroduce JUnit; this repo intentionally has no Java test suite.
- The structures JSON is a single minified line by design — never hand-edit it; re-run the generator if pool changes are needed.
- The generator references an absolute path to the Lycanites source clone; it is a dev-only tool and is not shipped in the jar.
- Illager-archetype pools are intentionally left unchanged (no thematic Lycanites match; preserves village/illager restraint).
