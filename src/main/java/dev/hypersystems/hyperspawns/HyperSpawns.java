package dev.hypersystems.hyperspawns;

import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import dev.hypersystems.hyperspawns.command.WandSelectionManager;
import dev.hypersystems.hyperspawns.config.HyperSpawnsConfig;
import dev.hypersystems.hyperspawns.persistence.ZonePersistence;
import dev.hypersystems.hyperspawns.system.HostileMobRemovalSystem;
import dev.hypersystems.hyperspawns.system.SpawnZoneCheckerSystem;
import dev.hypersystems.hyperspawns.system.SpawnZoneSuppressionResource;
import dev.hypersystems.hyperspawns.system.WandInteractionSystem;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.SpawnZone;
import dev.hypersystems.hyperspawns.zone.SpawnZoneManager;
import dev.hypersystems.hyperspawns.zone.ZoneFilter;
import dev.hypersystems.hyperspawns.zone.ZoneMode;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Core business logic singleton for HyperSpawns.
 * Provides access to all managers and services.
 */
public final class HyperSpawns {
    
    private static HyperSpawns instance;
    
    private final Path dataFolder;
    private final SpawnZoneManager zoneManager;
    private final ZonePersistence persistence;
    private final WandSelectionManager wandManager;
    private final WandInteractionSystem wandInteractionSystem;
    private final HostileMobRemovalSystem mobRemovalSystem;
    private final Map<String, SpawnZoneCheckerSystem> worldSystems;
    
    private ScheduledExecutorService autoSaveExecutor;
    private volatile boolean enabled = false;
    
