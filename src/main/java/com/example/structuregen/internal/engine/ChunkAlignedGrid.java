package com.example.structuregen.internal.engine;

import com.example.structuregen.api.GridMutationRecord;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chunk-aligned spatial grid pro BoundingBox collision detection.
 *
 * <p>Každá buňka gridu odpovídá jednomu {@link ChunkPos} (16×16 bloků).
 * Struktura je považována za obsazenou v buňce pokud její {@link BoundingBox}
 * překrývá daný chunk.
 *
 * <h2>Invariant</h2>
 * <p>Grid není nikdy mutován přímo — každá place operace vrátí
 * {@link GridMutationRecord} a rollback ho precizně zruší.
 */
final class ChunkAlignedGrid {

    /**
     * Mapa ChunkPos.toLong() → set structureId které chunk obsazují.
     */
    private final Map<Long, Set<ResourceLocation>> cells = new HashMap<>();

    // ---- Collision check ----------------------------------------------------

    /**
     * Zkontroluje zda by umístění struktury s daným BoundingBox kolidovalo
     * s již obsazenými buňkami gridu.
     *
     * @param bounds BoundingBox navrhovaného umístění
     * @return {@code true} pokud kolize existuje
     */
    boolean checkCollision(BoundingBox bounds) {
        for (long cellKey : getCellKeys(bounds)) {
            if (cells.containsKey(cellKey)) return true;
        }
        return false;
    }

    /**
     * Zkontroluje kolizi — ignoruje obsazení daným structureId
     * (pro případ kdy chceme ověřit kolizi s ostatními strukturami ale
     * ne sám se sebou při multi-room placement).
     *
     * @param bounds      BoundingBox k ověření
     * @param ignoreId    structureId které se ignoruje
     * @return {@code true} pokud kolize existuje s jinou strukturou
     */
    boolean checkCollisionIgnoring(BoundingBox bounds, ResourceLocation ignoreId) {
        for (long cellKey : getCellKeys(bounds)) {
            Set<ResourceLocation> occupants = cells.get(cellKey);
            if (occupants == null) continue;
            for (ResourceLocation occ : occupants) {
                if (!occ.equals(ignoreId)) return true;
            }
        }
        return false;
    }

    // ---- Occupy -------------------------------------------------------------

    /**
     * Označí všechny buňky gridu překryté daným BoundingBox jako obsazené
     * danou strukturou. Nikdy nemutuje grid mimo tuto metodu.
     *
     * @param bounds      BoundingBox místnosti
     * @param structureId ID struktury která obsazuje buňky
     * @return {@link GridMutationRecord} obsahující přesně obsazené buňky —
     *         použij pro rollback
     */
    GridMutationRecord occupyCells(BoundingBox bounds, ResourceLocation structureId) {
        List<Long> occupiedKeys = getCellKeys(bounds);
        List<ChunkPos> affectedCells = new ArrayList<>();

        for (long key : occupiedKeys) {
            cells.computeIfAbsent(key, k -> new HashSet<>()).add(structureId);
            affectedCells.add(new ChunkPos(key));
        }

        // Konvertuj pozice na ChunkPos pro GridMutationRecord
        List<ChunkPos> chunkPosList = new ArrayList<>();
        for (long key : occupiedKeys) {
            chunkPosList.add(new ChunkPos(key));
        }

        return GridMutationRecord.of(chunkPosList, structureId,
            new net.minecraft.core.BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()));
    }

    // ---- Rollback -----------------------------------------------------------

    /**
     * Odstraní přesně buňky zapsané v daném {@link GridMutationRecord}.
     * Žádné side effects na ostatní místnosti.
     *
     * <p><strong>Invariant:</strong> po {@code occupyCells()} → {@code rollback()}
     * je grid identický s pre-place stavem.
     *
     * @param record záznam z {@link #occupyCells}; nesmí být {@code null}
     */
    void rollback(GridMutationRecord record) {
        ResourceLocation structureId = record.getRoomTemplateId();
        for (ChunkPos cp : record.getAffectedCells()) {
            Set<ResourceLocation> occupants = cells.get(cp.toLong());
            if (occupants == null) continue;
            occupants.remove(structureId);
            if (occupants.isEmpty()) {
                cells.remove(cp.toLong());
            }
        }
    }

    // ---- Utility ------------------------------------------------------------

    /**
     * Vrátí seznam ChunkPos klíčů (jako long) pro všechny chunky překryté
     * daným BoundingBox.
     */
    private List<Long> getCellKeys(BoundingBox bounds) {
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;

        List<Long> keys = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                keys.add(new ChunkPos(cx, cz).toLong());
            }
        }
        return keys;
    }

    /** Vrátí počet obsazených buněk — pro debug. */
    int getOccupiedCellCount() {
        return cells.size();
    }
}