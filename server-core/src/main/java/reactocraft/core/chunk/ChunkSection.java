package reactocraft.core.chunk;

public class ChunkSection {
    public static final int SECTION_SIZE = 16;
    public static final int BLOCK_COUNT = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;

    private final byte[] blocks;
    private final int yIndex;

    public ChunkSection(int yIndex) {
        if (yIndex < 0) throw new IllegalArgumentException("yIndex must be >= 0");
        this.yIndex = yIndex;
        this.blocks = new byte[BLOCK_COUNT];
    }

    public int getBlock(int x, int y, int z) {
        checkBounds(x, y, z);
        int idx = (y * SECTION_SIZE + z) * SECTION_SIZE + x;
        return Byte.toUnsignedInt(blocks[idx]);
    }

    public void setBlock(int x, int y, int z, int blockId) {
        checkBounds(x, y, z);
        blocks[(y * SECTION_SIZE + z) * SECTION_SIZE + x] = (byte) (blockId & 0xFF);
    }

    private void checkBounds(int x, int y, int z) {
        if (x < 0 || x >= SECTION_SIZE
        || y < 0 || y >= SECTION_SIZE
        || z < 0 || z >= SECTION_SIZE) {
            throw new IndexOutOfBoundsException(
                    String.format("Coordinates out of range: x=%d, y=%d, z=%d", x, y, z)
            );
        }
    }

    public byte[] getRawData() {
        return blocks;
    }

    public void setRawData(byte[] data) {
        System.arraycopy(data, 0, this.blocks, 0, data.length);
    }

    public int getYIndex() {
        return yIndex;
    }
}
