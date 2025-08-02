package reactocraft.core.chunk;

import java.util.ArrayList;
import java.util.List;

public class FullChunk {
    public static final int CHUNK_SIZE = 16;
    public static final int HEIGHT = 64;
    private static final int SECTION_COUNT = HEIGHT / ChunkSection.SECTION_SIZE;

    private final List<ChunkSection> sections;
    private final int chunkX, chunkZ;

    private boolean isLoaded;

    public FullChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.isLoaded = false;

        sections = new ArrayList<>();
        for (int i = 0; i < SECTION_COUNT; i++) {
            sections.add(new ChunkSection(i));
        }
    }

    public ChunkSection getSection(int yIndex) {
        if (yIndex < 0 || yIndex >= sections.size()) {
            throw new IndexOutOfBoundsException("Invalid section index");
        }
        return sections.get(yIndex);
    }

    public void setBlock(int x, int y, int z, int blockId) {
        int sectionY = y / ChunkSection.SECTION_SIZE;
        int localY = y % ChunkSection.SECTION_SIZE;
        ChunkSection section = getSection(sectionY);
        section.setBlock(x, localY, z, blockId);
    }

    public int getBlock(int x, int y, int z) {
        int sectionY = y / ChunkSection.SECTION_SIZE;
        int localY = y % ChunkSection.SECTION_SIZE;
        ChunkSection section = getSection(sectionY);
        return section.getBlock(x, localY, z);
    }

    public byte[] getRawData() {
        byte[] data = new byte[ChunkSection.BLOCK_COUNT * SECTION_COUNT];
        int offset = 0;
        for (ChunkSection section : sections) {
            byte[] sectionData = section.getRawData();
            System.arraycopy(sectionData, 0, data, offset, sectionData.length);
            offset += sectionData.length;
        }
        return data;
    }

    public void load() {
        this.isLoaded = true;
        // Load additional chunk data if necessary
    }

    public void unload() {
        this.isLoaded = false;
        // Unload additional chunk data if necessary
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }
}
