package dev.hypersystems.hyperspawns.system;

import com.hypixel.fastutil.longs.Long2ObjectConcurrentHashMap;
import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.spawning.SpawningPlugin;
import com.hypixel.hytale.server.spawning.suppression.component.ChunkSuppressionEntry;
import com.hypixel.hytale.server.spawning.suppression.component.ChunkSuppressionQueue;
import com.hypixel.hytale.server.spawning.suppression.component.SpawnSuppressionController;
import dev.hypersystems.hyperspawns.util.ChunkIndexer;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.*;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Integrates HyperSpawns zones with Hytale's native spawn suppression system.
 * This is the critical component that makes spawn blocking actually work.
 *
 * Hytale's spawning system checks ChunkSuppressionEntry components on chunks
 * to determine which spawns to block. This class creates and manages those
 * entries based on HyperSpawns zone definitions.
 */
public class ChunkSuppressionIntegrator {

    // UUID prefix for HyperSpawns suppression entries to distinguish from native ones
    private static final UUID HYPERSPAWNS_PREFIX = UUID.fromString("48535053-0000-0000-0000-000000000000");

    private final String worldName;
    private final Set<UUID> activeSuppressionIds = Collections.synchronizedSet(new HashSet<>());

    public ChunkSuppressionIntegrator(@NotNull String worldName) {
        this.worldName = worldName;
    }

    /**
     * Apply all zone suppressions to the world's chunk suppression map.
     * This integrates our zones with Hytale's native spawning system.
     */
    public void applyToWorld(@NotNull World world, @NotNull SpawnZoneManager zoneManager) {
        Logger.info("ChunkSuppressionIntegrator.applyToWorld() called for world '%s'", worldName);

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ChunkStore chunkComponentStore = world.getChunkStore();
        Store<ChunkStore> chunkStore = chunkComponentStore.getStore();

        SpawnSuppressionController suppressionController = entityStore.getResource(
                SpawnSuppressionController.getResourceType());

        if (suppressionController == null) {
            Logger.warning("SpawnSuppressionController NOT AVAILABLE for world '%s'. " +
                    "Spawn suppression will NOT work! Is SpawningPlugin loaded?", worldName);
            return;
        }
        Logger.debug("SpawnSuppressionController found for world '%s'", worldName);

        // First, clear any existing HyperSpawns suppressions
        clearExistingSuppression(suppressionController, chunkComponentStore, chunkStore);

        Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkSuppressionMap =
                suppressionController.getChunkSuppressionMap();

        Collection<SpawnZone> zones = zoneManager.getAllZones();
        int zonesApplied = 0;
        int chunksAffected = 0;

        Logger.debug("Checking %d zones for world '%s'", zones.size(), worldName);

        for (SpawnZone zone : zones) {
            if (!zone.getWorld().equals(worldName)) {
                Logger.debug("Skipping zone '%s': wrong world (zone='%s', current='%s')",
                        zone.getName(), zone.getWorld(), worldName);
                continue;
            }
            if (!zone.isEnabled()) {
                Logger.debug("Skipping zone '%s': disabled", zone.getName());
                continue;
            }

            ZoneMode mode = zone.getMode();
            // Only BLOCK and DENY modes actually suppress spawning
            if (mode != ZoneMode.BLOCK && mode != ZoneMode.DENY) {
                Logger.debug("Skipping zone '%s': mode is %s (not BLOCK or DENY)", zone.getName(), mode);
                continue;
            }

            chunksAffected += applyZoneSuppression(zone, chunkSuppressionMap, chunkComponentStore, chunkStore);
            zonesApplied++;
        }

        Logger.info("Applied %d zones affecting %d chunks in world '%s'",
                zonesApplied, chunksAffected, worldName);
        Logger.debug("ChunkSuppressionMap now contains %d entries",
                suppressionController.getChunkSuppressionMap().size());
    }

    /**
     * Apply a single zone's suppression to the chunk suppression map.
     */
    private int applyZoneSuppression(
            @NotNull SpawnZone zone,
            @NotNull Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkSuppressionMap,
            @NotNull ChunkStore chunkComponentStore,
            @NotNull Store<ChunkStore> chunkStore) {

        ZoneBoundary boundary = zone.getBoundary();
        int minChunkX = boundary.getMinChunkX();
        int maxChunkX = boundary.getMaxChunkX();
        int minChunkZ = boundary.getMinChunkZ();
        int maxChunkZ = boundary.getMaxChunkZ();
        int minY = (int) Math.floor(boundary.getMinY());
        int maxY = (int) Math.ceil(boundary.getMaxY());

        // Generate a unique suppressor ID for this zone
        UUID suppressorId = generateSuppressionId(zone.getId());
        activeSuppressionIds.add(suppressorId);

        // Determine suppressed roles based on zone filter and mode
        IntSet suppressedRoles = getSuppressedRoles(zone);

        int chunksAffected = 0;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!boundary.intersectsChunk(chunkX, chunkZ)) {
                    continue;
                }

