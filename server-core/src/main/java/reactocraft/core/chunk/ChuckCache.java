package reactocraft.core.chunk;

import java.io.*;

public class ChuckCache {
    private static final String CACHE_DIR = "chuck_cache/";

    public void saveChuckToDisk(FullChunk chuck) {
        String filePath = CACHE_DIR + chuck.getChunkX() + "_" + chuck.getChunkZ() + ".dat";
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(chuck);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FullChunk loadChuckFromDisk(int chunkX, int chunkZ) {
        String filePath = CACHE_DIR + chunkX + "_" + chunkZ + ".dat";
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            return (FullChunk) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
