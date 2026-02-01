package dev.hypersystems.hyperspawns.zone;

import dev.hypersystems.hyperspawns.util.ChunkIndexer;
import dev.hypersystems.hyperspawns.util.Logger;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages all spawn zones with efficient spatial indexing for fast lookups.
 */
public final class SpawnZoneManager {
    
    // Primary storage: zone ID -> zone
    private final Map<UUID, SpawnZone> zonesById = new ConcurrentHashMap<>();
    
    // Name index: lowercase name -> zone
    private final Map<String, SpawnZone> zonesByName = new ConcurrentHashMap<>();
    
    // Spatial index: world -> chunk index -> set of zone IDs
    private final Map<String, Long2ObjectMap<Set<UUID>>> chunkIndexByWorld = new ConcurrentHashMap<>();
    
    // Lock for spatial index updates
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    
    // Dirty flag for persistence
    private volatile boolean dirty = false;
    
    // Listener for zone changes
    private Consumer<SpawnZone> changeListener;
    
    /**
     * Create a new zone manager.
     */
    public SpawnZoneManager() {
    }
    
    /**
     * Set a listener that's called when zones are created, modified, or deleted.
     */
    public void setChangeListener(@Nullable Consumer<SpawnZone> listener) {
        this.changeListener = listener;
    }
    
    /**
     * Create a new zone.
     * 
     * @param name Zone name (must be unique)
     * @param world World name
     * @param boundary Zone boundary
     * @return The created zone, or null if name already exists
     */
    @Nullable
    public SpawnZone createZone(@NotNull String name, @NotNull String world, @NotNull ZoneBoundary boundary) {
        String normalizedName = name.toLowerCase();
        
        if (zonesByName.containsKey(normalizedName)) {
            Logger.warning("Zone with name '%s' already exists", name);
            return null;
        }
        
        UUID id = UUID.randomUUID();
        SpawnZone zone = new SpawnZone(id, name, world, boundary);
        
        zonesById.put(id, zone);
        zonesByName.put(normalizedName, zone);
        indexZone(zone);
        
        markDirty();
        notifyChange(zone);
        
        Logger.info("Created zone '%s' in world '%s'", name, world);
        return zone;
    }
    
    /**
     * Delete a zone by ID.
     * 
     * @param id Zone ID
     * @return true if deleted, false if not found
     */
    public boolean deleteZone(@NotNull UUID id) {
        SpawnZone zone = zonesById.remove(id);
        if (zone == null) {
            return false;
        }
        
        zonesByName.remove(zone.getName().toLowerCase());
        unindexZone(zone);
        
        markDirty();
        notifyChange(zone);
        
        Logger.info("Deleted zone '%s'", zone.getName());
        return true;
    }
    
    /**
     * Delete a zone by name.
     * 
     * @param name Zone name (case-insensitive)
     * @return true if deleted, false if not found
     */
    public boolean deleteZone(@NotNull String name) {
        SpawnZone zone = zonesByName.get(name.toLowerCase());
        if (zone == null) {
            return false;
        }
        return deleteZone(zone.getId());
    }
    
    /**
     * Get a zone by ID.
     */
    @Nullable
    public SpawnZone getZone(@NotNull UUID id) {
        return zonesById.get(id);
    }
    
    /**
     * Get a zone by name (case-insensitive).
     */
    @Nullable
    public SpawnZone getZone(@NotNull String name) {
        return zonesByName.get(name.toLowerCase());
    }
    
    /**
     * Check if a zone with the given name exists.
     */
    public boolean zoneExists(@NotNull String name) {
        return zonesByName.containsKey(name.toLowerCase());
    }
    
    /**
     * Get all zones.
     */
    @NotNull
    public Collection<SpawnZone> getAllZones() {
        return Collections.unmodifiableCollection(zonesById.values());
    }
    
    /**
     * Get all zones in a world.
     */
    @NotNull
    public List<SpawnZone> getZonesInWorld(@NotNull String world) {
        return zonesById.values().stream()
                .filter(z -> z.getWorld().equals(world))
                .collect(Collectors.toList());
    }
    
