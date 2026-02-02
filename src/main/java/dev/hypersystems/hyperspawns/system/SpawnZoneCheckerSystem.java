package dev.hypersystems.hyperspawns.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.StoreSystem;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hypersystems.hyperspawns.HyperSpawns;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.SpawnZoneManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ECS system that initializes and manages spawn zone suppression for a world.
 * Integrates HyperSpawns zones with Hytale's native spawn suppression system.
 */
public class SpawnZoneCheckerSystem extends StoreSystem<EntityStore> {

    private final String worldName;
    private SpawnZoneSuppressionResource suppressionResource;
    private ChunkSuppressionIntegrator chunkIntegrator;

    public SpawnZoneCheckerSystem(@NotNull String worldName) {
        this.worldName = worldName;
    }

    @Override
    public void onSystemAddedToStore(@NotNull Store<EntityStore> store) {
        Logger.info("SpawnZoneCheckerSystem added to store for world '%s'", worldName);

        // Create and initialize the suppression resource for our custom checks
        suppressionResource = new SpawnZoneSuppressionResource(worldName);

        // Create the chunk suppression integrator for Hytale's native system
        chunkIntegrator = new ChunkSuppressionIntegrator(worldName);

        // Rebuild with current zones
        SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
        if (manager != null) {
            rebuild(manager);
        }
    }

    @Override
    public void onSystemRemovedFromStore(@NotNull Store<EntityStore> store) {
        Logger.info("SpawnZoneCheckerSystem removed from store for world '%s'", worldName);
        suppressionResource = null;
        chunkIntegrator = null;
    }

    /**
     * Rebuild the suppression data from the zone manager.
     * Call this when zones are modified.
     */
    public void rebuild(@NotNull SpawnZoneManager zoneManager) {
        Logger.info("Rebuilding spawn zone suppression for world '%s'", worldName);

        // Rebuild our custom suppression resource
        if (suppressionResource != null) {
            suppressionResource.rebuild(zoneManager);
        }

        // Apply to Hytale's native chunk suppression system
        if (chunkIntegrator != null) {
            World world = getWorld();
            if (world != null) {
                try {
                    chunkIntegrator.applyToWorld(world, zoneManager);
                    Logger.info("Zone suppression rebuild completed for world '%s'", worldName);
                } catch (Exception e) {
                    Logger.warning("Failed to apply zone suppression to Hytale system: %s", e.getMessage());
                }
            } else {
                Logger.warning("Cannot apply chunk suppression for world '%s': World object is null. " +
                        "This may be a timing issue - suppression will be applied when the world loads.", worldName);
            }
        } else {
            Logger.warning("Cannot apply chunk suppression for world '%s': ChunkIntegrator is null", worldName);
        }
    }

    /**
     * Apply suppression to a newly loaded chunk.
     */
    public void applyToChunk(long chunkIndex, @NotNull SpawnZoneManager zoneManager) {
        if (chunkIntegrator != null) {
            World world = getWorld();
            if (world != null) {
                chunkIntegrator.applyToChunk(world, chunkIndex, zoneManager);
            }
        }
    }

    /**
     * Get the suppression resource for this world.
     */
    @NotNull
    public SpawnZoneSuppressionResource getSuppressionResource() {
        if (suppressionResource == null) {
            throw new IllegalStateException("System not yet added to store");
        }
        return suppressionResource;
    }

    /**
     * Get the chunk suppression integrator.
     */
    @Nullable
    public ChunkSuppressionIntegrator getChunkIntegrator() {
        return chunkIntegrator;
    }

    /**
     * Get the world name this system is for.
     */
    @NotNull
    public String getWorldName() {
        return worldName;
    }

    /**
     * Get the World object for this system.
     */
    @Nullable
    private World getWorld() {
        Universe universe = Universe.get();
        if (universe != null) {
            return universe.getWorld(worldName);
        }
        return null;
    }
}