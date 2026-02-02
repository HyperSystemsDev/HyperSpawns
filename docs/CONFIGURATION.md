# HyperSpawns Configuration Reference

Complete reference for all HyperSpawns configuration files and options.

## Table of Contents

1. [File Locations](#file-locations)
2. [config.json Reference](#configjson-reference)
3. [zones.json Reference](#zonesjson-reference)
4. [Example Configurations](#example-configurations)

---

## File Locations

HyperSpawns stores configuration files in the plugin's data folder:

```
<server>/plugins/HyperSpawns/
├── config.json     # Plugin configuration
└── zones.json      # Zone data (auto-generated)
```

---

## config.json Reference

The main configuration file controls plugin behavior.

### Default Configuration

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

### Option Reference

#### debugMode

| Property | Value |
|----------|-------|
| Type | `boolean` |
| Default | `false` |
| Runtime | Yes (`/hyperspawns debug`) |

Enables detailed logging to console for troubleshooting.

**When enabled, logs include:**
- Zone suppression applications
- World name matching
- Chunk processing details
- Filter compilation results

```json
"debugMode": true
```

---

#### autoSaveIntervalMinutes

| Property | Value |
|----------|-------|
| Type | `integer` |
| Default | `5` |
| Minimum | `1` |
| Runtime | No (requires restart) |

How often zone data is automatically saved to disk.

**Considerations:**
- Lower values = more frequent saves, more disk I/O
- Higher values = less I/O, risk of data loss on crash
- Changes are also saved immediately when made via commands

```json
"autoSaveIntervalMinutes": 10
```

---

#### defaultZoneMode

| Property | Value |
|----------|-------|
| Type | `string` |
| Default | `"block"` |
| Valid Values | `"block"`, `"allow"`, `"deny"`, `"modify"`, `"replace"` |
| Runtime | Yes (requires reload) |

The default mode assigned to newly created zones.

**Mode descriptions:**
- `block` - Block all spawns (most common for safe zones)
- `allow` - Whitelist mode (only filtered mobs can spawn)
- `deny` - Blacklist mode (filtered mobs cannot spawn)
- `modify` - Adjust spawn rates
- `replace` - Replace spawning mobs

```json
"defaultZoneMode": "block"
```

---

#### bypassPermission

| Property | Value |
|----------|-------|
| Type | `string` |
| Default | `"hyperspawns.bypass"` |
| Runtime | Yes (requires reload) |

Permission node that allows players to bypass zone restrictions. Useful for admin testing.

```json
"bypassPermission": "hyperspawns.bypass"
```

---

#### wandItemId

| Property | Value |
|----------|-------|
| Type | `string` |
| Default | `"hytale:stick"` |
| Runtime | Yes (requires reload) |

The item used as the selection wand when wandmode is enabled.

**Format:** `namespace:item_id`

```json
"wandItemId": "hytale:stick"
```

---

#### particlesEnabled

| Property | Value |
|----------|-------|
| Type | `boolean` |
| Default | `true` |
| Runtime | Yes (requires reload) |

Whether to show particles for zone boundaries and selections.

```json
"particlesEnabled": true
```

---

#### particleViewDistance

| Property | Value |
|----------|-------|
| Type | `integer` |
| Default | `32` |
| Minimum | `8` |
| Maximum | `128` |
| Runtime | Yes (requires reload) |

Maximum distance (in blocks) at which zone particles are visible.

**Considerations:**
- Higher values = more visibility, more network traffic
- Lower values = less visibility, better performance

```json
"particleViewDistance": 32
```

---

#### globalSpawnMultiplier

| Property | Value |
|----------|-------|
| Type | `number` |
| Default | `1.0` |
| Minimum | `0.0` |
| Maximum | `10.0` |
| Runtime | Yes (`/hyperspawns global multiplier`) |

Server-wide spawn rate multiplier applied to all natural spawning.

**Value guide:**
- `0.0` - No natural spawns
- `0.5` - Half spawn rate
- `1.0` - Normal (default)
- `2.0` - Double spawn rate

```json
"globalSpawnMultiplier": 1.0
```

---

#### globalSpawnPaused

| Property | Value |
|----------|-------|
| Type | `boolean` |
| Default | `false` |
| Runtime | Yes (`/hyperspawns global pause/resume`) |

When true, all natural mob spawning is paused server-wide.

```json
"globalSpawnPaused": false
```

---

## zones.json Reference

Zone data is stored automatically. You typically don't need to edit this file manually.

### Schema

```json
{
  "schemaVersion": 1,
  "lastSaved": 1704067200000,
  "zones": [
    {
      "id": "uuid-string",
      "name": "zone_name",
      "world": "world_name",
      "boundary": { ... },
      "mode": "block",
      "filter": { ... },
      "priority": 0,
      "enabled": true,
      "spawnRateMultiplier": 1.0,
      "replacementNpc": null
    }
  ]
}
```

### Zone Object Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID string | Unique identifier (auto-generated) |
| `name` | string | Human-readable name |
| `world` | string | World name this zone belongs to |
| `boundary` | object | Spatial boundary definition |
| `mode` | string | Spawn control mode |
| `filter` | object | Filter criteria |
| `priority` | integer | Priority for overlap resolution |
| `enabled` | boolean | Whether zone is active |
| `spawnRateMultiplier` | number | Multiplier for MODIFY mode |
| `replacementNpc` | string or null | Replacement NPC for REPLACE mode |

### Boundary Types

#### Cuboid Boundary

```json
"boundary": {
  "type": "cuboid",
  "minX": -100,
  "minY": 0,
  "minZ": -100,
  "maxX": 100,
  "maxY": 256,
  "maxZ": 100
}
```

#### Sphere Boundary

```json
"boundary": {
  "type": "sphere",
  "centerX": 0,
  "centerY": 64,
  "centerZ": 0,
  "radius": 50
}
```

#### Cylinder Boundary

```json
"boundary": {
  "type": "cylinder",
  "centerX": 0,
  "centerZ": 0,
  "radius": 30,
  "minY": 0,
  "maxY": 128
}
```

### Filter Object

```json
"filter": {
  "npcGroups": ["hostile"],
  "npcRoles": ["zombie", "skeleton"],
  "minLightLevel": null,
  "maxLightLevel": 7,
  "minYLevel": null,
  "maxYLevel": 50,
  "timeOfDay": ["night"],
  "moonPhases": [0, 4]
}
```

All filter fields are optional. Empty/null means "match all" for that criterion.

---

## Example Configurations

### Safe Server (Minimal Spawning)

config.json:
```json
{
  "debugMode": false,
  "autoSaveIntervalMinutes": 10,
  "defaultZoneMode": "block",
  "bypassPermission": "hyperspawns.bypass",
  "wandItemId": "hytale:stick",
  "particlesEnabled": false,
  "particleViewDistance": 16,
  "globalSpawnMultiplier": 0.5,
  "globalSpawnPaused": false
}
```

### Hardcore Server (Enhanced Spawning)

config.json:
```json
{
  "debugMode": false,
  "autoSaveIntervalMinutes": 5,
  "defaultZoneMode": "modify",
  "bypassPermission": "hyperspawns.bypass",
  "wandItemId": "hytale:stick",
  "particlesEnabled": true,
  "particleViewDistance": 32,
  "globalSpawnMultiplier": 2.0,
  "globalSpawnPaused": false
}
```

### Development/Testing

config.json:
```json
{
  "debugMode": true,
  "autoSaveIntervalMinutes": 1,
  "defaultZoneMode": "block",
  "bypassPermission": "hyperspawns.bypass",
  "wandItemId": "hytale:stick",
  "particlesEnabled": true,
  "particleViewDistance": 64,
  "globalSpawnMultiplier": 1.0,
  "globalSpawnPaused": false
}
```

### Complete Zone Example

zones.json:
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
        "minX": -100,
        "minY": 0,
        "minZ": -100,
        "maxX": 100,
        "maxY": 256,
        "maxZ": 100
      },
      "mode": "block",
      "filter": {},
      "priority": 100,
      "enabled": true,
      "spawnRateMultiplier": 1.0,
      "replacementNpc": null
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "name": "dungeon_hostile",
      "world": "world",
      "boundary": {
        "type": "sphere",
        "centerX": 500,
        "centerY": 30,
        "centerZ": 500,
        "radius": 40
      },
      "mode": "allow",
      "filter": {
        "npcGroups": ["hostile"],
        "npcRoles": [],
        "minLightLevel": null,
        "maxLightLevel": null,
        "minYLevel": null,
        "maxYLevel": null,
        "timeOfDay": [],
        "moonPhases": []
      },
      "priority": 50,
      "enabled": true,
      "spawnRateMultiplier": 2.0,
      "replacementNpc": null
    },
    {
      "id": "770e8400-e29b-41d4-a716-446655440002",
      "name": "no_zombies_village",
      "world": "world",
      "boundary": {
        "type": "cylinder",
        "centerX": -200,
        "centerZ": 300,
        "radius": 60,
        "minY": 50,
        "maxY": 150
      },
      "mode": "deny",
      "filter": {
        "npcGroups": [],
        "npcRoles": ["zombie"],
        "minLightLevel": null,
        "maxLightLevel": null,
        "minYLevel": null,
        "maxYLevel": null,
        "timeOfDay": [],
        "moonPhases": []
      },
      "priority": 25,
      "enabled": true,
      "spawnRateMultiplier": 1.0,
      "replacementNpc": null
    }
  ]
}
```

---

## Reloading Configuration

After editing configuration files manually:

```
/hyperspawns reload
```

This reloads both `config.json` and `zones.json`.

---

## See Also

- [Admin Guide](ADMIN-GUIDE.md) - Complete administration guide
- [Commands Reference](COMMANDS.md) - Command documentation
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues
