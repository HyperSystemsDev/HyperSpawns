package dev.hypersystems.hyperspawns.config;

import com.google.gson.*;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.ZoneMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration singleton for HyperSpawns plugin settings.
 */
public final class HyperSpawnsConfig {
    
    private static final String CONFIG_FILE = "config.json";
    
    private static HyperSpawnsConfig instance;
    
    private final Path configFile;
    private final Gson gson;
    
    // Configuration values
    private boolean debugMode = false;
    private int autoSaveIntervalMinutes = 5;
    private ZoneMode defaultZoneMode = ZoneMode.BLOCK;
    private String bypassPermission = "hyperspawns.bypass";
    private String wandItemId = "hytale:stick";
    private boolean particlesEnabled = true;
    private int particleViewDistance = 32;
    private double globalSpawnMultiplier = 1.0;
    private boolean globalSpawnPaused = false;
    
    private HyperSpawnsConfig(@NotNull Path dataFolder) {
        this.configFile = dataFolder.resolve(CONFIG_FILE);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }
    
    /**
     * Initialize the configuration singleton.
     * 
     * @param dataFolder Plugin data folder
     * @return The configuration instance
     */
    @NotNull
    public static HyperSpawnsConfig init(@NotNull Path dataFolder) {
        instance = new HyperSpawnsConfig(dataFolder);
        instance.load();
        return instance;
    }
    
    /**
     * Get the configuration instance.
     * 
     * @throws IllegalStateException if not initialized
     */
    @NotNull
    public static HyperSpawnsConfig get() {
        if (instance == null) {
            throw new IllegalStateException("HyperSpawnsConfig not initialized");
        }
        return instance;
    }
    
    /**
     * Check if the configuration has been initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }
    
    /**
     * Load configuration from file.
     */
    public void load() {
        if (!Files.exists(configFile)) {
            save(); // Create default config
            Logger.info("Created default configuration file");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            loadFromJson(json);
            Logger.info("Loaded configuration from file");
        } catch (Exception e) {
            Logger.warning("Failed to load configuration: %s", e.getMessage());
            Logger.info("Using default configuration values");
        }
    }
    
    /**
     * Save configuration to file.
     */
    public void save() {
        try {
            Files.createDirectories(configFile.getParent());
            
            JsonObject json = toJson();
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                gson.toJson(json, writer);
            }
            
            Logger.debug("Saved configuration to file");
        } catch (IOException e) {
            Logger.severe("Failed to save configuration: %s", e.getMessage());
        }
    }
    
    /**
     * Reload configuration from file.
     */
    public void reload() {
        load();
    }
    
    private void loadFromJson(@NotNull JsonObject json) {
        if (json.has("debugMode")) {
            debugMode = json.get("debugMode").getAsBoolean();
        }
        if (json.has("autoSaveIntervalMinutes")) {
            autoSaveIntervalMinutes = json.get("autoSaveIntervalMinutes").getAsInt();
        }
        if (json.has("defaultZoneMode")) {
            ZoneMode mode = ZoneMode.fromString(json.get("defaultZoneMode").getAsString());
            if (mode != null) {
                defaultZoneMode = mode;
            }
        }
        if (json.has("bypassPermission")) {
            bypassPermission = json.get("bypassPermission").getAsString();
        }
        if (json.has("wandItemId")) {
            wandItemId = json.get("wandItemId").getAsString();
        }
        if (json.has("particlesEnabled")) {
            particlesEnabled = json.get("particlesEnabled").getAsBoolean();
        }
        if (json.has("particleViewDistance")) {
            particleViewDistance = json.get("particleViewDistance").getAsInt();
        }
        if (json.has("globalSpawnMultiplier")) {
            globalSpawnMultiplier = json.get("globalSpawnMultiplier").getAsDouble();
        }
        if (json.has("globalSpawnPaused")) {
            globalSpawnPaused = json.get("globalSpawnPaused").getAsBoolean();
        }
    }
    
    @NotNull
    private JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("debugMode", debugMode);
        json.addProperty("autoSaveIntervalMinutes", autoSaveIntervalMinutes);
        json.addProperty("defaultZoneMode", defaultZoneMode.getId());
        json.addProperty("bypassPermission", bypassPermission);
        json.addProperty("wandItemId", wandItemId);
        json.addProperty("particlesEnabled", particlesEnabled);
        json.addProperty("particleViewDistance", particleViewDistance);
        json.addProperty("globalSpawnMultiplier", globalSpawnMultiplier);
        json.addProperty("globalSpawnPaused", globalSpawnPaused);
        return json;
    }
    
    // Getters and Setters
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    public int getAutoSaveIntervalMinutes() {
        return autoSaveIntervalMinutes;
    }
    
    public void setAutoSaveIntervalMinutes(int minutes) {
        this.autoSaveIntervalMinutes = Math.max(1, minutes);
    }
    
    @NotNull
    public ZoneMode getDefaultZoneMode() {
        return defaultZoneMode;
    }
    
    public void setDefaultZoneMode(@NotNull ZoneMode mode) {
        this.defaultZoneMode = mode;
    }
    
    @NotNull
    public String getBypassPermission() {
        return bypassPermission;
    }
    
    public void setBypassPermission(@NotNull String permission) {
        this.bypassPermission = permission;
    }
    
    @NotNull
    public String getWandItemId() {
        return wandItemId;
    }
    
    public void setWandItemId(@NotNull String itemId) {
        this.wandItemId = itemId;
    }
    
    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }
    
    public void setParticlesEnabled(boolean enabled) {
        this.particlesEnabled = enabled;
    }
    
    public int getParticleViewDistance() {
        return particleViewDistance;
    }
    
    public void setParticleViewDistance(int distance) {
        this.particleViewDistance = Math.max(8, Math.min(128, distance));
    }
    
    public double getGlobalSpawnMultiplier() {
        return globalSpawnMultiplier;
    }
    
    public void setGlobalSpawnMultiplier(double multiplier) {
        this.globalSpawnMultiplier = Math.max(0.0, Math.min(10.0, multiplier));
    }
    
    public boolean isGlobalSpawnPaused() {
        return globalSpawnPaused;
    }
    
    public void setGlobalSpawnPaused(boolean paused) {
        this.globalSpawnPaused = paused;
    }
}
