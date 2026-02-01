package dev.hypersystems.hyperspawns.zone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the spawn control mode for a zone.
 */
public enum ZoneMode {
    
    /**
     * Completely blocks all mob spawning in the zone.
     * No mobs can spawn regardless of filter settings.
     */
    BLOCK("block", "Blocks all mob spawning"),
    
    /**
     * Only allows mobs matching the filter to spawn.
     * All other mobs are prevented from spawning.
     */
    ALLOW("allow", "Only allows filtered mobs to spawn"),
    
    /**
     * Prevents mobs matching the filter from spawning.
     * All other mobs can spawn normally.
     */
    DENY("deny", "Prevents filtered mobs from spawning"),
    
    /**
     * Modifies the spawn rate of filtered mobs.
     * Uses the zone's spawnRateMultiplier setting.
     */
    MODIFY("modify", "Modifies spawn rate of filtered mobs"),
    
    /**
     * Replaces spawning mobs with a different NPC type.
     * Uses the zone's replacementNpc setting.
     */
    REPLACE("replace", "Replaces filtered mobs with another type");
    
    private final String id;
    private final String description;
    
    ZoneMode(@NotNull String id, @NotNull String description) {
        this.id = id;
        this.description = description;
    }
    
    /**
     * Get the lowercase identifier for this mode.
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * Get the human-readable description of this mode.
     */
    @NotNull
    public String getDescription() {
        return description;
    }
    
    /**
     * Parse a zone mode from a string (case-insensitive).
     * 
     * @param value The string to parse
     * @return The matching ZoneMode, or null if not found
     */
    @Nullable
    public static ZoneMode fromString(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        try {
            return ZoneMode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by ID
            for (ZoneMode mode : values()) {
                if (mode.id.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return null;
        }
    }
    
    /**
     * Check if this mode blocks spawning entirely.
     */
    public boolean blocksSpawning() {
        return this == BLOCK;
    }
    
    /**
     * Check if this mode requires filter matching.
     */
    public boolean requiresFilter() {
        return this == ALLOW || this == DENY || this == MODIFY || this == REPLACE;
    }
    
    /**
     * Check if this mode modifies spawn rates.
     */
    public boolean modifiesSpawnRate() {
        return this == MODIFY;
    }
    
    /**
     * Check if this mode replaces mob types.
     */
    public boolean replacesNpc() {
        return this == REPLACE;
    }
}
