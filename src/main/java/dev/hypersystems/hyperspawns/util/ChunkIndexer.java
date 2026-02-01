package dev.hypersystems.hyperspawns.util;

/**
 * Utility class for working with chunk coordinates and indices.
 * Uses the same indexing scheme as Hytale's ChunkUtil.
 */
public final class ChunkIndexer {
    
    /**
     * Chunk size in blocks (Hytale uses 32x32 chunks).
     */
    public static final int CHUNK_SIZE = 32;
    
    /**
     * Bit shift for chunk coordinate conversion.
     */
    public static final int CHUNK_SHIFT = 5; // log2(32)
    
    private ChunkIndexer() {}
    
    /**
     * Create a chunk index from chunk coordinates.
     * This packs two 32-bit integers into a 64-bit long.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return The combined chunk index
     */
    public static long indexChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    /**
     * Extract the chunk X coordinate from a chunk index.
     * 
     * @param chunkIndex The chunk index
     * @return The chunk X coordinate
     */
    public static int getChunkX(long chunkIndex) {
        return (int) (chunkIndex >> 32);
    }
    
    /**
     * Extract the chunk Z coordinate from a chunk index.
     * 
     * @param chunkIndex The chunk index
     * @return The chunk Z coordinate
     */
    public static int getChunkZ(long chunkIndex) {
        return (int) chunkIndex;
    }
    
    /**
     * Convert a block coordinate to a chunk coordinate.
     * 
     * @param blockCoord The block coordinate
     * @return The chunk coordinate
     */
    public static int blockToChunk(int blockCoord) {
        return blockCoord >> CHUNK_SHIFT;
    }
    
    /**
     * Convert a block coordinate to a chunk coordinate.
     * 
     * @param blockCoord The block coordinate
     * @return The chunk coordinate
     */
    public static int blockToChunk(double blockCoord) {
        return (int) Math.floor(blockCoord / CHUNK_SIZE);
    }
    
    /**
     * Get the chunk index for a block position.
     * 
     * @param blockX Block X coordinate
     * @param blockZ Block Z coordinate
     * @return The chunk index
     */
    public static long indexChunkFromBlock(int blockX, int blockZ) {
        return indexChunk(blockToChunk(blockX), blockToChunk(blockZ));
    }
    
    /**
     * Get the chunk index for a block position.
     * 
     * @param blockX Block X coordinate
     * @param blockZ Block Z coordinate
     * @return The chunk index
     */
    public static long indexChunkFromBlock(double blockX, double blockZ) {
        return indexChunk(blockToChunk(blockX), blockToChunk(blockZ));
    }
    
    /**
     * Get the minimum block X coordinate for a chunk.
     * 
     * @param chunkX Chunk X coordinate
     * @return Minimum block X
     */
    public static int getMinBlockX(int chunkX) {
        return chunkX * CHUNK_SIZE;
    }
    
    /**
     * Get the maximum block X coordinate for a chunk.
     * 
     * @param chunkX Chunk X coordinate
     * @return Maximum block X
     */
    public static int getMaxBlockX(int chunkX) {
        return chunkX * CHUNK_SIZE + CHUNK_SIZE - 1;
    }
    
    /**
     * Get the minimum block Z coordinate for a chunk.
     * 
     * @param chunkZ Chunk Z coordinate
     * @return Minimum block Z
     */
    public static int getMinBlockZ(int chunkZ) {
        return chunkZ * CHUNK_SIZE;
    }
    
    /**
     * Get the maximum block Z coordinate for a chunk.
     * 
     * @param chunkZ Chunk Z coordinate
     * @return Maximum block Z
     */
    public static int getMaxBlockZ(int chunkZ) {
        return chunkZ * CHUNK_SIZE + CHUNK_SIZE - 1;
    }
    
    /**
     * Get the local coordinate within a chunk (0-31).
     * 
     * @param blockCoord The block coordinate
     * @return The local coordinate
     */
    public static int localCoordinate(int blockCoord) {
        return blockCoord & (CHUNK_SIZE - 1);
    }
    
    /**
     * Format a chunk index as a readable string.
     * 
     * @param chunkIndex The chunk index
     * @return Formatted string like "chunk(10, -5)"
     */
    public static String formatChunkIndex(long chunkIndex) {
        return String.format("chunk(%d, %d)", getChunkX(chunkIndex), getChunkZ(chunkIndex));
    }
}
