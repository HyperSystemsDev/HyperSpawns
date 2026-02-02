package dev.hypersystems.hyperspawns;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hypersystems.hyperspawns.command.*;
import dev.hypersystems.hyperspawns.config.HyperSpawnsConfig;
import dev.hypersystems.hyperspawns.system.SpawnZoneCheckerSystem;
import dev.hypersystems.hyperspawns.system.WandInteractionSystem;
import dev.hypersystems.hyperspawns.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Main plugin entry point for HyperSpawns.
 * Handles plugin lifecycle and Hytale server integration.
 */
public class HyperSpawnsPlugin extends JavaPlugin {

    private static HyperSpawnsPlugin instance;

    private HyperSpawns core;
    private HyperSpawnsConfig config;

    /**
     * Creates a new HyperSpawnsPlugin instance.
     * Called by the Hytale plugin loader.
     *
     * @param init the plugin initialization data
     */
    public HyperSpawnsPlugin(JavaPluginInit init) {
        super(init);
    }

    /**
     * Get the plugin instance.
     */
    @NotNull
    public static HyperSpawnsPlugin get() {
        if (instance == null) {
            throw new IllegalStateException("HyperSpawnsPlugin not initialized");
        }
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;

        // Initialize logger
        Logger.init(java.util.logging.Logger.getLogger("HyperSpawns"));

        Logger.info("Setting up HyperSpawns plugin...");
    }

