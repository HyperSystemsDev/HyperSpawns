package dev.hypersystems.hyperspawns.util;

import org.jetbrains.annotations.NotNull;

/**
 * Centralized logging wrapper for HyperSpawns.
 */
public final class Logger {
    
    private static final String PREFIX = "[HyperSpawns] ";
    private static java.util.logging.Logger logger;
    
    private Logger() {}
    
    /**
     * Initialize the logger.
     */
    public static void init(@NotNull java.util.logging.Logger parentLogger) {
        logger = parentLogger;
    }
    
    /**
     * Log an info message.
     */
    public static void info(@NotNull String message) {
        if (logger != null) {
            logger.info(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[INFO] " + message);
        }
    }
    
    /**
     * Log an info message with format arguments.
     */
    public static void info(@NotNull String format, Object... args) {
        info(String.format(format, args));
    }
    
    /**
     * Log a warning message.
     */
    public static void warning(@NotNull String message) {
        if (logger != null) {
            logger.warning(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[WARN] " + message);
        }
    }
    
    /**
     * Log a warning message with format arguments.
     */
    public static void warning(@NotNull String format, Object... args) {
        warning(String.format(format, args));
    }
    
    /**
     * Log a severe error message.
     */
    public static void severe(@NotNull String message) {
        if (logger != null) {
            logger.severe(PREFIX + message);
        } else {
            System.err.println(PREFIX + "[SEVERE] " + message);
        }
    }
    
    /**
     * Log a severe error message with format arguments.
     */
    public static void severe(@NotNull String format, Object... args) {
        severe(String.format(format, args));
    }
    
    /**
     * Log a debug message (FINE level).
     */
    public static void debug(@NotNull String message) {
        if (logger != null) {
            logger.fine(PREFIX + message);
        }
    }
    
    /**
     * Log a debug message with format arguments.
     */
    public static void debug(@NotNull String format, Object... args) {
        debug(String.format(format, args));
    }
    
    /**
     * Log a trace message (FINEST level).
     */
    public static void trace(@NotNull String message) {
        if (logger != null) {
            logger.finest(PREFIX + message);
        }
    }
    
    /**
     * Log a trace message with format arguments.
     */
    public static void trace(@NotNull String format, Object... args) {
        trace(String.format(format, args));
    }
    
    /**
     * Check if debug logging is enabled.
     */
    public static boolean isDebugEnabled() {
        return logger != null && logger.isLoggable(java.util.logging.Level.FINE);
    }
    
    /**
     * Check if trace logging is enabled.
     */
    public static boolean isTraceEnabled() {
        return logger != null && logger.isLoggable(java.util.logging.Level.FINEST);
    }
}
