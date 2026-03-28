# Changelog

## [0.9.0] - 2026-03-28

### Initial Public Release

Runic Structures is a Minecraft Forge mod (1.20.1) that transforms every generated structure into permanently hostile territory. Built for the Runecraft modpack.

### Spawning System
- Automatic structure detection for all registered structures (vanilla + modded) with village exclusion
- Initial population: 2-5 mobs spawn throughout a structure on first discovery each session
- Periodic reinforcement spawning at configurable intervals to maintain mob presence
- Structure piece-aware targeting: mobs spawn inside rooms and corridors, not random bounding box points
- Column-based floor finding with multi-floor distribution across structure levels
- Pack spawning with vanilla biome spawn weights for natural-feeling encounters
- Hazard avoidance: skips lava, water, and rooftop positions
- Difficulty scaling: spawn cap and interval scale with Easy/Normal/Hard
- Night spawn boost, min player distance, and spawn interval jitter
- Dimension filtering with support for modded dimensions (Mining Dimension, Dungeon Realm)

### Per-Structure Profiles
- **238 individualized structure profiles** across 27 mods for the Runecraft modpack
- Per-structure JSON overrides (`runicstructures-structures.json`) for spawn cap, elite chance, elite name prefix, armor tiers, weapon pool, shield pool, enchantment level, environmental effects (darkness, mining fatigue, slowness, ambient sounds), mob persistence, and progression gating
- Calibrated sizing from 1-mob tiny ruins to 16-mob colossal fortresses based on actual structure footprints
- 16 thematic elite name prefixes: Ancient, Arcane, Commander, Corrupted, Cursed, Deep, Dragonsworn, Dread, Fallen, Feral, Hexed, Illager, Lord, Sculk-Infested, Warchief, Wither
- Progression gating by server day count (early/mid/late/veteran tiers)
- Mods covered: Vanilla, YUNG's Better Series, When Dungeons Arise (+Seven Seas), Dungeons and Taverns, Hopo Better Mineshaft, Moog's Mods (Missing Villages, Nether Structures, Temples Reimagined), Underground Rooms, Medieval Buildings (Overworld + Nether), Medieval End, Battle Towers, Jaden's Nether Expansion, Ice and Fire, Iron's Spellbooks, Ars Nouveau, Galosphere, Mowzie's Mobs, Goblin's Tyranny, The Orcs, and more

### Mob Enhancement
- Randomized armor from configurable tier pool (leather through netherite) with per-tier drop chance multipliers
- Config-driven weapon pool supporting any modded weapon (Spartan Weaponry, Cataclysm Tools, etc.)
- Custom armor set definitions for modded armor (`name=helmet,chest,legs,boots` format)
- Shield/offhand support (opt-in) with configurable chance and item pool
- Equipment enchantments (opt-in): Protection/Blast/Projectile/Thorns on armor, Sharpness/Knockback/Fire Aspect on weapons
- Permanent Fire Resistance prevents sunlight burning and lava/fire cheese
- Anti-stacking guards: respects existing armor/weapons from InControl and Majrusz's Difficulty

### Elite Mobs (opt-in)
- Configurable spawn chance with boosted health and damage multipliers
- Glowing effect (visible through walls), custom name prefix, and bonus XP on kill
- Per-structure elite chance, name prefix, and stat overrides via JSON profiles
- Health threshold (`maxBaseHealth`) skips elite buffs on mobs already buffed by other systems

### Environmental Effects (opt-in, per-structure or global)
- Darkness: pulsing Warden-style vision effect inside structures
- Mining Fatigue: slows block-breaking to discourage cheese strategies
- Slowness: subtle movement penalty that increases danger
- Ambient sounds: weighted pool of ominous cave/nether ambience at configurable intervals

### Admin Commands
- `/rs status` -- mod status and config summary
- `/rs info` -- check if standing inside a runic structure
- `/rs list` -- nearby structures with runic/excluded status and coordinates
- `/rs spawns` / `/rs spawns detail` -- spawn statistics, per-structure breakdown
- `/rs reload` -- reload structure profiles and invalidate caches
- `/rs validate` -- re-run config validation and report issues
- `/rs debug on|off` -- toggle per-spawn debug logging (survives config reloads)

### Configuration
- 60+ server config options across 10 sections in `runicstructures-server.toml`
- Structure whitelist/blacklist by ID or tag, mob whitelist/blacklist
- 45+ boss mobs blacklisted by default (Cataclysm, BOMD, Mowzie's, Ice & Fire, Stalwart Dungeons, Saints Dragons, Iron's Spellbooks, Ars Nouveau, RealmRPG, Galosphere, Dark Doppelganger, Majrusz's Difficulty)
- Boss arena structures blacklisted (Cataclysm, Stalwart Dungeons, BOMD) to preserve scripted encounters
- Difficulty scaling tuned conservatively to complement InControl and Majrusz's Difficulty
- Structure size-based cap scaling for proportional mob density

### API
- `RunicStructuresAPI.registerRunicStructure()` / `unregisterRunicStructure()` for other mods
- `RunicStructuresAPI.isPositionRunic()` for querying structure status at any position

### Compatibility
- Built for Runecraft modpack (Forge 1.20.1, 427 mods)
- Complementary with InControl (biome/weather/phase spawning) and Majrusz's Difficulty (scaling)
- Server-side only -- no client mod required
- Localized command output with translation keys (`en_us.json`)
