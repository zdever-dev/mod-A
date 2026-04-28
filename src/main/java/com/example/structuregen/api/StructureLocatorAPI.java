package com.example.structuregen.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Read-only query API pro vyhledávání a dotazování vygenerovaných struktur
 * v herním světě.
 *
 * <h2>Thread safety</h2>
 * <p>Všechny metody jsou <strong>lock-free</strong> a čtou z
 * {@code AtomicReference<ImmutableSpatialSnapshot>} (viz A-4-6).
 * Je bezpečné volat je z main server threadu, z {@code EntityJoinLevelEvent},
 * i z libovolného jiného threadu — nikdy nedojde k deadlocku s worldgen
 * worker threadem.
 *
 * <p><strong>Stub notice:</strong> tato třída obsahuje pouze signatury.
 * Plná implementace bude přidána v kroku A-12.
 */
public final class StructureLocatorAPI {

    /**
     * Najde nejbližší instanci dané struktury k zadané pozici.
     *
     * <p><strong>Thread safety:</strong> lock-free, bezpečné z main threadu.
     *
     * @param level       dimenze ve které hledáme; nesmí být {@code null}
     * @param from        výchozí pozice pro měření vzdálenosti; nesmí být {@code null}
     * @param structureId ID struktury; nesmí být {@code null}
     * @return {@link Optional} s origin BlockPos nejbližší instance,
     *         nebo prázdný pokud žádná instance neexistuje
     */
    public static Optional<BlockPos> findNearest(ServerLevel level,
                                                  BlockPos from,
                                                  ResourceLocation structureId) {
        // TODO (A-12-1): implementovat přes ImmutableSpatialSnapshot
        return Optional.empty();
    }

    /**
     * Vrátí seznam origin pozic všech instancí dané struktury v dimenzi.
     *
     * <p><strong>Thread safety:</strong> lock-free, bezpečné z main threadu.
     *
     * @param level       dimenze; nesmí být {@code null}
     * @param structureId ID struktury; nesmí být {@code null}
     * @return immutable seznam pozic; prázdný pokud žádná instance neexistuje;
     *         nikdy {@code null}
     */
    public static List<BlockPos> getInstancesOf(ServerLevel level,
                                                  ResourceLocation structureId) {
        // TODO (A-12-1): implementovat přes ImmutableSpatialSnapshot
        return Collections.emptyList();
    }

    /**
     * Vrátí zda se daná pozice nachází uvnitř jakékoli vygenerované struktury.
     *
     * <p><strong>Thread safety:</strong> lock-free, bezpečné z main threadu
     * i z {@code EntityJoinLevelEvent} — nikdy nezpůsobí deadlock s worldgen
     * worker threadem.
     *
     * @param level dimenze; nesmí být {@code null}
     * @param pos   pozice k ověření; nesmí být {@code null}
     * @return {@code true} pokud je pozice uvnitř struktury
     */
    public static boolean isInsideStructure(ServerLevel level, BlockPos pos) {
        // TODO (A-12-2): lookup v snapshot ChunkPos indexu + BoundingBox přesný check
        return false;
    }

    /**
     * Vrátí ID struktury na dané pozici.
     *
     * <p><strong>Thread safety:</strong> lock-free, bezpečné z main threadu.
     *
     * @param level dimenze; nesmí být {@code null}
     * @param pos   pozice k ověření; nesmí být {@code null}
     * @return {@link Optional} s ID struktury, nebo prázdný pokud na pozici
     *         žádná struktura není
     */
    public static Optional<ResourceLocation> getStructureAt(ServerLevel level, BlockPos pos) {
        // TODO (A-12-2): implementovat přes ImmutableSpatialSnapshot
        return Optional.empty();
    }

    /** Utility třída — žádná instance. */
    private StructureLocatorAPI() {
        throw new UnsupportedOperationException("StructureLocatorAPI is a utility class");
    }
}