package reactocraft.core.chunk;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FullChunk implements Serializable {
    public static final int CHUNK_SIZE = 16;
    public static final int HEIGHT = 64;
    private static final int SECTION_COUNT = HEIGHT / ChunkSection.SECTION_SIZE;

    public final List<ChunkSection> sections;
    private final int chunkX, chunkZ;

    private transient boolean isLoaded;
    private transient WeakReference<ChunkManager> managerRef;

    public FullChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.isLoaded = false;

        this.sections = new ArrayList<>();
        for (int i = 0; i < SECTION_COUNT; i++) {
            sections.add(new ChunkSection(i));
        }
    }

    public void setManager(ChunkManager manager) {
        this.managerRef = new WeakReference<>(manager);
    }

    private void notifyManagerDirty(int x, int y, int z) {
        if (managerRef != null) {
            ChunkManager mgr = managerRef.get();
            if (mgr != null) {
                mgr.markChunkDirty(chunkX, chunkZ, x, y, z);
            }
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

        int currentBlock = getBlock(x, localY, z);
        if (currentBlock == blockId) {
            return;
        }
        getSection(sectionY).setBlock(x, localY, z, blockId);

        notifyManagerDirty(x, y, z);
    }

    public int getBlock(int x, int y, int z) {
        int sectionY = y / ChunkSection.SECTION_SIZE;
        int localY = y % ChunkSection.SECTION_SIZE;
        return getSection(sectionY).getBlock(x, localY, z);
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
    }

    public void unload() {
        this.isLoaded = false;
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

    public Mono<Void> saveAsync(ChunkCache cache) {
        return cache.saveChunkToDisk(this);
    }

    public static Mono<FullChunk> loadAsync(ChunkCache cache, int chunkX, int chunkZ) {
        return cache.loadChunkFromDisk(chunkX, chunkZ);
    }
}
