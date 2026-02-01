package dev.hypersystems.hyperspawns.zone;

import org.jetbrains.annotations.NotNull;

/**
 * A spherical boundary defined by a center point and radius.
 */
public final class SphereBoundary implements ZoneBoundary {
    
    public static final String TYPE = "sphere";
    
    private final double centerX, centerY, centerZ;
    private final double radius;
    private final double radiusSquared;
    
    /**
     * Create a spherical boundary.
     * 
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param centerZ Center Z coordinate
     * @param radius Radius of the sphere (must be positive)
     * @throws IllegalArgumentException if radius is not positive
     */
    public SphereBoundary(double centerX, double centerY, double centerZ, double radius) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + radius);
        }
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }
    
    @Override
    @NotNull
    public String getType() {
        return TYPE;
    }
    
    @Override
    public boolean contains(double x, double y, double z) {
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
    }
    
    @Override
    public boolean intersectsChunk(int chunkX, int chunkZ) {
        // Find the closest point in the chunk to the sphere center
        int chunkMinX = chunkX * 32;
        int chunkMaxX = chunkMinX + 31;
        int chunkMinZ = chunkZ * 32;
        int chunkMaxZ = chunkMinZ + 31;
        
        double closestX = Math.max(chunkMinX, Math.min(centerX, chunkMaxX));
        double closestZ = Math.max(chunkMinZ, Math.min(centerZ, chunkMaxZ));
        
        double dx = centerX - closestX;
        double dz = centerZ - closestZ;
        
        // Check if closest point is within radius (ignoring Y for chunk intersection)
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
        return centerY - radius;
    }
    
    @Override
    public double getMaxY() {
        return centerY + radius;
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
        return (4.0 / 3.0) * Math.PI * radius * radius * radius;
    }
    
    @Override
    @NotNull
    public String describe() {
        return String.format("Sphere at (%.1f, %.1f, %.1f) with radius %.1f",
                centerX, centerY, centerZ, radius);
    }
    
    /**
     * Get the center X coordinate.
     */
    public double getCenterX() {
        return centerX;
    }
    
    /**
     * Get the center Y coordinate.
     */
    public double getCenterY() {
        return centerY;
    }
    
    /**
     * Get the center Z coordinate.
     */
    public double getCenterZ() {
        return centerZ;
    }
    
    /**
     * Get the radius of this sphere.
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
    
    @Override
    public String toString() {
        return "SphereBoundary{" +
                "center=(" + centerX + ", " + centerY + ", " + centerZ + ")" +
                ", radius=" + radius +
                '}';
    }
}
