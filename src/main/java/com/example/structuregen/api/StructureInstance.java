package com.example.structuregen.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable výsledek jednoho úspěšného assembly — reprezentuje jednu
 * vygenerovanou instanci struktury v herním světě.
 *
 * <p>Instance je vytvořena assembly enginem po úspěšném dokončení
 * generování a předávána přes eventy a {@link SpawnPopulator}.
 * Po vytvoření není nikdy mutována.
 *
 * <h2>Dostupné informace</h2>
 * <ul>
 *   <li>{@link #structureId} — ID registrované {@link StructureDefinition}</li>
 *   <li>{@link #seed} — deterministický seed použitý pro tento assembly</li>
 *   <li>{@link #chunkPos} — chunk ve kterém je origin struktury</li>
 *   <li>{@link #roomPositions} — mapa místností na absolutní origin pozice</li>
 *   <li>{@link #metadata} — agregovaná metadata ze všech umístěných místností</li>
 * </ul>
 */
public final class StructureInstance {

    // ---- Fields --------------------------------------------------------------

    /** ID registrované {@link StructureDefinition}. Nikdy {@code null}. */
    private final ResourceLocation structureId;

    /**
     * Deterministický seed použitý pro tento assembly.
     * Vypočten jako {@code worldSeed ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L)}
     * pokud {@link GenerationRules#isUseDeterministicSeed()} == true.
     */
    private final long seed;

    /**
     * Mapa každého umístěného {@link RoomTemplate} na jeho absolutní origin
     * {@link net.minecraft.core.BlockPos} v herním světě.
     *
     * <p>Immutable po vytvoření instance.
     */
    private final Map<RoomTemplate, net.minecraft.core.BlockPos> roomPositions;

    /**
     * Agregovaná metadata ze všech umístěných místností.
     * Vznikají spojením {@link RoomTemplate#getMetadata()} ze všech
     * úspěšně umístěných místností — při konfliktu klíčů má přednost
     * poslední umístěná místnost.
     *
     * <p>Immutable po vytvoření instance.
     */
    private final Map<String, Object> metadata;

    /**
     * Chunk ve kterém se nachází origin (ROOT template) struktury.
     * Používán jako klíč v {@code StructureWorldState}.
     */
    private final ChunkPos chunkPos;

    // ---- Constructor ---------------------------------------------------------

    /**
     * Vytvoří novou immutable instanci. Voláno výhradně assembly enginem.
     *
     * @param structureId   ID struktury; nesmí být {@code null}
     * @param seed          seed tohoto assembly
     * @param roomPositions mapa místností → pozice; nesmí být {@code null}
     * @param metadata      agregovaná metadata; nesmí být {@code null}
     * @param chunkPos      chunk origin; nesmí být {@code null}
     */
    public StructureInstance(
            ResourceLocation structureId,
            long seed,
            Map<RoomTemplate, net.minecraft.core.BlockPos> roomPositions,
            Map<String, Object> metadata,
            ChunkPos chunkPos) {

        this.structureId   = Objects.requireNonNull(structureId, "structureId must not be null");
        this.seed          = seed;
        this.roomPositions = Collections.unmodifiableMap(new HashMap<>(roomPositions));
        this.metadata      = Collections.unmodifiableMap(new HashMap<>(metadata));
        this.chunkPos      = Objects.requireNonNull(chunkPos, "chunkPos must not be null");
    }

    // ---- Accessors -----------------------------------------------------------

    /** @return ID registrované struktury; nikdy {@code null} */
    public ResourceLocation getStructureId()                                         { return structureId; }

    /** @return seed použitý pro tento assembly */
    public long getSeed()                                                             { return seed; }

    /**
     * @return immutable mapa {@link RoomTemplate} → absolutní origin pozice;
     *         nikdy {@code null}
     */
    public Map<RoomTemplate, net.minecraft.core.BlockPos> getRoomPositions()         { return roomPositions; }

    /**
     * @return immutable mapa agregovaných metadata; nikdy {@code null}
     */
    public Map<String, Object> getMetadata()                                          { return metadata; }

    /** @return chunk origin struktury; nikdy {@code null} */
    public ChunkPos getChunkPos()                                                     { return chunkPos; }

    @Override
    public String toString() {
        return "StructureInstance{id=" + structureId
            + ", chunk=" + chunkPos
            + ", rooms=" + roomPositions.size()
            + ", seed=" + seed + "}";
    }
}