                long chunkIndex = ChunkUtil.indexChunk(chunkX, chunkZ);

                // Get or create the suppression span list for this chunk
                ChunkSuppressionEntry oldEntry = chunkSuppressionMap.get(chunkIndex);
                List<ChunkSuppressionEntry.SuppressionSpan> spanList;

                if (oldEntry != null) {
                    spanList = new ObjectArrayList<>(oldEntry.getSuppressionSpans());
                } else {
                    spanList = new ObjectArrayList<>();
                }

                // Add our suppression span
                ChunkSuppressionEntry.SuppressionSpan span = new ChunkSuppressionEntry.SuppressionSpan(
                        suppressorId, minY, maxY, suppressedRoles);
                spanList.add(span);

                // Sort by minY for efficient lookup
                spanList.sort(Comparator.comparingInt(ChunkSuppressionEntry.SuppressionSpan::getMinY));

                // Create new entry and update map
                ChunkSuppressionEntry newEntry = new ChunkSuppressionEntry(spanList);
                chunkSuppressionMap.put(chunkIndex, newEntry);

                // Queue update for loaded chunks
                Ref<ChunkStore> chunkRef = chunkComponentStore.getChunkReference(chunkIndex);
                if (chunkRef != null) {
                    try {
                        ChunkSuppressionQueue queue = chunkStore.getResource(
                                SpawningPlugin.get().getChunkSuppressionQueueResourceType());
                        if (queue != null) {
                            queue.queueForAdd(chunkRef, newEntry);
                        }
                    } catch (Exception e) {
                        Logger.debug("Could not queue chunk suppression update: %s", e.getMessage());
                    }
                }