    private HyperSpawns(@NotNull Path dataFolder) {
        this.dataFolder = dataFolder;
        this.zoneManager = new SpawnZoneManager();
        this.persistence = new ZonePersistence(dataFolder);
        this.wandManager = new WandSelectionManager();
        this.wandInteractionSystem = new WandInteractionSystem();
        this.mobRemovalSystem = new HostileMobRemovalSystem(zoneManager);
        this.worldSystems = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the HyperSpawns core.
     * 
     * @param dataFolder Plugin data folder
     * @return The HyperSpawns instance
     */
    @NotNull
    public static HyperSpawns init(@NotNull Path dataFolder) {
        if (instance != null) {
            throw new IllegalStateException("HyperSpawns already initialized");
        }
        instance = new HyperSpawns(dataFolder);
        return instance;
    }
    
    /**
     * Get the HyperSpawns instance.
     * 
     * @throws IllegalStateException if not initialized
     */
    @NotNull
    public static HyperSpawns get() {
        if (instance == null) {
            throw new IllegalStateException("HyperSpawns not initialized");
        }
        return instance;
    }
    
    /**
     * Check if HyperSpawns has been initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }
    
    /**
     * Start the HyperSpawns system.
     */
    public void start() {
        if (enabled) {
            return;
        }
        
        Logger.info("Starting HyperSpawns v%s", BuildInfo.VERSION);
        
        // Load zones from file
        zoneManager.loadZones(persistence.loadZones());

        // Compile filters
        compileAllFilters();

        // Set up zone change listener
        zoneManager.setChangeListener(this::onZoneChanged);

        // Rebuild all world systems with loaded zones
        // This ensures zones are applied after they're loaded from disk
        rebuildAllWorldSystems();

        // Start the hostile mob removal system
        mobRemovalSystem.start();
        
        // Start auto-save
        startAutoSave();
        
        enabled = true;
        Logger.info("HyperSpawns started - %d zones loaded", zoneManager.getZoneCount());
    }
    
    /**
     * Stop the HyperSpawns system.
     */
    public void stop() {
        if (!enabled) {
            return;
        }
        
        Logger.info("Stopping HyperSpawns...");
        
        // Stop the hostile mob removal system
        mobRemovalSystem.stop();
        
        // Stop auto-save
        stopAutoSave();
        
        // Save zones
        saveZones();
        
        // Clear systems
        worldSystems.clear();
        
        enabled = false;
        Logger.info("HyperSpawns stopped");
    }
    
    /**
     * Reload the plugin.
     */
    public void reload() {
        Logger.info("Reloading HyperSpawns...");
        
        // Reload config
        HyperSpawnsConfig.get().reload();
        
        // Reload zones
        zoneManager.loadZones(persistence.loadZones());
        compileAllFilters();
        
        // Rebuild all world systems
        rebuildAllWorldSystems();
        
        // Clear mob removal cache
        mobRemovalSystem.clearCache();
        
        // Restart auto-save with new interval
        stopAutoSave();
        startAutoSave();
        
        Logger.info("HyperSpawns reloaded - %d zones", zoneManager.getZoneCount());
    }
    
    /**
     * Save zones to disk.
     */
    public void saveZones() {
        if (zoneManager.isDirty()) {
            persistence.saveZones(zoneManager.getZonesForSave());
            zoneManager.clearDirty();
            Logger.debug("Saved %d zones", zoneManager.getZoneCount());
        }
    }
    
    /**
     * Get the zone manager.
     */
    @NotNull
    public SpawnZoneManager getZoneManager() {
        return zoneManager;
    }
    
    /**
     * Get the persistence handler.
     */
    @NotNull
    public ZonePersistence getPersistence() {
        return persistence;
    }
    
    /**
     * Get the wand selection manager.
     */
    @NotNull
    public WandSelectionManager getWandManager() {
        return wandManager;
    }
    
    /**
     * Get the wand interaction system.
     */
    @NotNull
    public WandInteractionSystem getWandInteractionSystem() {
        return wandInteractionSystem;
    }
    
    /**
     * Get the hostile mob removal system.
     */
    @NotNull
    public HostileMobRemovalSystem getMobRemovalSystem() {
        return mobRemovalSystem;
    }
    
    /**
     * Get the data folder path.
     */
    @NotNull
    public Path getDataFolder() {
        return dataFolder;
    }
    
    /**
     * Check if the system is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Register a world system.
     */
    public void registerWorldSystem(@NotNull String worldName, @NotNull SpawnZoneCheckerSystem system) {
        worldSystems.put(worldName, system);
        system.rebuild(zoneManager);
        Logger.debug("Registered system for world '%s'", worldName);
    }
    
    /**
     * Unregister a world system.
     */
    public void unregisterWorldSystem(@NotNull String worldName) {
        worldSystems.remove(worldName);
        Logger.debug("Unregistered system for world '%s'", worldName);
    }
    
    /**
     * Get the system for a world.
     */
    @Nullable
    public SpawnZoneCheckerSystem getWorldSystem(@NotNull String worldName) {
        return worldSystems.get(worldName);
    }
    
    /**
     * Get the suppression resource for a world.
     */
    @Nullable
    public SpawnZoneSuppressionResource getSuppressionResource(@NotNull String worldName) {
        SpawnZoneCheckerSystem system = worldSystems.get(worldName);
        return system != null ? system.getSuppressionResource() : null;
    }
    
    /**
     * Rebuild suppression data for a specific world.
     */
    public void rebuildWorldSuppression(@NotNull String worldName) {
        SpawnZoneCheckerSystem system = worldSystems.get(worldName);
        if (system != null) {
            system.rebuild(zoneManager);
            Logger.debug("Rebuilt suppression for world '%s'", worldName);
        }
    }
    
    /**
     * Rebuild suppression data for all worlds.
     */
    public void rebuildAllWorldSystems() {
        for (Map.Entry<String, SpawnZoneCheckerSystem> entry : worldSystems.entrySet()) {
            entry.getValue().rebuild(zoneManager);
        }
        Logger.debug("Rebuilt suppression for %d worlds", worldSystems.size());
    }
    
    /**
     * Compile all zone filters for efficient runtime lookups.
     */
    public void compileAllFilters() {
        zoneManager.compileAllFilters(createRoleResolver());
    }
    
    /**
     * Create a role resolver that uses the Hytale TagSet system.
     * Note: This is a placeholder implementation - actual NPCGroup index lookup
     * depends on Hytale's specific API which may vary.
     */
    @NotNull
    private ZoneFilter.RoleResolver createRoleResolver() {
        return new ZoneFilter.RoleResolver() {
            @Override
            public int getRoleIndex(@NotNull String roleName) {
                try {
                    // Use Hytale's NPC role system
                    var assetMap = NPCGroup.getAssetMap();
                    if (assetMap != null) {
                        // Try to get asset by key - return hash as index placeholder
                        var asset = assetMap.getAsset(roleName);
                        if (asset != null) {
                            return roleName.hashCode() & Integer.MAX_VALUE;
                        }
                    }
                } catch (Exception e) {
                    Logger.trace("Could not resolve role '%s': %s", roleName, e.getMessage());
                }
                return -1;
            }
            
            @Override
            @Nullable
            public IntSet getRolesInGroup(@NotNull String groupName) {
                try {
                    var tagSetPlugin = TagSetPlugin.get(NPCGroup.class);
                    if (tagSetPlugin != null) {
                        var assetMap = NPCGroup.getAssetMap();
                        if (assetMap != null) {
                            var asset = assetMap.getAsset(groupName);
                            if (asset != null) {
                                int groupIndex = groupName.hashCode() & Integer.MAX_VALUE;
                                return tagSetPlugin.getSet(groupIndex);
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.trace("Could not resolve group '%s': %s", groupName, e.getMessage());
                }
                return null;
            }
        };
    }
    
    /**
     * Called when a zone is changed (created, modified, or deleted).
     */
    private void onZoneChanged(@NotNull SpawnZone zone) {
        // Recompile the zone's filter
        zone.compile(createRoleResolver());
        
        // Rebuild the affected world's suppression data
        rebuildWorldSuppression(zone.getWorld());
        
        // If zone is now in BLOCK mode and enabled, remove existing hostile mobs
        if (zone.getMode() == ZoneMode.BLOCK && zone.isEnabled()) {
            mobRemovalSystem.removeHostileMobsFromZone(zone);
        }
    }
    
    private void startAutoSave() {
        int interval = HyperSpawnsConfig.get().getAutoSaveIntervalMinutes();
        autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HyperSpawns-AutoSave");
            t.setDaemon(true);
            return t;
        });
        autoSaveExecutor.scheduleAtFixedRate(this::saveZones, interval, interval, TimeUnit.MINUTES);
        Logger.debug("Started auto-save with %d minute interval", interval);
    }
    
    private void stopAutoSave() {
        if (autoSaveExecutor != null) {
            autoSaveExecutor.shutdown();
            try {
                if (!autoSaveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            autoSaveExecutor = null;
        }
    }
}
