package com.example.structuregen.internal.terrain;

import com.example.structuregen.api.TerrainAdapter;
import com.example.structuregen.api.TerrainAdaptationMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

/**
 * Placeholder adapter pro {@link TerrainAdaptationMode#BURY}.
 *
 * <p>Deleguje na {@link NoneTerrainAdapter} dokud není plně implementován.
 * Plná implementace plánována v budoucí verzi.
 */
@ApiStatus.Experimental
public final class BuryTerrainAdapter implements TerrainAdapter {

    public static final BuryTerrainAdapter INSTANCE = new BuryTerrainAdapter();

    private BuryTerrainAdapter() {}

    @Override
    public void adapt(ServerLevel level, BoundingBox structureBounds,
                      TerrainAdaptationMode mode, long seed) {
        // TODO: pohřbít strukturu — výpočet hloubky tak aby byl strop překryt
        // zadaným počtem vrstev terénu.
        NoneTerrainAdapter.INSTANCE.adapt(level, structureBounds, mode, seed);
    }
}