package dev.hypersystems.hyperspawns.persistence;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Handles JSON serialization and deserialization of spawn zones.
 */
public final class ZonePersistence {
    
    private static final int SCHEMA_VERSION = 1;
    private static final String ZONES_FILE = "zones.json";
    
    private final Path dataFolder;
    private final Gson gson;
    
    /**
     * Create a new persistence handler.
     * 
     * @param dataFolder Plugin data folder
     */
    public ZonePersistence(@NotNull Path dataFolder) {
        this.dataFolder = dataFolder;
        this.gson = createGson();
    }
    
    /**
     * Create the GSON instance with custom adapters.
     */
    private Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(ZoneBoundary.class, new ZoneBoundaryAdapter())
                .registerTypeAdapter(ZoneFilter.class, new ZoneFilterAdapter())
                .registerTypeAdapter(ZoneMode.class, new ZoneModeAdapter())
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .create();
    }
    
    /**
     * Load zones from the data file.
     * 
     * @return List of loaded zones, empty if file doesn't exist
     */
    @NotNull
    public List<SpawnZone> loadZones() {
        Path zonesFile = dataFolder.resolve(ZONES_FILE);
        
        if (!Files.exists(zonesFile)) {
            Logger.info("No zones file found, starting fresh");
            return Collections.emptyList();
        }
        
        try (Reader reader = Files.newBufferedReader(zonesFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            // Check schema version
            int version = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 0;
            if (version > SCHEMA_VERSION) {
                Logger.warning("Zones file schema version %d is newer than supported %d", version, SCHEMA_VERSION);
            }
            
            // Parse zones array
            JsonArray zonesArray = root.getAsJsonArray("zones");
            if (zonesArray == null) {
                Logger.warning("No zones array found in file");
                return Collections.emptyList();
            }
            
            List<SpawnZone> zones = new ArrayList<>();
            for (JsonElement element : zonesArray) {
                try {
                    SpawnZone zone = parseZone(element.getAsJsonObject());
                    if (zone != null) {
                        zones.add(zone);
                    }
                } catch (Exception e) {
                    Logger.warning("Failed to parse zone: %s", e.getMessage());
                }
            }
            
            Logger.info("Loaded %d zones from file", zones.size());
            return zones;
            
        } catch (IOException e) {
            Logger.severe("Failed to load zones: %s", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Save zones to the data file.
     * 
     * @param zones Collection of zones to save
     */
    public void saveZones(@NotNull Collection<SpawnZone> zones) {
        Path zonesFile = dataFolder.resolve(ZONES_FILE);
        Path tempFile = dataFolder.resolve(ZONES_FILE + ".tmp");
        Path backupFile = dataFolder.resolve(ZONES_FILE + ".bak");
        
        try {
            // Ensure directory exists
            Files.createDirectories(dataFolder);
            
            // Build JSON structure
            JsonObject root = new JsonObject();
            root.addProperty("schemaVersion", SCHEMA_VERSION);
            root.addProperty("lastSaved", System.currentTimeMillis());
            
            JsonArray zonesArray = new JsonArray();
            for (SpawnZone zone : zones) {
                zonesArray.add(serializeZone(zone));
            }
            root.add("zones", zonesArray);
            
            // Write to temp file first
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }
            
            // Backup existing file
            if (Files.exists(zonesFile)) {
                Files.copy(zonesFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Move temp to actual file
            Files.move(tempFile, zonesFile, StandardCopyOption.REPLACE_EXISTING);
            
            Logger.debug("Saved %d zones to file", zones.size());
            
        } catch (IOException e) {
            Logger.severe("Failed to save zones: %s", e.getMessage());
        }
    }
    
    /**
     * Parse a zone from JSON.
     */
    @Nullable
    private SpawnZone parseZone(@NotNull JsonObject json) {
        UUID id = gson.fromJson(json.get("id"), UUID.class);
        String name = json.get("name").getAsString();
        String world = json.get("world").getAsString();
        ZoneBoundary boundary = gson.fromJson(json.get("boundary"), ZoneBoundary.class);
        
        if (id == null || name == null || world == null || boundary == null) {
            return null;
        }
        
        ZoneMode mode = json.has("mode") 
                ? gson.fromJson(json.get("mode"), ZoneMode.class) 
                : ZoneMode.BLOCK;
        
        ZoneFilter filter = json.has("filter")
                ? gson.fromJson(json.get("filter"), ZoneFilter.class)
                : ZoneFilter.empty();
        
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
        double multiplier = json.has("spawnRateMultiplier") 
                ? json.get("spawnRateMultiplier").getAsDouble() 
                : 1.0;
        String replacementNpc = json.has("replacementNpc") && !json.get("replacementNpc").isJsonNull()
                ? json.get("replacementNpc").getAsString()
                : null;
        
        return new SpawnZone(id, name, world, boundary, mode, filter, priority, enabled, multiplier, replacementNpc);
    }
    
    /**
     * Serialize a zone to JSON.
     */
    @NotNull
    private JsonObject serializeZone(@NotNull SpawnZone zone) {
        JsonObject json = new JsonObject();
        json.add("id", gson.toJsonTree(zone.getId()));
        json.addProperty("name", zone.getName());
        json.addProperty("world", zone.getWorld());
        json.add("boundary", gson.toJsonTree(zone.getBoundary(), ZoneBoundary.class));
        json.add("mode", gson.toJsonTree(zone.getMode()));
        json.add("filter", gson.toJsonTree(zone.getFilter(), ZoneFilter.class));
        json.addProperty("priority", zone.getPriority());
        json.addProperty("enabled", zone.isEnabled());
        json.addProperty("spawnRateMultiplier", zone.getSpawnRateMultiplier());
        if (zone.getReplacementNpc() != null) {
            json.addProperty("replacementNpc", zone.getReplacementNpc());
        }
        return json;
    }
    
    /**
     * Adapter for ZoneBoundary sealed interface.
     */
    private static class ZoneBoundaryAdapter implements JsonSerializer<ZoneBoundary>, JsonDeserializer<ZoneBoundary> {
        
        @Override
        public JsonElement serialize(ZoneBoundary src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("type", src.getType());
            
            switch (src) {
                case CuboidBoundary cuboid -> {
                    json.addProperty("minX", cuboid.getMinX());
                    json.addProperty("minY", cuboid.getMinY());
                    json.addProperty("minZ", cuboid.getMinZ());
                    json.addProperty("maxX", cuboid.getMaxX());
                    json.addProperty("maxY", cuboid.getMaxY());
                    json.addProperty("maxZ", cuboid.getMaxZ());
                }
                case SphereBoundary sphere -> {
                    json.addProperty("centerX", sphere.getCenterX());
                    json.addProperty("centerY", sphere.getCenterY());
                    json.addProperty("centerZ", sphere.getCenterZ());
                    json.addProperty("radius", sphere.getRadius());
                }
                case CylinderBoundary cylinder -> {
                    json.addProperty("centerX", cylinder.getCenterX());
                    json.addProperty("centerZ", cylinder.getCenterZ());
                    json.addProperty("radius", cylinder.getRadius());
                    json.addProperty("minY", cylinder.getMinY());
                    json.addProperty("maxY", cylinder.getMaxY());
                }
            }
            
            return json;
        }
        
        @Override
        public ZoneBoundary deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();
            
            return switch (type) {
                case CuboidBoundary.TYPE -> new CuboidBoundary(
                        obj.get("minX").getAsDouble(),
                        obj.get("minY").getAsDouble(),
                        obj.get("minZ").getAsDouble(),
                        obj.get("maxX").getAsDouble(),
                        obj.get("maxY").getAsDouble(),
                        obj.get("maxZ").getAsDouble()
                );
                case SphereBoundary.TYPE -> new SphereBoundary(
                        obj.get("centerX").getAsDouble(),
                        obj.get("centerY").getAsDouble(),
                        obj.get("centerZ").getAsDouble(),
                        obj.get("radius").getAsDouble()
                );
                case CylinderBoundary.TYPE -> new CylinderBoundary(
                        obj.get("centerX").getAsDouble(),
                        obj.get("centerZ").getAsDouble(),
                        obj.get("radius").getAsDouble(),
                        obj.get("minY").getAsDouble(),
                        obj.get("maxY").getAsDouble()
                );
                default -> throw new JsonParseException("Unknown boundary type: " + type);
            };
        }
    }
    
    /**
     * Adapter for ZoneFilter.
     */
    private static class ZoneFilterAdapter implements JsonSerializer<ZoneFilter>, JsonDeserializer<ZoneFilter> {
        
        @Override
        public JsonElement serialize(ZoneFilter src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            
            if (!src.getNpcGroups().isEmpty()) {
                json.add("npcGroups", context.serialize(src.getNpcGroups()));
            }
            if (!src.getNpcRoles().isEmpty()) {
                json.add("npcRoles", context.serialize(src.getNpcRoles()));
            }
            if (src.getMinLightLevel() != null) {
                json.addProperty("minLightLevel", src.getMinLightLevel());
            }
            if (src.getMaxLightLevel() != null) {
                json.addProperty("maxLightLevel", src.getMaxLightLevel());
            }
            if (src.getMinYLevel() != null) {
                json.addProperty("minYLevel", src.getMinYLevel());
            }
            if (src.getMaxYLevel() != null) {
                json.addProperty("maxYLevel", src.getMaxYLevel());
            }
            if (!src.getTimeOfDay().isEmpty()) {
                json.add("timeOfDay", context.serialize(src.getTimeOfDay()));
            }
            if (!src.getMoonPhases().isEmpty()) {
                json.add("moonPhases", context.serialize(src.getMoonPhases()));
            }
            
            return json;
        }
        
        @Override
        public ZoneFilter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            ZoneFilter.Builder builder = ZoneFilter.builder();
            
            if (obj.has("npcGroups")) {
                Set<String> groups = context.deserialize(obj.get("npcGroups"), new TypeToken<Set<String>>(){}.getType());
                builder.npcGroups(groups);
            }
            if (obj.has("npcRoles")) {
                Set<String> roles = context.deserialize(obj.get("npcRoles"), new TypeToken<Set<String>>(){}.getType());
                builder.npcRoles(roles);
            }
            if (obj.has("minLightLevel")) {
                builder.minLightLevel(obj.get("minLightLevel").getAsInt());
            }
            if (obj.has("maxLightLevel")) {
                builder.maxLightLevel(obj.get("maxLightLevel").getAsInt());
            }
            if (obj.has("minYLevel")) {
                builder.minYLevel(obj.get("minYLevel").getAsInt());
            }
            if (obj.has("maxYLevel")) {
                builder.maxYLevel(obj.get("maxYLevel").getAsInt());
            }
            if (obj.has("timeOfDay")) {
                Set<String> times = context.deserialize(obj.get("timeOfDay"), new TypeToken<Set<String>>(){}.getType());
                builder.timeOfDay(times);
            }
            if (obj.has("moonPhases")) {
                Set<Integer> phases = context.deserialize(obj.get("moonPhases"), new TypeToken<Set<Integer>>(){}.getType());
                builder.moonPhases(phases);
            }
            
            return builder.build();
        }
    }
    
    /**
     * Adapter for ZoneMode enum.
     */
    private static class ZoneModeAdapter implements JsonSerializer<ZoneMode>, JsonDeserializer<ZoneMode> {
        
        @Override
        public JsonElement serialize(ZoneMode src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getId());
        }
        
        @Override
        public ZoneMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ZoneMode mode = ZoneMode.fromString(json.getAsString());
            return mode != null ? mode : ZoneMode.BLOCK;
        }
    }
    
    /**
     * Adapter for UUID.
     */
    private static class UUIDAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        
        @Override
        public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        
        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return UUID.fromString(json.getAsString());
        }
    }
}
