package com.example.structuregen.internal.terrain;

import com.example.structuregen.api.TerrainAdapter;
import com.example.structuregen.api.TerrainAdaptationMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

/**
 * Placeholder adapter pro {@link TerrainAdaptationMode#SMOOTH_TRANSITION}.
 *
 * <p>Deleguje na {@link NoneTerrainAdapter} dokud není plně implementován.
 * Plná implementace plánována v budoucí verzi.
 */
@ApiStatus.Experimental
public final class SmoothTransitionTerrainAdapter implements TerrainAdapter {

    public static final SmoothTransitionTerrainAdapter INSTANCE =
        new SmoothTransitionTerrainAdapter();

    private SmoothTransitionTerrainAdapter() {}

    @Override
    public void adapt(ServerLevel level, BoundingBox structureBounds,
                      TerrainAdaptationMode mode, long seed) {
        // TODO: implementovat plynulý přechod terén ↔ struktura
        NoneTerrainAdapter.INSTANCE.adapt(level, structureBounds, mode, seed);
    }
}