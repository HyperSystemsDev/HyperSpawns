# HyperSpawns

A comprehensive Hytale server mod for controlling mob spawning through named zones with customizable boundaries, spawn control modes, and filtering options.

## Features

- **Named Spawn Zones** - Create and manage named zones with unique identifiers
- **Multiple Boundary Types** - Support for cuboid, sphere, and cylinder boundaries
- **Spawn Control Modes**:
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

## How It Works

HyperSpawns integrates directly with Hytale's native spawn suppression system through the `ChunkSuppressionIntegrator`. When you create a zone with BLOCK or DENY mode, the plugin:

1. **Calculates affected chunks** - Determines which chunks intersect with the zone boundary
2. **Creates suppression entries** - Adds `ChunkSuppressionEntry` components to each affected chunk
3. **Registers suppressed roles** - For BLOCK mode, all NPC roles are suppressed; for DENY mode, only filtered roles are suppressed
4. **Updates loaded chunks** - Queues updates for already-loaded chunks so changes take effect immediately

This approach is highly efficient because:
- Spawn checks are handled by Hytale's native spawning system (no event listeners)
- Chunk-based indexing means O(1) lookups regardless of zone count
- Suppression data is maintained per-chunk, not per-spawn-attempt

> **Important:** Zones only affect the world they're created in. The zone's world name must exactly match the server's world name for spawns to be blocked.

## Installation

1. Build the plugin:
   ```bash
   ./gradlew shadowJar
   ```

2. Copy the built JAR to your Hytale mods folder:
   ```bash
   cp build/libs/HyperSpawns-1.0.0.jar ~/Documents/Hytale/mods/
   ```

3. Start your Hytale server

4. **Verify installation:**
   ```
   /hyperspawns stats
   ```
   You should see version information and zone statistics.

## Quick Start

### Block all spawns near spawn point

```
# Set the first corner
/hyperspawns pos1 -100 0 -100

# Set the second corner
/hyperspawns pos2 100 256 100

# Create the zone
/hyperspawns zone create spawn_protection cuboid

# Set mode to BLOCK
/hyperspawns zone mode spawn_protection BLOCK
```

### Create a sphere zone

```
# Option 1: Using pos1 as center
/hyperspawns pos1 0 64 0
/hyperspawns sphere safe_zone 50

# Option 2: Specify coordinates directly
/hyperspawns sphere safe_zone 50 0 64 0
```

## Commands

### Main Command
`/hyperspawns` (aliases: `/hspawn`, `/spawns`, `/hs`)

### Zone Management

| Command | Description |
|---------|-------------|
| `/hyperspawns zone create <name> [cuboid\|sphere\|cylinder]` | Create a new zone from selection |
| `/hyperspawns zone delete <name>` | Delete a zone |
| `/hyperspawns zone list [page]` | List all zones |
| `/hyperspawns zone info <name>` | Show detailed zone information |
| `/hyperspawns zone redefine <name>` | Update zone boundary from selection |
| `/hyperspawns zone mode <name> <mode>` | Set zone spawn control mode |
| `/hyperspawns zone multiplier <name> <0.0-10.0>` | Set spawn rate multiplier (MODIFY mode) |
| `/hyperspawns zone priority <name> <number>` | Set zone priority |
| `/hyperspawns zone enable <name>` | Enable a zone |
| `/hyperspawns zone disable <name>` | Disable a zone |

### Global Controls

| Command | Description |
|---------|-------------|
| `/hyperspawns global` | Show global spawn settings |
| `/hyperspawns global multiplier <0.0-10.0>` | Set global spawn rate multiplier |
| `/hyperspawns global pause` | Pause all mob spawning server-wide |
| `/hyperspawns global resume` | Resume mob spawning |

### Selection & Utility

| Command | Description |
|---------|-------------|
| `/hyperspawns wand` | Get selection wand information |
| `/hyperspawns wandmode` | Toggle wand selection mode (click to select) |
| `/hyperspawns pos1 <x> <y> <z> [world]` | Set selection position 1 |
| `/hyperspawns pos2 <x> <y> <z> [world]` | Set selection position 2 |
| `/hyperspawns sphere <name> <radius> [x y z] [world]` | Create sphere zone directly |
| `/hyperspawns reload` | Reload configuration and zones |
| `/hyperspawns stats` | View spawn zone statistics |
| `/hyperspawns debug [on\|off]` | Toggle debug mode |

## Permissions

| Permission | Description |
|------------|-------------|
| `hyperspawns.admin` | Full access (wildcard) |
| `hyperspawns.zone.create` | Create zones |
| `hyperspawns.zone.delete` | Delete zones |
| `hyperspawns.zone.modify` | Modify zone settings |
| `hyperspawns.zone.list` | List zones |
| `hyperspawns.global` | Global spawn controls |
| `hyperspawns.wand` | Use selection wand |
| `hyperspawns.reload` | Reload configuration |
| `hyperspawns.stats` | View spawn statistics |
| `hyperspawns.debug` | Toggle debug mode |
| `hyperspawns.bypass` | Bypass all zone restrictions |

## Configuration

Configuration is stored in `config.json`:

```json
{
  "debugMode": false,
  "autoSaveIntervalMinutes": 5,
  "defaultZoneMode": "block",
  "bypassPermission": "hyperspawns.bypass",
  "wandItemId": "hytale:stick",
  "particlesEnabled": true,
  "particleViewDistance": 32,
  "globalSpawnMultiplier": 1.0,
  "globalSpawnPaused": false
}
```

See [docs/CONFIGURATION.md](docs/CONFIGURATION.md) for detailed configuration reference.

## Troubleshooting

### Zone not blocking spawns?

1. **Check the world name** - Use `/hyperspawns zone info <name>` and verify the world name matches your server's world exactly
2. **Verify the mode** - Ensure the zone mode is set to `BLOCK` or `DENY` (not `ALLOW`, `MODIFY`, or `REPLACE`)
3. **Check if enabled** - Make sure the zone is enabled with `/hyperspawns zone enable <name>`
4. **Enable debug mode** - Run `/hyperspawns debug on` and check console logs for suppression details
5. **Reload the plugin** - Run `/hyperspawns reload` to re-apply all zone suppressions

### Mobs still appearing in zone?

- Existing mobs are not removed when a zone is created; only new spawns are blocked
- Some mobs may wander into the zone from outside
- Check chunk boundaries - zones affect entire chunks, but the boundary check is per-block

See [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for more detailed troubleshooting.

## Documentation

- [Admin Guide](docs/ADMIN-GUIDE.md) - Complete server administrator guide
- [Commands Reference](docs/COMMANDS.md) - Detailed command documentation
- [Configuration](docs/CONFIGURATION.md) - All configuration options
- [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues and solutions
- [Mob Spawning](docs/MOB-SPAWNING.md) - How Hytale mob spawning works
- [Architecture](docs/ARCHITECTURE.md) - Technical documentation for developers

## Building from Source

Requirements:
- Java 25 (Temurin recommended)
- Gradle 9.3+

```bash
# Build
./gradlew shadowJar

# Clean build
./gradlew clean shadowJar

# Development build
./gradlew buildDev
```

## Support

- Discord: https://discord.gg/SNPjyfkYPc
- GitHub: https://github.com/HyperSystemsDev/HyperSpawns

## License

Copyright (c) 2025 HyperSystems. All rights reserved.
