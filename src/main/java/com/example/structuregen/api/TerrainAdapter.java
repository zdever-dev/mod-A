package com.example.structuregen.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Implementuje terrain adaptation pro jeden {@link TerrainAdaptationMode}.
 *
 * <p>Voláno assembly enginem po dokončení room placement a před
 * {@code PostProcessorChain} (viz A-6-1). Každý mód má vlastní
 * implementaci registrovanou v {@link TerrainAdapterRegistry}.
 *
 * <h2>Thread safety</h2>
 * <p>Implementace musí být bezstavová nebo thread-safe — jedna instance
 * může být volána z více threadů paralelně.
 *
 * <h2>Constraint</h2>
 * <p>Implementace nesmí přistupovat k fyzickým blokům v sousedních
 * chuncích mimo aktuálně zpracovávaný chunk. Veškeré výpočty výšky
 * a biome musí být noise-based.
 */
public interface TerrainAdapter {

    /**
     * Přizpůsobí terén kolem struktury.
     *
     * @param level           server level; nesmí být {@code null}
     * @param structureBounds BoundingBox celé struktury v absolutních
     *                        souřadnicích; nesmí být {@code null}
     * @param mode            aktivní mód; nesmí být {@code null}
     * @param seed            seed instance pro seed-based operace
     */
    void adapt(ServerLevel level,
               BoundingBox structureBounds,
               TerrainAdaptationMode mode,
               long seed);
}