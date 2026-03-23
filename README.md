# Dangerous Structures

A Minecraft Forge mod that transforms structures into hostile territory. All structures — vanilla and modded — become permanent mob hotbeds where hostile creatures spawn around the clock, regardless of light level or time of day.

## Features

- **Automatic structure detection** — All registered structures (including modded) are dangerous by default
- **Village exclusion** — Villages are excluded by default, with a toggle to include them
- **Structure whitelist/blacklist** — Fine-grained control over which structures are affected
- **Structure tag support** — Target or exclude entire groups of structures by tag
- **Mob whitelist/blacklist** — Control which hostile mobs can spawn
- **Periodic spawning** — Optional forced spawning during daytime when vanilla wouldn't attempt spawns
- **Dimension filtering** — Choose which dimensions are affected
- **Buffer radius** — Expand structure boundaries for spawn detection
- **Fully server-side** — No client installation required; works on dedicated servers

## Requirements

| | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.2.0+ |
| Java | 17+ |

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) for 1.20.1
2. Download the latest `dangerousstructures-x.x.x.jar` from [Releases](../../releases)
3. Place the JAR in your `mods/` folder
4. Launch the game — config generates on first server start

## Configuration

The config file is generated at `world/serverconfig/dangerousstructures-server.toml` on first world load.

### General

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Master enable/disable for the mod |
| `autoDetectStructures` | Boolean | `true` | Automatically treat ALL registered structures as dangerous. When enabled, whitelist options are ignored. Modded structures are included automatically. |
| `includeVillages` | Boolean | `false` | Include village structures when auto-detect is enabled. Has no effect when auto-detect is disabled. |

### Structure Filtering

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `structureWhitelist` | List | `["minecraft:fortress", "minecraft:stronghold", "minecraft:monument"]` | Structure IDs to make dangerous. Only used when `autoDetectStructures` is disabled. |
| `structureTagWhitelist` | List | `[]` | Structure tags to include. Only used when `autoDetectStructures` is disabled. |
| `structureBlacklist` | List | `[]` | Structure IDs to NEVER make dangerous. Applied in all modes. |
| `structureTagBlacklist` | List | `[]` | Structure tags to NEVER make dangerous. Applied in all modes. |

### Mob Filtering

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `mobWhitelist` | List | `[]` | If non-empty, ONLY these mob IDs will be force-spawned. Takes priority over blacklist. |
| `mobBlacklist` | List | `[]` | Mob IDs to exclude from forced spawning. |

### Other Settings

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `allowedDimensions` | List | `["minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"]` | Dimensions where the mod is active. |
| `bufferRadius` | Integer (0-16) | `0` | Extra blocks around structure bounding boxes to consider "inside". 0 = strict bounding box. |
| `debugLogging` | Boolean | `false` | Log each spawn override to the server console. |

### Periodic Spawning

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `periodicSpawning.enabled` | Boolean | `false` | Enable periodic forced spawning inside dangerous structures during daytime. |
| `periodicSpawning.spawnInterval` | Integer (20-6000) | `200` | Ticks between periodic spawn attempts. 20 ticks = 1 second. |
| `periodicSpawning.spawnCapPerStructure` | Integer (1-64) | `8` | Maximum hostile mobs the periodic spawner will maintain per structure. |

## Usage Examples

### Default (auto-detect all structures, exclude villages)

No configuration needed. All structures except villages are dangerous out of the box.

### Include villages too

```toml
[general]
    includeVillages = true
```

### Manual mode with specific structures

```toml
[general]
    autoDetectStructures = false

[structures]
    structureWhitelist = ["minecraft:fortress", "minecraft:stronghold", "minecraft:monument", "minecraft:ancient_city"]
    structureTagWhitelist = ["minecraft:village"]
```

### Auto-detect but exclude specific structures

```toml
[general]
    autoDetectStructures = true

[structures]
    structureBlacklist = ["minecraft:igloo", "minecraft:trail_ruins"]
    structureTagBlacklist = ["minecraft:ocean_ruin"]
```

### Only spawn zombies and skeletons

```toml
[mobs]
    mobWhitelist = ["minecraft:zombie", "minecraft:skeleton"]
```

### Enable daytime spawning

```toml
[periodicSpawning]
    enabled = true
    spawnInterval = 100
    spawnCapPerStructure = 12
```

## Building from Source

```bash
git clone https://github.com/Otectus/Dangerous-Structures.git
cd Dangerous-Structures
./gradlew build
```

The built JAR will be in `build/libs/`.

## How It Works

The mod uses two complementary mechanisms:

1. **Spawn event interception** — Hooks into Forge's `MobSpawnEvent.SpawnPlacementCheck` to force-allow hostile mob spawns inside dangerous structures, bypassing light level and time-of-day checks.

2. **Periodic spawning** (optional) — A server tick handler that independently spawns hostile mobs inside structures at configurable intervals, enabling daytime spawns when vanilla wouldn't attempt them. Mobs are properly initialized with equipment and difficulty-scaled stats.

Structure detection uses Minecraft's built-in `StructureManager` for efficient, cross-chunk lookups that work correctly with large multi-chunk structures like fortresses and strongholds.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
