# HyperSpawns

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?logo=discord&logoColor=white)](https://discord.gg/SNPjyfkYPc)
[![GitHub](https://img.shields.io/github/stars/HyperSystemsDev/HyperSpawns?style=social)](https://github.com/HyperSystemsDev/HyperSpawns)

A comprehensive Hytale server mod for controlling mob spawning through named zones with customizable boundaries, spawn control modes, and filtering options. Part of the **HyperSystems** plugin suite.

**Version:** 1.0.0
**Game:** Hytale Early Access
**License:** GPLv3

---

## Overview

HyperSpawns integrates directly with Hytale's native spawn suppression system to control mob spawning through named zones. Create zones with customizable boundaries (cuboid, sphere, cylinder), set spawn control modes (block, allow, deny, modify, replace), and use advanced filtering by NPC type, light level, Y level, and more.

---

## Key Features

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

---

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

4. Verify installation:
   ```
   /hyperspawns stats
   ```

---

## Quick Start

### Block all spawns near spawn point

```
/hyperspawns pos1 -100 0 -100
/hyperspawns pos2 100 256 100
/hyperspawns zone create spawn_protection cuboid
/hyperspawns zone mode spawn_protection BLOCK
```

### Create a sphere zone

```
/hyperspawns pos1 0 64 0
/hyperspawns sphere safe_zone 50
```

---

## Commands

Main command: `/hyperspawns` (aliases: `/hspawn`, `/spawns`, `/hs`)

### Zone Management

| Command | Description | Permission |
|---------|-------------|------------|
| `/hyperspawns zone create <name> [type]` | Create a new zone from selection | `hyperspawns.zone.create` |
| `/hyperspawns zone delete <name>` | Delete a zone | `hyperspawns.zone.delete` |
| `/hyperspawns zone list [page]` | List all zones | `hyperspawns.zone.list` |
| `/hyperspawns zone info <name>` | Show detailed zone information | `hyperspawns.zone.list` |
| `/hyperspawns zone redefine <name>` | Update zone boundary from selection | `hyperspawns.zone.modify` |
| `/hyperspawns zone mode <name> <mode>` | Set zone spawn control mode | `hyperspawns.zone.modify` |
| `/hyperspawns zone multiplier <name> <0.0-10.0>` | Set spawn rate multiplier | `hyperspawns.zone.modify` |
| `/hyperspawns zone priority <name> <number>` | Set zone priority | `hyperspawns.zone.modify` |
| `/hyperspawns zone enable <name>` | Enable a zone | `hyperspawns.zone.modify` |
| `/hyperspawns zone disable <name>` | Disable a zone | `hyperspawns.zone.modify` |

### Global Controls

| Command | Description | Permission |
|---------|-------------|------------|
| `/hyperspawns global` | Show global spawn settings | `hyperspawns.global` |
| `/hyperspawns global multiplier <0.0-10.0>` | Set global spawn rate multiplier | `hyperspawns.global` |
| `/hyperspawns global pause` | Pause all mob spawning server-wide | `hyperspawns.global` |
| `/hyperspawns global resume` | Resume mob spawning | `hyperspawns.global` |

### Selection & Utility

| Command | Description | Permission |
|---------|-------------|------------|
| `/hyperspawns wand` | Get selection wand information | `hyperspawns.wand` |
| `/hyperspawns wandmode` | Toggle wand selection mode | `hyperspawns.wand` |
| `/hyperspawns pos1 <x> <y> <z> [world]` | Set selection position 1 | `hyperspawns.wand` |
| `/hyperspawns pos2 <x> <y> <z> [world]` | Set selection position 2 | `hyperspawns.wand` |
| `/hyperspawns sphere <name> <radius> [x y z]` | Create sphere zone directly | `hyperspawns.zone.create` |
| `/hyperspawns reload` | Reload configuration and zones | `hyperspawns.reload` |
| `/hyperspawns stats` | View spawn zone statistics | `hyperspawns.stats` |
| `/hyperspawns debug [on\|off]` | Toggle debug mode | `hyperspawns.debug` |

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperspawns.admin` | Full access (wildcard) | op |
| `hyperspawns.zone.create` | Create zones | op |
| `hyperspawns.zone.delete` | Delete zones | op |
| `hyperspawns.zone.modify` | Modify zone settings | op |
| `hyperspawns.zone.list` | List zones | op |
| `hyperspawns.global` | Global spawn controls | op |
| `hyperspawns.wand` | Use selection wand | op |
| `hyperspawns.reload` | Reload configuration | op |
| `hyperspawns.stats` | View spawn statistics | op |
| `hyperspawns.debug` | Toggle debug mode | op |
| `hyperspawns.bypass` | Bypass all zone restrictions | op |

---

## Configuration

Configuration file: `mods/com.hyperspawns_HyperSpawns/config.json`

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

---

## How It Works

HyperSpawns integrates directly with Hytale's native spawn suppression system through the `ChunkSuppressionIntegrator`. When you create a zone with BLOCK or DENY mode:

1. **Calculates affected chunks** - Determines which chunks intersect with the zone boundary
2. **Creates suppression entries** - Adds `ChunkSuppressionEntry` components to each affected chunk
3. **Registers suppressed roles** - For BLOCK mode, all NPC roles are suppressed; for DENY mode, only filtered roles
4. **Updates loaded chunks** - Queues updates for already-loaded chunks so changes take effect immediately

This approach is highly efficient because spawn checks are handled by Hytale's native spawning system.

---

## Documentation

| Document | Description |
|----------|-------------|
| [Admin Guide](docs/ADMIN-GUIDE.md) | Complete server administrator guide |
| [Commands Reference](docs/COMMANDS.md) | Detailed command documentation |
| [Configuration](docs/CONFIGURATION.md) | All configuration options |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common issues and solutions |
| [Mob Spawning](docs/MOB-SPAWNING.md) | How Hytale mob spawning works |
| [Architecture](docs/ARCHITECTURE.md) | Technical documentation for developers |

---

## Building from Source

### Requirements

- Java 21+ (for building)
- Java 25 (for running on Hytale server)
- Gradle 8.12+
- Hytale Server (Early Access)

```bash
./gradlew shadowJar
```

The output JAR will be in `build/libs/`.

---

## Support

- **Discord:** https://discord.gg/SNPjyfkYPc
- **GitHub Issues:** https://github.com/HyperSystemsDev/HyperSpawns/issues

---

## Credits

Developed by **HyperSystemsDev**

Part of the **HyperSystems** plugin suite:
- [HyperPerms](https://github.com/HyperSystemsDev/HyperPerms) - Advanced permissions
- [HyperHomes](https://github.com/HyperSystemsDev/HyperHomes) - Home teleportation
- [HyperFactions](https://github.com/HyperSystemsDev/HyperFactions) - Faction management
- [HyperWarp](https://github.com/HyperSystemsDev/HyperWarp) - Warps, spawns, TPA

---

*HyperSpawns - Control Every Creature*
