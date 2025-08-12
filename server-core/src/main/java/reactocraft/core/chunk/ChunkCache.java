package reactocraft.core.chunk;

import net.querz.nbt.io.NamedTag;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ChunkCache {

    private static final String CACHE_DIR = "chunk_cache/";

    public Mono<Void> saveChunkToDisk(FullChunk chunk) {
        return Mono.fromRunnable(() -> {
            CompoundTag root = new CompoundTag();
            root.putInt("chunkX", chunk.getChunkX());
            root.putInt("chunkZ", chunk.getChunkZ());

            ListTag<CompoundTag> sectionsList = new ListTag<>(CompoundTag.class);

            for (int i = 0; i < chunk.sections.size(); i++) {
                ChunkSection section = chunk.getSection(i);
                CompoundTag sectionTag = new CompoundTag();
                sectionTag.putInt("yIndex", i);
                sectionTag.putByteArray("blocks", section.getRawData());
                sectionsList.add(sectionTag);
            }

            root.put("sections", sectionsList);

            try {
                NBTUtil.write(root, CACHE_DIR + chunk.getChunkX() + "_" + chunk.getChunkZ() + ".dat");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<FullChunk> loadChunkFromDisk(int chunkX, int chunkZ) {
        return Mono.fromCallable(() -> {
            String filePath = CACHE_DIR + chunkX + "_" + chunkZ + ".dat";
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) {
                return null;
            }

            NamedTag namedTag = NBTUtil.read(filePath);
            CompoundTag root = (CompoundTag) namedTag.getTag();

            int loadedChunkX = root.getInt("chunkX");
            int loadedChunkZ = root.getInt("chunkZ");

            FullChunk chunk = new FullChunk(loadedChunkX, loadedChunkZ);

            ListTag<CompoundTag> sectionsList = root.getListTag("sections").asCompoundTagList();
            for (CompoundTag sectionTag : sectionsList) {
                int yIndex = sectionTag.getInt("yIndex");
                byte[] blocks = sectionTag.getByteArray("blocks");
                ChunkSection section = chunk.getSection(yIndex);
                section.setRawData(blocks);
            }

            chunk.load();
            return chunk;
        }).subscribeOn(Schedulers.boundedElastic());
    }


    // --- Partial changes ---

    public Mono<Void> savePartialChanges(int chunkX, int chunkZ, Set<BlockPos> dirtyBlocks, FullChunk chunk) {
        return Mono.fromRunnable(() -> {
            CompoundTag root = new CompoundTag();
            root.putInt("chunkX", chunkX);
            root.putInt("chunkZ", chunkZ);

            ListTag<CompoundTag> changedBlocksList = new ListTag<>(CompoundTag.class);
            for (BlockPos pos : dirtyBlocks) {
                CompoundTag blockTag = new CompoundTag();
                blockTag.putInt("x", pos.x());
                blockTag.putInt("y", pos.y());
                blockTag.putInt("z", pos.z());
                blockTag.putInt("blockId", chunk.getBlock(pos.x(), pos.y(), pos.z()));
                changedBlocksList.add(blockTag);
            }

            root.put("changedBlocks", changedBlocksList);

            String filePath = CACHE_DIR + chunkX + "_" + chunkZ + "_partial.dat";
            File parent = new File(CACHE_DIR);
            if (!parent.exists()) parent.mkdirs();

            try {
                NBTUtil.write(root, filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> loadPartialChanges(int chunkX, int chunkZ, FullChunk chunk) {
        return Mono.fromRunnable(() -> {
            String filePath = CACHE_DIR + chunkX + "_" + chunkZ + "_partial.dat";
            File file = new File(filePath);
            if (!file.exists()) return;

            try {
                NamedTag namedTag = NBTUtil.read(filePath);
                CompoundTag root = (CompoundTag) namedTag.getTag();

                ListTag<CompoundTag> changedBlocksList = root.getListTag("changedBlocks").asCompoundTagList();
                for (CompoundTag blockTag : changedBlocksList) {
                    int x = blockTag.getInt("x");
                    int y = blockTag.getInt("y");
                    int z = blockTag.getInt("z");
                    int blockId = blockTag.getInt("blockId");
                    chunk.setBlock(x, y, z, blockId);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
