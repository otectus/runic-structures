# Dangerous Structures

**Structures are no longer safe.** This mod transforms every structure in Minecraft into hostile territory where mobs spawn around the clock — day or night, torches or not. Villages, temples, fortresses, strongholds, and any modded structures become permanent danger zones teeming with hostile creatures.

## What Does It Do?

Dangerous Structures overrides vanilla spawn rules inside structures, forcing hostile mobs to appear regardless of light level or time of day. Step inside a desert temple at noon? Expect zombies. Explore an ocean monument? Guardians and drowned are waiting. Venture into a Nether fortress? It's packed with blazes and wither skeletons.

The mod uses two systems working together:
- **Spawn event interception** makes vanilla spawn checks always pass inside structures
- **Periodic spawning** actively creates mobs inside structures even when vanilla wouldn't try

## Key Features

**Smart Spawning**
- Structures are pre-populated with 3-6 mobs the moment you discover them
- Spawns within actual rooms and corridors, not random points in empty space
- Mobs appear in natural-feeling packs matching vanilla group sizes
- Respects biome spawn weights — you'll see the right mobs in the right places
- Avoids lava, water, and positions too close to players
- Scales with game difficulty — fewer mobs on Easy, more on Hard

**Armored and Dangerous**
- Mod-spawned mobs wear randomized armor (leather through diamond, netherite opt-in)
- Configurable armor tier pool and drop chances
- Permanent Fire Resistance protects undead mobs from sunlight

**Full Control**
- Auto-detects all structures (vanilla + modded) with no setup needed
- Whitelist/blacklist structures by ID or tag
- Whitelist/blacklist specific mob types
- Filter by dimension
- 50+ config options for fine-tuning every aspect

**Elite Mobs** *(opt-in)*
- Rare enhanced mobs with boosted health and damage
- Glowing effect visible through walls
- Custom name tags and bonus XP drops

**Atmospheric Effects** *(opt-in)*
- Darkness effect (like the Warden) when inside dangerous structures
- Ominous ambient cave sounds

**Admin Commands**
- `/ds info` — Check if your position is in a dangerous structure
- `/ds list` — See all nearby dangerous structures
- `/ds status` — View mod configuration summary
- `/ds spawns` — View spawn statistics
- `/ds reload` — Force cache refresh
- `/ds debug` — Toggle debug logging

**Server-Side Only**
- Works on dedicated servers without any client mod
- Players just connect and play — no extra downloads needed

## Configuration

Config auto-generates at `world/serverconfig/dangerousstructures-server.toml` on first world load. Everything works out of the box with sensible defaults.

**Quick examples:**

Include villages in the danger zone:
```toml
[general]
    includeVillages = true
```

Only specific structures:
```toml
[general]
    autoDetectStructures = false
[structures]
    structureWhitelist = ["minecraft:fortress", "minecraft:stronghold"]
```

Enable the full experience with elite mobs and darkness:
```toml
[eliteSpawns]
    enabled = true
[environmentalEffects]
    applyDarknessEffect = true
    playAmbientSounds = true
```

## Mod API

Other mods can register structures as dangerous programmatically:

```java
DangerousStructuresAPI.registerDangerousStructure(
    new ResourceLocation("mymod", "my_dungeon"));
```

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17+

## Links

- [Source Code](https://github.com/Otectus/Dangerous-Structures)
- [Issue Tracker](https://github.com/Otectus/Dangerous-Structures/issues)
