# Dangerous Structures

A Minecraft Forge mod that transforms structures into hostile territory. All structures — vanilla and modded — become permanent mob hotbeds where hostile creatures spawn around the clock, regardless of light level or time of day.

## Features

### Core
- **Automatic structure detection** — All registered structures (including modded) are dangerous by default
- **Village exclusion** — Villages are excluded by default, with a toggle to include them
- **Structure whitelist/blacklist** — Fine-grained control by ID or tag
- **Mob whitelist/blacklist** — Control which hostile mobs can spawn
- **Dimension filtering** — Choose which dimensions are affected
- **Buffer radius** — Expand structure boundaries for spawn detection
- **Fully server-side** — No client installation required

### Spawning (v1.1.0)
- **Structure piece-aware spawning** — Targets individual rooms and corridors instead of the full bounding box
- **Column-based floor finding** — Efficient vertical scanning for valid spawn positions
- **Pack spawning** — Mobs spawn in groups matching vanilla pack sizes
- **Vanilla spawn weights** — Respects biome mob probabilities
- **Hazard avoidance** — Skips lava and water positions
- **Difficulty scaling** — Spawn cap and interval scale with Easy/Normal/Hard
- **Night spawn boost** — Configurable faster spawning at night
- **Min player distance** — Prevents mobs spawning on top of players

### New in v1.1.0
- **Initial population** — 3-6 mobs spawn throughout a structure the first time it's discovered each session
- **Armor system** — Mod-spawned mobs are equipped with randomized armor (configurable tiers and drop chance)
- **Fire immunity** — Mod-spawned mobs receive permanent Fire Resistance to survive outside structures
- **Admin commands** — `/ds status`, `/ds info`, `/ds list`, `/ds reload`, `/ds debug`, `/ds spawns`, `/ds validate`
- **Elite mob spawns** — Rare enhanced mobs with boosted stats, glow, and bonus XP
- **Environmental effects** — Darkness effect and ambient sounds inside structures (opt-in)
- **Mob persistence** — Optional flag to prevent spawned mobs from despawning
- **Mod API** — Public API for other mods to register structures as dangerous
- **Config validation** — Warns about invalid structure/mob IDs on server start

## Requirements

| | Version |
|---|---|
| Mod | 1.1.1 |
| Minecraft | 1.20.1 |
| Forge | 47.2.0+ |
| Java | 17+ |

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) for 1.20.1
2. Download the latest `dangerousstructures-x.x.x.jar` from [Releases](../../releases)
3. Place the JAR in your `mods/` folder
4. Launch the game — config generates on first server start

## Commands

All commands require OP level 2. Available under `/dangerousstructures` or the alias `/ds`.

| Command | Description |
|---------|-------------|
| `/ds status` | Show mod status, config summary |
| `/ds info` | Show which dangerous structure(s) contain your position |
| `/ds list` | List nearby dangerous structures |
| `/ds reload` | Force-invalidate all caches |
| `/ds debug [on/off]` | Toggle debug logging at runtime (survives config reloads) |
| `/ds spawns` | Show spawn statistics |
| `/ds validate` | Re-run config validation and report issues |

## Configuration

The config file is generated at `world/serverconfig/dangerousstructures-server.toml` on first world load.

### General

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Master enable/disable |
| `autoDetectStructures` | Boolean | `true` | Auto-detect all structures. When enabled, whitelists are ignored. |
| `includeVillages` | Boolean | `false` | Include villages when auto-detect is enabled |

### Structure Filtering

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `structureWhitelist` | List | `["minecraft:fortress", ...]` | Structure IDs to make dangerous (manual mode only) |
| `structureTagWhitelist` | List | `[]` | Structure tags to include (manual mode only) |
| `structureBlacklist` | List | `[]` | Structure IDs to NEVER make dangerous (all modes) |
| `structureTagBlacklist` | List | `[]` | Structure tags to NEVER make dangerous (all modes) |

### Mob Filtering

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `mobWhitelist` | List | `[]` | If non-empty, ONLY these mobs spawn. Priority over blacklist. |
| `mobBlacklist` | List | `["minecraft:ender_dragon", "minecraft:wither"]` | Mobs to exclude |
| `dimensionSpecificMobs` | Boolean | `true` | Pick mobs from the current biome's spawn list |

### Other Settings

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `allowedDimensions` | List | `["minecraft:overworld", ...]` | Active dimensions |
| `bufferRadius` | Integer (0-16) | `0` | Extra blocks around structure bounds |
| `debugLogging` | Boolean | `false` | Log each spawn to console |

