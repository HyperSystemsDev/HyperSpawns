# HyperSpawns Command Reference

Complete reference for all HyperSpawns commands, including syntax, parameters, permissions, and examples.

## Main Command

```
/hyperspawns
```

**Aliases:** `/hspawn`, `/spawns`, `/hs`

Running the main command without arguments displays the help menu.

---

## Zone Management Commands

### zone create

Creates a new spawn zone from your current selection.

```
/hyperspawns zone create <name> [type]
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Unique name for the zone |
| `type` | No | Boundary type: `cuboid` (default), `sphere`, `cylinder` |

**Permission:** `hyperspawns.zone.create`

**Examples:**
```
/hyperspawns zone create spawn_protection cuboid
/hyperspawns zone create arena sphere
/hyperspawns zone create tower cylinder
```

**Notes:**
- Requires pos1 and pos2 to be set first (except for `sphere` command shortcut)
- Zone is created in the world where selections were made
- Default mode is set from config (`defaultZoneMode`)

---

### zone delete

Deletes an existing zone.

```
/hyperspawns zone delete <name>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Name of zone to delete |

**Permission:** `hyperspawns.zone.delete`

**Example:**
```
/hyperspawns zone delete old_zone
```

---

### zone list

Lists all zones with pagination.

```
/hyperspawns zone list [page]
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `page` | No | Page number (default: 1) |

**Permission:** `hyperspawns.zone.list`

**Output shows:**
- `+` or `-` indicating enabled/disabled
- Zone name
- Mode in brackets
- World name

**Example:**
```
/hyperspawns zone list
/hyperspawns zone list 2
```

---

### zone info

Displays detailed information about a zone.

```
/hyperspawns zone info <name>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |

**Permission:** `hyperspawns.zone.list`

**Output includes:**
- Zone ID (UUID)
- World name
- Enabled status
- Mode
- Priority
- Boundary description
- Spawn multiplier (if MODIFY mode)
- Replacement NPC (if REPLACE mode)
- Filter criteria (if set)

**Example:**
```
/hyperspawns zone info spawn_protection
```

---

### zone redefine

Updates a zone's boundary from your current selection.

```
/hyperspawns zone redefine <name>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name to redefine |

**Permission:** `hyperspawns.zone.modify`

**Notes:**
- Requires pos1 and pos2 to be set
- Creates a cuboid boundary
- Clears selection after redefining

**Example:**
```
/hyperspawns pos1 0 0 0
/hyperspawns pos2 100 100 100
/hyperspawns zone redefine my_zone
```

---

### zone mode

Sets the spawn control mode for a zone.

```
/hyperspawns zone mode <name> <mode>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |
| `mode` | Yes | One of: `BLOCK`, `ALLOW`, `DENY`, `MODIFY`, `REPLACE` |

**Permission:** `hyperspawns.zone.modify`

**Mode descriptions:**
- `BLOCK` - Block all spawns
- `ALLOW` - Only allow filtered mobs (whitelist)
- `DENY` - Block filtered mobs (blacklist)
- `MODIFY` - Adjust spawn rates
- `REPLACE` - Replace spawning mobs

**Examples:**
```
/hyperspawns zone mode spawn_protection BLOCK
/hyperspawns zone mode dungeon ALLOW
/hyperspawns zone mode village DENY
```

---

### zone multiplier

Sets the spawn rate multiplier (for MODIFY mode).

```
/hyperspawns zone multiplier <name> <value>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |
| `value` | Yes | Multiplier from 0.0 to 10.0 |

**Permission:** `hyperspawns.zone.modify`

**Value guide:**
- `0.0` - No spawns
- `0.5` - Half spawn rate
- `1.0` - Normal (default)
- `2.0` - Double spawn rate
- `10.0` - Maximum (10x)

**Example:**
```
/hyperspawns zone multiplier dungeon 2.5
```

---

### zone priority

Sets the zone priority for overlap resolution.

```
/hyperspawns zone priority <name> <value>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |
| `value` | Yes | Priority integer (higher = checked first) |

**Permission:** `hyperspawns.zone.modify`

**Notes:**
- Default priority is 0
- Higher priority zones override lower priority
- Negative values are valid

**Example:**
```
/hyperspawns zone priority spawn_protection 100
/hyperspawns zone priority farm_zone -10
```

---

### zone enable

Enables a disabled zone.

```
/hyperspawns zone enable <name>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |

**Permission:** `hyperspawns.zone.modify`

**Example:**
```
/hyperspawns zone enable my_zone
```

---

### zone disable

Disables a zone without deleting it.

```
/hyperspawns zone disable <name>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |

**Permission:** `hyperspawns.zone.modify`

**Example:**
```
/hyperspawns zone disable my_zone
```

---

### zone filter add

Adds a filter criterion to a zone.

```
/hyperspawns zone filter <name> add <type> <value>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |
| `type` | Yes | Filter type (see below) |
| `value` | Yes | Filter value |

**Permission:** `hyperspawns.zone.modify`

**Filter types:**

| Type | Description | Example Values |
|------|-------------|----------------|
| `group` | NPC group | `hostile`, `passive`, `neutral` |
| `role` | NPC role/type | `zombie`, `skeleton`, `spider` |
| `time` | Time of day | `day`, `night`, `dawn`, `dusk` |
| `moon` | Moon phase | `0` (full) to `7` |
| `minlight` | Min light level | `0` to `15` |
| `maxlight` | Max light level | `0` to `15` |
| `miny` | Min Y coordinate | Any integer |
| `maxy` | Max Y coordinate | Any integer |

**Examples:**
```
/hyperspawns zone filter dungeon add group hostile
/hyperspawns zone filter village add role zombie
/hyperspawns zone filter cave add maxlight 7
/hyperspawns zone filter surface add miny 60
```

---

### zone filter remove

Removes a filter criterion from a zone.

```
/hyperspawns zone filter <name> remove <type> <value>
```

**Permission:** `hyperspawns.zone.modify`

**Example:**
```
/hyperspawns zone filter dungeon remove group hostile
```

---

### zone filter clear

Clears all filter criteria from a zone.

```
/hyperspawns zone filter <name> clear
```

**Permission:** `hyperspawns.zone.modify`

**Example:**
```
/hyperspawns zone filter dungeon clear
```

---

## Global Control Commands

### global

Shows current global spawn settings.

```
/hyperspawns global
```

**Permission:** `hyperspawns.global`

**Output shows:**
- Global spawn multiplier
- Paused status

---

### global multiplier

Sets the server-wide spawn rate multiplier.

```
/hyperspawns global multiplier <value>
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `value` | Yes | Multiplier from 0.0 to 10.0 |

**Permission:** `hyperspawns.global`

**Example:**
```
/hyperspawns global multiplier 0.5
```

---

### global pause

Pauses all mob spawning server-wide.

```
/hyperspawns global pause
```

**Permission:** `hyperspawns.global`

---

### global resume

Resumes mob spawning after a pause.

```
/hyperspawns global resume
```

**Permission:** `hyperspawns.global`

---

## Selection Commands

### pos1

Sets selection position 1.

```
/hyperspawns pos1 <x> <y> <z> [world]
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `x` | Yes | X coordinate |
| `y` | Yes | Y coordinate |
| `z` | Yes | Z coordinate |
| `world` | No | World name (default: "default") |

**Permission:** `hyperspawns.wand`

**Example:**
```
/hyperspawns pos1 -100 0 -100
/hyperspawns pos1 0 64 0 nether
```

---

### pos2

Sets selection position 2.

```
/hyperspawns pos2 <x> <y> <z> [world]
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `x` | Yes | X coordinate |
| `y` | Yes | Y coordinate |
| `z` | Yes | Z coordinate |
| `world` | No | World name (default: "default") |

**Permission:** `hyperspawns.wand`

**Example:**
```
/hyperspawns pos2 100 256 100
```

---

### sphere

Creates a spherical zone directly.

```
/hyperspawns sphere <name> <radius> [x] [y] [z] [world]
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `name` | Yes | Zone name |
| `radius` | Yes | Sphere radius |
| `x` | No | Center X (uses pos1 if not provided) |
| `y` | No | Center Y (uses pos1 if not provided) |
| `z` | No | Center Z (uses pos1 if not provided) |
| `world` | No | World name |

**Permission:** `hyperspawns.zone.create`

**Examples:**
```
# Using pos1 as center
/hyperspawns pos1 0 64 0
/hyperspawns sphere safe_zone 50

# Specifying coordinates
/hyperspawns sphere arena 30 100 65 200
```

---

### wand

Shows selection wand information.

```
/hyperspawns wand
```

**Permission:** `hyperspawns.wand`

---

### wandmode

Toggles wand selection mode for click-based selection.

```
/hyperspawns wandmode
```

**Permission:** `hyperspawns.wand`

**Notes:**
- When enabled, left-click sets pos1
- Right-click sets pos2
- Toggle again to disable

---

## Utility Commands

### reload

Reloads configuration and zone data.

```
/hyperspawns reload
```

**Permission:** `hyperspawns.reload`

**Notes:**
- Reloads `config.json`
- Reloads `zones.json`
- Re-applies all zone suppressions

---

### stats

Shows plugin statistics.

```
/hyperspawns stats
```

**Permission:** `hyperspawns.stats`

**Output includes:**
- Plugin version
- Zone statistics
- Global multiplier
- Paused status
- Debug mode status

---

### debug

Toggles or sets debug mode.

```
/hyperspawns debug [on|off]
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `state` | No | `on` or `off` (toggles if not provided) |

**Permission:** `hyperspawns.debug`

**Examples:**
```
/hyperspawns debug        # Toggle
/hyperspawns debug on     # Enable
/hyperspawns debug off    # Disable
```

---

### help

Shows help information.

```
/hyperspawns help
```

---

## Permission Summary

| Permission | Description |
|------------|-------------|
| `hyperspawns.admin` | Full access (wildcard) |
| `hyperspawns.zone.create` | Create zones |
| `hyperspawns.zone.delete` | Delete zones |
| `hyperspawns.zone.modify` | Modify zone settings (mode, filter, priority, etc.) |
| `hyperspawns.zone.list` | List and view zone info |
| `hyperspawns.global` | Global spawn controls |
| `hyperspawns.wand` | Selection wand and position commands |
| `hyperspawns.reload` | Reload configuration |
| `hyperspawns.stats` | View statistics |
| `hyperspawns.debug` | Toggle debug mode |
| `hyperspawns.bypass` | Bypass zone restrictions (for admin testing) |

---

## Tab Completion

Most commands support tab completion:
- Zone names auto-complete for existing zones
- Mode names auto-complete (`BLOCK`, `ALLOW`, etc.)
- Filter types auto-complete
- Subcommands auto-complete

---

## See Also

- [Admin Guide](ADMIN-GUIDE.md) - Complete administration guide
- [Configuration](CONFIGURATION.md) - Configuration options
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues
