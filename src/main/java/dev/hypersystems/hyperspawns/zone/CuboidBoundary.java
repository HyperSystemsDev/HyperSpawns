package dev.hypersystems.hyperspawns.zone;

import org.jetbrains.annotations.NotNull;

/**
 * A rectangular cuboid (box) boundary defined by two corner points.
 */
public final class CuboidBoundary implements ZoneBoundary {
    
    public static final String TYPE = "cuboid";
    
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;
    
    /**
     * Create a cuboid boundary from two corner points.
     * Coordinates are automatically normalized so min <= max.
     */
    public CuboidBoundary(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }
    
    @Override
    @NotNull
    public String getType() {
        return TYPE;
    }
    
    @Override
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    @Override
    public boolean intersectsChunk(int chunkX, int chunkZ) {
        // Chunk block coordinates
        int chunkMinX = chunkX * 32;
        int chunkMaxX = chunkMinX + 31;
        int chunkMinZ = chunkZ * 32;
        int chunkMaxZ = chunkMinZ + 31;
        
        // Check for overlap on X and Z axes
        return maxX >= chunkMinX && minX <= chunkMaxX &&
               maxZ >= chunkMinZ && minZ <= chunkMaxZ;
    }
    
    @Override
    public double getMinX() {
        return minX;
    }
    
    @Override
    public double getMaxX() {
        return maxX;
    }
    
    @Override
    public double getMinY() {
        return minY;
    }
    
    @Override
    public double getMaxY() {
        return maxY;
    }
    
    @Override
    public double getMinZ() {
        return minZ;
    }
    
    @Override
    public double getMaxZ() {
        return maxZ;
    }
    
    @Override
    public double getVolume() {
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }
    
    @Override
    @NotNull
    public String describe() {
        return String.format("Cuboid from (%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f)",
                minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Get the width (X dimension) of the cuboid.
     */
    public double getWidth() {
        return maxX - minX;
    }
    
    /**
     * Get the height (Y dimension) of the cuboid.
     */
    public double getHeight() {
        return maxY - minY;
    }
    
    /**
     * Get the depth (Z dimension) of the cuboid.
     */
    public double getDepth() {
        return maxZ - minZ;
    }
    
    /**
     * Get the center X coordinate.
     */
    public double getCenterX() {
        return (minX + maxX) / 2.0;
    }
    
    /**
     * Get the center Y coordinate.
     */
    public double getCenterY() {
        return (minY + maxY) / 2.0;
    }
    
    /**
     * Get the center Z coordinate.
     */
    public double getCenterZ() {
        return (minZ + maxZ) / 2.0;
    }
    
    @Override
    public String toString() {
        return "CuboidBoundary{" +
                "min=(" + minX + ", " + minY + ", " + minZ + ")" +
                ", max=(" + maxX + ", " + maxY + ", " + maxZ + ")" +
                '}';
    }
}
