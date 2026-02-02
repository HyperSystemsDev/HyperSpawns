# HyperSpawns Architecture

Technical documentation for developers who want to understand, modify, or contribute to HyperSpawns.

## Table of Contents

1. [Project Structure](#project-structure)
2. [Core Components](#core-components)
3. [Spawn Suppression Flow](#spawn-suppression-flow)
4. [Spatial Indexing](#spatial-indexing)
5. [Filter Compilation](#filter-compilation)
6. [ECS Integration](#ecs-integration)
7. [Key Classes](#key-classes)

---

## Project Structure

```
src/main/java/dev/hypersystems/hyperspawns/
├── HyperSpawns.java              # Main plugin class
├── HyperSpawnsPlugin.java        # Plugin entry point
├── command/
│   ├── HyperSpawnsCommand.java   # Command handler
│   └── WandSelectionManager.java # Selection state management
├── config/
│   └── HyperSpawnsConfig.java    # Configuration singleton
├── persistence/
│   └── ZonePersistence.java      # JSON serialization
├── system/
│   ├── ChunkSuppressionIntegrator.java  # Core suppression logic
│   ├── SpawnZoneCheckerSystem.java      # ECS system for checking
│   ├── SpawnZoneSuppressionResource.java # ECS resource
│   ├── WandInteractionSystem.java       # Wand click handling
│   └── HostileMobRemovalSystem.java     # Optional mob cleanup
├── util/
│   ├── ChunkIndexer.java         # Chunk coordinate utilities
│   └── Logger.java               # Logging utilities
└── zone/
    ├── SpawnZone.java            # Zone data class
    ├── SpawnZoneManager.java     # Zone collection management
    ├── ZoneBoundary.java         # Boundary interface
    ├── CuboidBoundary.java       # Box boundary
    ├── SphereBoundary.java       # Sphere boundary
    ├── CylinderBoundary.java     # Cylinder boundary
    ├── ZoneFilter.java           # Filter criteria
    └── ZoneMode.java             # Mode enum
```

---

## Core Components

### Plugin Lifecycle

```
HyperSpawnsPlugin (Entry Point)
    │
    └── HyperSpawns (Main Class)
            │
            ├── HyperSpawnsConfig (Configuration)
            ├── SpawnZoneManager (Zone Storage)
            ├── ZonePersistence (Serialization)
            ├── WandSelectionManager (Selection State)
            ├── ChunkSuppressionIntegrator (Suppression)
            └── ECS Systems (Wand, Checker, etc.)
```

### Data Flow

```
User Command
    │
    ▼
HyperSpawnsCommand
    │
    ▼
SpawnZoneManager (modify zones)
    │
    ▼
ChunkSuppressionIntegrator (apply to world)
    │
    ▼
Hytale SpawnSuppressionController
    │
    ▼
ChunkSuppressionEntry (per-chunk data)
```

---

## Spawn Suppression Flow

The key to understanding HyperSpawns is the `ChunkSuppressionIntegrator`. This class bridges our zone definitions with Hytale's native spawn suppression system.

### How Hytale Spawn Suppression Works

Hytale uses an ECS-based spawning system with suppression data stored at the chunk level:

1. **SpawnSuppressionController** - World-level resource containing a map of chunk indices to suppression entries
2. **ChunkSuppressionEntry** - Contains a list of SuppressionSpans for a chunk
3. **SuppressionSpan** - Defines Y-range and suppressed NPC roles for a suppressor

### HyperSpawns Integration

```java
// Simplified flow from ChunkSuppressionIntegrator.applyToWorld()

1. Get SpawnSuppressionController from world's entity store
2. Clear any existing HyperSpawns suppression entries
3. For each zone in the current world:
   a. Skip if not BLOCK or DENY mode
   b. Skip if disabled
   c. Calculate affected chunk range from boundary
   d. For each affected chunk:
      - Create SuppressionSpan with zone's Y-range and suppressed roles
      - Add to chunk's ChunkSuppressionEntry
      - Queue update for loaded chunks
```

### Suppression ID Generation

Each zone gets a unique suppressor ID derived from a prefix and the zone's UUID:

```java
private static final UUID HYPERSPAWNS_PREFIX =
    UUID.fromString("48535053-0000-0000-0000-000000000000");

private UUID generateSuppressionId(UUID zoneId) {
    return new UUID(
        HYPERSPAWNS_PREFIX.getMostSignificantBits() ^ zoneId.getMostSignificantBits(),
        HYPERSPAWNS_PREFIX.getLeastSignificantBits() ^ zoneId.getLeastSignificantBits()
    );
}
```

This allows identifying and removing HyperSpawns entries without affecting other plugins' suppressions.

### Suppressed Roles Determination

```java
// For BLOCK mode: suppress ALL roles (return null)
// For DENY mode: suppress filtered roles only

private IntSet getSuppressedRoles(SpawnZone zone) {
    if (mode == ZoneMode.BLOCK) {
        return null; // null = suppress all
    }

    // For DENY, collect roles from filter
    IntSet suppressedRoles = new IntOpenHashSet();

    // Add compiled role indices from filter
    if (filter.getCompiledRoleIndices() != null) {
        suppressedRoles.addAll(filter.getCompiledRoleIndices());
    }

    // Add roles from NPC groups
    for (String groupName : filter.getNpcGroups()) {
        // Resolve group to individual roles via TagSetPlugin
    }

    return suppressedRoles.isEmpty() ? null : suppressedRoles;
}
```

---

## Spatial Indexing

HyperSpawns uses chunk-based spatial indexing for O(1) average zone lookup.

### ChunkIndexer Utilities

```java
public class ChunkIndexer {
    public static long indexChunk(int chunkX, int chunkZ) {
        return ChunkUtil.indexChunk(chunkX, chunkZ);
    }

    public static int getChunkX(long index) {
        return (int)(index >> 32);
    }

    public static int getChunkZ(long index) {
        return (int)index;
    }
}
```

### Boundary Chunk Intersection

Each boundary type implements `intersectsChunk()`:

```java
// CuboidBoundary
public boolean intersectsChunk(int chunkX, int chunkZ) {
    int chunkMinX = chunkX << 4;
    int chunkMaxX = chunkMinX + 15;
    int chunkMinZ = chunkZ << 4;
    int chunkMaxZ = chunkMinZ + 15;

    return !(chunkMaxX < minX || chunkMinX > maxX ||
             chunkMaxZ < minZ || chunkMinZ > maxZ);
}
```

### Chunk Range Calculation

Boundaries provide min/max chunk coordinates:

```java
// CuboidBoundary
public int getMinChunkX() { return (int)Math.floor(minX / 16.0); }
public int getMaxChunkX() { return (int)Math.floor(maxX / 16.0); }
// etc.
```

---

## Filter Compilation

Filters are compiled for efficient runtime matching.

### ZoneFilter Structure

```java
public final class ZoneFilter {
    private final Set<String> npcGroups;      // e.g., "hostile", "passive"
    private final Set<String> npcRoles;       // e.g., "zombie", "skeleton"
    private final Integer minLightLevel;       // 0-15
    private final Integer maxLightLevel;       // 0-15
    private final Integer minYLevel;           // Any int
    private final Integer maxYLevel;           // Any int
    private final Set<String> timeOfDay;       // "day", "night", etc.
    private final Set<Integer> moonPhases;     // 0-7

    // Compiled data
    private transient IntSet compiledRoleIndices;
    private transient boolean compiled;
}
```

### Compilation Process

```java
public void compile(RoleResolver roleResolver) {
    if (npcRoles.isEmpty() && npcGroups.isEmpty()) {
        compiledRoleIndices = null; // Match all roles
    } else {
        IntOpenHashSet indices = new IntOpenHashSet();

        // Add direct role indices
        for (String role : npcRoles) {
            int index = roleResolver.getRoleIndex(role);
            if (index >= 0) indices.add(index);
        }

        // Add roles from groups
        for (String group : npcGroups) {
            IntSet groupRoles = roleResolver.getRolesInGroup(group);
            if (groupRoles != null) indices.addAll(groupRoles);
        }

        compiledRoleIndices = IntSets.unmodifiable(indices);
    }
    compiled = true;
}
```

### Runtime Matching

```java
public boolean matchesRole(int roleIndex) {
    // null = matches all
    return compiledRoleIndices == null ||
           compiledRoleIndices.contains(roleIndex);
}

public boolean matchesEnvironment(int light, int y, String time, int moon) {
    if (minLightLevel != null && light < minLightLevel) return false;
    if (maxLightLevel != null && light > maxLightLevel) return false;
    if (minYLevel != null && y < minYLevel) return false;
    if (maxYLevel != null && y > maxYLevel) return false;
    if (!timeOfDay.isEmpty() && !timeOfDay.contains(time)) return false;
    if (!moonPhases.isEmpty() && !moonPhases.contains(moon)) return false;
    return true;
}
```

---

## ECS Integration

HyperSpawns uses Hytale's ECS (Entity Component System) for various runtime operations.

### Systems

| System | Purpose |
|--------|---------|
| `SpawnZoneCheckerSystem` | Monitors spawn events for logging/debugging |
| `WandInteractionSystem` | Handles wand click events for selection |
| `HostileMobRemovalSystem` | Optional cleanup of existing mobs in zones |

### Resources

| Resource | Purpose |
|----------|---------|
| `SpawnZoneSuppressionResource` | Provides zone manager access to ECS systems |

### Example: WandInteractionSystem

```java
public class WandInteractionSystem {
    private final Set<UUID> wandModeEnabled = new HashSet<>();

    public boolean toggleWandMode(UUID playerId) {
        if (wandModeEnabled.contains(playerId)) {
            wandModeEnabled.remove(playerId);
            return false;
        } else {
            wandModeEnabled.add(playerId);
            return true;
        }
    }

    // Called on player interaction events
    public void onInteract(Player player, InteractionType type, BlockPos pos) {
        if (!wandModeEnabled.contains(player.getUuid())) return;
        if (!isHoldingWand(player)) return;

        if (type == InteractionType.LEFT_CLICK) {
            wandManager.setPos1(player.getUuid(), world, pos);
        } else if (type == InteractionType.RIGHT_CLICK) {
            wandManager.setPos2(player.getUuid(), world, pos);
        }
    }
}
```

---

## Key Classes

### HyperSpawns (Main Class)

Singleton that coordinates all components:

```java
public final class HyperSpawns {
    private static HyperSpawns instance;

    private HyperSpawnsConfig config;
    private SpawnZoneManager zoneManager;
    private ZonePersistence persistence;
    private WandSelectionManager wandManager;
    private Map<String, ChunkSuppressionIntegrator> integrators;

    public void onEnable() {
        config = HyperSpawnsConfig.init(dataFolder);
        zoneManager = new SpawnZoneManager();
        persistence = new ZonePersistence(dataFolder);
        persistence.load(zoneManager);
        wandManager = new WandSelectionManager();
        // Register commands, systems, etc.
    }

    public void reload() {
        config.reload();
        persistence.load(zoneManager);
        reapplyAllSuppressions();
    }
}
```

### SpawnZoneManager

Manages the collection of zones:

```java
public final class SpawnZoneManager {
    private final Map<UUID, SpawnZone> zonesById = new ConcurrentHashMap<>();
    private final Map<String, SpawnZone> zonesByName = new ConcurrentHashMap<>();

    public SpawnZone createZone(String name, String world, ZoneBoundary boundary);
    public boolean deleteZone(String name);
    public SpawnZone getZone(String name);
    public Collection<SpawnZone> getAllZones();
    public Collection<SpawnZone> getZonesInWorld(String world);
    public void updateBoundary(SpawnZone zone, ZoneBoundary boundary);
    public void markModified(SpawnZone zone);
}
```

### SpawnZone

Data class representing a zone:

```java
public final class SpawnZone {
    private final UUID id;
    private String name;
    private String world;
    private ZoneBoundary boundary;
    private ZoneMode mode;
    private ZoneFilter filter;
    private int priority;
    private boolean enabled;
    private double spawnRateMultiplier;
    private String replacementNpc;

    public boolean shouldBlockSpawn(int roleIndex, int light, int y,
                                    String time, int moon);
    public boolean contains(double x, double y, double z);
    public boolean intersectsChunk(int chunkX, int chunkZ);
    public void compile(ZoneFilter.RoleResolver resolver);
}
```

### ZoneBoundary (Interface)

```java
public interface ZoneBoundary {
    String getType();
    boolean contains(double x, double y, double z);
    boolean intersectsChunk(int chunkX, int chunkZ);

    double getMinX();
    double getMaxX();
    double getMinY();
    double getMaxY();
    double getMinZ();
    double getMaxZ();

    int getMinChunkX();
    int getMaxChunkX();
    int getMinChunkZ();
    int getMaxChunkZ();

    String describe();
}
```

### ZonePersistence

Handles JSON serialization:

```java
public final class ZonePersistence {
    private static final String ZONES_FILE = "zones.json";

    public void load(SpawnZoneManager manager);
    public void save(SpawnZoneManager manager);

    // Schema versioning for future migrations
    private void migrateSchema(JsonObject json, int fromVersion);
}
```

---

## Extending HyperSpawns

### Adding a New Zone Mode

1. Add enum value to `ZoneMode.java`
2. Update `SpawnZone.shouldBlockSpawn()` logic
3. Update `ChunkSuppressionIntegrator` if mode affects suppression
4. Add command handling in `ZoneModeSubCommand`

### Adding a New Filter Type

1. Add field to `ZoneFilter` and `ZoneFilter.Builder`
2. Update `matchesEnvironment()` or create new matching method
3. Update `ZonePersistence` serialization
4. Add command handling in filter subcommand

### Adding a New Boundary Type

1. Create new class implementing `ZoneBoundary`
2. Implement all required methods (contains, intersectsChunk, etc.)
3. Add serialization in `ZonePersistence`
4. Add command support in `ZoneCreateSubCommand`

---

## Performance Notes

### Memory Usage

- Each zone: ~200-500 bytes
- Each chunk suppression entry: ~100-200 bytes per zone
- Compiled filters: FastUtil IntSet for efficient role matching

### CPU Usage

- Zone lookup: O(1) via chunk index
- Containment check: O(1) for cuboid, O(1) for sphere (distance calc)
- Suppression application: O(zones × chunks) on reload

### Best Practices

1. Use cuboids when possible (fastest containment check)
2. Limit overlapping zones
3. Use BLOCK mode when filters aren't needed
4. Compile filters after any modification

---

## See Also

- [Admin Guide](ADMIN-GUIDE.md) - Administration guide
- [Mob Spawning](MOB-SPAWNING.md) - How Hytale spawning works
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues
