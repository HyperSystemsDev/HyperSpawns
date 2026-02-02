# Understanding Hytale Mob Spawning

This document explains how Hytale's mob spawning system works and how HyperSpawns integrates with it to control spawns.

## Table of Contents

1. [Spawning Overview](#spawning-overview)
2. [NPC Roles and Groups](#npc-roles-and-groups)
3. [Spawn Suppression System](#spawn-suppression-system)
4. [Chunk-Based Spawning](#chunk-based-spawning)
5. [Environmental Factors](#environmental-factors)
6. [How HyperSpawns Integrates](#how-hyperspawns-integrates)

---

## Spawning Overview

Hytale uses an ECS (Entity Component System) architecture for mob spawning. The spawning system is managed by the `SpawningPlugin`, which handles:

- Natural mob spawning based on biome and conditions
- Spawn rate calculations
- Population density management
- Spawn suppression

### Spawning Lifecycle

```
Spawn Cycle
    │
    ├── 1. Select candidate chunks
    │
    ├── 2. Check spawn conditions
    │       ├── Light level
    │       ├── Y level
    │       ├── Time of day
    │       ├── Moon phase
    │       └── Biome rules
    │
    ├── 3. Check suppression
    │       └── ChunkSuppressionEntry
    │
    ├── 4. Select NPC type
    │       └── Based on spawn table
    │
    └── 5. Spawn entity
```

---

## NPC Roles and Groups

### NPC Roles

Each mob type in Hytale has a unique **role** identifier. Roles are integer indices that map to specific NPC types.

**Examples:**
- `zombie` → role index
- `skeleton` → role index
- `spider` → role index

When HyperSpawns filters by role, it uses these indices for efficient matching.

### NPC Groups

Roles are organized into **groups** for categorization:

| Group | Description | Examples |
|-------|-------------|----------|
| `hostile` | Aggressive mobs | Zombies, skeletons, spiders |
| `passive` | Non-aggressive mobs | Chickens, cows, pigs |
| `neutral` | Conditionally aggressive | Wolves, bees |

### TagSetPlugin

Hytale manages group membership through `TagSetPlugin<NPCGroup>`:

```java
TagSetPlugin<NPCGroup> tagSetPlugin = TagSetPlugin.get(NPCGroup.class);
IntSet rolesInGroup = tagSetPlugin.getSet(groupIndex);
```

HyperSpawns uses this to resolve group names to individual role indices.

---

## Spawn Suppression System

The spawn suppression system is Hytale's built-in mechanism for controlling spawns at the chunk level.

### Key Components

#### SpawnSuppressionController

A world-level resource that maintains the suppression state:

```java
SpawnSuppressionController controller = entityStore.getResource(
    SpawnSuppressionController.getResourceType()
);

// Access the chunk suppression map
Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkMap =
    controller.getChunkSuppressionMap();
```

The map key is a packed long representing chunk coordinates (`ChunkUtil.indexChunk(x, z)`).

#### ChunkSuppressionEntry

Contains suppression data for a single chunk:

```java
public class ChunkSuppressionEntry {
    private List<SuppressionSpan> suppressionSpans;

    public List<SuppressionSpan> getSuppressionSpans();
}
```

A chunk can have multiple suppression spans from different sources (plugins, world features, etc.).

#### SuppressionSpan

Defines a vertical range and which NPC roles are suppressed:

```java
public class SuppressionSpan {
    private UUID suppressorId;    // Who created this suppression
    private int minY;             // Minimum Y level
    private int maxY;             // Maximum Y level
    private IntSet suppressedRoles; // null = all roles

    public UUID getSuppressorId();
    public int getMinY();
    public int getMaxY();
    public IntSet getSuppressedRoles();
}
```

### Suppression Logic

When the spawning system attempts to spawn a mob:

1. Get the chunk's `ChunkSuppressionEntry`
2. For each `SuppressionSpan`:
   - Check if spawn Y is within [minY, maxY]
   - Check if the NPC's role is in `suppressedRoles` (or if null, all roles)
3. If any span matches, suppress the spawn

---

## Chunk-Based Spawning

### Chunk Coordinates

Hytale uses 16x16 block chunks. Conversion:

```java
int chunkX = blockX >> 4;  // or Math.floor(blockX / 16.0)
int chunkZ = blockZ >> 4;

long chunkIndex = ChunkUtil.indexChunk(chunkX, chunkZ);
```

### Why Chunk-Based?

1. **Efficiency** - O(1) lookup for suppression data
2. **Locality** - Spawning naturally clusters in chunks
3. **Loading** - Matches chunk loading/unloading lifecycle

### Chunk Loading Events

When chunks load, they need suppression data applied:

```java
ChunkSuppressionQueue queue = chunkStore.getResource(
    SpawningPlugin.get().getChunkSuppressionQueueResourceType()
);

// Queue suppression for newly loaded chunk
queue.queueForAdd(chunkRef, suppressionEntry);

// Queue removal when chunk unloads
queue.queueForRemove(chunkRef);
```

---

## Environmental Factors

Hytale's spawning considers various environmental conditions:

### Light Level

- Range: 0-15
- Hostile mobs typically spawn at low light (0-7)
- Passive mobs may spawn at any light level

```
Light Level 0-7:  Hostile spawning allowed
Light Level 8-15: Hostile spawning typically blocked
```

### Y Level

- Underground: More cave mobs
- Surface: Biome-specific spawns
- Sky: Flying mobs

### Time of Day

| Period | Description |
|--------|-------------|
| `day` | Full daylight hours |
| `night` | Nighttime |
| `dawn` | Sunrise transition |
| `dusk` | Sunset transition |

Hostile mobs typically spawn more at night.

### Moon Phase

Hytale uses a moon cycle (phases 0-7):

| Phase | Description |
|-------|-------------|
| 0 | Full moon |
| 4 | New moon |

Some mobs have increased spawn rates during specific moon phases.

### Biome

Each biome has its own spawn table defining:
- Which mobs can spawn
- Spawn weights
- Special conditions

---

## How HyperSpawns Integrates

### Integration Point

HyperSpawns integrates at the suppression system level, not the spawn event level. This is important for performance.

```
Traditional Approach (Event-Based):
Spawn Event → Event Handler → Check Zones → Allow/Cancel
❌ Runs on every spawn attempt
❌ Must iterate through zones
❌ Cancellation overhead

HyperSpawns Approach (Suppression-Based):
Zone Created → Update Suppression Map → Spawning System Checks Map
✅ One-time setup per zone change
✅ O(1) lookup via chunk index
✅ Native integration with spawning
```

### ChunkSuppressionIntegrator

This is the core integration class:

```java
public void applyToWorld(World world, SpawnZoneManager zoneManager) {
    // 1. Get suppression controller
    SpawnSuppressionController controller = ...;

    // 2. Clear existing HyperSpawns suppressions
    clearExistingSuppression(controller, ...);

    // 3. For each zone in this world
    for (SpawnZone zone : zones) {
        if (zone.getMode() != BLOCK && zone.getMode() != DENY) continue;
        if (!zone.isEnabled()) continue;

        // 4. Apply zone to all intersecting chunks
        applyZoneSuppression(zone, controller, ...);
    }
}
```

### Unique Suppressor IDs

HyperSpawns generates unique IDs with a recognizable prefix:

```java
// Prefix: "HSPS" in hex (HyperSpawns)
private static final UUID HYPERSPAWNS_PREFIX =
    UUID.fromString("48535053-0000-0000-0000-000000000000");

// Each zone gets a derived ID
UUID suppressorId = new UUID(
    HYPERSPAWNS_PREFIX.getMostSignificantBits() ^ zoneId.getMostSignificantBits(),
    HYPERSPAWNS_PREFIX.getLeastSignificantBits() ^ zoneId.getLeastSignificantBits()
);
```

This allows HyperSpawns to:
- Identify its own suppression entries
- Remove them without affecting other plugins
- Update zones without full re-application

### Mode-Specific Behavior

#### BLOCK Mode

```java
// Suppress ALL roles (null = all)
IntSet suppressedRoles = null;

// Creates span covering zone's Y range
new SuppressionSpan(suppressorId, minY, maxY, null);
```

#### DENY Mode

```java
// Suppress only filtered roles
IntSet suppressedRoles = filter.getCompiledRoleIndices();

// Plus roles from filter groups
for (String group : filter.getNpcGroups()) {
    IntSet groupRoles = resolveGroup(group);
    suppressedRoles.addAll(groupRoles);
}

new SuppressionSpan(suppressorId, minY, maxY, suppressedRoles);
```

#### Other Modes

ALLOW, MODIFY, and REPLACE modes don't use the suppression system directly. They would require event-based handling for full implementation.

---

## Performance Implications

### Suppression Map Size

```
Chunks affected = Σ (zone.getChunkCount()) for all zones
Memory per chunk = ~100-200 bytes per suppression span
```

For a 200x200 block zone (13x13 = 169 chunks), memory usage is minimal.

### Lookup Speed

```
Spawn check: O(1) map lookup + O(spans) iteration
Typically spans ≤ 5 per chunk
```

### When Suppression is Re-Applied

- Server start
- Plugin reload (`/hyperspawns reload`)
- Zone creation/modification
- Chunk loading (for new chunks)

---

## Comparison with Event-Based Systems

| Aspect | Suppression-Based | Event-Based |
|--------|-------------------|-------------|
| Setup cost | Higher (one-time) | Lower |
| Per-spawn cost | Minimal | Higher |
| Scalability | Excellent | Limited |
| Complexity | Requires native API | Simpler |
| Flexibility | Modes limited | Any logic possible |

HyperSpawns uses suppression for BLOCK/DENY (most common) and would need event handling for MODIFY/REPLACE/ALLOW to be fully implemented.

---

## See Also

- [Architecture](ARCHITECTURE.md) - HyperSpawns code structure
- [Admin Guide](ADMIN-GUIDE.md) - Using HyperSpawns
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues
