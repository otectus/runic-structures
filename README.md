# Runic Structures

A Minecraft Forge mod built for the **Runecraft modpack** that transforms every generated structure into permanently hostile territory. Hostile mobs spawn inside structures around the clock — regardless of light level, time of day, or torches — with randomized armor, fire immunity, and optional elite variants.

Designed to complement InControl's overworld spawn rules and Majrusz's Difficulty scaling without stacking conflicts.

## Features

### Core
- **Automatic structure detection** — All registered structures (vanilla + modded) are runic by default
- **Village exclusion** — Villages excluded by default (toggle available)
- **Structure whitelist/blacklist** — Fine-grained control by ID or tag
- **Mob whitelist/blacklist** — Control which hostile mobs can spawn (45+ bosses blacklisted by default)
- **Dimension filtering** — Choose which dimensions are affected
- **Buffer radius** — Expand structure boundaries for spawn detection
- **Fully server-side** — No client installation required

### Spawning
- **Initial population** — 2-5 mobs spawn throughout a structure the first time it's discovered each session
- **Periodic reinforcement** — Background spawner maintains mob presence at configurable intervals
- **Structure piece-aware** — Targets individual rooms and corridors, not the full bounding box
- **Column-based floor finding** — Efficient vertical scanning for valid spawn positions
- **Pack spawning** — Mobs spawn in groups matching vanilla biome pack sizes
- **Vanilla spawn weights** — Respects biome mob probabilities
- **Hazard avoidance** — Skips lava, water, and rooftop positions
- **Difficulty scaling** — Spawn cap and interval scale with Easy/Normal/Hard
- **Night spawn boost** — Configurable faster spawning at night
- **Min player distance** — Prevents mobs spawning on top of players

