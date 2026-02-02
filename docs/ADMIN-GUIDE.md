# HyperSpawns Admin Guide

A complete guide for server administrators on installing, configuring, and using HyperSpawns to control mob spawning on your Hytale server.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Creating Your First Zone](#creating-your-first-zone)
3. [Understanding Zone Modes](#understanding-zone-modes)
4. [Filter System](#filter-system)
5. [Boundary Types](#boundary-types)
6. [Best Practices](#best-practices)
7. [Performance Considerations](#performance-considerations)

---

## Getting Started

### Installation

1. **Build or download** the HyperSpawns JAR file
2. **Copy** to your Hytale server's mods folder:
   ```bash
   cp HyperSpawns-1.0.0.jar ~/Documents/Hytale/mods/
   ```
3. **Start** your server
4. **Verify** installation by running:
   ```
   /hyperspawns stats
   ```

### First-Time Setup

On first run, HyperSpawns creates these files in your data folder:

- `config.json` - Plugin configuration
- `zones.json` - Zone data (created after first zone)

### Permissions Setup

Grant yourself admin access:
```
hyperspawns.admin
```

Or assign specific permissions as needed (see [Commands Reference](COMMANDS.md)).

---

## Creating Your First Zone

Let's create a spawn protection zone that blocks all mob spawning around your server spawn.

### Step 1: Define the Area

First, decide on your zone boundaries. For this example, we'll create a 200x200 block area centered on spawn.

```
/hyperspawns pos1 -100 0 -100
/hyperspawns pos2 100 256 100
```

> **Tip:** The Y coordinates define the vertical range. Using 0-256 covers the entire world height.

### Step 2: Create the Zone

```
/hyperspawns zone create spawn_protection cuboid
```

### Step 3: Set the Mode

By default, zones use the mode specified in config (default: BLOCK). Verify or change:

```
/hyperspawns zone mode spawn_protection BLOCK
```

### Step 4: Verify

```
/hyperspawns zone info spawn_protection
```

You should see output showing the zone details including world, boundary, mode, and enabled status.

### Common Mistake: World Name Mismatch

If your zone isn't working, the most common cause is a **world name mismatch**. The zone stores the world name when created. Check with:

```
/hyperspawns zone info spawn_protection
```

If the "World" field doesn't match your actual world name, the zone won't affect that world.

---

## Understanding Zone Modes

HyperSpawns provides five spawn control modes. Each serves a different purpose:

### BLOCK Mode

**Purpose:** Completely prevent all mob spawning in the zone.

```
/hyperspawns zone mode <zone> BLOCK
```

- Blocks ALL mob spawns regardless of filter settings
- Most efficient mode - no per-mob checks needed
- Use for: Safe zones, towns, arenas, spawn areas

**Example:**
```
/hyperspawns zone create town_center cuboid
/hyperspawns zone mode town_center BLOCK
```

### ALLOW Mode (Whitelist)

**Purpose:** Only allow specific mobs to spawn; block everything else.

```
/hyperspawns zone mode <zone> ALLOW
```

- Only mobs matching the filter can spawn
- All other mobs are blocked
- Requires filter configuration to be useful
- Use for: Theme areas (only certain mob types), controlled dungeons

**Example - Only allow friendly animals:**
```
/hyperspawns zone create peaceful_forest cuboid
/hyperspawns zone mode peaceful_forest ALLOW
/hyperspawns zone filter peaceful_forest add group passive
```

### DENY Mode (Blacklist)

**Purpose:** Block specific mobs from spawning; allow everything else.

```
/hyperspawns zone mode <zone> DENY
```

- Mobs matching the filter cannot spawn
- All other mobs spawn normally
- Requires filter configuration
- Use for: Blocking specific threats, customizing spawn tables

**Example - No zombies in village:**
```
/hyperspawns zone create village sphere
/hyperspawns zone mode village DENY
/hyperspawns zone filter village add role zombie
```

### MODIFY Mode

**Purpose:** Adjust spawn rates without blocking.

```
/hyperspawns zone mode <zone> MODIFY
/hyperspawns zone multiplier <zone> 2.0
```

- Multiplies spawn rate by the specified value
- `0.5` = half spawn rate, `2.0` = double spawn rate
- Can be combined with filters for selective modification
- Use for: Dangerous areas, spawn farms, difficulty zones

**Example - Double hostile spawns in dungeon:**
```
/hyperspawns zone create dungeon cuboid
/hyperspawns zone mode dungeon MODIFY
/hyperspawns zone multiplier dungeon 2.0
/hyperspawns zone filter dungeon add group hostile
```

### REPLACE Mode

**Purpose:** Replace spawning mobs with a different type.

```
/hyperspawns zone mode <zone> REPLACE
```

- Spawning mobs are replaced with the configured replacement
- Original spawn is consumed, replacement appears instead
- Use for: Boss arenas, themed areas, custom mob zones

**Note:** REPLACE mode requires additional configuration for the replacement NPC type.

---

## Filter System

Filters determine which mobs are affected by ALLOW, DENY, MODIFY, and REPLACE modes. BLOCK mode ignores filters entirely.

### Filter Types

| Type | Description | Example Values |
|------|-------------|----------------|
| `group` | NPC group category | `hostile`, `passive`, `neutral` |
| `role` | Specific NPC type | `zombie`, `skeleton`, `spider` |
| `time` | Time of day | `day`, `night`, `dawn`, `dusk` |
| `moon` | Moon phase (0-7) | `0` (full), `4` (new) |
| `minlight` | Minimum light level | `0` to `15` |
| `maxlight` | Maximum light level | `0` to `15` |
| `miny` | Minimum Y level | Any integer |
| `maxy` | Maximum Y level | Any integer |

### Filter Commands

```
# Add a filter
/hyperspawns zone filter <zone> add <type> <value>

# Remove a filter
/hyperspawns zone filter <zone> remove <type> <value>

# Clear all filters
/hyperspawns zone filter <zone> clear
```

### Filter Logic

When multiple filters are set, **ALL** conditions must be met for a mob to match:

```
/hyperspawns zone filter dungeon add group hostile
/hyperspawns zone filter dungeon add time night
/hyperspawns zone filter dungeon add maxlight 7
```

This matches: hostile mobs AND nighttime AND light level 7 or below.

### Filter Examples

**Only block night spawns:**
```
/hyperspawns zone mode dark_zone DENY
/hyperspawns zone filter dark_zone add time night
```

**Allow cave mobs only underground:**
```
/hyperspawns zone mode underground ALLOW
/hyperspawns zone filter underground add maxy 50
/hyperspawns zone filter underground add maxlight 4
```

**Increase spawns during full moon:**
```
/hyperspawns zone mode spooky MODIFY
/hyperspawns zone multiplier spooky 3.0
/hyperspawns zone filter spooky add moon 0
/hyperspawns zone filter spooky add group hostile
```

---

## Boundary Types

HyperSpawns supports three boundary shapes:

### Cuboid (Box)

Best for: Buildings, rectangular regions, standard protected areas

```
/hyperspawns pos1 <x1> <y1> <z1>
/hyperspawns pos2 <x2> <y2> <z2>
/hyperspawns zone create <name> cuboid
```

- Defined by two opposite corners
- Most efficient boundary type
- Axis-aligned (cannot be rotated)

### Sphere

Best for: Circular protection, natural-looking zones

```
# Method 1: Using pos1 as center
/hyperspawns pos1 <centerX> <centerY> <centerZ>
/hyperspawns sphere <name> <radius>

# Method 2: Specifying coordinates
/hyperspawns sphere <name> <radius> <x> <y> <z>

# Method 3: Using two points (center + radius point)
/hyperspawns pos1 <centerX> <centerY> <centerZ>
/hyperspawns pos2 <radiusPointX> <radiusPointY> <radiusPointZ>
/hyperspawns zone create <name> sphere
```

- Defined by center point and radius
- Distance calculated in 3D
- Slightly more expensive containment checks

### Cylinder

Best for: Towers, wells, vertical structures

```
/hyperspawns pos1 <x1> <y1> <z1>
/hyperspawns pos2 <x2> <y2> <z2>
/hyperspawns zone create <name> cylinder
```

- Circular in X/Z plane
- Rectangular in Y axis
- Center and radius calculated from selection
- Height defined by Y range of selection

---

## Best Practices

### Naming Conventions

Use descriptive, consistent names:
- `spawn_protection` - not `zone1`
- `dungeon_east` - not `d1`
- `farm_hostile` - not `temp`

### Priority Management

When zones overlap, higher priority wins:
- Safe zones should have higher priority
- Default priority is 0
- Negative priorities are valid

```
/hyperspawns zone priority spawn_protection 100
/hyperspawns zone priority dungeon_boost 10
```

### Zone Organization

1. **Layer your zones** - Start with large blocking zones, add smaller specific zones inside
2. **Use priorities** - Ensure critical zones always win
3. **Document your setup** - Keep notes on what each zone does

### Testing Zones

1. Enable debug mode: `/hyperspawns debug on`
2. Check zone info: `/hyperspawns zone info <name>`
3. Verify world name matches
4. Try `/hyperspawns reload` if changes don't take effect

---

## Performance Considerations

### Zone Count

HyperSpawns uses efficient chunk-based indexing, but there are practical limits:

- **Low impact:** 1-50 zones
- **Moderate impact:** 50-200 zones
- **Consider optimization:** 200+ zones

### Zone Size

Large zones are handled efficiently, but consider:
- Very large spheres require more boundary calculations
- Prefer cuboids for huge areas
- Consider multiple smaller zones vs. one massive zone

### Chunk Coverage

Each zone affects all chunks it intersects:
- Zones crossing chunk boundaries affect multiple chunks
- Suppression data is per-chunk
- More chunk coverage = more memory usage

### Monitoring

Use `/hyperspawns stats` to monitor:
- Total zone count
- Enabled/disabled zones
- Chunk coverage

### Auto-Save

Default auto-save interval is 5 minutes. Adjust in config if needed:
```json
{
  "autoSaveIntervalMinutes": 10
}
```

---

## Next Steps

- [Commands Reference](COMMANDS.md) - Complete command documentation
- [Configuration](CONFIGURATION.md) - All configuration options
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues and solutions
