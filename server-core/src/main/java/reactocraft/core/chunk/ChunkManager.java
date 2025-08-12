package reactocraft.core.chunk;

import reactocraft.core.worldgen.Worldgen;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.EOFException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ChunkManager {

    private static final long UNLOAD_AFTER_MS = 60_000;
    private static final int MAX_LOADED_CHUNKS = 256;
    private static final Duration GC_INTERVAL = Duration.ofSeconds(10);
    private static final Duration SAVE_DIRTY_INTERVAL = Duration.ofSeconds(30);
    private static final int MAX_DIRTY_BLOCKS_BEFORE_FULL_SAVE = 1000;

    private final Map<ChunkPos, ManagedChunk> loadedChunks = new ConcurrentHashMap<>();
    private final Map<SectionLockKey, ReentrantLock> sectionLocks = new ConcurrentHashMap<>();
    private final Map<ChunkPos, ReentrantLock> chunkLocks = new ConcurrentHashMap<>();
    private final ChunkCache cache = new ChunkCache();

    private Disposable gcDisposable;
    private Disposable saveDisposable;

    private static class ManagedChunk {
        final FullChunk chunk;
        volatile long lastAccess;
        volatile boolean dirty;

        final Map<Integer, BitSet> dirtySections = new ConcurrentHashMap<>();

        ManagedChunk(FullChunk chunk) {
            this.chunk = chunk;
            this.lastAccess = Instant.now().toEpochMilli();
            this.dirty = false;
        }

        void touch() {
            this.lastAccess = Instant.now().toEpochMilli();
        }

        void markDirty(int x, int y, int z) {
            int sectionIndex = y / 16;
            int localX = x & 0xF;
            int localY = y & 0xF;
            int localZ = z & 0xF;
            int blockIndex = (localY << 8) | (localZ << 4) | localX;

            dirtySections.computeIfAbsent(sectionIndex, k -> new BitSet(4096)).set(blockIndex);
            this.dirty = true;
            touch();
        }

        int getDirtyBlockCount() {
            return dirtySections.values().stream().mapToInt(BitSet::cardinality).sum();
        }

        void clearDirty() {
            dirtySections.clear();
            this.dirty = false;
        }

        boolean shouldSaveFullChunk(int threshold) {
            return getDirtyBlockCount() > threshold;
        }
    }



    private ReentrantLock getSectionLock(ChunkPos pos, int sectionIndex) {
        SectionLockKey key = new SectionLockKey(pos, sectionIndex);
        return sectionLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    public synchronized void startAutoGc() {
        if (gcDisposable != null && !gcDisposable.isDisposed()) return;

        gcDisposable = reactor.core.publisher.Flux.interval(GC_INTERVAL)
                .flatMap(t -> unloadInactiveChunks()
                        .then(enforceMaxLimitMono()))
                .subscribeOn(Schedulers.parallel())
                .subscribe(
                        unused -> { /* tick */ },
                        err -> System.err.println("ChunkManager GC error: " + err.getMessage())
                );

        saveDisposable = reactor.core.publisher.Flux.interval(SAVE_DIRTY_INTERVAL)
                .flatMap(t -> saveDirtyChunks())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        unused -> { /* tick */ },
                        err -> System.err.println("ChunkManager save error: " + err.getMessage())
                );
    }

    public synchronized void stopAutoGc() {
        if (gcDisposable != null && !gcDisposable.isDisposed()) {
            gcDisposable.dispose();
        }
        if (saveDisposable != null && !saveDisposable.isDisposed()) {
            saveDisposable.dispose();
        }
    }

    public void markChunkDirty(int chunkX, int chunkZ, int blockX, int blockY, int blockZ) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        int sectionIndex = blockY / 16;
        ReentrantLock sectionLock = getSectionLock(pos, sectionIndex);

        sectionLock.lock();
        try {
            ManagedChunk managed = loadedChunks.get(pos);
            if (managed != null) {
                managed.markDirty(blockX, blockY, blockZ);
            }
        } finally {
            sectionLock.unlock();
        }
    }

    public Mono<FullChunk> getChunk(int chunkX, int chunkZ) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        return Mono.fromCallable(() -> loadedChunks.computeIfAbsent(pos, p -> {
            enforceMaxLimit();
            FullChunk chunk = null;
            try {
                chunk = cache.loadChunkFromDisk(chunkX, chunkZ)
                        .onErrorResume(e -> {
                            if (e instanceof EOFException) {
                                System.err.println("Chunk file corrupted or empty: " + chunkX + "," + chunkZ + ", ignoring load");
                                return Mono.empty();
                            }
                            return Mono.error(e);
                        })
                        .block();

                if (chunk == null) {
                    chunk = Worldgen.generateChunkAsync(chunkX, chunkZ).block();
                } else {
                    cache.loadPartialChanges(chunkX, chunkZ, chunk).block();
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load or generate chunk " + chunkX + "," + chunkZ, ex);
            }

            assert chunk != null;
            chunk.setManager(this);
            return new ManagedChunk(chunk);
        }).chunk).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> unloadChunk(int chunkX, int chunkZ) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        return Mono.fromRunnable(() -> {
            ManagedChunk managed = loadedChunks.get(pos);
            if (managed != null) {
                ReentrantLock lock = getLockFor(pos);
                lock.lock();
                try {
                    if (managed.shouldSaveFullChunk(MAX_DIRTY_BLOCKS_BEFORE_FULL_SAVE)) {
                        managed.chunk.saveAsync(cache).block();
                        System.out.println("Full chunk saved on unload for " + chunkX + "," + chunkZ);
                    } else {
                        Set<BlockPos> dirtyBlocksSnapshot = getDirtyBlockPositions(managed);
                        List<ReentrantLock> sectionLocks = acquireSectionLocksForChunk(pos, dirtyBlocksSnapshot);
                        try {
                            cache.savePartialChanges(pos.x(), pos.z(), dirtyBlocksSnapshot, managed.chunk).block();
                            System.out.println("Partial changes saved on unload for " + chunkX + "," + chunkZ);
                        } finally {
                            releaseLocks(sectionLocks);
                        }
                    }
                    loadedChunks.remove(pos);
                    chunkLocks.remove(pos);
                } finally {
                    lock.unlock();
                }
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }



    private static Set<BlockPos> getBlockPos(ManagedChunk managed) {
        int sectionCount = managed.chunk.sections.size();  // or however many sections in your chunk
        Set<BlockPos> allBlocks = new HashSet<>();
        for (int sectionIdx = 0; sectionIdx < sectionCount; sectionIdx++) {
            int baseY = sectionIdx * 16;
            for (int y = baseY; y < baseY + 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        allBlocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return allBlocks;
    }

    public void listLoadedChunks() {
        if (loadedChunks.isEmpty()) {
            System.out.println("No chunks currently loaded in memory.");
            return;
        }
        System.out.println("Loaded chunks in memory: " + loadedChunks.size());
        loadedChunks.forEach((pos, managed) ->
                System.out.println(" - " + pos.x() + "," + pos.z()
                        + " lastAccess=" + managed.lastAccess
                        + " dirty=" + managed.dirty));
    }

    public Mono<Void> unloadInactiveChunks() {
        long now = Instant.now().toEpochMilli();
        List<Mono<Void>> tasks = new ArrayList<>();

        for (Map.Entry<ChunkPos, ManagedChunk> entry : loadedChunks.entrySet()) {
            if (now - entry.getValue().lastAccess > UNLOAD_AFTER_MS) {
                ChunkPos p = entry.getKey();
                tasks.add(unloadChunk(p.x(), p.z()));
            }
        }
        return Mono.when(tasks).then();
    }

    public Mono<Void> saveDirtyChunks() {
        List<Mono<Void>> tasks = new ArrayList<>();
        for (Map.Entry<ChunkPos, ManagedChunk> entry : loadedChunks.entrySet()) {
            ManagedChunk m = entry.getValue();
            if (m.dirty) {
                ChunkPos pos = entry.getKey();
                if (m.shouldSaveFullChunk(MAX_DIRTY_BLOCKS_BEFORE_FULL_SAVE)) {
                    Mono<Void> task = Mono.fromRunnable(() -> {
                        ReentrantLock lock = getLockFor(pos);
                        lock.lock();
                        try {
                            m.chunk.saveAsync(cache).block();
                            m.clearDirty();
                            System.out.println("Saved full chunk for " + pos.x() + "," + pos.z() + " due to too many dirty blocks.");
                        } finally {
                            lock.unlock();
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).then();

                    tasks.add(task);
                } else {
                    Set<BlockPos> dirtyBlocksSnapshot = getDirtyBlockPositions(m);
                    Mono<Void> task = Mono.fromRunnable(() -> {
                        List<ReentrantLock> sectionLocks = acquireSectionLocksForChunk(pos, dirtyBlocksSnapshot);
                        try {
                            cache.savePartialChanges(pos.x(), pos.z(), dirtyBlocksSnapshot, m.chunk).block();
                            m.clearDirty();
                            System.out.println("Saved partial changes for chunk " + pos.x() + "," + pos.z() +
                                    " (" + dirtyBlocksSnapshot.size() + " blocks)");
                        } finally {
                            releaseLocks(sectionLocks);
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).then();

                    tasks.add(task);
                }
            }
        }
        return Mono.when(tasks).then();
    }

    private Set<BlockPos> getDirtyBlockPositions(ManagedChunk m) {
        Set<BlockPos> positions = new HashSet<>();
        for (Map.Entry<Integer, BitSet> entry : m.dirtySections.entrySet()) {
            int sectionIndex = entry.getKey();
            BitSet bits = entry.getValue();

            int baseY = sectionIndex * 16;
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i+1)) {
                int localX = i & 0xF;
                int localZ = (i >> 4) & 0xF;
                int localY = (i >> 8) & 0xF;
                positions.add(new BlockPos(localX, baseY + localY, localZ));
            }
        }
        return positions;
    }

    public Mono<Void> enforceMaxLimitMono() {
        return Mono.fromRunnable(this::enforceMaxLimit)
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void enforceMaxLimit() {
        int size = loadedChunks.size();
        if (size < MAX_LOADED_CHUNKS) {
            return;
        }
        List<Map.Entry<ChunkPos, ManagedChunk>> sorted = new ArrayList<>(loadedChunks.entrySet());
        sorted.sort(Comparator.comparingLong(e -> e.getValue().lastAccess));

        int chunksToRemove = size - MAX_LOADED_CHUNKS + 1;
        for (int i = 0; i < chunksToRemove; i++) {
            ChunkPos pos = sorted.get(i).getKey();
            unloadChunk(pos.x(), pos.z()).block();
        }
    }

    public Mono<Void> unloadAllChunks() {
        List<Mono<Void>> tasks = new ArrayList<>();
        for (ChunkPos pos : new ArrayList<>(loadedChunks.keySet())) {
            tasks.add(unloadChunk(pos.x(), pos.z()));
        }
        return Mono.when(tasks).then();
    }

    private List<ReentrantLock> acquireSectionLocksForChunk(ChunkPos pos, Set<BlockPos> dirtyBlocks) {
        List<Integer> sectionIndicesToLock = dirtyBlocks.stream()
                .map(posBlock -> posBlock.y() / 16)
                .distinct()
                .sorted()
                .toList();

        List<ReentrantLock> locks = new ArrayList<>();
        for (int sectionIndex : sectionIndicesToLock) {
            ReentrantLock lock = getSectionLock(pos, sectionIndex);
            lock.lock();
            locks.add(lock);
        }
        return locks;
    }

    private void releaseLocks(List<ReentrantLock> locks) {
        for (int i = locks.size() - 1; i >= 0; i--) {
            locks.get(i).unlock();
        }
    }

    private List<ReentrantLock> lockWholeChunk(ChunkPos pos) {
        ManagedChunk managed = loadedChunks.get(pos);
        if (managed == null) {
            return Collections.emptyList();
        }

        int sectionCount = managed.chunk.sections.size();
        List<ReentrantLock> locks = new ArrayList<>(sectionCount);

        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            ReentrantLock lock = getSectionLock(pos, sectionIndex);
            lock.lock();
            locks.add(lock);
        }

        return locks;
    }

    private ReentrantLock getLockFor(ChunkPos pos) {
        return chunkLocks.computeIfAbsent(pos, k -> new ReentrantLock());
    }

}
