package com.example.structuregen.internal.engine;

import com.example.structuregen.StructureGenMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Fronta odložených block placements.
 *
 * <p>Bloky nejsou nikdy umísťovány přímo během assembly algoritmu —
 * vše jde přes tuto frontu. Při flush engine fetchuje aktuální chunk
 * objekt pro každý placement — eliminuje stale reference na
 * {@code IChunkAccess} při chunk promotion z {@code ProtoChunk}
 * na {@code LevelChunk}.
 *
 * <h2>Použití</h2>
 * <ol>
 *   <li>Assembly engine enqueue-uje bloky přes {@link #enqueue}</li>
 *   <li>Po dokončení assembly volá {@link #flush} pro fyzické umístění</li>
 *   <li>Při rollback volá {@link #clear} pro zahození fronty</li>
 * </ol>
 */
final class BlockPlacementQueue {

    private final Deque<DeferredPlacement> queue = new ArrayDeque<>();

    /** Přidá placement do fronty. */
    void enqueue(DeferredPlacement placement) {
        queue.addLast(placement);
    }

    /**
     * Fyzicky umístí všechny bloky ve frontě do světa.
     *
     * <p>Pro každý placement: fetchuje aktuální {@link ServerLevel} přes
     * {@code server.getLevel(dimension)} a poté chunk přes
     * {@code level.getChunk(chunkPos)} — nikdy stale reference.
     *
     * @param server Minecraft server instance; nesmí být {@code null}
     * @return seznam absolutních pozic úspěšně umístěných bloků
     */
    List<net.minecraft.core.BlockPos> flush(MinecraftServer server) {
        List<net.minecraft.core.BlockPos> placed = new ArrayList<>();

        while (!queue.isEmpty()) {
            DeferredPlacement p = queue.pollFirst();

            ServerLevel level = server.getLevel(p.dimension());
            if (level == null) {
                StructureGenMod.LOGGER.warn(
                    "BlockPlacementQueue.flush(): dimension '{}' not found, "
                    + "skipping placement at {}.", p.dimension().location(), p.blockPos()
                );
                continue;
            }

            // Fetch aktuálního chunk objektu — funguje pro ProtoChunk i LevelChunk
            level.getChunk(p.chunkPos().x, p.chunkPos().z);

            level.setBlock(p.blockPos(), p.state(), 3);

            // Aplikuj NBT pokud přítomno (BlockEntity data)
            if (p.nbt() != null) {
                BlockEntity be = level.getBlockEntity(p.blockPos());
                if (be != null) {
                    be.load(p.nbt());
                }
            }

            placed.add(p.blockPos());
        }

        return placed;
    }

    /**
     * Zahodí všechny čekající placements bez fyzického umístění.
     * Voláno při rollback nebo assembly timeout.
     */
    void clear() {
        queue.clear();
    }

    /** @return počet čekajících placements */
    int size() {
        return queue.size();
    }

    /** @return {@code true} pokud je fronta prázdná */
    boolean isEmpty() {
        return queue.isEmpty();
    }
}