    /**
     * Get zones that contain a specific point, sorted by priority (highest first).
     * 
     * @param world World name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return List of zones containing the point, sorted by priority
     */
    @NotNull
    public List<SpawnZone> getZonesAtPoint(@NotNull String world, double x, double y, double z) {
        long chunkIndex = ChunkIndexer.indexChunkFromBlock(x, z);
        Set<UUID> candidateIds = getZonesInChunkInternal(world, chunkIndex);
        
        if (candidateIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<SpawnZone> matching = new ArrayList<>();
        for (UUID id : candidateIds) {
            SpawnZone zone = zonesById.get(id);
            if (zone != null && zone.isEnabled() && zone.contains(x, y, z)) {
                matching.add(zone);
            }
        }
        
        // Sort by priority (highest first)
        matching.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return matching;
    }
    
    /**
     * Get the highest priority zone at a point.
     */
    @Nullable
    public SpawnZone getHighestPriorityZoneAtPoint(@NotNull String world, double x, double y, double z) {
        List<SpawnZone> zones = getZonesAtPoint(world, x, y, z);
        return zones.isEmpty() ? null : zones.getFirst();
    }
    
    /**
     * Get zone IDs that intersect with a chunk.
     */
    @NotNull
    public Set<UUID> getZonesInChunk(@NotNull String world, int chunkX, int chunkZ) {
        return getZonesInChunkInternal(world, ChunkIndexer.indexChunk(chunkX, chunkZ));
    }
    
    /**
     * Get zone IDs that intersect with a chunk.
     */
    @NotNull
    public Set<UUID> getZonesInChunk(@NotNull String world, long chunkIndex) {
        return getZonesInChunkInternal(world, chunkIndex);
    }
    
    @NotNull
    private Set<UUID> getZonesInChunkInternal(@NotNull String world, long chunkIndex) {
        indexLock.readLock().lock();
        try {
            Long2ObjectMap<Set<UUID>> worldIndex = chunkIndexByWorld.get(world);
            if (worldIndex == null) {
                return Collections.emptySet();
            }
            Set<UUID> zoneIds = worldIndex.get(chunkIndex);
            return zoneIds != null ? new HashSet<>(zoneIds) : Collections.emptySet();
        } finally {
            indexLock.readLock().unlock();
        }
    }
    
    /**
     * Update a zone's boundary and reindex.
     */
    public void updateBoundary(@NotNull SpawnZone zone, @NotNull ZoneBoundary newBoundary) {
        unindexZone(zone);
        zone.setBoundary(newBoundary);
        indexZone(zone);
        markDirty();
        notifyChange(zone);
    }
    
    /**
     * Update a zone's world and reindex.
     */
    public void updateWorld(@NotNull SpawnZone zone, @NotNull String newWorld) {
        unindexZone(zone);
        zone.setWorld(newWorld);
        indexZone(zone);
        markDirty();
        notifyChange(zone);
    }
    
    /**
     * Mark a zone as modified (triggers dirty flag and change notification).
     */
    public void markModified(@NotNull SpawnZone zone) {
        markDirty();
        notifyChange(zone);
    }
    
    /**
     * Index a zone in the spatial index.
     */
    private void indexZone(@NotNull SpawnZone zone) {
        indexLock.writeLock().lock();
        try {
            String world = zone.getWorld();
            Long2ObjectMap<Set<UUID>> worldIndex = chunkIndexByWorld.computeIfAbsent(
                    world, w -> new Long2ObjectOpenHashMap<>());
            
            ZoneBoundary boundary = zone.getBoundary();
            int minChunkX = boundary.getMinChunkX();
            int maxChunkX = boundary.getMaxChunkX();
            int minChunkZ = boundary.getMinChunkZ();
            int maxChunkZ = boundary.getMaxChunkZ();
            
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    if (boundary.intersectsChunk(cx, cz)) {
                        long chunkIndex = ChunkIndexer.indexChunk(cx, cz);
                        worldIndex.computeIfAbsent(chunkIndex, k -> ConcurrentHashMap.newKeySet())
                                .add(zone.getId());
                    }
                }
            }
            
            Logger.trace("Indexed zone '%s' across %d chunk(s)", 
                    zone.getName(), (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1));
        } finally {
            indexLock.writeLock().unlock();
        }
    }
    
    /**
     * Remove a zone from the spatial index.
     */
    private void unindexZone(@NotNull SpawnZone zone) {
        indexLock.writeLock().lock();
        try {
            String world = zone.getWorld();
            Long2ObjectMap<Set<UUID>> worldIndex = chunkIndexByWorld.get(world);
            if (worldIndex == null) {
                return;
            }
            
            ZoneBoundary boundary = zone.getBoundary();
            int minChunkX = boundary.getMinChunkX();
            int maxChunkX = boundary.getMaxChunkX();
            int minChunkZ = boundary.getMinChunkZ();
            int maxChunkZ = boundary.getMaxChunkZ();
            
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    long chunkIndex = ChunkIndexer.indexChunk(cx, cz);
                    Set<UUID> zoneIds = worldIndex.get(chunkIndex);
                    if (zoneIds != null) {
                        zoneIds.remove(zone.getId());
                        if (zoneIds.isEmpty()) {
                            worldIndex.remove(chunkIndex);
                        }
                    }
                }
            }
        } finally {
            indexLock.writeLock().unlock();
        }
    }
    
    /**
     * Reindex a zone (call after modifying boundary or world).
     */
    public void reindexZone(@NotNull SpawnZone zone) {
        unindexZone(zone);
        indexZone(zone);
    }
    
    /**
     * Load zones from a collection (clears existing zones).
     */
    public void loadZones(@NotNull Collection<SpawnZone> zones) {
        clear();
        for (SpawnZone zone : zones) {
            zonesById.put(zone.getId(), zone);
            zonesByName.put(zone.getName().toLowerCase(), zone);
            indexZone(zone);
        }
        dirty = false;
        Logger.info("Loaded %d zones", zones.size());
    }
    
    /**
     * Get all zones for saving.
     */
    @NotNull
    public Collection<SpawnZone> getZonesForSave() {
        return getAllZones();
    }
    
    /**
     * Clear all zones.
     */
    public void clear() {
        zonesById.clear();
        zonesByName.clear();
        indexLock.writeLock().lock();
        try {
            chunkIndexByWorld.clear();
        } finally {
            indexLock.writeLock().unlock();
        }
    }
    
    /**
     * Mark that data has changed and needs to be saved.
     */
    public void markDirty() {
        dirty = true;
    }
    
    /**
     * Check if data has changed since last save.
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Clear the dirty flag (call after saving).
     */
    public void clearDirty() {
        dirty = false;
    }
    
    /**
     * Get the total number of zones.
     */
    public int getZoneCount() {
        return zonesById.size();
    }
    
    /**
     * Get statistics about the zone manager.
     */
    @NotNull
    public String getStats() {
        int totalChunks = 0;
        indexLock.readLock().lock();
        try {
            for (Long2ObjectMap<Set<UUID>> worldIndex : chunkIndexByWorld.values()) {
                totalChunks += worldIndex.size();
            }
        } finally {
            indexLock.readLock().unlock();
        }
        
        return String.format("Zones: %d, Worlds: %d, Indexed chunks: %d",
                zonesById.size(), chunkIndexByWorld.size(), totalChunks);
    }
    
    private void notifyChange(@NotNull SpawnZone zone) {
        if (changeListener != null) {
            try {
                changeListener.accept(zone);
            } catch (Exception e) {
                Logger.warning("Error notifying zone change listener: %s", e.getMessage());
            }
        }
    }
    
    /**
     * Compile all zone filters for runtime lookups.
     */
    public void compileAllFilters(@NotNull ZoneFilter.RoleResolver resolver) {
        for (SpawnZone zone : zonesById.values()) {
            zone.compile(resolver);
        }
        Logger.debug("Compiled filters for %d zones", zonesById.size());
    }
}
