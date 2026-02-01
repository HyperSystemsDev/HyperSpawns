package dev.hypersystems.hyperspawns.command;

import dev.hypersystems.hyperspawns.zone.CuboidBoundary;
import dev.hypersystems.hyperspawns.zone.ZoneBoundary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages wand-based position selections for players.
 * Used for defining zone boundaries.
 */
public final class WandSelectionManager {
    
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();
    
    /**
     * Set position 1 for a player.
     * 
     * @param playerId Player UUID
     * @param world World name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setPos1(@NotNull UUID playerId, @NotNull String world, double x, double y, double z) {
        Selection selection = selections.computeIfAbsent(playerId, k -> new Selection());
        selection.world = world;
        selection.pos1X = x;
        selection.pos1Y = y;
        selection.pos1Z = z;
        selection.pos1Set = true;
    }
    
    /**
     * Set position 2 for a player.
     * 
     * @param playerId Player UUID
     * @param world World name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setPos2(@NotNull UUID playerId, @NotNull String world, double x, double y, double z) {
        Selection selection = selections.computeIfAbsent(playerId, k -> new Selection());
        // If world differs from pos1, update the world (pos1 will need to be reset)
        if (selection.pos1Set && !world.equals(selection.world)) {
            selection.pos1Set = false;
        }
        selection.world = world;
        selection.pos2X = x;
        selection.pos2Y = y;
        selection.pos2Z = z;
        selection.pos2Set = true;
    }
    
    /**
     * Get the current selection for a player.
     * 
     * @param playerId Player UUID
     * @return The selection, or null if no selection exists
     */
    @Nullable
    public Selection getSelection(@NotNull UUID playerId) {
        return selections.get(playerId);
    }
    
    /**
     * Check if a player has a complete selection (both positions set).
     */
    public boolean hasCompleteSelection(@NotNull UUID playerId) {
        Selection selection = selections.get(playerId);
        return selection != null && selection.isComplete();
    }
    
    /**
     * Get a cuboid boundary from the player's selection.
     * 
     * @param playerId Player UUID
     * @return The boundary, or null if selection is incomplete
     */
    @Nullable
    public CuboidBoundary getCuboidBoundary(@NotNull UUID playerId) {
        Selection selection = selections.get(playerId);
        if (selection == null || !selection.isComplete()) {
            return null;
        }
        return new CuboidBoundary(
                selection.pos1X, selection.pos1Y, selection.pos1Z,
                selection.pos2X, selection.pos2Y, selection.pos2Z
        );
    }
    
    /**
     * Get the world of the player's selection.
     * 
     * @param playerId Player UUID
     * @return The world name, or null if no selection
     */
    @Nullable
    public String getSelectionWorld(@NotNull UUID playerId) {
        Selection selection = selections.get(playerId);
        return selection != null ? selection.world : null;
    }
    
    /**
     * Clear a player's selection.
     * 
     * @param playerId Player UUID
     */
    public void clearSelection(@NotNull UUID playerId) {
        selections.remove(playerId);
    }
    
    /**
     * Get the number of active selections.
     */
    public int getSelectionCount() {
        return selections.size();
    }
    
    /**
     * Represents a player's region selection.
     */
    public static final class Selection {
        private String world;
        private double pos1X, pos1Y, pos1Z;
        private double pos2X, pos2Y, pos2Z;
        private boolean pos1Set;
        private boolean pos2Set;
        
        /**
         * Check if both positions are set.
         */
        public boolean isComplete() {
            return pos1Set && pos2Set;
        }
        
        /**
         * Check if position 1 is set.
         */
        public boolean isPos1Set() {
            return pos1Set;
        }
        
        /**
         * Check if position 2 is set.
         */
        public boolean isPos2Set() {
            return pos2Set;
        }
        
        /**
         * Get the world name.
         */
        @Nullable
        public String getWorld() {
            return world;
        }
        
        /**
         * Get position 1 X coordinate.
         */
        public double getPos1X() {
            return pos1X;
        }
        
        /**
         * Get position 1 Y coordinate.
         */
        public double getPos1Y() {
            return pos1Y;
        }
        
        /**
         * Get position 1 Z coordinate.
         */
        public double getPos1Z() {
            return pos1Z;
        }
        
        /**
         * Get position 2 X coordinate.
         */
        public double getPos2X() {
            return pos2X;
        }
        
        /**
         * Get position 2 Y coordinate.
         */
        public double getPos2Y() {
            return pos2Y;
        }
        
        /**
         * Get position 2 Z coordinate.
         */
        public double getPos2Z() {
            return pos2Z;
        }
        
        /**
         * Get the width (X dimension) of the selection.
         */
        public double getWidth() {
            return Math.abs(pos2X - pos1X);
        }
        
        /**
         * Get the height (Y dimension) of the selection.
         */
        public double getHeight() {
            return Math.abs(pos2Y - pos1Y);
        }
        
        /**
         * Get the depth (Z dimension) of the selection.
         */
        public double getDepth() {
            return Math.abs(pos2Z - pos1Z);
        }
        
        /**
         * Get the volume of the selection in cubic blocks.
         */
        public double getVolume() {
            if (!isComplete()) {
                return 0;
            }
            return (getWidth() + 1) * (getHeight() + 1) * (getDepth() + 1);
        }
        
        /**
         * Get the center X coordinate.
         */
        public double getCenterX() {
            return (pos1X + pos2X) / 2.0;
        }
        
        /**
         * Get the center Y coordinate.
         */
        public double getCenterY() {
            return (pos1Y + pos2Y) / 2.0;
        }
        
        /**
         * Get the center Z coordinate.
         */
        public double getCenterZ() {
            return (pos1Z + pos2Z) / 2.0;
        }
        
        /**
         * Get the horizontal radius (average of X and Z dimensions / 2).
         */
        public double getHorizontalRadius() {
            return (getWidth() + getDepth()) / 4.0;
        }
        
        /**
         * Get a readable description of the selection state.
         */
        @NotNull
        public String describe() {
            if (!pos1Set && !pos2Set) {
                return "No positions set";
            }
            StringBuilder sb = new StringBuilder();
            if (pos1Set) {
                sb.append(String.format("Pos1: (%.1f, %.1f, %.1f)", pos1X, pos1Y, pos1Z));
            } else {
                sb.append("Pos1: not set");
            }
            sb.append(" | ");
            if (pos2Set) {
                sb.append(String.format("Pos2: (%.1f, %.1f, %.1f)", pos2X, pos2Y, pos2Z));
            } else {
                sb.append("Pos2: not set");
            }
            if (isComplete()) {
                sb.append(String.format(" | Size: %.0f x %.0f x %.0f", 
                        getWidth() + 1, getHeight() + 1, getDepth() + 1));
            }
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return "Selection{" + describe() + "}";
        }
    }
}