### Per-Structure Identity
- **Structure-aware spawn pipeline** — Every spawn path knows which specific structure it's inside
- **Per-structure JSON profiles** — Override spawn cap, elite chance, armor tiers, weapon pool, shields, enchantment level, and environmental effects per structure
- **Progression gating** — Per-structure profiles can require a minimum game day (early/mid/late/veteran) before activating
- **238 default profiles** across 27 mods (Vanilla, YUNG's, Dungeons Arise, Dungeons and Taverns, Hopo, Moog's, Underground Rooms, Medieval Buildings, Battle Towers, Ice and Fire, Iron's Spellbooks, Ars Nouveau, and more) with individualized settings

### Mob Enhancement
- **Randomized armor** — Configurable tier pool (chainmail through diamond by default) with per-tier drop chance multipliers and custom modded armor set support
- **Randomized weapons** — Config-driven weapon pool supporting any modded weapon (Spartan Weaponry, Cataclysm Tools, etc.)
- **Shield/offhand support** (opt-in) — Melee mobs receive shields (Spartan Shields, vanilla shield, or any modded shield)
- **Equipment enchantments** (opt-in) — Weighted random enchantments (Protection/Blast/Projectile/Thorns on armor, Sharpness/Knockback/Fire Aspect on weapons)
- **Fire immunity** — Permanent Fire Resistance prevents sunlight burning and also blocks lava/fire attrition when enabled
- **Anti-stacking guards** — Respects existing armor/weapons from InControl/Majrusz's Difficulty; skips elite buffs on already-buffed mobs
- **Elite mobs** (opt-in) — Rare enhanced mobs with per-structure custom name prefixes, boosted stats, glow, and bonus XP

### Atmospheric Effects (per-structure or global)
- **Darkness effect** — Pulsing Warden-style vision inside structures
- **Mining Fatigue** — Slows block-breaking, discourages cheese strategies
- **Slowness** — Subtle movement penalty that increases danger
- **Ambient sounds** — Weighted pool of ominous ambient sounds at configurable intervals
- All effects can be enabled/disabled per structure via JSON profiles

### Admin Commands
All commands require OP level 2. Available under `/runicstructures` or the alias `/rs`.

| Command | Description |
|---------|-------------|
| `/rs status` | Show mod status and config summary |
| `/rs info` | Check if you're inside a runic structure |
| `/rs list` | List nearby structures with runic/excluded status and coordinates |
| `/rs reload` | Reload structure profiles, invalidate runtime caches, and report validation issues |
| `/rs debug on|off|true|false` | Toggle debug logging (survives config reloads) |
| `/rs spawns` | Show spawn statistics |
| `/rs spawns detail` | Show per-structure spawn statistics (top 10) |
| `/rs validate` | Re-run config validation and report issues |

## Runecraft Modpack Integration

Runic Structures is the authoritative system for structure-based mob spawning in Runecraft. InControl's structure-specific spawn entries (stronghold sentinels, ancient city guardians, pyramid mimics) have been removed to avoid conflicts — RS handles all structure spawning uniformly.

### Compatibility
- **InControl** — RS owns structure spawning; InControl handles biome, weather, phase, and underground spawning. The two systems are complementary. InControl's global mob enhancements (3% elites, stat boosts) still apply to RS-spawned mobs.
- **Majrusz's Difficulty** — RS difficulty scaling is gentled (0.8x/1.2x cap, 1.1x/0.85x interval) to avoid stacking with Majrusz's Expert/Master tiers. The `eliteMaxBaseHealth` threshold prevents RS elite buffs from stacking on mobs already buffed by Majrusz's system.
- **Apotheosis** — RS's optional enchantment system applies basic enchantments (Protection, Sharpness, Power). Apotheosis's adventure module can further enhance RS-spawned mobs naturally.
- **Boss mods** — 45+ boss entities blacklisted by default (Cataclysm, BOMD, Mowzie's, Ice & Fire, Stalwart Dungeons, Saints Dragons, Iron's Spellbooks, Ars Nouveau, RealmRPG, Galosphere, Dark Doppelganger, Majrusz's Difficulty).
- **Structure mods** — Cataclysm arenas, Stalwart Dungeons boss arenas, and BOMD arenas are blacklisted to preserve their scripted encounters. All other structures (DungeonsArise, Yung's, Medieval Buildings, etc.) are runic by default.

## Requirements

| | Version |
|---|---|
| Mod | 0.9.0 |
| Minecraft | 1.20.1 |
| Forge | 47.2.0+ |
| Java | 17+ |

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) for 1.20.1
2. Download the latest `runicstructures-x.x.x.jar` from [Releases](../../releases)
3. Place the JAR in your `mods/` folder
4. Launch the game — config generates on first server start

## Configuration

The config file is generated at `world/serverconfig/runicstructures-server.toml` on first world load.

### General

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Master enable/disable |
| `autoDetectStructures` | Boolean | `true` | Auto-detect all structures. When enabled, whitelists are ignored. |
| `includeVillages` | Boolean | `false` | Include villages when auto-detect is enabled |

### Structure Filtering

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `structureWhitelist` | List | `["minecraft:fortress", ...]` | Structure IDs to make runic (manual mode only) |
| `structureTagWhitelist` | List | `[]` | Structure tags to include (manual mode only) |
| `structureBlacklist` | List | *(see below)* | Structure IDs to NEVER make runic (all modes) |
| `structureTagBlacklist` | List | `[]` | Structure tags to NEVER make runic (all modes) |

Default structure blacklist: Cataclysm arenas (`burning_arena`, `cursed_pyramid`, `ruined_citadel`, `soul_black_smith`, `sunken_city`), Stalwart Dungeons boss arenas (`awful_dungeon`, `nether_dungeon`, `end_dungeon`), BOMD arenas (`lich_tower`, `void_blossom_structure`, `gauntlet_arena`, `obsidilith_arena`), `minecraft:trail_ruins`, `minecraft:igloo`, `minecraft:desert_well`.

### Mob Filtering

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `mobWhitelist` | List | `[]` | If non-empty, ONLY these mobs spawn. Priority over blacklist. |
| `mobBlacklist` | List | *(45+ entries)* | Mobs to exclude (bosses from 14 mod namespaces) |
| `dimensionSpecificMobs` | Boolean | `true` | Pick mobs from the current biome's spawn list |

### Other Settings

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `allowedDimensions` | List | `["minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "mining_dimension:mining", "dungeon_realm:dungeon_realm"]` | Active dimensions |
| `bufferRadius` | Integer (0-16) | `0` | Extra blocks around structure bounds |
| `debugLogging` | Boolean | `false` | Log each spawn to console |

### Periodic Spawning

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable periodic forced spawning |
| `spawnInterval` | Integer (20-6000) | `300` | Ticks between spawn attempts |
| `spawnCapPerStructure` | Integer (1-64) | `6` | Max hostile mobs per structure |
| `maxPositionAttempts` | Integer (1-20) | `10` | Position attempts per spawn cycle |
| `minPlayerDistance` | Integer (0-48) | `6` | Min distance from players (initial population ignores this) |
| `chunkSearchRadius` | Integer (2-8) | `3` | Chunk radius to search for structures |
| `randomizeInterval` | Boolean | `true` | Add jitter to prevent synchronized waves |
| `useStructurePieces` | Boolean | `true` | Spawn within individual structure pieces |
| `weightPiecesByArea` | Boolean | `true` | Prefer larger pieces (rooms) for spawning |
| `useVanillaSpawnWeights` | Boolean | `true` | Respect biome spawn weight probabilities (requires `dimensionSpecificMobs`) |
| `packSpawning` | Boolean | `true` | Spawn mobs in packs |
| `maxPackSize` | Integer (1-8) | `3` | Maximum pack size |
| `scaleCapByStructureSize` | Boolean | `true` | Scale cap by structure footprint area |
| `mobsPerChunkArea` | Integer (1-16) | `2` | Mobs per 16x16 area (when scaling) |
| `maxScaledCap` | Integer (1-128) | `20` | Absolute max cap (when scaling) |
| `fireImmunity` | Boolean | `true` | Grant permanent Fire Resistance |
| `persistentMobs` | Boolean | `false` | Prevent spawned mobs from despawning |
| `nightSpawnMultiplier` | Double (1.0-4.0) | `1.0` | Night spawn rate multiplier |
| `initialPopulationEnabled` | Boolean | `true` | Spawn a group on first discovery |
| `initialPopulationMin` | Integer (1-16) | `2` | Minimum mobs for initial population |
| `initialPopulationMax` | Integer (1-32) | `5` | Maximum mobs for initial population |

### Armor

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `armorEnabled` | Boolean | `true` | Equip mod-spawned mobs with randomized armor |
| `armorDropChance` | Double (0.0-1.0) | `0.05` | Drop chance per armor piece on death |
| `allowLeather` | Boolean | `false` | Include leather armor in the pool |
| `allowChainmail` | Boolean | `true` | Include chainmail armor in the pool |
| `allowIron` | Boolean | `true` | Include iron armor in the pool |
| `allowGold` | Boolean | `true` | Include gold armor in the pool |
| `allowDiamond` | Boolean | `true` | Include diamond armor in the pool |
| `allowNetherite` | Boolean | `false` | Include netherite armor in the pool |
| `respectExistingArmor` | Boolean | `true` | Skip armor if mob already has equipment (prevents InControl/Majrusz's stacking) |
| `diamondDropMultiplier` | Double (0.0-2.0) | `0.5` | Drop chance multiplier for diamond armor |
| `netheriteDropMultiplier` | Double (0.0-2.0) | `0.25` | Drop chance multiplier for netherite armor |

### Weapons

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `weaponsEnabled` | Boolean | `false` | Equip melee mobs with random swords (skeletons excluded) |
| `weaponDropChance` | Double (0.0-1.0) | `0.05` | Weapon drop chance on death |
| `respectExistingWeapon` | Boolean | `true` | Skip weapon if mob already has a mainhand item |

### Enchantments

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enchantmentsEnabled` | Boolean | `false` | Apply random enchantments to mod-applied equipment |
| `maxEnchantmentLevel` | Integer (1-5) | `2` | Maximum enchantment level |

### Equipment Pools

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `weaponPool` | List | `[]` | Custom weapon items (replaces hardcoded swords when non-empty) |
| `customArmorSets` | List | `[]` | Custom armor sets: `"name=helmet,chest,legs,boots"` |
| `shieldsEnabled` | Boolean | `false` | Enable offhand shield equipment |
| `shieldPool` | List | `["minecraft:shield"]` | Shield items for offhand |
| `shieldChance` | Double (0.0-1.0) | `0.2` | Chance for melee mobs to receive a shield |
| `progressionScalingEnabled` | Boolean | `false` | Enable day-count-based profile progression gating |

### Per-Structure Profiles

Per-structure overrides are defined in `serverconfig/runicstructures-structures.json`. Each structure ID maps to an override object. Any field omitted uses the global TOML config value.

Available override fields: `spawnCap`, `eliteChance`, `eliteNamePrefix`, `persistentMobs`, `armorTiers`, `weaponPool`, `shieldChance`, `shieldPool`, `enchantmentLevel`, `darkness`, `miningFatigue`, `slowness`, `ambientSounds`, `progressionTier`.

### Difficulty Scaling

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable difficulty-based scaling |
| `easyCapMultiplier` | Double (0.1-4.0) | `0.5` | Cap multiplier on Easy |
| `normalCapMultiplier` | Double (0.1-4.0) | `0.8` | Cap multiplier on Normal |
| `hardCapMultiplier` | Double (0.1-4.0) | `1.2` | Cap multiplier on Hard |
| `easyIntervalMultiplier` | Double (0.25-4.0) | `1.5` | Interval multiplier on Easy (higher = slower) |
| `normalIntervalMultiplier` | Double (0.25-4.0) | `1.1` | Interval multiplier on Normal |
| `hardIntervalMultiplier` | Double (0.25-4.0) | `0.85` | Interval multiplier on Hard (lower = faster) |

### Environmental Effects

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `applyDarknessEffect` | Boolean | `false` | Apply Darkness effect to players in structures |
| `darknessAmplifier` | Integer (0-4) | `0` | Darkness effect amplifier level |
| `applyMiningFatigue` | Boolean | `false` | Apply Mining Fatigue to players in structures |
| `miningFatigueAmplifier` | Integer (0-4) | `0` | Mining Fatigue amplifier level |
| `applySlowness` | Boolean | `false` | Apply Slowness to players in structures |
| `slownessAmplifier` | Integer (0-4) | `0` | Slowness amplifier level |
| `playAmbientSounds` | Boolean | `false` | Play ominous ambient sounds in structures |
| `ambientSoundInterval` | Integer (40-2400) | `400` | Ticks between ambient sounds |

### Elite Spawns

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable rare elite mob spawns |
| `spawnChance` | Double (0.0-1.0) | `0.02` | Chance for each spawn to be elite |
| `healthMultiplier` | Double (1.0-10.0) | `2.5` | Elite health multiplier |
| `damageMultiplier` | Double (1.0-10.0) | `1.75` | Elite damage multiplier |
| `glowingEffect` | Boolean | `true` | Elite mobs glow through walls |
| `namePrefix` | String | `"Runic"` | Custom name prefix for elite mobs |
| `bonusXP` | Integer (0-500) | `30` | Bonus XP when an elite is killed by a player |
| `maxBaseHealth` | Double (0.0-1000.0) | `40.0` | Skip elite buffs if mob's base health exceeds this (0 = disabled) |

## API

Other mods can interact with Runic Structures via `RunicStructuresAPI`:

```java
import com.otectus.runicstructures.api.RunicStructuresAPI;

// Register a structure as runic
RunicStructuresAPI.registerRunicStructure(new ResourceLocation("mymod", "my_dungeon"));

// Check if a position is runic
boolean runic = RunicStructuresAPI.isPositionRunic(serverLevel, blockPos);

// Unregister
RunicStructuresAPI.unregisterRunicStructure(new ResourceLocation("mymod", "my_dungeon"));
```

## How It Works

The mod uses three complementary mechanisms:

1. **Initial population** — When a structure is first discovered each session, 2-5 mobs are immediately spawned throughout its rooms and corridors. Uses aggressive position-finding (50 scan attempts) with distributed placement across structure pieces.

2. **Spawn event interception** — Hooks into Forge's `MobSpawnEvent.SpawnPlacementCheck` and `MobSpawnEvent.PositionCheck` to force-allow hostile mob spawns inside runic structures, bypassing light level and time-of-day checks.

3. **Periodic spawning** — A server tick handler maintains mob presence inside structures at configurable intervals. Uses structure piece-aware targeting, column-based floor finding, pack spawning, and hazard avoidance.

All mod-spawned mobs — regardless of spawn path — receive the full enhancement pipeline: elite roll, armor, weapons, enchantments, and Fire Resistance (each gated by config). Structure detection uses Minecraft's `StructureManager` with a per-chunk cache and fast-reject gate (`hasAnyStructureAt`) for efficient lookups.

## Building from Source

```bash
git clone https://github.com/Otectus/Runic-Structures.git
cd Runic-Structures
./gradlew build
```

The built JAR will be in `build/libs/`.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
