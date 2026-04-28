package com.example.structuregen.internal.world;

import com.example.structuregen.StructureGenMod;
import com.example.structuregen.api.UniqueRule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Per-dimension persistent world state pro Structure Generator mod.
 *
 * <p>Rozšiřuje {@link SavedData} — data jsou uložena přímo do world save
 * souboru a přežívají restarty serveru i session přechody.
 *
 * <h2>Concurrency architektura</h2>
 * <ul>
 *   <li>{@link #rwLock} write lock je držen <strong>výhradně</strong> po dobu
 *       atomické sekvence: UniqueRule check → distance check → zápis instance
 *       → {@code setDirty()} → publish snapshot. Assembly samotný běží
 *       <strong>bez</strong> jakéhokoli zámku.</li>
 *   <li>{@link #snapshot} je {@code AtomicReference<ImmutableSpatialSnapshot>} —
 *       čtení přes {@code isInsideStructure()} a podobné query metody je vždy
 *       lock-free. Nikdy neblokuje main thread.</li>
 * </ul>
 *
 * <h2>Přístup</h2>
 * <p>Vždy přes {@link #get(ServerLevel)} — nikdy přímou konstrukcí.
 */
public final class StructureWorldState extends SavedData {

    // ---- SavedData identifikátory -------------------------------------------

    /** Klíč pod kterým je SavedData uložena v world save. */
    public static final String DATA_NAME = "structuregen_world_state";

    /**
     * Verze NBT schématu. Inkrementovat při breaking změnách formátu.
     * Verze 0 = chybějící VERSION field (legacy, prázdná migrace).
     */
    public static final int DATA_VERSION = 1;

    // ---- Interní state ------------------------------------------------------

    /**
     * Mapa structureId → seznam záznamů instancí.
     * Mutována výhradně pod write lockem.
     */
    private final Map<ResourceLocation, List<InstanceRecord>> instances = new HashMap<>();

    /**
     * Permanent "failed" markery.
     * Key = structureId string, value = set ChunkPos.asLong() hodnot.
     * Mutována výhradně pod write lockem.
     */
    private final Map<String, Set<Long>> failedMarkers = new HashMap<>();

    /**
     * Read-write lock.
     * Write lock scope: UniqueRule check + zápis + snapshot publish.
     * Read lock: používán pro čtení instances mapy přímo.
     */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Immutable snapshot pro lock-free query reads.
     * Vždy čtěte přes {@code snapshot.get()} — nikdy přes {@link #instances}
     * přímo z query metod.
     */
    private final AtomicReference<ImmutableSpatialSnapshot> snapshot =
        new AtomicReference<>(ImmutableSpatialSnapshot.EMPTY);

    // ---- Přístup ------------------------------------------------------------

    /**
     * Pohodlný přístup k {@link StructureWorldState} pro danou dimenzi.
     * Vytvoří novou instanci pokud ještě neexistuje.
     *
     * @param level server level; nesmí být {@code null}
     * @return existující nebo nově vytvořená instance pro tuto dimenzi
     */
    public static StructureWorldState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            StructureWorldState::load,  // Function<CompoundTag, T> — načtení z NBT
            StructureWorldState::new,   // Supplier<T> — nová prázdná instance
            DATA_NAME                   // název souboru v world save
        );
    }

    // ---- Konstruktory -------------------------------------------------------

    /** Konstruktor pro novou (prázdnou) instanci. Voláno Forge jako Supplier. */
    private StructureWorldState() {}

    // =========================================================================
    // NBT serializace — A-4-2
    // =========================================================================

    /**
     * Serializuje celý state do NBT.
     * Voláno Forge automaticky při ukládání světa pokud {@code isDirty()}.
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("DATA_VERSION", DATA_VERSION);

        // ---- Instance záznamy -----------------------------------------------
        CompoundTag instancesTag = new CompoundTag();
        rwLock.readLock().lock();
        try {
            for (Map.Entry<ResourceLocation, List<InstanceRecord>> entry : instances.entrySet()) {
                String structId = entry.getKey().toString();
                ListTag recordList = new ListTag();

                for (InstanceRecord rec : entry.getValue()) {
                    CompoundTag recTag = new CompoundTag();
                    recTag.putString("structureId", structId);
                    recTag.putInt("instanceCount", entry.getValue().size());
                    recTag.putLong("timestamp", rec.timestamp());
                    recTag.putLong("seed", rec.seed());
                    recTag.putBoolean("pendingPopulation", rec.pendingPopulation());
                    recTag.put("chunkPos", new IntArrayTag(new int[]{
                        rec.chunkPos().x, rec.chunkPos().z
                    }));
                    recordList.add(recTag);
                }

                instancesTag.put(structId, recordList);
            }
        } finally {
            rwLock.readLock().unlock();
        }
        tag.put("instances", instancesTag);

        // ---- Failed markery — A-4-3 -----------------------------------------
        CompoundTag failedTag = new CompoundTag();
        rwLock.readLock().lock();
        try {
            for (Map.Entry<String, Set<Long>> entry : failedMarkers.entrySet()) {
                long[] longs = entry.getValue().stream()
                    .mapToLong(Long::longValue).toArray();
                failedTag.put(entry.getKey(), new LongArrayTag(longs));
            }
        } finally {
            rwLock.readLock().unlock();
        }
        tag.put("failed", failedTag);

        return tag;
    }

    /**
     * Načte state z NBT s migration supportem.
     * Voláno Forge jako Function&lt;CompoundTag, StructureWorldState&gt;.
     *
     * @param tag zdrojový NBT tag
     * @return nová instance s načteným state
     */
    private static StructureWorldState load(CompoundTag tag) {
        StructureWorldState state = new StructureWorldState();

        // A-4-4 — migrace
        int version = tag.contains("DATA_VERSION") ? tag.getInt("DATA_VERSION") : 0;
        CompoundTag migratedTag = state.migrate(tag, version);

        // ---- Instance záznamy -----------------------------------------------
        if (migratedTag.contains("instances", Tag.TAG_COMPOUND)) {
            CompoundTag instancesTag = migratedTag.getCompound("instances");
            for (String structId : instancesTag.getAllKeys()) {
                ResourceLocation id = new ResourceLocation(structId);
                List<InstanceRecord> records = new ArrayList<>();
                ListTag recordList = instancesTag.getList(structId, Tag.TAG_COMPOUND);

                for (int i = 0; i < recordList.size(); i++) {
                    CompoundTag recTag = recordList.getCompound(i);
                    int[] chunkArr = recTag.getIntArray("chunkPos");
                    ChunkPos chunkPos = chunkArr.length >= 2
                        ? new ChunkPos(chunkArr[0], chunkArr[1])
                        : new ChunkPos(0, 0);

                    records.add(new InstanceRecord(
                        chunkPos,
                        recTag.getLong("timestamp"),
                        recTag.getLong("seed"),
                        recTag.getBoolean("pendingPopulation")
                    ));
                }
                state.instances.put(id, records);
            }
        }

        // ---- Failed markery -------------------------------------------------
        if (migratedTag.contains("failed", Tag.TAG_COMPOUND)) {
            CompoundTag failedTag = migratedTag.getCompound("failed");
            for (String structId : failedTag.getAllKeys()) {
                long[] longs = failedTag.getLongArray(structId);
                Set<Long> set = new HashSet<>();
                for (long l : longs) set.add(l);
                state.failedMarkers.put(structId, set);
            }
        }

        // Publikuj počáteční snapshot
        state.publishSnapshot();

        StructureGenMod.LOGGER.debug(
            "StructureWorldState loaded: {} structure types, {} failed marker groups.",
            state.instances.size(), state.failedMarkers.size()
        );

        return state;
    }

    // =========================================================================
    // Permanent "failed" marker systém — A-4-3
    // =========================================================================

    /**
     * Zapíše permanent "failed" marker pro daný chunk a strukturu.
     *
     * @param structureId ID struktury; nesmí být {@code null}
     * @param chunkPos    chunk k označení; nesmí být {@code null}
     */
    public void markFailed(ResourceLocation structureId, ChunkPos chunkPos) {
        rwLock.writeLock().lock();
        try {
            failedMarkers
                .computeIfAbsent(structureId.toString(), k -> new HashSet<>())
                .add(chunkPos.toLong());
            setDirty();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Vrátí zda je daný chunk permanentně označen jako failed.
     *
     * @param structureId ID struktury
     * @param chunkPos    chunk k ověření
     * @return {@code true} pokud je chunk označen jako failed
     */
    public boolean isFailed(ResourceLocation structureId, ChunkPos chunkPos) {
        Set<Long> set = failedMarkers.get(structureId.toString());
        return set != null && set.contains(chunkPos.toLong());
    }

    /**
     * Smaže všechny "failed" markery pro danou strukturu.
     * Voláno při {@code /sgdebug reset}.
     *
     * @param structureId ID struktury; nesmí být {@code null}
     */
    public void clearFailed(ResourceLocation structureId) {
        rwLock.writeLock().lock();
        try {
            failedMarkers.remove(structureId.toString());
            setDirty();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // =========================================================================
    // SavedData migrace — A-4-4
    // =========================================================================

    private CompoundTag migrate(CompoundTag tag, int fromVersion) {
        if (fromVersion == DATA_VERSION) return tag;

        switch (fromVersion) {
            case 0 -> {
                StructureGenMod.LOGGER.info(
                    "StructureWorldState: migrating from version 0 to {}. "
                    + "No data preserved (v0 had no stable format).", DATA_VERSION
                );
                return new CompoundTag();
            }
            default -> {
                StructureGenMod.LOGGER.warn(
                    "StructureWorldState: unknown version {} — treating as empty state.",
                    fromVersion
                );
                return new CompoundTag();
            }
        }
    }

    // =========================================================================
    // AtomicReference snapshot — A-4-5 / A-4-6
    // =========================================================================

    /**
     * Publikuje nový immutable snapshot z aktuálního stavu {@link #instances}.
     * Musí být voláno pod write lockem nebo z load() před zveřejněním instance.
     */
    private void publishSnapshot() {
        Map<ResourceLocation, List<ChunkPos>> map = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<InstanceRecord>> entry : instances.entrySet()) {
            List<ChunkPos> positions = new ArrayList<>();
            for (InstanceRecord rec : entry.getValue()) {
                positions.add(rec.chunkPos());
            }
            map.put(entry.getKey(), positions);
        }
        snapshot.set(new ImmutableSpatialSnapshot(map));
    }

    /**
     * Vrátí aktuální immutable snapshot pro lock-free query reads.
     * Bezpečné volat z libovolného threadu — nikdy neblokuje.
     *
     * @return aktuální snapshot; nikdy {@code null}
     */
    public ImmutableSpatialSnapshot getSnapshot() {
        return snapshot.get();
    }

    // =========================================================================
    // Atomická check+write sekvence — A-4-7
    // =========================================================================

    /**
     * Atomicky ověří UniqueRule a distance podmínky a pokud projdou,
     * zaregistruje novou instanci.
     *
     * <p>Celá sekvence (check → zápis → setDirty → publishSnapshot) probíhá
     * pod write lockem — je garantovaně atomická.
     *
     * @param structureId ID struktury; nesmí být {@code null}
     * @param chunkPos    chunk origin nové instance; nesmí být {@code null}
     * @param seed        seed použitý pro assembly
     * @param uniqueRule  pravidla unikátnosti; nesmí být {@code null}
     * @return {@link Optional} s novou {@link InstanceRecord}, nebo prázdný
     *         pokud byla instance odmítnuta
     */
    public Optional<InstanceRecord> tryRegisterInstance(
            ResourceLocation structureId,
            ChunkPos chunkPos,
            long seed,
            UniqueRule uniqueRule) {

        rwLock.writeLock().lock();
        try {
            List<InstanceRecord> existing =
                instances.getOrDefault(structureId, Collections.emptyList());

            // UniqueRule: maxInstances check
            if (uniqueRule.hasInstanceLimit()
                    && existing.size() >= uniqueRule.getMaxInstances()) {
                StructureGenMod.LOGGER.debug(
                    "tryRegisterInstance: rejected '{}' at {} — maxInstances ({}) reached.",
                    structureId, chunkPos, uniqueRule.getMaxInstances()
                );
                return Optional.empty();
            }

            // UniqueRule: minChunkDistance check
            int minDist = uniqueRule.getMinChunkDistanceBetweenInstances();
            if (minDist > 0) {
                for (InstanceRecord rec : existing) {
                    int dx = Math.abs(rec.chunkPos().x - chunkPos.x);
                    int dz = Math.abs(rec.chunkPos().z - chunkPos.z);
                    int chebyshev = Math.max(dx, dz);
                    if (chebyshev < minDist) {
                        StructureGenMod.LOGGER.debug(
                            "tryRegisterInstance: rejected '{}' at {} — too close to "
                            + "existing instance at {} (dist={}, min={}).",
                            structureId, chunkPos, rec.chunkPos(), chebyshev, minDist
                        );
                        return Optional.empty();
                    }
                }
            }

            // Zápis nové instance
            InstanceRecord newRecord = new InstanceRecord(
                chunkPos,
                System.currentTimeMillis(),
                seed,
                true  // pendingPopulation = true dokud SpawnPopulator nedoběhne
            );

            instances.computeIfAbsent(structureId, k -> new ArrayList<>()).add(newRecord);
            setDirty();
            publishSnapshot();

            StructureGenMod.LOGGER.debug(
                "tryRegisterInstance: registered '{}' at {} (seed={}).",
                structureId, chunkPos, seed
            );

            return Optional.of(newRecord);

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // =========================================================================
    // Veřejné utility metody
    // =========================================================================

    /**
     * Nastaví {@code pendingPopulation} flag pro danou instanci.
     *
     * @param structureId ID struktury
     * @param chunkPos    chunk origin instance
     * @param pending     nová hodnota flagu
     */
    public void setPendingPopulation(ResourceLocation structureId,
                                      ChunkPos chunkPos,
                                      boolean pending) {
        rwLock.writeLock().lock();
        try {
            List<InstanceRecord> recs = instances.get(structureId);
            if (recs == null) return;
            for (int i = 0; i < recs.size(); i++) {
                if (recs.get(i).chunkPos().equals(chunkPos)) {
                    recs.set(i, recs.get(i).withPendingPopulation(pending));
                    setDirty();
                    return;
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Vrátí všechny instance s {@code pendingPopulation = true}.
     * Voláno při server startu pro SpawnPopulator recovery (viz A-5-16).
     *
     * @return list pending entries; nikdy {@code null}
     */
    public List<PendingEntry> getPendingPopulations() {
        rwLock.readLock().lock();
        try {
            List<PendingEntry> result = new ArrayList<>();
            for (Map.Entry<ResourceLocation, List<InstanceRecord>> entry : instances.entrySet()) {
                for (InstanceRecord rec : entry.getValue()) {
                    if (rec.pendingPopulation()) {
                        result.add(new PendingEntry(entry.getKey(), rec.chunkPos(), rec.seed()));
                    }
                }
            }
            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Smaže všechny instance záznamy pro danou strukturu.
     * Voláno při {@code /sgdebug reset}.
     *
     * @param structureId ID struktury
     */
    public void clearInstances(ResourceLocation structureId) {
        rwLock.writeLock().lock();
        try {
            instances.remove(structureId);
            setDirty();
            publishSnapshot();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Smaže všechny pending population záznamy pro danou strukturu.
     *
     * @param structureId ID struktury
     */
    public void clearPendingPopulation(ResourceLocation structureId) {
        rwLock.writeLock().lock();
        try {
            List<InstanceRecord> recs = instances.get(structureId);
            if (recs == null) return;
            recs.replaceAll(r -> r.withPendingPopulation(false));
            setDirty();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Vrátí počet instancí dané struktury v této dimenzi.
     *
     * @param structureId ID struktury
     * @return počet instancí
     */
    public int getInstanceCount(ResourceLocation structureId) {
        rwLock.readLock().lock();
        try {
            List<InstanceRecord> recs = instances.get(structureId);
            return recs == null ? 0 : recs.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Vrátí počet failed markerů pro danou strukturu.
     *
     * @param structureId ID struktury
     * @return počet failed markerů
     */
    public int getFailedCount(ResourceLocation structureId) {
        Set<Long> set = failedMarkers.get(structureId.toString());
        return set == null ? 0 : set.size();
    }

    // =========================================================================
    // Pomocné record třídy
    // =========================================================================

    /**
     * Immutable záznam jedné vygenerované instance.
     */
    public record InstanceRecord(
        ChunkPos chunkPos,
        long timestamp,
        long seed,
        boolean pendingPopulation
    ) {
        /** Vytvoří kopii s novým {@code pendingPopulation} příznakem. */
        InstanceRecord withPendingPopulation(boolean pending) {
            return new InstanceRecord(chunkPos, timestamp, seed, pending);
        }
    }

    /**
     * Entry pro pending population recovery.
     */
    public record PendingEntry(
        ResourceLocation structureId,
        ChunkPos chunkPos,
        long seed
    ) {}
}