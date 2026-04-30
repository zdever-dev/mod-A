package com.example.structuregen.api;

import com.example.structuregen.StructureGenMod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton registr mapující {@link TerrainAdaptationMode} na
 * {@link TerrainAdapter} implementace.
 *
 * <p>Built-in adaptery jsou registrovány při inicializaci modu.
 * Třetí strany mohou registrovat custom adaptér pro {@link TerrainAdaptationMode#CUSTOM}
 * přes {@link #register(TerrainAdaptationMode, TerrainAdapter)}.
 *
 * <h2>Přístup</h2>
 * <p>Přes {@link #getInstance()} — singleton.
 */
public final class TerrainAdapterRegistry {

    private static final TerrainAdapterRegistry INSTANCE = new TerrainAdapterRegistry();

    private final Map<TerrainAdaptationMode, TerrainAdapter> adapters = new HashMap<>();

    private TerrainAdapterRegistry() {}

    /** @return singleton instance; nikdy {@code null} */
    public static TerrainAdapterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registruje adapter pro daný mód.
     * Přepisuje existující registraci pokud existuje.
     *
     * @param mode    terrain adaptation mód; nesmí být {@code null}
     * @param adapter implementace; nesmí být {@code null}
     */
    public void register(TerrainAdaptationMode mode, TerrainAdapter adapter) {
        Objects.requireNonNull(mode,    "mode must not be null");
        Objects.requireNonNull(adapter, "adapter must not be null");
        adapters.put(mode, adapter);
        StructureGenMod.LOGGER.debug(
            "TerrainAdapterRegistry: registered adapter for mode '{}'.", mode
        );
    }

    /**
     * Vrátí adapter pro daný mód.
     * Pokud pro mód není registrován adapter, vrátí NONE adapter (no-op).
     *
     * @param mode terrain adaptation mód
     * @return adapter; nikdy {@code null}
     */
    public TerrainAdapter getAdapter(TerrainAdaptationMode mode) {
        return adapters.getOrDefault(mode, adapters.get(TerrainAdaptationMode.NONE));
    }

    /**
     * Aplikuje terrain adaptation pro daný mód.
     * Convenience metoda kombinující {@link #getAdapter} + {@link TerrainAdapter#adapt}.
     *
     * @param level           server level
     * @param structureBounds BoundingBox struktury
     * @param mode            mód k aplikaci
     * @param seed            seed instance
     */
    public void adapt(net.minecraft.server.level.ServerLevel level,
                      net.minecraft.world.level.levelgen.structure.BoundingBox structureBounds,
                      TerrainAdaptationMode mode,
                      long seed) {
        if (mode == TerrainAdaptationMode.NONE) return; // Fast path
        getAdapter(mode).adapt(level, structureBounds, mode, seed);
    }
}