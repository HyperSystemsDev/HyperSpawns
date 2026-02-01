package dev.hypersystems.hyperspawns.zone;

import org.jetbrains.annotations.NotNull;

/**
 * A cylindrical boundary defined by center XZ, radius, and Y range.
 * The cylinder extends vertically from minY to maxY.
 */
public final class CylinderBoundary implements ZoneBoundary {
    
    public static final String TYPE = "cylinder";
    
    private final double centerX, centerZ;
    private final double radius;
    private final double radiusSquared;
    private final double minY, maxY;
    
    /**
     * Create a cylindrical boundary.
     * 
     * @param centerX Center X coordinate
     * @param centerZ Center Z coordinate
     * @param radius Radius of the cylinder (must be positive)
     * @param y1 First Y coordinate (min or max)
     * @param y2 Second Y coordinate (min or max)
     * @throws IllegalArgumentException if radius is not positive
     */
    public CylinderBoundary(double centerX, double centerZ, double radius, double y1, double y2) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + radius);
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;
        this.minY = Math.min(y1, y2);
        this.maxY = Math.max(y1, y2);
    }
    
    @Override
    @NotNull
    public String getType() {
        return TYPE;
    }
    
    @Override
    public boolean contains(double x, double y, double z) {
        // Check Y range first (cheap)
        if (y < minY || y > maxY) {
            return false;
        }
        
        // Check distance from center axis (XZ plane)
        double dx = x - centerX;
        double dz = z - centerZ;
        return (dx * dx + dz * dz) <= radiusSquared;
    }
    
    @Override
    public boolean intersectsChunk(int chunkX, int chunkZ) {
        // Find the closest point in the chunk to the cylinder center
        int chunkMinX = chunkX * 32;
        int chunkMaxX = chunkMinX + 31;
        int chunkMinZ = chunkZ * 32;
        int chunkMaxZ = chunkMinZ + 31;
        
        double closestX = Math.max(chunkMinX, Math.min(centerX, chunkMaxX));
        double closestZ = Math.max(chunkMinZ, Math.min(centerZ, chunkMaxZ));
        
        double dx = centerX - closestX;
        double dz = centerZ - closestZ;
        
        return (dx * dx + dz * dz) <= radiusSquared;
    }
    
    @Override
    public double getMinX() {
        return centerX - radius;
    }
    
    @Override
    public double getMaxX() {
        return centerX + radius;
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
        return centerZ - radius;
    }
    
    @Override
    public double getMaxZ() {
        return centerZ + radius;
    }
    
    @Override
    public double getVolume() {
        double height = maxY - minY;
        return Math.PI * radiusSquared * height;
    }
    
    @Override
    @NotNull
    public String describe() {
        return String.format("Cylinder at (%.1f, %.1f) with radius %.1f, Y: %.1f to %.1f",
                centerX, centerZ, radius, minY, maxY);
    }
    
    /**
     * Get the center X coordinate.
     */
    public double getCenterX() {
        return centerX;
    }
    
    /**
     * Get the center Z coordinate.
     */
    public double getCenterZ() {
        return centerZ;
    }
    
    /**
     * Get the radius of this cylinder.
     */
    public double getRadius() {
        return radius;
    }
    
    /**
     * Get the squared radius (useful for distance comparisons without sqrt).
     */
    public double getRadiusSquared() {
        return radiusSquared;
    }
    
    /**
     * Get the height of the cylinder.
     */
    public double getHeight() {
        return maxY - minY;
    }
    
    /**
     * Get the center Y coordinate.
     */
    public double getCenterY() {
        return (minY + maxY) / 2.0;
    }
    
    @Override
    public String toString() {
        return "CylinderBoundary{" +
                "center=(" + centerX + ", " + centerZ + ")" +
                ", radius=" + radius +
                ", y=[" + minY + ", " + maxY + "]" +
                '}';
    }
}
