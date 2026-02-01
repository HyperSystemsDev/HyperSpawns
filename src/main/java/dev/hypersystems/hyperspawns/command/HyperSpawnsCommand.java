package dev.hypersystems.hyperspawns.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.hypersystems.hyperspawns.BuildInfo;
import dev.hypersystems.hyperspawns.HyperSpawns;
import dev.hypersystems.hyperspawns.config.HyperSpawnsConfig;
import dev.hypersystems.hyperspawns.util.Logger;
import dev.hypersystems.hyperspawns.zone.*;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Main HyperSpawns command handler.
 * Provides /hyperspawns command with various subcommands.
 */
public class HyperSpawnsCommand extends AbstractCommand {
    
    // Colors for messaging
    private static final Color CYAN = new Color(0, 255, 255);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GRAY = Color.GRAY;
    private static final Color WHITE = Color.WHITE;
    
    @SuppressWarnings("this-escape")
    public HyperSpawnsCommand() {
        super("hyperspawns", "Spawn zone management command");
        
        // Add subcommands
        addSubCommand(new HelpSubCommand());
        addSubCommand(new ZoneSubCommand());
        addSubCommand(new GlobalSubCommand());
        addSubCommand(new WandSubCommand());
        addSubCommand(new Pos1SubCommand());
        addSubCommand(new Pos2SubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new StatsSubCommand());
        addSubCommand(new DebugSubCommand());
        
        // Add aliases
        addAliases("hspawn", "spawns", "hs");
    }
    
    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        ctx.sender().sendMessage(buildHelpMessage());
        return CompletableFuture.completedFuture(null);
    }
    
    private Message buildHelpMessage() {
        List<Message> parts = new ArrayList<>();
        String label = "HyperSpawns v" + BuildInfo.VERSION;
        int width = 42;
        int padding = width - label.length() - 2;
        int left = 3;
        int right = Math.max(3, padding - left);
        
        parts.add(Message.raw("-".repeat(left) + " ").color(GRAY));
        parts.add(Message.raw(label).color(CYAN));
        parts.add(Message.raw(" " + "-".repeat(right) + "\n").color(GRAY));
        parts.add(Message.raw("  Spawn zone management\n\n").color(WHITE));
        parts.add(Message.raw("  Commands:\n").color(GOLD));
        parts.add(Message.raw("    zone").color(GREEN));
        parts.add(Message.raw(" - Manage spawn zones\n").color(GRAY));
        parts.add(Message.raw("    global").color(GREEN));
        parts.add(Message.raw(" - Global spawn controls\n").color(GRAY));
        parts.add(Message.raw("    wand").color(GREEN));
        parts.add(Message.raw(" - Get selection wand\n").color(GRAY));
        parts.add(Message.raw("    pos1/pos2").color(GREEN));
        parts.add(Message.raw(" - Set selection positions\n").color(GRAY));
        parts.add(Message.raw("    reload").color(GREEN));
        parts.add(Message.raw(" - Reload configuration\n").color(GRAY));
        parts.add(Message.raw("    stats").color(GREEN));
        parts.add(Message.raw(" - View statistics\n").color(GRAY));
        parts.add(Message.raw("\n  Use /hyperspawns <command> for details\n").color(GRAY));
        parts.add(Message.raw("-".repeat(width) + "\n").color(GRAY));
        
        return Message.join(parts.toArray(new Message[0]));
    }
    
    /**
     * Send a success message.
     */
    private static void sendSuccess(CommandContext ctx, String message) {
        ctx.sender().sendMessage(Message.join(
                Message.raw("[HyperSpawns] ").color(CYAN),
                Message.raw(message).color(GREEN)));
    }
    
    /**
     * Send an error message.
     */
    private static void sendError(CommandContext ctx, String message) {
        ctx.sender().sendMessage(Message.join(
                Message.raw("[HyperSpawns] ").color(CYAN),
                Message.raw(message).color(RED)));
    }
    
    /**
     * Send an info message.
     */
    private static void sendInfo(CommandContext ctx, String message) {
        ctx.sender().sendMessage(Message.join(
                Message.raw("[HyperSpawns] ").color(CYAN),
                Message.raw(message).color(WHITE)));
    }
    
    /**
     * Get player from command context.
     */
    private static Player getPlayer(CommandContext ctx) {
        if (ctx.sender() instanceof Player player) {
            return player;
        }
        return null;
    }
    
    // ========== Help Subcommand ==========
    
    private class HelpSubCommand extends AbstractCommand {
        public HelpSubCommand() {
            super("help", "Show help information");
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(buildHelpMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== Zone Subcommand ==========
    
    private class ZoneSubCommand extends AbstractCommand {
        @SuppressWarnings("this-escape")
        public ZoneSubCommand() {
            super("zone", "Manage spawn zones");
            
            addSubCommand(new ZoneCreateSubCommand());
            addSubCommand(new ZoneDeleteSubCommand());
            addSubCommand(new ZoneListSubCommand());
            addSubCommand(new ZoneInfoSubCommand());
            addSubCommand(new ZoneRedefineSubCommand());
            addSubCommand(new ZoneModeSubCommand());
            addSubCommand(new ZoneMultiplierSubCommand());
            addSubCommand(new ZonePrioritySubCommand());
            addSubCommand(new ZoneEnableSubCommand());
            addSubCommand(new ZoneDisableSubCommand());
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            sendInfo(ctx, "Zone subcommands: create, delete, list, info, redefine, mode, multiplier, priority, enable, disable");
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Create
    private class ZoneCreateSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        private final OptionalArg<String> typeArg;
        
        public ZoneCreateSubCommand() {
            super("create", "Create a new spawn zone");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
            this.typeArg = withOptionalArg("type", "Boundary type (cuboid, sphere, cylinder)", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            Player player = getPlayer(ctx);
            if (player == null) {
                sendError(ctx, "This command can only be used by players");
                return CompletableFuture.completedFuture(null);
            }
            
            String name = ctx.get(nameArg);
            String type = ctx.get(typeArg);
            if (type == null || type.isBlank()) {
                type = "cuboid";
            }
            
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            WandSelectionManager wandManager = HyperSpawns.get().getWandManager();
            
            if (manager.zoneExists(name)) {
                sendError(ctx, "A zone named '" + name + "' already exists");
                return CompletableFuture.completedFuture(null);
            }
            
            World world = player.getWorld();
            String worldName = world != null ? world.getName() : "default";
            ZoneBoundary boundary;
            
            switch (type.toLowerCase()) {
                case "cuboid" -> {
                    WandSelectionManager.Selection selection = wandManager.getSelection(player.getUuid());
                    if (selection == null || !selection.isComplete()) {
                        sendError(ctx, "You must select two positions with pos1/pos2 first");
                        return CompletableFuture.completedFuture(null);
                    }
                    boundary = wandManager.getCuboidBoundary(player.getUuid());
                    if (selection.getWorld() != null) {
                        worldName = selection.getWorld();
                    }
                }
                case "sphere" -> {
                    WandSelectionManager.Selection selection = wandManager.getSelection(player.getUuid());
                    if (selection == null || !selection.isComplete()) {
                        sendError(ctx, "You must select two positions (center and radius point)");
                        return CompletableFuture.completedFuture(null);
                    }
                    double centerX = selection.getCenterX();
                    double centerY = selection.getCenterY();
                    double centerZ = selection.getCenterZ();
                    double radius = Math.sqrt(
                            Math.pow(selection.getPos2X() - centerX, 2) +
                            Math.pow(selection.getPos2Y() - centerY, 2) +
                            Math.pow(selection.getPos2Z() - centerZ, 2)
                    );
                    boundary = new SphereBoundary(centerX, centerY, centerZ, Math.max(1, radius));
                    if (selection.getWorld() != null) {
                        worldName = selection.getWorld();
                    }
                }
                case "cylinder" -> {
                    WandSelectionManager.Selection selection = wandManager.getSelection(player.getUuid());
                    if (selection == null || !selection.isComplete()) {
                        sendError(ctx, "You must select two positions (defines center, radius, and height)");
                        return CompletableFuture.completedFuture(null);
                    }
                    double centerX = selection.getCenterX();
                    double centerZ = selection.getCenterZ();
                    double radius = selection.getHorizontalRadius();
                    boundary = new CylinderBoundary(centerX, centerZ, Math.max(1, radius),
                            Math.min(selection.getPos1Y(), selection.getPos2Y()),
                            Math.max(selection.getPos1Y(), selection.getPos2Y()));
                    if (selection.getWorld() != null) {
                        worldName = selection.getWorld();
                    }
                }
                default -> {
                    sendError(ctx, "Unknown boundary type: " + type + " (use: cuboid, sphere, cylinder)");
                    return CompletableFuture.completedFuture(null);
                }
            }
            
            SpawnZone zone = manager.createZone(name, worldName, boundary);
            if (zone != null) {
                zone.setMode(HyperSpawnsConfig.get().getDefaultZoneMode());
                sendSuccess(ctx, "Created zone '" + name + "' with " + type + " boundary");
                wandManager.clearSelection(player.getUuid());
            } else {
                sendError(ctx, "Failed to create zone");
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Delete
    private class ZoneDeleteSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        
        public ZoneDeleteSubCommand() {
            super("delete", "Delete a spawn zone");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String name = ctx.get(nameArg);
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            
            if (manager.deleteZone(name)) {
                sendSuccess(ctx, "Deleted zone '" + name + "'");
            } else {
                sendError(ctx, "Zone '" + name + "' not found");
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone List
    private class ZoneListSubCommand extends AbstractCommand {
        private final OptionalArg<Integer> pageArg;
        
        public ZoneListSubCommand() {
            super("list", "List all spawn zones");
            this.pageArg = withOptionalArg("page", "Page number", ArgTypes.INTEGER);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            Integer pageVal = ctx.get(pageArg);
            int page = (pageVal != null) ? pageVal : 1;
            int perPage = 10;
            
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            List<SpawnZone> zones = new ArrayList<>(manager.getAllZones());
            zones.sort(Comparator.comparing(SpawnZone::getName));
            
            int totalPages = (int) Math.ceil((double) zones.size() / perPage);
            page = Math.max(1, Math.min(page, totalPages));
            
            List<Message> parts = new ArrayList<>();
            parts.add(Message.raw("--- Spawn Zones (Page " + page + "/" + Math.max(1, totalPages) + ") ---\n").color(CYAN));
            
            if (zones.isEmpty()) {
                parts.add(Message.raw("  No zones defined\n").color(GRAY));
            } else {
                int start = (page - 1) * perPage;
                int end = Math.min(start + perPage, zones.size());
                
                for (int i = start; i < end; i++) {
                    SpawnZone zone = zones.get(i);
                    String status = zone.isEnabled() ? "+" : "-";
                    Color statusColor = zone.isEnabled() ? GREEN : RED;
                    
                    parts.add(Message.raw("  " + status + " ").color(statusColor));
                    parts.add(Message.raw(zone.getName()).color(WHITE));
                    parts.add(Message.raw(" [" + zone.getMode().getId() + "]").color(GOLD));
                    parts.add(Message.raw(" - " + zone.getWorld() + "\n").color(GRAY));
                }
            }
            
            parts.add(Message.raw("Total: " + zones.size() + " zones\n").color(GRAY));
            
            ctx.sender().sendMessage(Message.join(parts.toArray(new Message[0])));
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Info
    private class ZoneInfoSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        
        public ZoneInfoSubCommand() {
            super("info", "Show zone information");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String name = ctx.get(nameArg);
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            SpawnZone zone = manager.getZone(name);
            
            if (zone == null) {
                sendError(ctx, "Zone '" + name + "' not found");
                return CompletableFuture.completedFuture(null);
            }
            
            List<Message> parts = new ArrayList<>();
            parts.add(Message.raw("--- Zone: " + zone.getName() + " ---\n").color(CYAN));
            parts.add(Message.raw("  ID: ").color(GRAY));
            parts.add(Message.raw(zone.getId().toString() + "\n").color(WHITE));
            parts.add(Message.raw("  World: ").color(GRAY));
            parts.add(Message.raw(zone.getWorld() + "\n").color(WHITE));
            parts.add(Message.raw("  Enabled: ").color(GRAY));
            parts.add(Message.raw((zone.isEnabled() ? "Yes" : "No") + "\n").color(zone.isEnabled() ? GREEN : RED));
            parts.add(Message.raw("  Mode: ").color(GRAY));
            parts.add(Message.raw(zone.getMode().name() + "\n").color(GOLD));
            parts.add(Message.raw("  Priority: ").color(GRAY));
            parts.add(Message.raw(zone.getPriority() + "\n").color(WHITE));
            parts.add(Message.raw("  Boundary: ").color(GRAY));
            parts.add(Message.raw(zone.getBoundary().describe() + "\n").color(WHITE));
            
            if (zone.getMode() == ZoneMode.MODIFY) {
                parts.add(Message.raw("  Spawn Multiplier: ").color(GRAY));
                parts.add(Message.raw(String.format("%.2fx\n", zone.getSpawnRateMultiplier())).color(WHITE));
            }
            if (zone.getMode() == ZoneMode.REPLACE && zone.getReplacementNpc() != null) {
                parts.add(Message.raw("  Replacement NPC: ").color(GRAY));
                parts.add(Message.raw(zone.getReplacementNpc() + "\n").color(WHITE));
            }
            
            ZoneFilter filter = zone.getFilter();
            if (!filter.isEmpty()) {
                parts.add(Message.raw("  Filter: ").color(GRAY));
                parts.add(Message.raw(filter.toString() + "\n").color(WHITE));
            }
            
            ctx.sender().sendMessage(Message.join(parts.toArray(new Message[0])));
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Redefine
    private class ZoneRedefineSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        
        public ZoneRedefineSubCommand() {
            super("redefine", "Redefine zone boundary from selection");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            Player player = getPlayer(ctx);
            if (player == null) {
                sendError(ctx, "This command can only be used by players");
                return CompletableFuture.completedFuture(null);
            }
            
            String name = ctx.get(nameArg);
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            WandSelectionManager wandManager = HyperSpawns.get().getWandManager();
            
            SpawnZone zone = manager.getZone(name);
            if (zone == null) {
                sendError(ctx, "Zone '" + name + "' not found");
                return CompletableFuture.completedFuture(null);
            }
            
            CuboidBoundary boundary = wandManager.getCuboidBoundary(player.getUuid());
            if (boundary == null) {
                sendError(ctx, "You must select two positions with pos1/pos2 first");
                return CompletableFuture.completedFuture(null);
            }
            
            manager.updateBoundary(zone, boundary);
            sendSuccess(ctx, "Redefined boundary for zone '" + name + "'");
            wandManager.clearSelection(player.getUuid());
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Mode
    private class ZoneModeSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        private final RequiredArg<String> modeArg;
        
        public ZoneModeSubCommand() {
            super("mode", "Set zone spawn control mode");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
            this.modeArg = withRequiredArg("mode", "Mode (BLOCK, ALLOW, DENY, MODIFY, REPLACE)", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String name = ctx.get(nameArg);
            String modeStr = ctx.get(modeArg);
            
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            SpawnZone zone = manager.getZone(name);
            
            if (zone == null) {
                sendError(ctx, "Zone '" + name + "' not found");
                return CompletableFuture.completedFuture(null);
            }
            
            ZoneMode mode = ZoneMode.fromString(modeStr);
            if (mode == null) {
                sendError(ctx, "Invalid mode: " + modeStr + " (use: BLOCK, ALLOW, DENY, MODIFY, REPLACE)");
                return CompletableFuture.completedFuture(null);
            }
            
            zone.setMode(mode);
            manager.markModified(zone);
            sendSuccess(ctx, "Set zone '" + name + "' mode to " + mode.name());
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Multiplier
    private class ZoneMultiplierSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        private final RequiredArg<Double> valueArg;
        
        public ZoneMultiplierSubCommand() {
            super("multiplier", "Set zone spawn rate multiplier");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
            this.valueArg = withRequiredArg("value", "Multiplier value (0.0-10.0)", ArgTypes.DOUBLE);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String name = ctx.get(nameArg);
            double value = ctx.get(valueArg);
            
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            SpawnZone zone = manager.getZone(name);
            
            if (zone == null) {
                sendError(ctx, "Zone '" + name + "' not found");
                return CompletableFuture.completedFuture(null);
            }
            
            zone.setSpawnRateMultiplier(value);
            manager.markModified(zone);
            sendSuccess(ctx, String.format("Set zone '%s' spawn multiplier to %.2fx", name, zone.getSpawnRateMultiplier()));
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Priority
    private class ZonePrioritySubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        private final RequiredArg<Integer> valueArg;
        
        public ZonePrioritySubCommand() {
            super("priority", "Set zone priority");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
            this.valueArg = withRequiredArg("value", "Priority value", ArgTypes.INTEGER);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String name = ctx.get(nameArg);
            int value = ctx.get(valueArg);
            
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            SpawnZone zone = manager.getZone(name);
            
            if (zone == null) {
                sendError(ctx, "Zone '" + name + "' not found");
                return CompletableFuture.completedFuture(null);
            }
            
            zone.setPriority(value);
            manager.markModified(zone);
            sendSuccess(ctx, "Set zone '" + name + "' priority to " + value);
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Enable
    private class ZoneEnableSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        
        public ZoneEnableSubCommand() {
            super("enable", "Enable a spawn zone");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String name = ctx.get(nameArg);
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            SpawnZone zone = manager.getZone(name);
            
            if (zone == null) {
                sendError(ctx, "Zone '" + name + "' not found");
                return CompletableFuture.completedFuture(null);
            }
            
            zone.setEnabled(true);
            manager.markModified(zone);
            sendSuccess(ctx, "Enabled zone '" + name + "'");
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Zone Disable
    private class ZoneDisableSubCommand extends AbstractCommand {
        private final RequiredArg<String> nameArg;
        
        public ZoneDisableSubCommand() {
            super("disable", "Disable a spawn zone");
            this.nameArg = withRequiredArg("name", "Zone name", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String name = ctx.get(nameArg);
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            SpawnZone zone = manager.getZone(name);
            
            if (zone == null) {
                sendError(ctx, "Zone '" + name + "' not found");
                return CompletableFuture.completedFuture(null);
            }
            
            zone.setEnabled(false);
            manager.markModified(zone);
            sendSuccess(ctx, "Disabled zone '" + name + "'");
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== Global Subcommand ==========
    
    private class GlobalSubCommand extends AbstractCommand {
        @SuppressWarnings("this-escape")
        public GlobalSubCommand() {
            super("global", "Global spawn controls");
            addSubCommand(new GlobalMultiplierSubCommand());
            addSubCommand(new GlobalPauseSubCommand());
            addSubCommand(new GlobalResumeSubCommand());
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            HyperSpawnsConfig config = HyperSpawnsConfig.get();
            
            List<Message> parts = new ArrayList<>();
            parts.add(Message.raw("--- Global Spawn Settings ---\n").color(CYAN));
            parts.add(Message.raw("  Multiplier: ").color(GRAY));
            parts.add(Message.raw(String.format("%.2fx\n", config.getGlobalSpawnMultiplier())).color(WHITE));
            parts.add(Message.raw("  Paused: ").color(GRAY));
            parts.add(Message.raw((config.isGlobalSpawnPaused() ? "Yes" : "No") + "\n").color(config.isGlobalSpawnPaused() ? RED : GREEN));
            
            ctx.sender().sendMessage(Message.join(parts.toArray(new Message[0])));
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private class GlobalMultiplierSubCommand extends AbstractCommand {
        private final RequiredArg<Double> valueArg;
        
        public GlobalMultiplierSubCommand() {
            super("multiplier", "Set global spawn rate multiplier");
            this.valueArg = withRequiredArg("value", "Multiplier value (0.0-10.0)", ArgTypes.DOUBLE);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            double value = ctx.get(valueArg);
            HyperSpawnsConfig config = HyperSpawnsConfig.get();
            config.setGlobalSpawnMultiplier(value);
            config.save();
            sendSuccess(ctx, String.format("Set global spawn multiplier to %.2fx", config.getGlobalSpawnMultiplier()));
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private class GlobalPauseSubCommand extends AbstractCommand {
        public GlobalPauseSubCommand() {
            super("pause", "Pause all mob spawning");
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            HyperSpawnsConfig config = HyperSpawnsConfig.get();
            config.setGlobalSpawnPaused(true);
            config.save();
            sendSuccess(ctx, "Paused all mob spawning globally");
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private class GlobalResumeSubCommand extends AbstractCommand {
        public GlobalResumeSubCommand() {
            super("resume", "Resume mob spawning");
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            HyperSpawnsConfig config = HyperSpawnsConfig.get();
            config.setGlobalSpawnPaused(false);
            config.save();
            sendSuccess(ctx, "Resumed mob spawning globally");
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== Wand Subcommand ==========
    
    private class WandSubCommand extends AbstractCommand {
        public WandSubCommand() {
            super("wand", "Get selection wand information");
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            sendInfo(ctx, "Use /hyperspawns pos1 and /hyperspawns pos2 to set selection positions");
            sendInfo(ctx, "Wand item: " + HyperSpawnsConfig.get().getWandItemId());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== Pos1/Pos2 Subcommands ==========
    
    private class Pos1SubCommand extends AbstractCommand {
        private final RequiredArg<Double> xArg;
        private final RequiredArg<Double> yArg;
        private final RequiredArg<Double> zArg;
        private final OptionalArg<String> worldArg;
        
        public Pos1SubCommand() {
            super("pos1", "Set selection position 1");
            this.xArg = withRequiredArg("x", "X coordinate", ArgTypes.DOUBLE);
            this.yArg = withRequiredArg("y", "Y coordinate", ArgTypes.DOUBLE);
            this.zArg = withRequiredArg("z", "Z coordinate", ArgTypes.DOUBLE);
            this.worldArg = withOptionalArg("world", "World name", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            double x = ctx.get(xArg);
            double y = ctx.get(yArg);
            double z = ctx.get(zArg);
            String worldName = ctx.get(worldArg);
            if (worldName == null || worldName.isBlank()) {
                worldName = "default";
            }
            
            // Get sender UUID (may be null for console)
            UUID senderId = ctx.sender().getUuid();
            if (senderId == null) {
                senderId = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Console placeholder
            }
            
            HyperSpawns.get().getWandManager().setPos1(senderId, worldName, x, y, z);
            sendSuccess(ctx, String.format("Position 1 set to (%.1f, %.1f, %.1f) in world '%s'", x, y, z, worldName));
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private class Pos2SubCommand extends AbstractCommand {
        private final RequiredArg<Double> xArg;
        private final RequiredArg<Double> yArg;
        private final RequiredArg<Double> zArg;
        private final OptionalArg<String> worldArg;
        
        public Pos2SubCommand() {
            super("pos2", "Set selection position 2");
            this.xArg = withRequiredArg("x", "X coordinate", ArgTypes.DOUBLE);
            this.yArg = withRequiredArg("y", "Y coordinate", ArgTypes.DOUBLE);
            this.zArg = withRequiredArg("z", "Z coordinate", ArgTypes.DOUBLE);
            this.worldArg = withOptionalArg("world", "World name", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            double x = ctx.get(xArg);
            double y = ctx.get(yArg);
            double z = ctx.get(zArg);
            String worldName = ctx.get(worldArg);
            if (worldName == null || worldName.isBlank()) {
                worldName = "default";
            }
            
            // Get sender UUID (may be null for console)
            UUID senderId = ctx.sender().getUuid();
            if (senderId == null) {
                senderId = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Console placeholder
            }
            
            HyperSpawns.get().getWandManager().setPos2(senderId, worldName, x, y, z);
            sendSuccess(ctx, String.format("Position 2 set to (%.1f, %.1f, %.1f) in world '%s'", x, y, z, worldName));
            
            WandSelectionManager.Selection selection = HyperSpawns.get().getWandManager().getSelection(senderId);
            if (selection != null && selection.isComplete()) {
                sendInfo(ctx, selection.describe());
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== Reload Subcommand ==========
    
    private class ReloadSubCommand extends AbstractCommand {
        public ReloadSubCommand() {
            super("reload", "Reload configuration and zones");
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            HyperSpawns.get().reload();
            sendSuccess(ctx, "Configuration and zones reloaded");
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== Stats Subcommand ==========
    
    private class StatsSubCommand extends AbstractCommand {
        public StatsSubCommand() {
            super("stats", "View spawn zone statistics");
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            SpawnZoneManager manager = HyperSpawns.get().getZoneManager();
            HyperSpawnsConfig config = HyperSpawnsConfig.get();
            
            List<Message> parts = new ArrayList<>();
            parts.add(Message.raw("--- HyperSpawns Statistics ---\n").color(CYAN));
            parts.add(Message.raw("  Version: ").color(GRAY));
            parts.add(Message.raw(BuildInfo.VERSION + "\n").color(WHITE));
            parts.add(Message.raw("  " + manager.getStats() + "\n").color(WHITE));
            parts.add(Message.raw("  Global Multiplier: ").color(GRAY));
            parts.add(Message.raw(String.format("%.2fx\n", config.getGlobalSpawnMultiplier())).color(WHITE));
            parts.add(Message.raw("  Global Paused: ").color(GRAY));
            parts.add(Message.raw((config.isGlobalSpawnPaused() ? "Yes" : "No") + "\n").color(config.isGlobalSpawnPaused() ? RED : GREEN));
            parts.add(Message.raw("  Debug Mode: ").color(GRAY));
            parts.add(Message.raw((config.isDebugMode() ? "On" : "Off") + "\n").color(config.isDebugMode() ? GOLD : WHITE));
            
            ctx.sender().sendMessage(Message.join(parts.toArray(new Message[0])));
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== Debug Subcommand ==========
    
    private class DebugSubCommand extends AbstractCommand {
        private final OptionalArg<String> stateArg;
        
        public DebugSubCommand() {
            super("debug", "Toggle debug mode");
            this.stateArg = withOptionalArg("state", "on or off", ArgTypes.STRING);
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String state = ctx.get(stateArg);
            HyperSpawnsConfig config = HyperSpawnsConfig.get();
            
            if (state == null) {
                // Toggle
                config.setDebugMode(!config.isDebugMode());
            } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("true")) {
                config.setDebugMode(true);
            } else if (state.equalsIgnoreCase("off") || state.equalsIgnoreCase("false")) {
                config.setDebugMode(false);
            } else {
                sendError(ctx, "Invalid state: " + state + " (use: on/off)");
                return CompletableFuture.completedFuture(null);
            }
            
            config.save();
            sendSuccess(ctx, "Debug mode " + (config.isDebugMode() ? "enabled" : "disabled"));
            
            return CompletableFuture.completedFuture(null);
        }
    }
}
