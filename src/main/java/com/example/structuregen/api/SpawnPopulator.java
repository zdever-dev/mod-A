package com.example.structuregen.api;

import net.minecraft.server.level.ServerLevel;

/**
 * Callback volaný po dokončení generování celého komplexu pro umístění
 * entit, aktivaci mechanik nebo jakékoli jiné post-placement logiky.
 *
 * <h2>Kontrakt</h2>
 * <ul>
 *   <li>Zavolán <strong>přesně jednou</strong> per {@link StructureInstance}
 *       za normálního průběhu.</li>
 *   <li>Pokud server spadne mezi dokončením generování a zavoláním
 *       {@code populate()}, instance je při příštím loadu světa označena
 *       jako {@code pendingPopulation = true} v {@code StructureWorldState}.
 *       {@code populate()} je pak zavolán na prvním server tiku po loadu
 *       (viz A-5-16).</li>
 *   <li><strong>Thread safety:</strong> implementace nesmí předpokládat
 *       specifický thread. V normálním flow je volán ze server logického
 *       threadu uvnitř assembly pipeline. Při recovery po crashu je volán
 *       z {@code TickEvent.ServerTickEvent} handleru — stále server thread,
 *       ale jiný call stack.</li>
 *   <li>Implementace nesmí volat {@code StructureGeneratorAPI.generateAt()}
 *       rekurzivně ze {@code populate()} — výsledek je nedefinovaný.</li>
 * </ul>
 *
 * <h2>Příklad použití</h2>
 * <pre>{@code
 * SpawnPopulator populator = (instance, level) -> {
 *     BlockPos spawnPos = (BlockPos) instance.getMetadata().get("spawn_pos");
 *     if (spawnPos == null) return;
 *     MyEntity entity = new MyEntity(level);
 *     entity.moveTo(spawnPos, 0f, 0f);
 *     level.addFreshEntityWithPassengers(entity);
 * };
 * }</pre>
 */
@FunctionalInterface
public interface SpawnPopulator {

    /**
     * Naplní strukturu entitami nebo provede jiné post-placement akce.
     *
     * @param instance finální {@link StructureInstance} s kompletními
     *                 souřadnicemi místností, seedem a metadata;
     *                 nikdy {@code null}
     * @param level    server level ve kterém byla struktura vygenerována;
     *                 nikdy {@code null}
     */
    void populate(StructureInstance instance, net.minecraft.server.level.ServerLevel level);
}