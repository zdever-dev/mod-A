package com.example.structuregen.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Záznam přesně těch grid buněk které byly obsazeny při umístění jedné místnosti.
 *
 * <p>Používán pro přesný rollback v assembly backtracking systému (viz A-5-2).
 * {@code rollback(GridMutationRecord)} na {@code ChunkAlignedGrid} odstraní
 * <strong>výhradně</strong> buňky zapsané v tomto recordu — žádné side effects
 * na ostatní místnosti.
 *
 * <h2>Invariant</h2>
 * <p>Po {@code place()} → {@code rollback(record)} musí být grid identický
 * s pre-place stavem. Tento invariant je testován v A-5-2.
 */
public final class GridMutationRecord {

    // ---- Fields --------------------------------------------------------------

    /**
     * Seznam {@link ChunkPos} buněk obsazených tímto placement.
     * Prázdný seznam = žádné buňky nebyly obsazeny (empty record).
     */
    private final List<ChunkPos> affectedCells;

    /**
     * ID {@link RoomTemplate} která způsobila tuto mutaci.
     * Nikdy {@code null}.
     */
    private final ResourceLocation roomTemplateId;

    /**
     * Absolutní origin pozice umístěné místnosti.
     * Nikdy {@code null}.
     */
    private final BlockPos placedAt;

    // ---- Constructor (private) -----------------------------------------------

    private GridMutationRecord(List<ChunkPos> affectedCells,
                                ResourceLocation roomTemplateId,
                                BlockPos placedAt) {
        this.affectedCells  = Collections.unmodifiableList(new ArrayList<>(affectedCells));
        this.roomTemplateId = Objects.requireNonNull(roomTemplateId, "roomTemplateId must not be null");
        this.placedAt       = Objects.requireNonNull(placedAt,       "placedAt must not be null");
    }

    // ---- Static factories ----------------------------------------------------

    /**
     * Vytvoří prázdný record pro místnost která neobsadila žádné buňky
     * (např. při dry-run collision check bez commitování).
     *
     * @param roomTemplateId ID template; nesmí být {@code null}
     * @param placedAt       origin pozice; nesmí být {@code null}
     * @return prázdný record
     */
    public static GridMutationRecord empty(ResourceLocation roomTemplateId, BlockPos placedAt) {
        return new GridMutationRecord(Collections.emptyList(), roomTemplateId, placedAt);
    }

    /**
     * Vytvoří record s danými obsazenými buňkami.
     *
     * @param affectedCells  seznam obsazených buněk; nesmí být {@code null}
     * @param roomTemplateId ID template; nesmí být {@code null}
     * @param placedAt       origin pozice; nesmí být {@code null}
     * @return nový record
     */
    public static GridMutationRecord of(List<ChunkPos> affectedCells,
                                         ResourceLocation roomTemplateId,
                                         BlockPos placedAt) {
        return new GridMutationRecord(affectedCells, roomTemplateId, placedAt);
    }

    // ---- Accessors -----------------------------------------------------------

    /**
     * @return immutable seznam obsazených grid buněk; nikdy {@code null};
     *         může být prázdný
     */
    public List<ChunkPos> getAffectedCells()    { return affectedCells; }

    /**
     * @return ID template která způsobila mutaci; nikdy {@code null}
     */
    public ResourceLocation getRoomTemplateId() { return roomTemplateId; }

    /**
     * @return origin pozice umístěné místnosti; nikdy {@code null}
     */
    public BlockPos getPlacedAt()               { return placedAt; }

    /** @return {@code true} pokud nebyla obsazena žádná buňka */
    public boolean isEmpty()                    { return affectedCells.isEmpty(); }

    @Override
    public String toString() {
        return "GridMutationRecord{template=" + roomTemplateId
            + ", at=" + placedAt
            + ", cells=" + affectedCells.size() + "}";
    }
}