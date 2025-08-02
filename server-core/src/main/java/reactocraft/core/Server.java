package reactocraft.core;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import reactocraft.core.chunk.FullChunk;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class Server {

    public static void main(String[] args) {
        System.out.println("Starting ReactoCraft...");

        FullChunk fullChunk = new FullChunk(0, 0);

        Flux.interval(Duration.ofMillis(50))
                .doOnNext(tick -> {
                    IntByReference lenRef = new IntByReference();
                    Pointer ptr = IWorldGen.INSTANCE.generate_flat_chunk(0, 0, lenRef);
                    byte[] chunkData = ptr.getByteArray(0, lenRef.getValue());

                    fullChunk.load();
                    for (int y = 0; y < FullChunk.HEIGHT; y++) {
                        for (int z = 0; z < FullChunk.CHUNK_SIZE; z++) {
                            for (int x = 0; x < FullChunk.CHUNK_SIZE; x++) {
                                int index = y * FullChunk.CHUNK_SIZE * FullChunk.CHUNK_SIZE + z * FullChunk.CHUNK_SIZE + x;
                                if (index >= chunkData.length) {
                                    continue;
                                }
                                int blockId = chunkData[index] & 0xFF;
                                fullChunk.setBlock(x, y, z, blockId);
                            }
                        }
                    }
                    IWorldGen.INSTANCE.free_buffer(ptr, lenRef.getValue());

                    System.out.println("Full chunk loaded and populated!");
                })
                .blockLast();
    }
}
