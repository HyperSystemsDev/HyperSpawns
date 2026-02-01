package dev.hypersystems.hyperspawns.system;

import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hypersystems.hyperspawns.util.ChunkIndexer;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.*;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * World-level ECS resource that stores compiled zone suppression data
 * for efficient spawn checking.
 */
public class SpawnZoneSuppressionResource implements Resource<EntityStore> {
    
    // Chunk index -> list of suppression spans affecting that chunk
    private final Long2ObjectMap<List<ZoneSuppressionSpan>> chunkSuppressionSpans;
    
    // Reference to the zone manager for rebuilding
    private final String worldName;
    
    public SpawnZoneSuppressionResource(@NotNull String worldName) {
        this.worldName = worldName;
        this.chunkSuppressionSpans = new Long2ObjectOpenHashMap<>();
    }
    
    /**
     * Rebuild the suppression data from the zone manager.
     * Call this when zones are added, removed, or modified.
     */
    public void rebuild(@NotNull SpawnZoneManager zoneManager) {
        chunkSuppressionSpans.clear();
        
        Collection<SpawnZone> zones = zoneManager.getAllZones();
        int spansCreated = 0;
        
        for (SpawnZone zone : zones) {
            if (!zone.getWorld().equals(worldName) || !zone.isEnabled()) {
                continue;
            }
            
            ZoneBoundary boundary = zone.getBoundary();
            int minChunkX = boundary.getMinChunkX();
            int maxChunkX = boundary.getMaxChunkX();
            int minChunkZ = boundary.getMinChunkZ();
            int maxChunkZ = boundary.getMaxChunkZ();
            
            // Create a span for this zone
            ZoneSuppressionSpan span = new ZoneSuppressionSpan(
                    zone.getId(),
                    (int) Math.floor(boundary.getMinY()),
                    (int) Math.ceil(boundary.getMaxY()),
                    zone.getMode(),
                    zone.getFilter(),
                    zone.getPriority(),
                    zone.getSpawnRateMultiplier(),
                    zone.getReplacementNpc(),
                    boundary
            );
            
            // Add span to each affected chunk
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    if (boundary.intersectsChunk(cx, cz)) {
                        long chunkIndex = ChunkIndexer.indexChunk(cx, cz);
                        chunkSuppressionSpans.computeIfAbsent(chunkIndex, k -> new ArrayList<>())
                                .add(span);
                        spansCreated++;
                    }
                }
            }
        }
        
        // Sort spans by priority (highest first) and minY
        for (List<ZoneSuppressionSpan> spans : chunkSuppressionSpans.values()) {
            spans.sort((a, b) -> {
                int priorityCompare = Integer.compare(b.priority(), a.priority());
                if (priorityCompare != 0) return priorityCompare;
                return Integer.compare(a.minY(), b.minY());
            });
        }
        
        Logger.debug("Rebuilt suppression resource for world '%s': %d zones, %d chunk-spans", 
                worldName, zones.size(), spansCreated);
    }
    
    /**
     * Get the suppression spans affecting a chunk.
     * 
     * @param chunkIndex The chunk index
     * @return List of spans, or empty list if none
     */
    @NotNull
    public List<ZoneSuppressionSpan> getSpansForChunk(long chunkIndex) {
        List<ZoneSuppressionSpan> spans = chunkSuppressionSpans.get(chunkIndex);
        return spans != null ? spans : Collections.emptyList();
    }
    
    /**
     * Get the suppression spans affecting a chunk.
     */
    @NotNull
    public List<ZoneSuppressionSpan> getSpansForChunk(int chunkX, int chunkZ) {
        return getSpansForChunk(ChunkIndexer.indexChunk(chunkX, chunkZ));
    }
    
    /**
     * Check if a spawn should be blocked at the given position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param roleIndex NPC role index
     * @param lightLevel Current light level
     * @param timeOfDay Current time of day
     * @param moonPhase Current moon phase
     * @return true if spawn should be blocked
     */
    public boolean shouldBlockSpawn(double x, double y, double z, int roleIndex, 
                                    int lightLevel, @Nullable String timeOfDay, int moonPhase) {
        long chunkIndex = ChunkIndexer.indexChunkFromBlock(x, z);
        List<ZoneSuppressionSpan> spans = getSpansForChunk(chunkIndex);
        
        if (spans.isEmpty()) {
            return false;
        }
        
        int yInt = (int) Math.floor(y);
        
        for (ZoneSuppressionSpan span : spans) {
            // Check Y range
            if (yInt < span.minY() || yInt > span.maxY()) {
                continue;
            }
            
            // Check precise boundary containment
            if (!span.boundary().contains(x, y, z)) {
                continue;
            }
            
            ZoneMode mode = span.mode();
            ZoneFilter filter = span.filter();
            
            switch (mode) {
                case BLOCK:
                    // Block all spawns in zone
                    return true;
                    
                case DENY:
                    // Block if filter matches
                    if (filter.matchesRole(roleIndex) &&
                        filter.matchesEnvironment(lightLevel, yInt, timeOfDay, moonPhase)) {
                        return true;
                    }
                    break;
                    
                case ALLOW:
                    // This is a whitelist zone - allow matching spawns, block others
                    if (filter.matchesRole(roleIndex) &&
                        filter.matchesEnvironment(lightLevel, yInt, timeOfDay, moonPhase)) {
                        return false; // Explicitly allowed
                    } else {
                        return true; // Not in whitelist, block
                    }
                    
                case MODIFY:
                case REPLACE:
                    // These don't block spawns directly
                    break;
            }
        }
        
        return false;
    }
    
    /**
     * Get the spawn rate modifier at a position.
     * 
     * @return The combined multiplier, or 1.0 if no modification
     */
    public double getSpawnRateModifier(double x, double y, double z, int roleIndex,
                                       int lightLevel, @Nullable String timeOfDay, int moonPhase) {
        long chunkIndex = ChunkIndexer.indexChunkFromBlock(x, z);
        List<ZoneSuppressionSpan> spans = getSpansForChunk(chunkIndex);
        
        if (spans.isEmpty()) {
            return 1.0;
        }
        
        int yInt = (int) Math.floor(y);
        double multiplier = 1.0;
        
        for (ZoneSuppressionSpan span : spans) {
            if (span.mode() != ZoneMode.MODIFY) {
                continue;
            }
            
            if (yInt < span.minY() || yInt > span.maxY()) {
                continue;
            }
            
            if (!span.boundary().contains(x, y, z)) {
                continue;
            }
            
            ZoneFilter filter = span.filter();
            if (filter.matchesRole(roleIndex) &&
                filter.matchesEnvironment(lightLevel, yInt, timeOfDay, moonPhase)) {
                multiplier *= span.spawnRateMultiplier();
            }
        }
        
        return multiplier;
    }
    
    /**
     * Get the replacement NPC for a spawn, if any.
     * 
     * @return The replacement NPC ID, or null if no replacement
     */
    @Nullable
    public String getReplacementNpc(double x, double y, double z, int roleIndex,
                                    int lightLevel, @Nullable String timeOfDay, int moonPhase) {
        long chunkIndex = ChunkIndexer.indexChunkFromBlock(x, z);
        List<ZoneSuppressionSpan> spans = getSpansForChunk(chunkIndex);
        
        if (spans.isEmpty()) {
            return null;
        }
        
        int yInt = (int) Math.floor(y);
        
        for (ZoneSuppressionSpan span : spans) {
            if (span.mode() != ZoneMode.REPLACE || span.replacementNpc() == null) {
                continue;
            }
            
            if (yInt < span.minY() || yInt > span.maxY()) {
                continue;
            }
            
            if (!span.boundary().contains(x, y, z)) {
                continue;
            }
            
            ZoneFilter filter = span.filter();
            if (filter.matchesRole(roleIndex) &&
                filter.matchesEnvironment(lightLevel, yInt, timeOfDay, moonPhase)) {
                return span.replacementNpc();
            }
        }
        
        return null;
    }
    
    /**
     * Get the world name this resource is for.
     */
    @NotNull
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Get statistics about this resource.
     */
    @NotNull
    public String getStats() {
        int totalSpans = 0;
        for (List<ZoneSuppressionSpan> spans : chunkSuppressionSpans.values()) {
            totalSpans += spans.size();
        }
        return String.format("Chunks indexed: %d, Total spans: %d", 
                chunkSuppressionSpans.size(), totalSpans);
    }
    
    @NotNull
    @Override
    public Resource<EntityStore> clone() {
        SpawnZoneSuppressionResource clone = new SpawnZoneSuppressionResource(worldName);
        clone.chunkSuppressionSpans.putAll(this.chunkSuppressionSpans);
        return clone;
    }
    
    /**
     * Represents a zone's suppression data for a chunk.
     */
    public record ZoneSuppressionSpan(
            UUID zoneId,
            int minY,
            int maxY,
            ZoneMode mode,
            ZoneFilter filter,
            int priority,
            double spawnRateMultiplier,
            @Nullable String replacementNpc,
            ZoneBoundary boundary
    ) {}
}
