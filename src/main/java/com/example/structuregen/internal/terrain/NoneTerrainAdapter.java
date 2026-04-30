package com.example.structuregen.internal.terrain;

import com.example.structuregen.api.TerrainAdapter;
import com.example.structuregen.api.TerrainAdaptationMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * No-op terrain adapter pro {@link TerrainAdaptationMode#NONE}.
 *
 * <p>Výchozí adapter — registrován jako fallback pro všechny módy
 * bez explicitní registrace.
 *
 * <p>Také slouží jako fallback pro {@link TerrainAdaptationMode#SMOOTH_TRANSITION}
 * a {@link TerrainAdaptationMode#BURY} dokud nejsou plně implementovány.
 */
public final class NoneTerrainAdapter implements TerrainAdapter {

    public static final NoneTerrainAdapter INSTANCE = new NoneTerrainAdapter();

    private NoneTerrainAdapter() {}

    @Override
    public void adapt(ServerLevel level, BoundingBox structureBounds,
                      TerrainAdaptationMode mode, long seed) {
        // No-op — záměrně prázdné
    }
}