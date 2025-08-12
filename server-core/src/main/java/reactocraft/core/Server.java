package reactocraft.core;

import reactocraft.core.chunk.ChunkManager;
import reactocraft.core.chunk.FullChunk;
import reactor.core.publisher.Mono;

public class Server {

    public static void main(String[] args) {
        System.out.println("Starting ReactoCraft server...");

        ChunkManager chunkManager = new ChunkManager();
        chunkManager.startAutoGc();

        // Load chunk 0,0
        Mono<FullChunk> chunkMono = chunkManager.getChunk(0, 0);

        chunkMono.subscribe(chunk -> {
            System.out.println("Chunk loaded: " + chunk.getChunkX() + "," + chunk.getChunkZ());

            // Change some blocks to test partial save
            chunk.setBlock(1, 10, 1, 5);
            chunk.setBlock(2, 10, 2, 7);

            // Wait and trigger a manual save (just for demo)
            try {
                Thread.sleep(35000); // wait 35s for auto-save to pick it up
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Test complete. Listing loaded chunks:");
            chunkManager.listLoadedChunks();

            // Cleanup: unload all chunks gracefully
            chunkManager.unloadAllChunks()
                    .doFinally(signal -> {
                        System.out.println("All chunks unloaded. Stopping GC.");
                        chunkManager.stopAutoGc();
                    })
                    .block();
        });

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
