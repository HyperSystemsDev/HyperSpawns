package dev.hypersystems.hyperspawns;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.hypersystems.hyperspawns.command.*;
import dev.hypersystems.hyperspawns.config.HyperSpawnsConfig;
import dev.hypersystems.hyperspawns.system.SpawnZoneCheckerSystem;
import dev.hypersystems.hyperspawns.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.logging.Level;

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
     */
    public void registerWorldSystem(@NotNull World world) {
        String worldName = world.getName();
        SpawnZoneCheckerSystem system = new SpawnZoneCheckerSystem(worldName);
        
        // Track in core
        core.registerWorldSystem(worldName, system);
        
        Logger.debug("Registered spawn zone system for world '%s'", worldName);
    }
    
    /**
     * Unregister a spawn zone system for a world.
     */
    public void unregisterWorldSystem(@NotNull World world) {
        String worldName = world.getName();
        
        SpawnZoneCheckerSystem system = core.getWorldSystem(worldName);
        if (system != null) {
            core.unregisterWorldSystem(worldName);
            Logger.debug("Unregistered spawn zone system for world '%s'", worldName);
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
}
