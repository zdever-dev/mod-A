package com.example.structuregen.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Post-processing krok spouštěný po dokončení assembly a terrain adaptation.
 *
 * <p>Built-in procesory (priority 0–99) jsou spouštěny před custom procesory
 * (priority ≥ 100). Nižší číslo priority = dřívější spuštění.
 *
 * <p>Pořadí built-in procesorů:
 * <ol>
 *   <li>Priority 0 — {@code BiomeBlockReplacer}</li>
 *   <li>Priority 1 — {@code MossAndAgeProcessor}</li>
 *   <li>Priority 2 — {@code LightFillProcessor}</li>
 *   <li>Priority 3 — {@code LootTableInjector}</li>
 * </ol>
 *
 * <p>Plná implementace v kroku A-9.
 */
public interface PostProcessor {

    /**
     * Vrátí prioritu tohoto procesoru. Nižší = dříve.
     * Custom procesory by měly používat priority ≥ 100.
     *
     * @return priorita
     */
    int getPriority();

    /**
     * Provede post-processing nad umístěnými bloky struktury.
     *
     * @param level           server level; nikdy {@code null}
     * @param instance        finální instance struktury; nikdy {@code null}
     * @param placedPositions seznam absolutních pozic všech umístěných bloků;
     *                        nikdy {@code null}
     */
    void process(ServerLevel level, StructureInstance instance, List<BlockPos> placedPositions);
}