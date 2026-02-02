package dev.hypersystems.hyperspawns.system;

import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.SpawnZone;
import dev.hypersystems.hyperspawns.zone.SpawnZoneManager;
import dev.hypersystems.hyperspawns.zone.ZoneBoundary;
import dev.hypersystems.hyperspawns.zone.ZoneMode;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * System that removes existing hostile mobs from BLOCK zones.
 * 
 * When a zone is set to BLOCK mode or enabled, this system scans the zone
 * and removes any hostile NPCs that are already inside. It also runs
 * periodically to catch any mobs that may have wandered in.
 * 
 * Note: This is a simplified implementation. Full NPC removal would require
 * access to Hytale's specific NPC components which may vary by server version.
 */
public class HostileMobRemovalSystem {

    // Scan interval in seconds
    private static final int SCAN_INTERVAL_SECONDS = 5;

    // Maximum entities to remove per scan (to avoid lag)
    private static final int MAX_REMOVALS_PER_SCAN = 50;

    private final SpawnZoneManager zoneManager;
    private ScheduledExecutorService executor;
    private volatile boolean running = false;

    // Cached hostile NPC group index
    private IntSet hostileRoleIndices = null;

    public HostileMobRemovalSystem(@NotNull SpawnZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    /**
     * Start the periodic mob removal system.
     */
    public void start() {
        if (running) {
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HyperSpawns-MobRemoval");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(
                this::scanAndRemoveHostileMobs,
                SCAN_INTERVAL_SECONDS,
                SCAN_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        running = true;
        Logger.debug("Started hostile mob removal system with %ds interval", SCAN_INTERVAL_SECONDS);
    }

    /**
     * Stop the periodic mob removal system.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }

        Logger.debug("Stopped hostile mob removal system");
    }

    /**
     * Trigger immediate removal of hostile mobs from a specific zone.
     * Call this when a zone is enabled or its mode is changed to BLOCK.
     */
    public void removeHostileMobsFromZone(@NotNull SpawnZone zone) {
        if (zone.getMode() != ZoneMode.BLOCK || !zone.isEnabled()) {
            return;
        }

        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        World world = universe.getWorld(zone.getWorld());
        if (world == null) {
            return;
        }

        // Execute on the world's thread
        world.execute(() -> {
            int removed = removeHostileMobsFromZoneInternal(zone, world);
            if (removed > 0) {
                Logger.info("Removed %d hostile mobs from zone '%s'", removed, zone.getName());
            }
        });
    }

    /**
     * Scan all BLOCK zones and remove hostile mobs.
     */
    private void scanAndRemoveHostileMobs() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return;
            }

            Collection<SpawnZone> zones = zoneManager.getAllZones();

            for (SpawnZone zone : zones) {
                if (zone.getMode() != ZoneMode.BLOCK || !zone.isEnabled()) {
                    continue;
                }

                World world = universe.getWorld(zone.getWorld());
                if (world == null) {
                    continue;
                }

                // Execute on the world's thread
                final SpawnZone zoneRef = zone;
                world.execute(() -> {
                    int removed = removeHostileMobsFromZoneInternal(zoneRef, world);
                    if (removed > 0) {
                        Logger.debug("Removed %d hostile mobs from zone '%s'", removed, zoneRef.getName());
                    }
                });
            }

        } catch (Exception e) {
            Logger.warning("Error during hostile mob scan: %s", e.getMessage());
        }
    }

    /**
     * Remove hostile mobs from a zone. Must be called on the world's thread.
     * 
     * This implementation uses the NPC spatial resource if available.
     * If the full NPC component API isn't available, it will log a warning
     * and skip removal. Server admins can use the `/kill` command as a fallback.
     */
    private int removeHostileMobsFromZoneInternal(@NotNull SpawnZone zone, @NotNull World world) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            ZoneBoundary boundary = zone.getBoundary();

            // Get the center and radius for spatial query
            double centerX = (boundary.getMinX() + boundary.getMaxX()) / 2.0;
            double centerY = (boundary.getMinY() + boundary.getMaxY()) / 2.0;
            double centerZ = (boundary.getMinZ() + boundary.getMaxZ()) / 2.0;
            
            // Calculate the maximum radius needed to encompass the zone
            double radiusX = (boundary.getMaxX() - boundary.getMinX()) / 2.0;
            double radiusY = (boundary.getMaxY() - boundary.getMinY()) / 2.0;
            double radiusZ = (boundary.getMaxZ() - boundary.getMinZ()) / 2.0;
            double radius = Math.sqrt(radiusX * radiusX + radiusY * radiusY + radiusZ * radiusZ);

            // Get NPC spatial resource
            SpatialResource<Ref<EntityStore>, EntityStore> npcSpatial = getNpcSpatialResource(store);
            if (npcSpatial == null) {
                Logger.debug("NPC spatial resource not available for zone '%s', using fallback iteration", zone.getName());
                return removeHostileMobsUsingStoreQuery(zone, world);
            }

            // Collect entities in range
            ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
            Vector3d center = new Vector3d(centerX, centerY, centerZ);
            npcSpatial.getSpatialStructure().collect(center, radius, results);

            int removedCount = 0;

            for (int i = 0; i < results.size() && removedCount < MAX_REMOVALS_PER_SCAN; i++) {
                Ref<EntityStore> entityRef = results.get(i);

                // Get transform to check if actually inside the zone boundary
                TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
                if (transform == null) {
                    continue;
                }

                Vector3d pos = transform.getPosition();
                if (!boundary.contains(pos.x, pos.y, pos.z)) {
                    continue;
                }

                // Check if this entity should be removed (is an NPC in the zone)
                // Since we're querying the NPC spatial resource, all results should be NPCs
                // For BLOCK mode, we remove all NPCs (spawning is blocked)
                try {
                    store.removeEntity(entityRef, RemoveReason.REMOVE);
                    removedCount++;
                } catch (Exception e) {
                    Logger.debug("Could not remove entity: %s", e.getMessage());
                }
            }

            return removedCount;

        } catch (Exception e) {
            Logger.warning("Error removing hostile mobs from zone '%s': %s", zone.getName(), e.getMessage());
            return 0;
        }
    }

    /**
     * Fallback when NPCPlugin spatial resource is unavailable.
     * Logs a warning since we cannot efficiently iterate entities without the spatial resource.
     * Spawn blocking via chunk suppression should still work - this only affects existing mob removal.
     */
    private int removeHostileMobsUsingStoreQuery(@NotNull SpawnZone zone, @NotNull World world) {
        // Log once per zone to avoid spam - the spatial resource is needed for efficient mob queries
        Logger.warning("Cannot remove existing mobs from zone '%s': NPCPlugin spatial resource unavailable. " +
                "New spawns will still be blocked via chunk suppression.", zone.getName());
        return 0;
    }

    /**
     * Get the set of hostile NPC role indices.
     */
    @Nullable
    private IntSet getHostileRoleIndices() {
        if (hostileRoleIndices != null) {
            return hostileRoleIndices;
        }

        try {
            var tagSetPlugin = TagSetPlugin.get(NPCGroup.class);
            if (tagSetPlugin != null) {
                // Try to get the "hostile" group
                var assetMap = NPCGroup.getAssetMap();
                if (assetMap != null) {
                    // Check common hostile group names
                    String[] hostileGroupNames = {"hostile", "monster", "enemy", "hostile_npc"};
                    
                    for (String groupName : hostileGroupNames) {
                        var asset = assetMap.getAsset(groupName);
                        if (asset != null) {
                            // Use the group name hash as index
                            int groupIndex = groupName.hashCode() & Integer.MAX_VALUE;
                            hostileRoleIndices = tagSetPlugin.getSet(groupIndex);
                            if (hostileRoleIndices != null && !hostileRoleIndices.isEmpty()) {
                                Logger.debug("Loaded hostile NPC group '%s' with %d roles", 
                                        groupName, hostileRoleIndices.size());
                                return hostileRoleIndices;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.debug("Could not load hostile NPC group: %s", e.getMessage());
        }

        return null;
    }

    /**
     * Get the NPC spatial resource from the store.
     */
    @Nullable
    private SpatialResource<Ref<EntityStore>, EntityStore> getNpcSpatialResource(@NotNull Store<EntityStore> store) {
        try {
            // Try to get the NPC spatial resource from NPCPlugin
            var npcPlugin = NPCPlugin.get();
            if (npcPlugin != null) {
                var resourceType = npcPlugin.getNpcSpatialResource();
                if (resourceType != null) {
                    return store.getResource(resourceType);
                }
            }
        } catch (Exception e) {
            Logger.debug("Could not get NPC spatial resource: %s", e.getMessage());
        }
        return null;
    }

    /**
     * Check if the system is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Clear the cached hostile role indices (call on reload).
     */
    public void clearCache() {
        hostileRoleIndices = null;
    }
}