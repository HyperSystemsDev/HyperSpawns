package dev.hypersystems.hyperspawns.zone;

import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface representing a spatial boundary for spawn zones.
 * Implementations must be one of the permitted types.
 */
public sealed interface ZoneBoundary permits CuboidBoundary, SphereBoundary, CylinderBoundary {
    
    /**
     * Get the type identifier for this boundary.
     */
    @NotNull
    String getType();
    
    /**
     * Check if a point is contained within this boundary.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if the point is inside the boundary
     */
    boolean contains(double x, double y, double z);
    
    /**
     * Check if this boundary intersects with a chunk.
     * Uses chunk coordinates (not block coordinates).
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return true if the boundary intersects the chunk
     */
    boolean intersectsChunk(int chunkX, int chunkZ);
    
    /**
     * Get the minimum X coordinate of this boundary's bounding box.
     */
    double getMinX();
    
    /**
     * Get the maximum X coordinate of this boundary's bounding box.
     */
    double getMaxX();
    
    /**
     * Get the minimum Y coordinate of this boundary's bounding box.
     */
    double getMinY();
    
    /**
     * Get the maximum Y coordinate of this boundary's bounding box.
     */
    double getMaxY();
    
    /**
     * Get the minimum Z coordinate of this boundary's bounding box.
     */
    double getMinZ();
    
    /**
     * Get the maximum Z coordinate of this boundary's bounding box.
     */
    double getMaxZ();
    
    /**
     * Get the minimum chunk X coordinate this boundary can intersect.
     */
    default int getMinChunkX() {
        return (int) Math.floor(getMinX() / 32.0);
    }
    
    /**
     * Get the maximum chunk X coordinate this boundary can intersect.
     */
    default int getMaxChunkX() {
        return (int) Math.floor(getMaxX() / 32.0);
    }
    
    /**
     * Get the minimum chunk Z coordinate this boundary can intersect.
     */
    default int getMinChunkZ() {
        return (int) Math.floor(getMinZ() / 32.0);
    }
    
    /**
     * Get the maximum chunk Z coordinate this boundary can intersect.
     */
    default int getMaxChunkZ() {
        return (int) Math.floor(getMaxZ() / 32.0);
    }
    
    /**
     * Get the volume of this boundary in cubic blocks.
     */
    double getVolume();
    
    /**
     * Get a human-readable description of this boundary.
     */
    @NotNull
    String describe();
}
