package reactocraft.core.worldgen;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import reactocraft.core.chunk.FullChunk;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Worldgen {
    public static Mono<FullChunk> generateChunkAsync(int chunkX, int chunkZ) {
        return Mono.fromCallable(() -> {
            IntByReference lenRef = new IntByReference();
            Pointer ptr = IWorldGen.INSTANCE.generate_flat_chunk(chunkX, chunkZ, lenRef);
            byte[] chunkData = ptr.getByteArray(0, lenRef.getValue());

            FullChunk fullChunk = new FullChunk(chunkX, chunkZ);
            int size = FullChunk.CHUNK_SIZE;
            int height = FullChunk.HEIGHT;

            for (int y = 0; y < height; y++) {
                for (int z = 0; z < size; z++) {
                    for (int x = 0; x < size; x++) {
                        int index = y * size * size + z * size + x;
                        if (index >= chunkData.length) continue;
                        int blockId = chunkData[index] & 0xFF;
                        fullChunk.setBlock(x, y, z, blockId);
                    }
                }
            }

            IWorldGen.INSTANCE.free_buffer(ptr, lenRef.getValue());
            return fullChunk;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
