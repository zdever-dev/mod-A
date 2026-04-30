package com.example.structuregen.internal.terrain;

import com.example.structuregen.StructureGenMod;
import com.example.structuregen.api.TerrainAdapter;
import com.example.structuregen.api.TerrainAdaptationMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Terrain adapter pro {@link TerrainAdaptationMode#FILL_BELOW}.
 *
 * <p>Pro každý sloupec v footprintu struktury zjistí nejnižší Y struktury
 * a vyplní vzduch od {@code minY - 1} dolů na pevný terén.
 * Materiál výplně = dominantní biome ground block (noise-based odhad).
 *
 * <h2>Constraint</h2>
 * <p>Výška terénu je odhadována přes {@link Heightmap} — noise-based,
 * bez přístupu k fyzickým blokům sousedních chunků.
 */
public final class FillBelowTerrainAdapter implements TerrainAdapter {

    public static final FillBelowTerrainAdapter INSTANCE = new FillBelowTerrainAdapter();

    private FillBelowTerrainAdapter() {}

    @Override
    public void adapt(ServerLevel level, BoundingBox structureBounds,
                      TerrainAdaptationMode mode, long seed) {

        int minY = structureBounds.minY();

        for (int x = structureBounds.minX(); x <= structureBounds.maxX(); x++) {
            for (int z = structureBounds.minZ(); z <= structureBounds.maxZ(); z++) {

                // Heightmap-based odhad povrchu — noise-based, bez sousedních chunků
                int surfaceY = level.getHeight(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    x, z
                );

                // Výplňový materiál dle biome
                BlockState fillBlock = getFillBlock(level, x, z);

                // Vyplň od minY-1 dolů na povrch
                for (int y = minY - 1; y >= surfaceY; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState existing = level.getBlockState(pos);

                    // Přepiš pouze vzduch a kapaliny — nechej pevné bloky
                    if (existing.isAir() || existing.liquid()) {
                        level.setBlock(pos, fillBlock, 3);
                    }
                }
            }
        }

        StructureGenMod.LOGGER.debug(
            "FillBelowTerrainAdapter: filled below structure at y={}, "
            + "footprint {}x{}.",
            minY,
            structureBounds.maxX() - structureBounds.minX() + 1,
            structureBounds.maxZ() - structureBounds.minZ() + 1
        );
    }

    /**
     * Vrátí vhodný fill block pro danou pozici dle biome.
     * Noise-based — používá biome kategorii pro výběr materiálu.
     *
     * @param level server level
     * @param x     world X
     * @param z     world Z
     * @return fill block state
     */
    private BlockState getFillBlock(ServerLevel level, int x, int z) {
        // Noise-based biome lookup — bezpečné bez přístupu k sousedním chunkům
        Biome biome = level.getBiome(new BlockPos(x, 64, z)).value();

        // Výběr materiálu dle biome teploty a srážek jako proxy pro typ biome
        float temperature  = biome.getBaseTemperature();
        float downfall     = biome.climateSettings().downfall();

        if (temperature > 1.5f && downfall < 0.1f) {
            // Pouštní biome — písek
            return Blocks.SANDSTONE.defaultBlockState();
        } else if (temperature < 0.15f) {
            // Snehový biome — led/kámen
            return Blocks.STONE.defaultBlockState();
        } else {
            // Výchozí — kámen
            return Blocks.STONE.defaultBlockState();
        }
    }
}