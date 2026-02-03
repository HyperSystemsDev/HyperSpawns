# Changelog

All notable changes to HyperSpawns will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - TBD

### Added

- **Named Spawn Zones** - Create and manage named zones with unique identifiers
- **Multiple Boundary Types** - Support for cuboid, sphere, and cylinder boundaries
- **Spawn Control Modes**
  - `BLOCK` - Completely block all mob spawning in the zone
  - `ALLOW` - Only allow mobs matching the filter to spawn (whitelist)
  - `DENY` - Prevent mobs matching the filter from spawning (blacklist)
  - `MODIFY` - Adjust spawn rates with a multiplier
  - `REPLACE` - Replace spawning mobs with a different NPC type
- **Advanced Filtering** - Filter by NPC groups, roles, light level, Y level, time of day, and moon phase
- **Priority System** - Overlapping zones resolved by priority
- **Efficient Spatial Indexing** - O(1) average lookup using chunk-based indexing
- **Auto-save** - Automatic periodic saving of zone data
- **Global Controls** - Server-wide spawn rate multiplier and pause functionality
- **Native Integration** - Direct integration with Hytale's spawn suppression system via `ChunkSuppressionIntegrator`

### Commands

**Zone Management:**
- `/hyperspawns zone create <name> [type]` - Create a new zone from selection
- `/hyperspawns zone delete <name>` - Delete a zone
- `/hyperspawns zone list [page]` - List all zones
- `/hyperspawns zone info <name>` - Show detailed zone information
- `/hyperspawns zone redefine <name>` - Update zone boundary from selection
- `/hyperspawns zone mode <name> <mode>` - Set zone spawn control mode
- `/hyperspawns zone multiplier <name> <0.0-10.0>` - Set spawn rate multiplier
- `/hyperspawns zone priority <name> <number>` - Set zone priority
- `/hyperspawns zone enable/disable <name>` - Toggle zone

**Global Controls:**
- `/hyperspawns global` - Show global spawn settings
- `/hyperspawns global multiplier <0.0-10.0>` - Set global spawn rate multiplier
- `/hyperspawns global pause/resume` - Pause/resume all mob spawning

**Selection & Utility:**
- `/hyperspawns wand` - Get selection wand information
- `/hyperspawns wandmode` - Toggle wand selection mode
- `/hyperspawns pos1/pos2 <x> <y> <z> [world]` - Set selection positions
- `/hyperspawns sphere <name> <radius> [x y z]` - Create sphere zone directly
- `/hyperspawns reload` - Reload configuration and zones
- `/hyperspawns stats` - View spawn zone statistics
- `/hyperspawns debug [on|off]` - Toggle debug mode
