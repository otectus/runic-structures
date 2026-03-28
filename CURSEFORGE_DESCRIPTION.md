# Runic Structures

*Built for the Runecraft modpack. Compatible with InControl, Majrusz's Difficulty, and 40+ structure mods.*

**Every structure is a dungeon now.**

Walk into a desert temple at high noon and find armored zombies waiting in the dark. Clear a stronghold room only for fresh hordes to fill it minutes later. Stumble into a woodland mansion where glowing elite mobs hit twice as hard and drop bonus XP when you finally take them down.

Runic Structures turns every generated structure in Minecraft — temples, fortresses, strongholds, monuments, mansions, and any modded structures — into permanently hostile territory. Mobs spawn inside them around the clock regardless of light level, time of day, or how many torches you place. Structures are no longer empty shells you loot once and forget. They're threats.

---

## How It Works

The mod uses three systems that work together to keep structures runic:

**Immediate Population** — The moment you discover a structure, 2-5 hostile mobs are already waiting inside, spread across its rooms and corridors. No empty temples. No quiet strongholds. Every structure is runic from the start.

**Spawn Interception** — Vanilla spawn rules are overridden inside structures. Hostile mobs bypass light level and time-of-day checks, so torches and daylight won't save you.

**Periodic Reinforcement** — A background spawner continuously maintains mob presence inside structures. Kill everything in a room and more will appear. The structure fights back.

---

## Armored & Armed Hostiles

Mobs spawned by this mod aren't pushovers. They come equipped with **randomized armor** pulled from a configurable tier pool — leather, chainmail, iron, gold, diamond, or even netherite. Each piece has an independent roll, so you might face a zombie in mismatched iron and gold, or a skeleton in full diamond. Diamond and netherite tiers have reduced drop chances to prevent loot inflation.

Enable **weapons** and melee mobs get stone, iron, or diamond swords (skeletons keep their bows). Enable **enchantments** and all equipment gets Protection, Sharpness, or Power at configurable levels.

All mod-spawned mobs also receive **permanent Fire Resistance**, so undead won't burn in sunlight and no mobs can be cheesed with lava traps or Fire Aspect. You can't wait them out.

**Smart anti-stacking** — If a mob already has armor or weapons from InControl or Majrusz's Difficulty, Runic Structures won't overwrite it. No more absurdly double-equipped mobs.

Armor tiers, weapons, enchantments, drop chances, and fire immunity are all configurable.

---

## Elite Mobs *(opt-in)*

Enable elite spawns for a chance that any mob spawned by the mod becomes an enhanced **Elite** variant:

- Boosted health and attack damage (configurable multipliers)
- **Glowing effect** — visible through walls so you know they're coming
- Custom name tag (e.g. "Elite Zombie")
- **Bonus XP** dropped on kill as a reward

Elite spawn chance, stat multipliers, glow, naming, and XP are all configurable. Disabled by default.

---

## Atmospheric Effects *(opt-in)*

For players who want structures to *feel* more runic:

- **Darkness effect** — the same pulsing vision from the Warden, applied while inside a runic structure
- **Mining Fatigue** — slows block-breaking to discourage cheese strategies
- **Slowness** — subtle movement penalty that increases danger
- **Ambient sounds** — weighted pool of ominous sounds (cave ambience, basalt deltas, soul sand valley) at configurable intervals

All disabled by default and fully configurable.

---

## Smart Spawning

The spawning system is designed to feel natural, not cheap:

- **Structure piece-aware** — mobs spawn inside actual rooms and corridors, not random points in the bounding box
- **Pack spawning** — mobs appear in groups matching vanilla biome pack sizes
- **Biome-correct mobs** — respects vanilla spawn weights so you see the right mobs in the right places
- **Hazard avoidance** — won't spawn mobs in lava, water, or on rooftops
- **Difficulty scaling** — fewer mobs on Easy, more on Hard, with configurable multipliers for cap and interval
- **Night boost** — optional faster spawn rate at night

---

## 238 Structure Profiles

Every structure in the Runecraft modpack has a custom-tailored profile. Spawn caps scale with structure size, armor tiers match difficulty, environmental effects fit the theme, and progression gates align with when players encounter each structure.

- **27 mods covered** -- Vanilla, YUNG's Better Series, When Dungeons Arise, Dungeons and Taverns, Hopo, Moog's, Underground Rooms, Medieval Buildings, Battle Towers, Ice and Fire, Iron's Spellbooks, Ars Nouveau, and more
- **Calibrated sizing** -- Tiny ruins get 1-2 mobs; colossal fortresses get 14-16. Every structure's cap matches its actual room count and footprint.
- **Thematic elite names** -- "Cursed" for undead crypts, "Wither" for nether fortresses, "Arcane" for magic towers, "Commander" for illager forts, "Dragonsworn" for Ice and Fire structures
- **Progression gating** -- Early-game temples get leather armor; late-game strongholds get diamond and enchantments. Profiles activate as the server progresses through game days.
- **Fully overridable** -- Edit `runicstructures-structures.json` to customize any structure

All 238 profiles ship as defaults. Delete the JSON file to regenerate with fresh defaults.

---

## Full Control

Everything is configurable through 60+ options in the server config:

- **Auto-detect** all structures (vanilla + modded) with no setup, or manually whitelist/blacklist by ID or tag
- **Mob filtering** — whitelist or blacklist specific mob types
- **Dimension filtering** — choose which dimensions are affected
- **Spawn tuning** — interval, cap, pack size, player distance, position attempts
- **Structure scaling** — optionally scale mob cap based on structure footprint area

### Quick Config Examples

Make only specific structures runic:
```toml
[general]
    autoDetectStructures = false
[structures]
    structureWhitelist = ["minecraft:fortress", "minecraft:stronghold", "minecraft:monument"]
```

Enable the full experience:
```toml
[eliteSpawns]
    enabled = true
    spawnChance = 0.05
[environmentalEffects]
    applyDarknessEffect = true
    playAmbientSounds = true
[armor]
    allowDiamond = true
    allowNetherite = true
```

Tune initial population:
```toml
[periodicSpawning]
    initialPopulationEnabled = true
    initialPopulationMin = 4
    initialPopulationMax = 8
```

---

## Admin Commands

All commands require OP level 2. Use `/runicstructures` or the shorthand `/rs`.

| Command | What it does |
|---------|-------------|
| `/rs info` | Check if you're standing inside a runic structure |
| `/rs list` | Show all nearby runic structures with coordinates |
| `/rs status` | View current mod configuration at a glance |
| `/rs spawns` | View spawn statistics (attempts, successes, failures) |
| `/rs spawns detail` | Per-structure spawn statistics (top 10 active structures) |
| `/rs reload` | Force-refresh all structure caches |
| `/rs debug [on/off]` | Toggle per-spawn debug logging (survives config reloads) |
| `/rs validate` | Re-run config validation and report issues in chat |

---

## Mod API

Other mods can register their structures as runic:

```java
RunicStructuresAPI.registerRunicStructure(
    new ResourceLocation("mymod", "my_dungeon"));

// Query whether a position is inside a runic structure
boolean runic = RunicStructuresAPI.isPositionRunic(level, pos);
```

---

## Runecraft Modpack Integration

Runic Structures is the authoritative system for structure-based mob spawning in the Runecraft modpack, with 238 individualized profiles across 27 mods:

- **InControl** handles biome, weather, phase, and underground spawning. RS handles structure spawning. No conflicts.
- **Majrusz's Difficulty** Expert/Master scaling works alongside RS without stat-stacking -- RS includes health thresholds that skip elite buffs on already-buffed mobs.
- **45+ boss mobs** blacklisted by default from Cataclysm, BOMD, Mowzie's, Ice & Fire, Stalwart Dungeons, and more.
- **Boss arenas** (Cataclysm, Stalwart, BOMD) are excluded to preserve their scripted encounters.
- All other structures -- Dungeons Arise, YUNG's, Medieval Buildings, Moog's, and more -- are runic by default with per-structure tuning.

## Server-Side Only

No client mod needed. Install on a dedicated server or singleplayer and it just works. Players connect and play — no extra downloads required.

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17+

## Links

- [Source Code](https://github.com/Otectus/Runic-Structures)
- [Issue Tracker](https://github.com/Otectus/Runic-Structures/issues)