                chunksAffected++;
            }
        }

        Logger.debug("Applied zone '%s' suppression to %d chunks", zone.getName(), chunksAffected);
        return chunksAffected;
    }

    /**
     * Clear all HyperSpawns-related suppressions from the chunk suppression map.
     */
    private void clearExistingSuppression(
            @NotNull SpawnSuppressionController suppressionController,
            @NotNull ChunkStore chunkComponentStore,
            @NotNull Store<ChunkStore> chunkStore) {

        Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkSuppressionMap =
                suppressionController.getChunkSuppressionMap();

        Set<UUID> idsToRemove = new HashSet<>(activeSuppressionIds);
        activeSuppressionIds.clear();

        if (idsToRemove.isEmpty()) {
            return;
        }

        // Iterate through all chunk entries and remove our spans
        for (long chunkIndex : new HashSet<>(chunkSuppressionMap.keySet())) {
            ChunkSuppressionEntry entry = chunkSuppressionMap.get(chunkIndex);
            if (entry == null) {
                continue;
            }

            List<ChunkSuppressionEntry.SuppressionSpan> oldSpans = entry.getSuppressionSpans();
            List<ChunkSuppressionEntry.SuppressionSpan> newSpans = new ObjectArrayList<>();
            boolean hasChanges = false;

            for (ChunkSuppressionEntry.SuppressionSpan span : oldSpans) {
                if (idsToRemove.contains(span.getSuppressorId()) || isHyperSpawnsId(span.getSuppressorId())) {
                    hasChanges = true;
                } else {
                    newSpans.add(span);
                }
            }

            if (hasChanges) {
                if (newSpans.isEmpty()) {
                    chunkSuppressionMap.remove(chunkIndex);
                    // Queue removal for loaded chunks
                    Ref<ChunkStore> chunkRef = chunkComponentStore.getChunkReference(chunkIndex);
                    if (chunkRef != null) {
                        try {
                            ChunkSuppressionQueue queue = chunkStore.getResource(
                                    SpawningPlugin.get().getChunkSuppressionQueueResourceType());
                            if (queue != null) {
                                queue.queueForRemove(chunkRef);
                            }
                        } catch (Exception e) {
                            Logger.debug("Could not queue chunk suppression removal: %s", e.getMessage());
                        }
                    }
                } else {
                    ChunkSuppressionEntry newEntry = new ChunkSuppressionEntry(newSpans);
                    chunkSuppressionMap.put(chunkIndex, newEntry);
                    // Queue update for loaded chunks
                    Ref<ChunkStore> chunkRef = chunkComponentStore.getChunkReference(chunkIndex);
                    if (chunkRef != null) {
                        try {
                            ChunkSuppressionQueue queue = chunkStore.getResource(
                                    SpawningPlugin.get().getChunkSuppressionQueueResourceType());
                            if (queue != null) {
                                queue.queueForAdd(chunkRef, newEntry);
                            }
                        } catch (Exception e) {
                            Logger.debug("Could not queue chunk suppression update: %s", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the set of NPC roles to suppress based on zone mode and filter.
     * Returns null to suppress ALL roles (for BLOCK mode).
     */
    @Nullable
    private IntSet getSuppressedRoles(@NotNull SpawnZone zone) {
        ZoneMode mode = zone.getMode();

        // BLOCK mode suppresses ALL spawns (null means all roles)
        if (mode == ZoneMode.BLOCK) {
            return null;
        }

        // DENY mode suppresses specific roles based on filter
        ZoneFilter filter = zone.getFilter();
        if (filter == null || filter.isEmpty()) {
            // No filter means block all in DENY mode
            return null;
        }

        IntSet suppressedRoles = new IntOpenHashSet();

        // Get the compiled role indices from the filter
        IntSet compiledRoles = filter.getCompiledRoleIndices();
        if (compiledRoles != null && !compiledRoles.isEmpty()) {
            suppressedRoles.addAll(compiledRoles);
        }

        // Also resolve NPC groups to individual roles
        Set<String> filterGroups = filter.getNpcGroups();
        if (filterGroups != null) {
            for (String groupName : filterGroups) {
                try {
                    var tagSetPlugin = TagSetPlugin.get(NPCGroup.class);
                    if (tagSetPlugin != null) {
                        var assetMap = NPCGroup.getAssetMap();
                        if (assetMap != null) {
                            var asset = assetMap.getAsset(groupName);
                            if (asset != null) {
                                int groupIndex = groupName.hashCode() & Integer.MAX_VALUE;
                                IntSet groupRoles = tagSetPlugin.getSet(groupIndex);
                                if (groupRoles != null) {
                                    suppressedRoles.addAll(groupRoles);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.debug("Could not resolve NPC group '%s': %s", groupName, e.getMessage());
                }
            }
        }

        // If we ended up with no specific roles, suppress all
        return suppressedRoles.isEmpty() ? null : suppressedRoles;
    }

    /**
     * Generate a unique suppression ID for a zone.
     * Uses a prefix to identify HyperSpawns-generated IDs.
     */
    @NotNull
    private UUID generateSuppressionId(@NotNull UUID zoneId) {
        // Combine our prefix with the zone ID for uniqueness
        return new UUID(
                HYPERSPAWNS_PREFIX.getMostSignificantBits() ^ zoneId.getMostSignificantBits(),
                HYPERSPAWNS_PREFIX.getLeastSignificantBits() ^ zoneId.getLeastSignificantBits()
        );
    }

    /**
     * Check if a suppressor ID was generated by HyperSpawns.
     */
    private boolean isHyperSpawnsId(@NotNull UUID id) {
        // Check if the high bits match our prefix pattern
        long prefixHigh = HYPERSPAWNS_PREFIX.getMostSignificantBits();
        long idHigh = id.getMostSignificantBits();
        // Use a mask to check the prefix bytes
        long mask = 0xFFFFFFFF00000000L;
        return (idHigh & mask) == (prefixHigh & mask);
    }

    /**
     * Apply suppression to a newly loaded chunk.
     */
    public void applyToChunk(@NotNull World world, long chunkIndex, @NotNull SpawnZoneManager zoneManager) {
        int chunkX = ChunkIndexer.getChunkX(chunkIndex);
        int chunkZ = ChunkIndexer.getChunkZ(chunkIndex);

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ChunkStore chunkComponentStore = world.getChunkStore();
        Store<ChunkStore> chunkStore = chunkComponentStore.getStore();

        SpawnSuppressionController suppressionController = entityStore.getResource(
                SpawnSuppressionController.getResourceType());

        if (suppressionController == null) {
            return;
        }

        Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkSuppressionMap =
                suppressionController.getChunkSuppressionMap();

        // Check if we already have suppression for this chunk
        ChunkSuppressionEntry existingEntry = chunkSuppressionMap.get(chunkIndex);

        // Apply chunk component if we have suppression data
        if (existingEntry != null) {
            Ref<ChunkStore> chunkRef = chunkComponentStore.getChunkReference(chunkIndex);
            if (chunkRef != null) {
                try {
                    ChunkSuppressionQueue queue = chunkStore.getResource(
                            SpawningPlugin.get().getChunkSuppressionQueueResourceType());
                    if (queue != null) {
                        queue.queueForAdd(chunkRef, existingEntry);
                    }
                } catch (Exception e) {
                    Logger.debug("Could not apply chunk suppression to loaded chunk: %s", e.getMessage());
                }
            }
        }
    }

    /**
     * Get the world name this integrator is for.
     */
    @NotNull
    public String getWorldName() {
        return worldName;
    }

    /**
     * Get the number of active suppression entries.
     */
    public int getActiveSuppressionCount() {
        return activeSuppressionIds.size();
    }
}
