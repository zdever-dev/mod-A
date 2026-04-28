package com.example.structuregen.internal.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot prostorového indexu všech vygenerovaných instancí
 * v jedné dimenzi.
 *
 * <p>Po každém úspěšném zápisu nové instance worker thread vytvoří nový
 * snapshot a atomicky ho vymění přes {@code AtomicReference} v
 * {@link StructureWorldState}. Main thread který volá
 * {@code isInsideStructure()} vždy čte z aktuálního snapshotu bez
 * čekání na žádný zámek — nikdy neblokuje.
 *
 * <p>Tato třída je {@code package-private} — není součástí veřejného API.
 */
record ImmutableSpatialSnapshot(
        Map<ResourceLocation, List<ChunkPos>> instancesByStructure
) {

    /** Prázdný snapshot — používán jako výchozí hodnota AtomicReference. */
    static final ImmutableSpatialSnapshot EMPTY =
        new ImmutableSpatialSnapshot(Collections.emptyMap());

    /**
     * Vytvoří defensivní kopii předané mapy.
     *
     * @param instancesByStructure mapa structureId → seznam ChunkPos instancí
     */ImmutableSpatialSnapshot(Map<ResourceLocation, List<ChunkPos>> instancesByStructure) {
    
        // Defensivní kopie — snapshot je skutečně immutable
        Map<ResourceLocation, List<ChunkPos>> copy = new HashMap<>();
        instancesByStructure.forEach((key, value) ->
            copy.put(key, Collections.unmodifiableList(List.copyOf(value)))
        );
        this.instancesByStructure = Collections.unmodifiableMap(copy);
    }

    /**
     * Vrátí zda daný ChunkPos obsahuje instanci jakékoli struktury.
     *
     * @param chunkPos chunk k ověření
     * @return {@code true} pokud chunk obsahuje origin jakékoli struktury
     */
    boolean containsAnyAt(ChunkPos chunkPos) {
        for (List<ChunkPos> positions : instancesByStructure.values()) {
            if (positions.contains(chunkPos)) return true;
        }
        return false;
    }

    /**
     * Vrátí zda daný ChunkPos obsahuje instanci konkrétní struktury.
     *
     * @param structureId ID struktury
     * @param chunkPos    chunk k ověření
     * @return {@code true} pokud chunk obsahuje origin dané struktury
     */
    boolean containsAt(ResourceLocation structureId, ChunkPos chunkPos) {
        List<ChunkPos> positions = instancesByStructure.get(structureId);
        return positions != null && positions.contains(chunkPos);
    }
}