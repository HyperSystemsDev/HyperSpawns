package dev.hypersystems.hyperspawns.system;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hypersystems.hyperspawns.HyperSpawns;
import dev.hypersystems.hyperspawns.command.WandSelectionManager;
import dev.hypersystems.hyperspawns.config.HyperSpawnsConfig;
import dev.hypersystems.hyperspawns.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that handles wand interactions for zone selection.
 * 
 * Since Hytale's block interaction events may vary, this system provides
 * multiple approaches:
 * 1. Direct position commands (/hyperspawns pos1/pos2) - always works
 * 2. Wand mode toggle - players can enable wand mode to use raycasting
 * 3. Block interaction events - if available in the server API
 * 
 * When wand mode is enabled for a player and they hold the configured
 * wand item (default: hytale:stick), their targeting reticle position
 * can be used to set pos1/pos2.
 */
public class WandInteractionSystem {

    // Colors for messaging
    private static final Color CYAN = new Color(0, 255, 255);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    // Players with wand mode enabled
    private final Set<UUID> wandModeEnabled = ConcurrentHashMap.newKeySet();

    // Last known target block positions for players (updated via tick or event)
    private final Map<UUID, TargetPosition> lastTargetPositions = new ConcurrentHashMap<>();

    public WandInteractionSystem() {
    }

    /**
     * Check if wand mode is enabled for a player.
     */
    public boolean isWandModeEnabled(@NotNull UUID playerId) {
        return wandModeEnabled.contains(playerId);
    }

    /**
     * Toggle wand mode for a player.
     * 
     * @return true if wand mode is now enabled, false if disabled
     */
    public boolean toggleWandMode(@NotNull UUID playerId) {
        if (wandModeEnabled.contains(playerId)) {
            wandModeEnabled.remove(playerId);
            return false;
        } else {
            wandModeEnabled.add(playerId);
            return true;
        }
    }

    /**
     * Enable wand mode for a player.
     */
    public void enableWandMode(@NotNull UUID playerId) {
        wandModeEnabled.add(playerId);
    }

    /**
     * Disable wand mode for a player.
     */
    public void disableWandMode(@NotNull UUID playerId) {
        wandModeEnabled.remove(playerId);
    }

    /**
     * Handle a left-click block interaction (sets pos1).
     *
     * @param playerRef The player reference
     * @param worldName World name
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return true if the interaction was handled as a wand selection
     */
    public boolean handleLeftClick(@NotNull PlayerRef playerRef, @NotNull String worldName,
                                   int x, int y, int z) {
        UUID playerId = playerRef.getUuid();

        // Check if player has wand mode enabled or is holding wand item
        if (!shouldHandleWandInteraction(playerId)) {
            return false;
        }

        WandSelectionManager wandManager = HyperSpawns.get().getWandManager();
        wandManager.setPos1(playerId, worldName, x, y, z);

        // Send feedback
        playerRef.sendMessage(Message.join(
                Message.raw("[HyperSpawns] ").color(CYAN),
                Message.raw(String.format("Position 1 set to (%d, %d, %d)", x, y, z)).color(GREEN)
        ));

        // Show selection info if both positions set
        WandSelectionManager.Selection selection = wandManager.getSelection(playerId);
        if (selection != null && selection.isComplete()) {
            playerRef.sendMessage(Message.join(
                    Message.raw("[HyperSpawns] ").color(CYAN),
                    Message.raw(selection.describe()).color(YELLOW)
            ));
        }

        Logger.debug("Player %s set pos1 to (%d, %d, %d) via wand", playerId, x, y, z);
        return true;
    }

    /**
     * Handle a right-click block interaction (sets pos2).
     *
     * @param playerRef The player reference
     * @param worldName World name
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return true if the interaction was handled as a wand selection
     */
    public boolean handleRightClick(@NotNull PlayerRef playerRef, @NotNull String worldName,
                                    int x, int y, int z) {
        UUID playerId = playerRef.getUuid();

        // Check if player has wand mode enabled or is holding wand item
        if (!shouldHandleWandInteraction(playerId)) {
            return false;
        }

        WandSelectionManager wandManager = HyperSpawns.get().getWandManager();
        wandManager.setPos2(playerId, worldName, x, y, z);

        // Send feedback
        playerRef.sendMessage(Message.join(
                Message.raw("[HyperSpawns] ").color(CYAN),
                Message.raw(String.format("Position 2 set to (%d, %d, %d)", x, y, z)).color(GREEN)
        ));

        // Show selection info if both positions set
        WandSelectionManager.Selection selection = wandManager.getSelection(playerId);
        if (selection != null && selection.isComplete()) {
            playerRef.sendMessage(Message.join(
                    Message.raw("[HyperSpawns] ").color(CYAN),
                    Message.raw(selection.describe()).color(YELLOW)
            ));
        }

        Logger.debug("Player %s set pos2 to (%d, %d, %d) via wand", playerId, x, y, z);
        return true;
    }

    /**
     * Check if we should handle this player's interaction as a wand selection.
     */
    private boolean shouldHandleWandInteraction(@NotNull UUID playerId) {
        // Always handle if wand mode is explicitly enabled
        if (wandModeEnabled.contains(playerId)) {
            return true;
        }

        // For now, wand mode must be explicitly enabled via /hyperspawns wandmode
        // In the future, this could be enhanced to check held item via inventory API
        return false;
    }

    /**
     * Update the last known target position for a player.
     * Can be called from a tick system that performs raycasting.
     */
    public void updateTargetPosition(@NotNull UUID playerId, @NotNull String worldName,
                                     int x, int y, int z) {
        lastTargetPositions.put(playerId, new TargetPosition(worldName, x, y, z));
    }

    /**
     * Get the last known target position for a player.
     */
    @Nullable
    public TargetPosition getLastTargetPosition(@NotNull UUID playerId) {
        return lastTargetPositions.get(playerId);
    }

    /**
     * Clear target position tracking for a player.
     */
    public void clearTargetPosition(@NotNull UUID playerId) {
        lastTargetPositions.remove(playerId);
    }

    /**
     * Handle player disconnect - cleanup any state.
     */
    public void handlePlayerDisconnect(@NotNull UUID playerId) {
        wandModeEnabled.remove(playerId);
        lastTargetPositions.remove(playerId);
    }

    /**
     * Get the number of players with wand mode enabled.
     */
    public int getWandModePlayerCount() {
        return wandModeEnabled.size();
    }

    /**
     * Represents a target block position.
     */
    public record TargetPosition(
            @NotNull String worldName,
            int x,
            int y,
            int z
    ) {}
}
