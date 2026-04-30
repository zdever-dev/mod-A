package com.example.structuregen.internal.terrain;

import com.example.structuregen.StructureGenMod;
import com.example.structuregen.api.TerrainAdapter;
import com.example.structuregen.api.TerrainAdaptationMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Terrain adapter pro {@link TerrainAdaptationMode#CARVE_ABOVE}.
 *
 * <p>Pro každý sloupec v footprintu struktury zjistí nejvyšší Y struktury
 * a odstraní (nahradí {@code AIR}) pevné bloky od {@code maxY + 1} nahoru
 * do povrchu terénu nad strukturou.
 *
 * <h2>Constraint</h2>
 * <p>Výška terénu je odhadována přes {@link Heightmap} — noise-based.
 */
public final class CarveAboveTerrainAdapter implements TerrainAdapter {

    public static final CarveAboveTerrainAdapter INSTANCE = new CarveAboveTerrainAdapter();

    private CarveAboveTerrainAdapter() {}

    @Override
    public void adapt(ServerLevel level, BoundingBox structureBounds,
                      TerrainAdaptationMode mode, long seed) {

        int maxY = structureBounds.maxY();

        int carvedCount = 0;

        for (int x = structureBounds.minX(); x <= structureBounds.maxX(); x++) {
            for (int z = structureBounds.minZ(); z <= structureBounds.maxZ(); z++) {

                // Heightmap-based odhad povrchu
                int surfaceY = level.getHeight(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    x, z
                );

                // Odstraň bloky od maxY+1 nahoru do povrchu
                for (int y = maxY + 1; y <= surfaceY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState existing = level.getBlockState(pos);

                    // Odstraňuj pouze pevné bloky — nechej kapaliny (nezaplaví díru)
                    if (!existing.isAir() && !existing.liquid()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        carvedCount++;
                    }
                }
            }
        }

        StructureGenMod.LOGGER.debug(
            "CarveAboveTerrainAdapter: carved {} blocks above structure at y={}, "
            + "footprint {}x{}.",
            carvedCount, maxY,
            structureBounds.maxX() - structureBounds.minX() + 1,
            structureBounds.maxZ() - structureBounds.minZ() + 1
        );
    }
}