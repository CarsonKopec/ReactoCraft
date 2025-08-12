package reactocraft.core.chunk;

public record ChunkPos(int x, int z) {
    @Override
    public boolean equals(Object o) {
        return o instanceof ChunkPos other && x == other.x && z == other.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }
}
