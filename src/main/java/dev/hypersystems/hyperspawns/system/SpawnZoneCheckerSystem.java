package dev.hypersystems.hyperspawns.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.StoreSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hypersystems.hyperspawns.HyperSpawns;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.SpawnZoneManager;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system that initializes and manages the spawn zone suppression resource.
 */
public class SpawnZoneCheckerSystem extends StoreSystem<EntityStore> {
    
    private final String worldName;
    private SpawnZoneSuppressionResource suppressionResource;
    
    public SpawnZoneCheckerSystem(@NotNull String worldName) {
        this.worldName = worldName;
    }
    
    @Override
    public void onSystemAddedToStore(@NotNull Store<EntityStore> store) {
        Logger.debug("SpawnZoneCheckerSystem added to store for world '%s'", worldName);
        
        // Create and initialize the suppression resource
        suppressionResource = new SpawnZoneSuppressionResource(worldName);
        
        // Register the resource with the store
        // Note: The actual resource type registration happens in the plugin
        
        // Rebuild with current zones
        SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
        if (manager != null) {
            suppressionResource.rebuild(manager);
        }
    }
    
    @Override
    public void onSystemRemovedFromStore(@NotNull Store<EntityStore> store) {
        Logger.debug("SpawnZoneCheckerSystem removed from store for world '%s'", worldName);
        suppressionResource = null;
    }
    
    /**
     * Rebuild the suppression data from the zone manager.
     * Call this when zones are modified.
     */
    public void rebuild(@NotNull SpawnZoneManager zoneManager) {
        if (suppressionResource != null) {
            suppressionResource.rebuild(zoneManager);
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
     * Get the world name this system is for.
     */
    @NotNull
    public String getWorldName() {
        return worldName;
    }
}
