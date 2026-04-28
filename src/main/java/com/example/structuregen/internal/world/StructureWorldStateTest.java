package com.example.structuregen.internal.world;

import com.example.structuregen.api.UniqueRule;
import com.example.structuregen.internal.world.ImmutableSpatialSnapshot;
import com.example.structuregen.internal.world.StructureWorldState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testy pro {@link StructureWorldState} — A-4-10.
 *
 * <p>Testy nepotřebují Minecraft server — StructureWorldState je testována
 * přímo přes reflection přístup k private konstruktoru (přes testovací
 * factory metodu níže).
 */
class StructureWorldStateTest {

    private static final ResourceLocation STRUCT_A =
        new ResourceLocation("test", "structure_a");
    private static final ResourceLocation STRUCT_B =
        new ResourceLocation("test", "structure_b");

    /** Vytvoří čistou instanci bez Minecraft contextu pro testování. */
    private static StructureWorldState createFresh() {
        // Voláme přes NBT round-trip s prázdným tagem — simuluje "nový svět"
        return StructureWorldStateTestAccess.createEmpty();
    }

    // =========================================================================
    // Test 1 — Concurrent read/write
    // =========================================================================

    /**
     * 10 simultánních writer threadů registruje instance různých struktur
     * zatímco main thread průběžně volá snapshot reads.
     * Očekáváme: žádný deadlock, žádná stale data po dokončení.
     */
    @Test
    void concurrentReadWrite_noDeadlock() throws InterruptedException {
        StructureWorldState state = createFresh();
        UniqueRule unlimited = UniqueRule.unlimited();

        int writerCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(writerCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(writerCount + 1);

        // Writer threads
        for (int i = 0; i < writerCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    ChunkPos cp = new ChunkPos(idx * 10, idx * 10);
                    Optional<StructureWorldState.InstanceRecord> result =
                        state.tryRegisterInstance(STRUCT_A, cp, idx * 1000L, unlimited);
                    if (result.isPresent()) successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Writer thread threw: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Reader thread — main thread
        pool.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 1000; i++) {
                    // isInsideStructure je lock-free — nesmí nikdy hodit výjimku
                    ImmutableSpatialSnapshot snap = state.getSnapshot();
                    assertNotNull(snap);
                }
            } catch (Exception e) {
                fail("Reader thread threw: " + e.getMessage());
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Writers did not finish in time");
        pool.shutdown();

        assertEquals(writerCount, successCount.get(),
            "All writer registrations should succeed with UniqueRule.unlimited()");
        assertEquals(writerCount, state.getInstanceCount(STRUCT_A));
    }

    // =========================================================================
    // Test 2 — NBT round-trip (simulace server crash recovery)
    // =========================================================================

    /**
     * Registruje instance, serializuje do NBT, deserializuje zpět.
     * Ověřuje data integrity — simuluje server crash a recovery.
     */
    @Test
    void nbtRoundTrip_dataIntegrity() {
        StructureWorldState original = createFresh();
        UniqueRule unlimited = UniqueRule.unlimited();

        ChunkPos cp1 = new ChunkPos(5, 10);
        ChunkPos cp2 = new ChunkPos(20, 30);
        long seed1 = 123456L;
        long seed2 = 789012L;

        original.tryRegisterInstance(STRUCT_A, cp1, seed1, unlimited);
        original.tryRegisterInstance(STRUCT_B, cp2, seed2, unlimited);
        original.markFailed(STRUCT_A, new ChunkPos(99, 99));

        // Serializace
        CompoundTag tag = original.save(new CompoundTag());

        // Deserializace — simulace recovery po crashu
        StructureWorldState recovered = StructureWorldStateTestAccess.loadFromTag(tag);

        assertEquals(1, recovered.getInstanceCount(STRUCT_A));
        assertEquals(1, recovered.getInstanceCount(STRUCT_B));
        assertTrue(recovered.isFailed(STRUCT_A, new ChunkPos(99, 99)));
        assertFalse(recovered.isFailed(STRUCT_B, new ChunkPos(99, 99)));
    }

    // =========================================================================
    // Test 3 — Migrace v0 → v1
    // =========================================================================

    /**
     * NBT tag bez DATA_VERSION field = verze 0.
     * Očekáváme čistou prázdnou instanci po migraci (v0 nemá stabilní formát).
     */
    @Test
    void migration_v0ToV1_producesEmptyState() {
        CompoundTag v0Tag = new CompoundTag();
        // Záměrně neklademe DATA_VERSION — simuluje v0

        StructureWorldState migrated = StructureWorldStateTestAccess.loadFromTag(v0Tag);

        assertEquals(0, migrated.getInstanceCount(STRUCT_A));
        assertEquals(0, migrated.getFailedCount(STRUCT_A));
    }

    // =========================================================================
    // Test 4 — Snapshot konzistence
    // =========================================================================

    /**
     * Snapshot čtený po write vždy obsahuje nová data —
     * AtomicReference publish je viditelný okamžitě.
     */
    @Test
    void snapshot_afterWrite_containsNewData() {
        StructureWorldState state = createFresh();
        ChunkPos cp = new ChunkPos(7, 7);

        ImmutableSpatialSnapshot before = state.getSnapshot();
        assertFalse(before.containsAt(STRUCT_A, cp));

        state.tryRegisterInstance(STRUCT_A, cp, 42L, UniqueRule.unlimited());

        ImmutableSpatialSnapshot after = state.getSnapshot();
        assertTrue(after.containsAt(STRUCT_A, cp),
            "Snapshot must contain new instance immediately after write");
    }

    // =========================================================================
    // Test 5 — UniqueRule.once() odmítne druhou instanci
    // =========================================================================

    @Test
    void uniqueRule_once_rejectsSecondInstance() {
        StructureWorldState state = createFresh();
        UniqueRule once = UniqueRule.once();

        Optional<StructureWorldState.InstanceRecord> first =
            state.tryRegisterInstance(STRUCT_A, new ChunkPos(0, 0), 1L, once);
        Optional<StructureWorldState.InstanceRecord> second =
            state.tryRegisterInstance(STRUCT_A, new ChunkPos(5, 5), 2L, once);

        assertTrue(first.isPresent(),  "First registration should succeed");
        assertFalse(second.isPresent(), "Second registration should be rejected by UniqueRule.once()");
        assertEquals(1, state.getInstanceCount(STRUCT_A));
    }

    // =========================================================================
    // Test 6 — minChunkDistance odmítne příliš blízkou instanci
    // =========================================================================

    @Test
    void uniqueRule_minChunkDistance_rejectsTooClose() {
        StructureWorldState state = createFresh();
        UniqueRule rule = UniqueRule.of(0, 0, 10); // min 10 chunks vzdálenost

        state.tryRegisterInstance(STRUCT_A, new ChunkPos(0, 0), 1L, rule);

        // 5 chunks = příliš blízko
        Optional<StructureWorldState.InstanceRecord> tooClose =
            state.tryRegisterInstance(STRUCT_A, new ChunkPos(5, 0), 2L, rule);
        assertFalse(tooClose.isPresent(), "Instance too close should be rejected");

        // 15 chunks = dostatečně daleko
        Optional<StructureWorldState.InstanceRecord> farEnough =
            state.tryRegisterInstance(STRUCT_A, new ChunkPos(15, 0), 3L, rule);
        assertTrue(farEnough.isPresent(), "Instance far enough should be accepted");
    }
}