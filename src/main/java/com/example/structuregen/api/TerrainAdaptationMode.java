package com.example.structuregen.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Určuje jak se vygenerovaná struktura přizpůsobí okolnímu terénu.
 *
 * <p>Každý {@link RoomTemplate} může deklarovat vlastní mód přes
 * {@link RoomTemplate#getTerrainAdaptationMode()}. Pokud template žádný
 * mód nedeklaruje, použije se {@link #NONE}.
 *
 * <h2>Stav implementace</h2>
 * <ul>
 *   <li>{@link #NONE} — implementováno (v1.0)</li>
 *   <li>{@link #FILL_BELOW} — implementováno (v1.0)</li>
 *   <li>{@link #CARVE_ABOVE} — implementováno (v1.0)</li>
 *   <li>{@link #SMOOTH_TRANSITION} — placeholder, implementace v budoucí verzi</li>
 *   <li>{@link #BURY} — placeholder, implementace v budoucí verzi</li>
 *   <li>{@link #CUSTOM} — rezervováno pro třetí strany přes {@code TerrainAdapterRegistry}</li>
 * </ul>
 *
 * <p>Terrain adaptation se spouští po dokončení room placement a před
 * {@code PostProcessorChain} (viz A-6-1).
 */
public enum TerrainAdaptationMode {

    /**
     * Žádná adaptace. Struktura je umístěna přesně na zadané souřadnice
     * bez jakýchkoli úprav okolního terénu.
     *
     * <p>Výchozí hodnota pro všechny {@link RoomTemplate} které nepřepisují
     * {@link RoomTemplate#getTerrainAdaptationMode()}.
     *
     * <p><strong>Stav:</strong> implementováno (v1.0).
     */
    NONE,

    /**
     * Vyplní vzduch pod strukturou pevným materiálem (dominantní biome ground block)
     * až na pevný terén. Použitelné pro struktury generované na povrchu nebo
     * na hraně terénu.
     *
     * <p>Výpočet probíhá noise-based — bez přístupu k fyzickým blokům
     * sousedních chunků mimo aktuálně zpracovávaný chunk.
     *
     * <p><strong>Stav:</strong> implementováno (v1.0).
     */
    FILL_BELOW,

    /**
     * Odstraní (nahradí {@code AIR}) bloky nad strukturou od jejího stropu
     * až po pevný terén nad ní. Použitelné pro podzemní struktury kde je
     * potřeba vytvořit prostor.
     *
     * <p><strong>Stav:</strong> implementováno (v1.0).
     */
    CARVE_ABOVE,

    /**
     * Vytvoří plynulý přechod mezi strukturou a okolním terénem — postupné
     * schodovité nebo zaoblené okraje.
     *
     * <p><strong>Stav:</strong> placeholder — deleguje na {@link #NONE}.
     * Plná implementace plánována v budoucí verzi.
     */
    @ApiStatus.Experimental
    SMOOTH_TRANSITION,

    /**
     * Pohřbí strukturu do terénu — výpočet hloubky tak aby byl strop
     * struktury překryt zadaným počtem vrstev původního terénu.
     *
     * <p><strong>Stav:</strong> placeholder — deleguje na {@link #NONE}.
     * Plná implementace plánována v budoucí verzi.
     */
    @ApiStatus.Experimental
    BURY,

    /**
     * Uživatelsky definovaný mód. Vývojář třetí strany registruje vlastní
     * {@code TerrainAdapter} implementaci přes {@code TerrainAdapterRegistry}
     * (viz A-6-1) namapovanou na tuto hodnotu.
     *
     * <p>Pokud žádná custom implementace není registrována, chová se jako
     * {@link #NONE}.
     */
    CUSTOM
}