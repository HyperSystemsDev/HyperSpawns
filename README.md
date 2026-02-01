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

## Commands

### Main Command
`/hyperspawns` (aliases: `/hspawn`, `/spawns`, `/hs`)

### Zone Management

| Command | Description |
|---------|-------------|
| `/hyperspawns zone create <name> [cuboid\|sphere\|cylinder]` | Create a new zone from wand selection |
| `/hyperspawns zone delete <name>` | Delete a zone |
| `/hyperspawns zone list [page]` | List all zones |
| `/hyperspawns zone info <name>` | Show detailed zone information |
| `/hyperspawns zone redefine <name>` | Update zone boundary from wand selection |
| `/hyperspawns zone mode <name> <mode>` | Set zone spawn control mode |
| `/hyperspawns zone multiplier <name> <0.0-10.0>` | Set spawn rate multiplier (MODIFY mode) |
| `/hyperspawns zone priority <name> <number>` | Set zone priority |
| `/hyperspawns zone enable <name>` | Enable a zone |
| `/hyperspawns zone disable <name>` | Disable a zone |

### Zone Filtering

| Command | Description |
|---------|-------------|
| `/hyperspawns zone filter <name> add <type> <value>` | Add filter criteria |
| `/hyperspawns zone filter <name> remove <type> <value>` | Remove filter criteria |
| `/hyperspawns zone filter <name> clear` | Clear all filter criteria |

**Filter Types:**
- `group` - NPCGroup ID
- `role` - Specific NPC role
- `time` - Time of day (day, night, dawn, dusk)
- `moon` - Moon phase (0-7)
- `minlight` / `maxlight` - Light level range (0-15)
- `miny` / `maxy` - Y level range

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
| `/hyperspawns wand` | Get information about the selection wand |
| `/hyperspawns pos1` | Set selection position 1 at current location |
| `/hyperspawns pos2` | Set selection position 2 at current location |
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

## Zone Data

Zones are stored in `zones.json`:

```json
{
  "schemaVersion": 1,
  "lastSaved": 1704067200000,
  "zones": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "spawn_protection",
      "world": "world",
      "boundary": {
        "type": "cuboid",
        "minX": -100, "minY": 0, "minZ": -100,
        "maxX": 100, "maxY": 319, "maxZ": 100
      },
      "mode": "block",
      "filter": {},
      "priority": 10,
      "enabled": true,
      "spawnRateMultiplier": 1.0
    }
  ]
}
```

## Usage Examples

### Block all spawns near spawn point
```
/hyperspawns pos1
# Walk to opposite corner
/hyperspawns pos2
/hyperspawns zone create spawn_protection cuboid
/hyperspawns zone mode spawn_protection BLOCK
```

### Create a hostile-only dungeon zone
```
/hyperspawns zone create dungeon cuboid
/hyperspawns zone mode dungeon ALLOW
/hyperspawns zone filter dungeon add group hostile
/hyperspawns zone multiplier dungeon 2.0
```

### Prevent zombie spawns in a village
```
/hyperspawns zone create village sphere
/hyperspawns zone mode village DENY
/hyperspawns zone filter village add role zombie
```

### Create a night-only spawn boost zone
```
/hyperspawns zone create night_zone cuboid
/hyperspawns zone mode night_zone MODIFY
/hyperspawns zone filter night_zone add time night
/hyperspawns zone multiplier night_zone 1.5
```

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

Copyright © 2024 HyperSystems. All rights reserved.