### Periodic Spawning

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable periodic forced spawning during daytime |
| `spawnInterval` | Integer (20-6000) | `200` | Ticks between spawn attempts |
| `spawnCapPerStructure` | Integer (1-64) | `8` | Max hostile mobs per structure |
| `maxPositionAttempts` | Integer (1-20) | `10` | Position attempts per spawn cycle |
| `minPlayerDistance` | Integer (0-48) | `6` | Min distance from players for spawns |
| `chunkSearchRadius` | Integer (2-8) | `4` | Chunk radius to search for structures |
| `randomizeInterval` | Boolean | `true` | Add jitter to prevent synchronized waves |
| `useStructurePieces` | Boolean | `true` | Spawn within individual structure pieces |
| `weightPiecesByArea` | Boolean | `true` | Prefer larger pieces (rooms) for spawning |
| `useVanillaSpawnWeights` | Boolean | `true` | Respect biome spawn weight probabilities |
| `packSpawning` | Boolean | `true` | Spawn mobs in packs |
| `maxPackSize` | Integer (1-8) | `4` | Maximum pack size |
| `scaleCapByStructureSize` | Boolean | `false` | Scale cap by structure footprint |
| `mobsPerChunkArea` | Integer (1-16) | `3` | Mobs per 16x16 area (when scaling) |
| `maxScaledCap` | Integer (1-128) | `32` | Absolute max cap (when scaling) |
| `fireImmunity` | Boolean | `true` | Grant permanent Fire Resistance (prevents sunlight burning, fire, lava, Fire Aspect) |
| `persistentMobs` | Boolean | `false` | Prevent spawned mobs from despawning |
| `nightSpawnMultiplier` | Double (1.0-4.0) | `1.0` | Night spawn rate multiplier |
| `initialPopulationEnabled` | Boolean | `true` | Spawn a group of mobs when a structure is first discovered |
| `initialPopulationMin` | Integer (1-16) | `3` | Minimum mobs for initial population |
| `initialPopulationMax` | Integer (1-32) | `6` | Maximum mobs for initial population |

### Armor

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `armorEnabled` | Boolean | `true` | Equip mod-spawned mobs with randomized armor |
| `armorDropChance` | Double (0.0-1.0) | `0.085` | Drop chance per armor piece on death |
| `allowLeather` | Boolean | `true` | Include leather armor in the pool |
| `allowChainmail` | Boolean | `true` | Include chainmail armor in the pool |
| `allowIron` | Boolean | `true` | Include iron armor in the pool |
| `allowGold` | Boolean | `true` | Include gold armor in the pool |
| `allowDiamond` | Boolean | `true` | Include diamond armor in the pool |
| `allowNetherite` | Boolean | `false` | Include netherite armor in the pool |

### Difficulty Scaling

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable difficulty-based scaling |
| `easyCapMultiplier` | Double (0.1-4.0) | `0.5` | Cap multiplier on Easy |
| `normalCapMultiplier` | Double (0.1-4.0) | `1.0` | Cap multiplier on Normal |
| `hardCapMultiplier` | Double (0.1-4.0) | `1.5` | Cap multiplier on Hard |
| `easyIntervalMultiplier` | Double (0.25-4.0) | `1.5` | Interval multiplier on Easy (higher = slower) |
| `normalIntervalMultiplier` | Double (0.25-4.0) | `1.0` | Interval multiplier on Normal |
| `hardIntervalMultiplier` | Double (0.25-4.0) | `0.75` | Interval multiplier on Hard (lower = faster) |

### Environmental Effects

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `applyDarknessEffect` | Boolean | `false` | Apply Darkness effect to players in structures |
| `darknessAmplifier` | Integer (0-4) | `0` | Darkness effect amplifier level |
| `playAmbientSounds` | Boolean | `false` | Play ominous cave sounds in structures |
| `ambientSoundInterval` | Integer (40-2400) | `400` | Ticks between ambient sounds |

### Elite Spawns

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable rare elite mob spawns |
| `spawnChance` | Double (0.0-1.0) | `0.02` | Chance for each spawn to be elite |
| `healthMultiplier` | Double (1.0-10.0) | `2.0` | Elite health multiplier |
| `damageMultiplier` | Double (1.0-10.0) | `1.5` | Elite damage multiplier |
| `glowingEffect` | Boolean | `true` | Elite mobs glow through walls |
| `namePrefix` | String | `"Elite"` | Custom name prefix for elite mobs |
| `bonusXP` | Integer (0-500) | `20` | Bonus XP when an elite is killed by a player |

## API

Other mods can interact with Dangerous Structures via `DangerousStructuresAPI`:

```java
import com.otectus.dangerousstructures.api.DangerousStructuresAPI;

// Register a structure as dangerous
DangerousStructuresAPI.registerDangerousStructure(new ResourceLocation("mymod", "my_dungeon"));

// Check if a position is dangerous
boolean dangerous = DangerousStructuresAPI.isPositionDangerous(serverLevel, blockPos);

// Unregister
DangerousStructuresAPI.unregisterDangerousStructure(new ResourceLocation("mymod", "my_dungeon"));
```

## How It Works

The mod uses three complementary mechanisms:

1. **Initial population** — When a structure is first discovered each session, 3-6 mobs are immediately spawned throughout its rooms and corridors. Uses aggressive position-finding (50 scan attempts) with distributed placement across structure pieces so mobs are spread naturally, not clustered.

2. **Spawn event interception** — Hooks into Forge's `MobSpawnEvent.SpawnPlacementCheck` and `MobSpawnEvent.PositionCheck` to force-allow hostile mob spawns inside dangerous structures, bypassing light level and time-of-day checks.

3. **Periodic spawning** — A server tick handler that maintains mob presence inside structures at configurable intervals. Uses structure piece-aware targeting, column-based floor finding, pack spawning, and hazard avoidance for efficient, natural-feeling spawns.

All mod-spawned mobs are equipped with randomized armor and granted Fire Resistance for sunlight immunity. Structure detection uses Minecraft's `StructureManager` with a fast-reject gate (`hasAnyStructureAt`) for efficient cross-chunk lookups.

## Building from Source

```bash
git clone https://github.com/Otectus/Dangerous-Structures.git
cd Dangerous-Structures
./gradlew build
```

The built JAR will be in `build/libs/`.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
