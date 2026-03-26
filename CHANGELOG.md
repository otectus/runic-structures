# Changelog

## [1.1.0] - 2026-03-25

### Bug Fixes
- Fixed hostile mobs spawning on Peaceful difficulty — both event-based and periodic spawning now correctly skip Peaceful
- Fixed mobs spawning on structure rooftops instead of inside — added sky exposure filter (`canSeeSky`) to exclude open-air positions
- Fixed armor and Fire Resistance not applying to periodic spawns — enhancements are now applied directly by the spawner rather than relying solely on event handler detection
- Fixed `minPlayerDistance` default of 16 blocks making interior spawns impossible in small structures — reduced to 6 blocks
- Fixed chunk force-loading in periodic spawner — no longer loads unloaded chunks when scanning for structures
- Fixed potential spawn duplication when other mods cancel `FinalizeSpawn` events — canceled spawns are now properly discarded
- Fixed structure caches not invalidating on `/reload` — datapack reloads now correctly clear all caches
- Fixed license mismatch between gradle.properties and mods.toml

### Spawning Improvements
- **Structure piece-aware spawning** — Mobs now spawn within individual structure pieces (rooms, corridors) instead of the full bounding box, dramatically improving spawn accuracy for large structures like Nether fortresses and strongholds
- **Column-based floor finding** — Replaced random Y coordinate picking with downward column scanning, greatly increasing the chance of finding valid spawn positions
- **Pack spawning** — Mobs now spawn in groups of 1-4 matching vanilla biome pack sizes, creating more natural encounters
- **Vanilla spawn weights** — Biome mob selection now respects vanilla spawn weight probabilities instead of uniform random
- **Hazard avoidance** — Spawn positions in or above lava and in water are automatically skipped
- **Minimum player distance** — Mobs won't spawn within 6 blocks of any player (configurable), preventing frustrating instant-spawn deaths
- **Spawn interval jitter** — Randomized +/-25% timing prevents synchronized spawn waves across structures
- **Multi-floor distribution** — Valid floors are collected per column and selected randomly, spreading spawns across multiple levels in multi-story structures
- **Early-exit entity counting** — Monster cap checks now exit as soon as the cap is reached instead of counting every entity
- **Fast-reject gate** — Structure detection now uses `hasAnyStructureAt()` as a cheap pre-check before per-structure iteration

### New Features
- **Initial population** — 3-6 mobs spawn throughout a structure the first time it's discovered each session, guaranteeing structures are populated on arrival (configurable min/max count)
- **Armor system** — Mod-spawned mobs are equipped with randomized armor from a configurable tier pool (leather through netherite) with configurable drop chance
- **Sunlight immunity** — Mod-spawned mobs receive permanent Fire Resistance, preventing undead from burning outside structures
- **Admin commands** — `/ds status`, `/ds info`, `/ds list`, `/ds reload`, `/ds debug [on/off]`, `/ds spawns` (requires OP level 2)
- **Difficulty scaling** — Spawn cap and interval automatically scale with game difficulty (configurable multipliers for Easy/Normal/Hard)
- **Elite mob spawns** — Rare enhanced mobs with boosted health, damage, glowing effect, custom name, and bonus XP on kill (opt-in, disabled by default)
- **Environmental effects** — Darkness effect and ambient cave sounds for players inside dangerous structures (opt-in, disabled by default)
- **Mob persistence** — Optional flag to prevent periodic-spawned mobs from despawning when players leave
- **Night spawn boost** — Configurable spawn rate multiplier that increases frequency at night
- **Structure size-based cap scaling** — Opt-in larger mob caps for larger structures based on footprint area
- **Spawn statistics** — Track spawn attempts, successes, cap rejections, and position failures via `/ds spawns`
- **Config validation** — Unknown structure IDs, mob IDs, and contradictory config options now produce warnings on server start

### API
- **Public mod API** — `DangerousStructuresAPI` class for other mods to register/unregister dangerous structures and query positions

### Configuration
- Added 25+ new config options across 4 new sections: Periodic Spawning (expanded), Difficulty Scaling, Environmental Effects, and Elite Spawns
- Configurable chunk search radius (was hardcoded to 4)
- Configurable max position attempts (was hardcoded to 5, now defaults to 10)
- Shared structure cache between StructureDetection and TickSpawnHandler

### Internal
- Centralized dangerous structure cache in `StructureDetection.getDangerousStructures()`
- Improved code organization with new `command`, `api`, and effect handler packages

## [1.0.0] - 2025-12-01

### Initial Release
- Automatic structure detection with village exclusion toggle
- Structure whitelist/blacklist by ID and tag
- Mob whitelist/blacklist with biome-specific option
- Periodic spawning for daytime surface spawns
- Dimension filtering
- Buffer radius for structure detection
- Server-side only — no client mod required
- Hot-reloadable server config
