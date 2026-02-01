package dev.hypersystems.hyperspawns.zone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a spawn control zone with boundaries, mode, and filter settings.
 */
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
    
    /**
     * Create a new spawn zone with default settings.
     * 
     * @param id Unique identifier
     * @param name Human-readable name
     * @param world World name
     * @param boundary Zone boundary
     */
    public SpawnZone(@NotNull UUID id, @NotNull String name, @NotNull String world, @NotNull ZoneBoundary boundary) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.boundary = boundary;
        this.mode = ZoneMode.BLOCK;
        this.filter = ZoneFilter.empty();
        this.priority = 0;
        this.enabled = true;
        this.spawnRateMultiplier = 1.0;
        this.replacementNpc = null;
    }
    
    /**
     * Full constructor for deserialization.
     */
    public SpawnZone(
            @NotNull UUID id,
            @NotNull String name,
            @NotNull String world,
            @NotNull ZoneBoundary boundary,
            @NotNull ZoneMode mode,
            @NotNull ZoneFilter filter,
            int priority,
            boolean enabled,
            double spawnRateMultiplier,
            @Nullable String replacementNpc) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.boundary = boundary;
        this.mode = mode;
        this.filter = filter;
        this.priority = priority;
        this.enabled = enabled;
        this.spawnRateMultiplier = spawnRateMultiplier;
        this.replacementNpc = replacementNpc;
    }
    
    /**
     * Get the unique identifier for this zone.
     */
    @NotNull
    public UUID getId() {
        return id;
    }
    
    /**
     * Get the human-readable name of this zone.
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * Set the zone name.
     */
    public void setName(@NotNull String name) {
        this.name = name;
    }
    
    /**
     * Get the world this zone is in.
     */
    @NotNull
    public String getWorld() {
        return world;
    }
    
    /**
     * Set the world this zone is in.
     */
    public void setWorld(@NotNull String world) {
        this.world = world;
    }
    
    /**
     * Get the spatial boundary of this zone.
     */
    @NotNull
    public ZoneBoundary getBoundary() {
        return boundary;
    }
    
    /**
     * Set the zone boundary.
     */
    public void setBoundary(@NotNull ZoneBoundary boundary) {
        this.boundary = boundary;
    }
    
    /**
     * Get the spawn control mode.
     */
    @NotNull
    public ZoneMode getMode() {
        return mode;
    }
    
    /**
     * Set the spawn control mode.
     */
    public void setMode(@NotNull ZoneMode mode) {
        this.mode = mode;
    }
    
    /**
     * Get the spawn filter criteria.
     */
    @NotNull
    public ZoneFilter getFilter() {
        return filter;
    }
    
    /**
     * Set the spawn filter criteria.
     */
    public void setFilter(@NotNull ZoneFilter filter) {
        this.filter = filter;
    }
    
    /**
     * Get the priority of this zone (higher = checked first).
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Set the zone priority.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * Check if this zone is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enable or disable this zone.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the spawn rate multiplier (for MODIFY mode).
     * Values < 1.0 reduce spawn rate, > 1.0 increase spawn rate.
     */
    public double getSpawnRateMultiplier() {
        return spawnRateMultiplier;
    }
    
    /**
     * Set the spawn rate multiplier.
     * 
     * @param multiplier Value between 0.0 and 10.0
     */
    public void setSpawnRateMultiplier(double multiplier) {
        this.spawnRateMultiplier = Math.max(0.0, Math.min(10.0, multiplier));
    }
    
    /**
     * Get the replacement NPC type (for REPLACE mode).
     */
    @Nullable
    public String getReplacementNpc() {
        return replacementNpc;
    }
    
    /**
     * Set the replacement NPC type.
     */
    public void setReplacementNpc(@Nullable String replacementNpc) {
        this.replacementNpc = replacementNpc;
    }
    
    /**
     * Check if a point is contained within this zone's boundary.
     */
    public boolean contains(double x, double y, double z) {
        return boundary.contains(x, y, z);
    }
    
    /**
     * Check if this zone intersects with a chunk.
     */
    public boolean intersectsChunk(int chunkX, int chunkZ) {
        return boundary.intersectsChunk(chunkX, chunkZ);
    }
    
    /**
     * Check if this zone should block a spawn at the given position.
     * 
     * @param roleIndex The role index of the mob trying to spawn
     * @param lightLevel Current light level
     * @param yLevel Y coordinate
     * @param timeOfDay Current time of day
     * @param moonPhase Current moon phase
     * @return true if the spawn should be blocked
     */
    public boolean shouldBlockSpawn(int roleIndex, int lightLevel, int yLevel,
                                    @Nullable String timeOfDay, int moonPhase) {
        if (!enabled) {
            return false;
        }
        
        switch (mode) {
            case BLOCK:
                // Block all spawns in zone
                return true;
                
            case DENY:
                // Block only if filter matches
                return filter.matchesRole(roleIndex) &&
                       filter.matchesEnvironment(lightLevel, yLevel, timeOfDay, moonPhase);
                
            case ALLOW:
                // Block if filter does NOT match (only allow matching spawns)
                return !(filter.matchesRole(roleIndex) &&
                        filter.matchesEnvironment(lightLevel, yLevel, timeOfDay, moonPhase));
                
            case MODIFY:
            case REPLACE:
                // These modes don't directly block spawns
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Check if this zone should modify the spawn rate.
     */
    public boolean shouldModifySpawnRate(int roleIndex, int lightLevel, int yLevel,
                                         @Nullable String timeOfDay, int moonPhase) {
        if (!enabled || mode != ZoneMode.MODIFY) {
            return false;
        }
        return filter.matchesRole(roleIndex) &&
               filter.matchesEnvironment(lightLevel, yLevel, timeOfDay, moonPhase);
    }
    
    /**
     * Check if this zone should replace the spawning mob.
     */
    public boolean shouldReplace(int roleIndex, int lightLevel, int yLevel,
                                 @Nullable String timeOfDay, int moonPhase) {
        if (!enabled || mode != ZoneMode.REPLACE || replacementNpc == null) {
            return false;
        }
        return filter.matchesRole(roleIndex) &&
               filter.matchesEnvironment(lightLevel, yLevel, timeOfDay, moonPhase);
    }
    
    /**
     * Compile this zone's filter for runtime lookups.
     */
    public void compile(@NotNull ZoneFilter.RoleResolver resolver) {
        filter.compile(resolver);
    }
    
    @Override
    public String toString() {
        return "SpawnZone{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", world='" + world + '\'' +
                ", mode=" + mode +
                ", priority=" + priority +
                ", enabled=" + enabled +
                ", boundary=" + boundary.getType() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnZone spawnZone = (SpawnZone) o;
        return id.equals(spawnZone.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
