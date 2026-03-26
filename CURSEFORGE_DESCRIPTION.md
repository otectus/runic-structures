# Dangerous Structures

**Every structure is a dungeon now.**

Walk into a desert temple at high noon and find armored zombies waiting in the dark. Clear a stronghold room only for fresh hordes to fill it minutes later. Stumble into a woodland mansion where glowing elite mobs hit twice as hard and drop bonus XP when you finally take them down.

Dangerous Structures turns every generated structure in Minecraft — temples, fortresses, strongholds, monuments, mansions, and any modded structures — into permanently hostile territory. Mobs spawn inside them around the clock regardless of light level, time of day, or how many torches you place. Structures are no longer empty shells you loot once and forget. They're threats.

---

## How It Works

The mod uses three systems that work together to keep structures dangerous:

**Immediate Population** — The moment you discover a structure, 3-6 hostile mobs are already waiting inside, spread across its rooms and corridors. No empty temples. No quiet strongholds. Every structure is dangerous from the start.

**Spawn Interception** — Vanilla spawn rules are overridden inside structures. Hostile mobs bypass light level and time-of-day checks, so torches and daylight won't save you.

**Periodic Reinforcement** — A background spawner continuously maintains mob presence inside structures. Kill everything in a room and more will appear. The structure fights back.

---

## Armored Hostiles

Mobs spawned by this mod aren't pushovers. They come equipped with **randomized armor** pulled from a configurable tier pool — leather, chainmail, iron, gold, diamond, or even netherite. Each piece has an independent roll, so you might face a zombie in mismatched iron and gold, or a skeleton in full diamond.

All mod-spawned mobs also receive **permanent Fire Resistance**, so undead won't burn in sunlight and no mobs can be cheesed with lava traps or Fire Aspect. You can't wait them out.

Armor tiers, drop chances, and fire immunity are all configurable.

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

For players who want structures to *feel* more dangerous:

- **Darkness effect** — the same pulsing vision from the Warden, applied while inside a dangerous structure
- **Ambient sounds** — ominous cave sounds play at configurable intervals

Both are disabled by default and fully configurable.

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

## Full Control

Everything is configurable through 50+ options in the server config:

- **Auto-detect** all structures (vanilla + modded) with no setup, or manually whitelist/blacklist by ID or tag
- **Mob filtering** — whitelist or blacklist specific mob types
- **Dimension filtering** — choose which dimensions are affected
- **Spawn tuning** — interval, cap, pack size, player distance, position attempts
- **Structure scaling** — optionally scale mob cap based on structure footprint area

### Quick Config Examples

Make only specific structures dangerous:
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

All commands require OP level 2. Use `/dangerousstructures` or the shorthand `/ds`.

| Command | What it does |
|---------|-------------|
| `/ds info` | Check if you're standing inside a dangerous structure |
| `/ds list` | Show all nearby dangerous structures with coordinates |
| `/ds status` | View current mod configuration at a glance |
| `/ds spawns` | View spawn statistics (attempts, successes, failures) |
| `/ds reload` | Force-refresh all structure caches |
| `/ds debug [on/off]` | Toggle per-spawn debug logging (survives config reloads) |
| `/ds validate` | Re-run config validation and report issues in chat |

---

## Mod API

Other mods can register their structures as dangerous:

```java
DangerousStructuresAPI.registerDangerousStructure(
    new ResourceLocation("mymod", "my_dungeon"));

// Query whether a position is inside a dangerous structure
boolean dangerous = DangerousStructuresAPI.isPositionDangerous(level, pos);
```

---

## Server-Side Only

No client mod needed. Install on a dedicated server or singleplayer and it just works. Players connect and play — no extra downloads required.

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17+

## Links

- [Source Code](https://github.com/Otectus/Dangerous-Structures)
- [Issue Tracker](https://github.com/Otectus/Dangerous-Structures/issues)