    @Override
    protected void start() {
        Logger.info("Starting HyperSpawns v%s...", getManifest().getVersion());

        try {
            // Initialize configuration
            Path dataFolder = getDataDirectory();
            config = HyperSpawnsConfig.init(dataFolder);

            // Initialize core
            core = HyperSpawns.init(dataFolder);
            core.start();

            // Register commands
            registerCommands();

            // Register systems for existing worlds
            registerWorldSystems();

            // Register wand interaction event systems
            registerWandEventSystems();

            Logger.info("HyperSpawns v%s started successfully!", getManifest().getVersion());

        } catch (Exception e) {
            Logger.severe("Failed to start HyperSpawns: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void shutdown() {
        Logger.info("Shutting down HyperSpawns...");

        try {
            if (core != null) {
                core.stop();
            }

            // Save config
            if (config != null) {
                config.save();
            }

            Logger.info("HyperSpawns shut down successfully");

        } catch (Exception e) {
            Logger.severe("Error during shutdown: %s", e.getMessage());
            e.printStackTrace();
        }

        instance = null;
    }

    /**
     * Register all commands.
     */
    private void registerCommands() {
        // Register the main command collection
        HyperSpawnsCommand mainCommand = new HyperSpawnsCommand();
        getCommandRegistry().registerCommand(mainCommand);

        Logger.debug("Registered commands");
    }

    /**
     * Register spawn zone systems for all loaded worlds.
     */
    private void registerWorldSystems() {
        Universe universe = Universe.get();
        if (universe == null) {
            Logger.warning("Universe not available, skipping world system registration");
            return;
        }

        universe.getWorlds().forEach((name, world) -> {
            registerWorldSystem(world);
        });
    }

    /**
     * Register a spawn zone system for a world.
     * The system is registered with the ComponentRegistry and will have
     * onSystemAddedToStore() called when added to the world's EntityStore.
     */
    public void registerWorldSystem(@NotNull World world) {
        String worldName = world.getName();
        SpawnZoneCheckerSystem system = new SpawnZoneCheckerSystem(worldName);

        // Track in core
        core.registerWorldSystem(worldName, system);

        // Register with ComponentRegistry - use bypassClassCheck=true for per-world instances
        // This triggers onSystemAddedToStore() which initializes ChunkSuppressionIntegrator
        try {
            getEntityStoreRegistry().registerSystem(system, true);
            Logger.debug("Registered SpawnZoneCheckerSystem with EntityStoreRegistry for world '%s'", worldName);
        } catch (Exception e) {
            Logger.warning("Failed to register SpawnZoneCheckerSystem for world '%s': %s", worldName, e.getMessage());
        }

        Logger.debug("Registered spawn zone system for world '%s'", worldName);
    }

    /**
     * Unregister a spawn zone system for a world.
     * Note: Due to the ECS architecture, systems registered with bypassClassCheck=true
     * cannot be easily unregistered individually. The system will be cleaned up on shutdown.
     */
    public void unregisterWorldSystem(@NotNull World world) {
        String worldName = world.getName();

        SpawnZoneCheckerSystem system = core.getWorldSystem(worldName);
        if (system != null) {
            // Note: Systems registered with bypassClassCheck=true cannot be easily
            // unregistered via unregisterSystem(Class). They are cleaned up on shutdown.
            core.unregisterWorldSystem(worldName);
            Logger.debug("Unregistered spawn zone system for world '%s'", worldName);
        }
    }

    /**
     * Register wand interaction event systems for block selection.
     */
    private void registerWandEventSystems() {
        try {
            getEntityStoreRegistry().registerSystem(new WandLeftClickSystem(core.getWandInteractionSystem()));
            getEntityStoreRegistry().registerSystem(new WandRightClickSystem(core.getWandInteractionSystem()));
            Logger.debug("Registered wand interaction event systems");
        } catch (Exception e) {
            Logger.severe("Failed to register wand event systems: %s", e.getMessage());
        }
    }

    /**
     * Get the HyperSpawns core.
     */
    @NotNull
    public HyperSpawns getCore() {
        return core;
    }

    /**
     * Get the configuration.
     */
    @NotNull
    public HyperSpawnsConfig getHyperSpawnsConfig() {
        return config;
    }

    /**
     * Reload the plugin.
     */
    public void reload() {
        if (core != null) {
            core.reload();
        }
    }

    // ========================================
    // Wand Interaction Event Systems
    // ========================================

    /**
     * ECS Event System for handling left-click block interactions (pos1 selection).
     * Listens for DamageBlockEvent and delegates to WandInteractionSystem.
     */
    private static class WandLeftClickSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
        private final WandInteractionSystem wandSystem;

        public WandLeftClickSystem(WandInteractionSystem wandSystem) {
            super(DamageBlockEvent.class);
            this.wandSystem = wandSystem;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           DamageBlockEvent event) {
            try {
                // Get player info from entity components
                PlayerRef playerRef = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
                if (playerRef == null) return;

                Vector3i targetBlock = event.getTargetBlock();
                if (targetBlock == null) return;

                // Get world name from store
                String worldName = getWorldName(store);
                if (worldName == null) return;

                boolean handled = wandSystem.handleLeftClick(playerRef, worldName,
                    targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());

                if (handled) {
                    event.setCancelled(true);
                }
            } catch (Exception e) {
                Logger.warning("Error in wand left-click: %s", e.getMessage());
            }
        }

        private String getWorldName(Store<EntityStore> store) {
            try {
                return store.getExternalData().getWorld().getName();
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * ECS Event System for handling right-click block interactions (pos2 selection).
     * Listens for UseBlockEvent.Pre and delegates to WandInteractionSystem.
     */
    private static class WandRightClickSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
        private final WandInteractionSystem wandSystem;

        public WandRightClickSystem(WandInteractionSystem wandSystem) {
            super(UseBlockEvent.Pre.class);
            this.wandSystem = wandSystem;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                           UseBlockEvent.Pre event) {
            try {
                // Get player info from entity components
                PlayerRef playerRef = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
                if (playerRef == null) return;

                Vector3i targetBlock = event.getTargetBlock();
                if (targetBlock == null) return;

                // Get world name from store
                String worldName = getWorldName(store);
                if (worldName == null) return;

                boolean handled = wandSystem.handleRightClick(playerRef, worldName,
                    targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());

                if (handled) {
                    event.setCancelled(true);
                }
            } catch (Exception e) {
                Logger.warning("Error in wand right-click: %s", e.getMessage());
            }
        }

        private String getWorldName(Store<EntityStore> store) {
            try {
                return store.getExternalData().getWorld().getName();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
