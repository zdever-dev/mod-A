package com.example.structuregen.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Kompletní sada pravidel řídících kde a jak smí být struktura vygenerována.
 *
 * <p>Vytvářejte výhradně přes {@link Builder} — přímá konstrukce není možná.
 * Všechna pole jsou neměnná po zavolání {@link Builder#build()}.
 *
 * <p>Config override hodnoty (viz A-11) jsou aplikovány přes
 * {@code ConfiguredGenerationRules.applyOverrides(GenerationRules base)}
 * těsně před každým generováním — nikdy se nekešují.
 *
 * <h2>Základní prostorová pole (A-2-5)</h2>
 * <ul>
 *   <li>{@link #allowedBiomes} — povolené biomy (prázdná = všechny)</li>
 *   <li>{@link #allowedDimensions} — povolené dimenze (prázdná = všechny)</li>
 *   <li>{@link #minDepth} — minimální hloubka od povrchu (bloky, Y-osa)</li>
 *   <li>{@link #maxDepth} — maximální hloubka od povrchu</li>
 *   <li>{@link #minDistanceFromSurface} — min vzdálenost stropu struktury od povrchu</li>
 *   <li>{@link #minDistanceBetweenInstances} — min chunk vzdálenost mezi instancemi stejného ID</li>
 * </ul>
 *
 * <h2>Pokročilá prostorová pole (A-2-6)</h2>
 * <ul>
 *   <li>{@link #flatnessThreshold} — max výšková odchylka terénu v footprintu</li>
 *   <li>{@link #biomeClusterCheck} — vyžaduje cluster stejného biome</li>
 *   <li>{@link #exclusionZones} — AABB oblasti kde se nesmí generovat</li>
 *   <li>{@link #terrainAdaptation} — globální terrain adaptation mód</li>
 *   <li>{@link #structureIntegrity} — globální integrity profil</li>
 * </ul>
 *
 * <h2>Assembly řídící pole (A-2-7)</h2>
 * <ul>
 *   <li>{@link #maxRoomCount} — max počet místností v komplexu</li>
 *   <li>{@link #requiredRoomTypes} — typy místností které musí být přítomny</li>
 *   <li>{@link #maxAssemblyTimeMs} — max doba assembly pokusu v ms</li>
 *   <li>{@link #maxRetriesPerChunkPos} — max počet pokusů per ChunkPos</li>
 *   <li>{@link #useDeterministicSeed} — deterministický seed výpočet</li>
 * </ul>
 */
public final class GenerationRules {

    // =========================================================================
    // A-2-5 — Základní prostorová pole
    // =========================================================================

    /**
     * Sada povolených biomů (jako {@link ResourceLocation}).
     * Prázdná množina = struktura smí být generována v jakémkoli biomu.
     *
     * <p>Lookup probíhá noise-based přes {@code BiomeSource.getNoiseBiome()} —
     * nikoli fyzickými bloky (viz A-7-6).
     */
    private final Set<ResourceLocation> allowedBiomes;

    /**
     * Sada povolených dimenzí (jako {@link ResourceKey}).
     * Prázdná množina = struktura smí být generována v jakékoli dimenzi.
     */
    private final Set<ResourceKey<Level>> allowedDimensions;

    /**
     * Minimální hloubka od povrchu v blocích (měřeno po Y-ose dolů).
     * {@code 0} = bez omezení shora.
     *
     * <p>Výchozí hodnota: {@code 0}.
     */
    private final int minDepth;

    /**
     * Maximální hloubka od povrchu v blocích.
     * {@code Integer.MAX_VALUE} = bez omezení zdola.
     *
     * <p>Výchozí hodnota: {@link Integer#MAX_VALUE}.
     */
    private final int maxDepth;

    /**
     * Minimální vzdálenost stropu struktury od povrchu terénu v blocích.
     * Zabraňuje aby strop prokoukl na povrch.
     *
     * <p>Výchozí hodnota: {@code 0} (žádné omezení).
     */
    private final int minDistanceFromSurface;

    /**
     * Minimální vzdálenost v chuncích mezi dvěma instancemi stejného
     * {@code structureId} ve stejné dimenzi.
     *
     * <p>Kontrola probíhá přes {@code StructureWorldState} — globální
     * persistent state. Cross-dimension instance se nepočítají.
     *
     * <p>Výchozí hodnota: {@code 0} (žádné omezení vzdálenosti).
     */
    private final int minDistanceBetweenInstances;

    // =========================================================================
    // A-2-6 — Pokročilá prostorová pole
    // =========================================================================

    /**
     * Maximální povolená výšková odchylka terénu v footprintu struktury
     * v blocích. Pokud je odchylka větší, generování je odmítnuto.
     *
     * <p>Výpočet probíhá noise-based (Heightmap nebo NoiseChunk sampling)
     * bez přístupu k fyzickým blokům sousedních chunků (viz A-7-7).
     *
     * <p>{@code Float.MAX_VALUE} = žádné omezení. Výchozí hodnota: {@link Float#MAX_VALUE}.
     */
    private final float flatnessThreshold;

    /**
     * Pokud {@code true}, engine ověří přes noise-based biome lookup
     * ({@code BiomeSource.getNoiseBiome()}) že v footprintu struktury
     * existuje dostatečně velký cluster stejného biome.
     *
     * <p>Práh clusteru je konfigurovatelný přes {@link Builder#biomeClusterCheckRadius}.
     * Výchozí hodnota: {@code false}.
     */
    private final boolean biomeClusterCheck;

    /**
     * Minimální poloměr biome clusteru v blocích pro {@link #biomeClusterCheck}.
     * Ignorováno pokud {@code biomeClusterCheck == false}.
     *
     * <p>Výchozí hodnota: {@code 16}.
     */
    private final int biomeClusterCheckRadius;

    /**
     * Oblasti ve světě kde nesmí být struktura generována.
     * Každá {@link AABB} definuje exkluzní zónu v absolutních světových
     * souřadnicích.
     *
     * <p>Výchozí hodnota: prázdný seznam (žádné exkluzní zóny).
     */
    private final List<AABB> exclusionZones;

    /**
     * Globální terrain adaptation mód pro celou strukturu.
     * Individuální {@link RoomTemplate#getTerrainAdaptationMode()} override
     * má přednost před tímto globálním nastavením.
     *
     * <p>Výchozí hodnota: {@link TerrainAdaptationMode#NONE}.
     */
    private final TerrainAdaptationMode terrainAdaptation;

    /**
     * Globální integrity profil pro celou strukturu.
     * Individuální {@link RoomTemplate#getIntegrityProfile()} override
     * má přednost před tímto globálním nastavením.
     *
     * <p>Výchozí hodnota: {@link StructureIntegrityProfile#PERFECT}.
     */
    private final StructureIntegrityProfile structureIntegrity;

    // =========================================================================
    // A-2-7 — Assembly řídící pole
    // =========================================================================

    /**
     * Maximální počet místností v jednom komplexu (bez terminátorů).
     * Po dosažení limitu všechny volné ConnectionPointy dostanou terminator
     * nebo seal operaci.
     *
     * <p>Výchozí hodnota: {@code 50}.
     */
    private final int maxRoomCount;

    /**
     * Seznam {@link RoomType} hodnot které musí být přítomny ve výsledném
     * komplexu. Pokud assembly dokončí bez přítomnosti všech required typů,
     * výsledek je zahozen a engine provede retry.
     *
     * <p>Validace při registraci ověří že všechny required typy jsou dosažitelné
     * z ROOT template přes graf ConnectionPoint tagů (viz A-3-5).
     *
     * <p>Výchozí hodnota: prázdný seznam (žádné povinné typy).
     */
    private final List<RoomType> requiredRoomTypes;

    /**
     * Maximální doba v milisekundách kterou smí jeden assembly pokus běžet.
     * Při překročení je assembly přerušen a zahozen.
     *
     * <p>Timeout se aplikuje per pokus — {@link #maxRetriesPerChunkPos}
     * omezuje celkový počet pokusů.
     *
     * <p>Výchozí hodnota: {@code 50} ms.
     */
    private final long maxAssemblyTimeMs;

    /**
     * Maximální počet assembly pokusů per ChunkPos pro jednu strukturu.
     * Po vyčerpání je zapsán permanent "failed" marker a chunk již nikdy
     * nebude znovu pokoušen pro tuto strukturu.
     *
     * <p>Výchozí hodnota: {@code 3}.
     */
    private final int maxRetriesPerChunkPos;

    /**
     * Pokud {@code true}, seed instance je vypočten deterministicky jako
     * {@code worldSeed ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L)}.
     * Stejná pozice vždy produkuje stejnou strukturu bez ohledu na instance
     * counter.
     *
     * <p>Výchozí hodnota: {@code true}.
     */
    private final boolean useDeterministicSeed;

    // =========================================================================
    // Constructor (private)
    // =========================================================================

    private GenerationRules(Builder b) {
        this.allowedBiomes               = Collections.unmodifiableSet(new HashSet<>(b.allowedBiomes));
        this.allowedDimensions           = Collections.unmodifiableSet(new HashSet<>(b.allowedDimensions));
        this.minDepth                    = b.minDepth;
        this.maxDepth                    = b.maxDepth;
        this.minDistanceFromSurface      = b.minDistanceFromSurface;
        this.minDistanceBetweenInstances = b.minDistanceBetweenInstances;
        this.flatnessThreshold           = b.flatnessThreshold;
        this.biomeClusterCheck           = b.biomeClusterCheck;
        this.biomeClusterCheckRadius     = b.biomeClusterCheckRadius;
        this.exclusionZones              = Collections.unmodifiableList(new ArrayList<>(b.exclusionZones));
        this.terrainAdaptation           = b.terrainAdaptation;
        this.structureIntegrity          = b.structureIntegrity;
        this.maxRoomCount                = b.maxRoomCount;
        this.requiredRoomTypes           = Collections.unmodifiableList(new ArrayList<>(b.requiredRoomTypes));
        this.maxAssemblyTimeMs           = b.maxAssemblyTimeMs;
        this.maxRetriesPerChunkPos       = b.maxRetriesPerChunkPos;
        this.useDeterministicSeed        = b.useDeterministicSeed;
    }

    // ---- Static factory ------------------------------------------------------

    /** @return nový builder s výchozími hodnotami; nikdy {@code null} */
    public static Builder builder() {
        return new Builder();
    }

    // ---- Accessors -----------------------------------------------------------

    public Set<ResourceLocation> getAllowedBiomes()              { return allowedBiomes; }
    public Set<ResourceKey<Level>> getAllowedDimensions()        { return allowedDimensions; }
    public int getMinDepth()                                     { return minDepth; }
    public int getMaxDepth()                                     { return maxDepth; }
    public int getMinDistanceFromSurface()                       { return minDistanceFromSurface; }
    public int getMinDistanceBetweenInstances()                  { return minDistanceBetweenInstances; }
    public float getFlatnessThreshold()                          { return flatnessThreshold; }
    public boolean isBiomeClusterCheck()                         { return biomeClusterCheck; }
    public int getBiomeClusterCheckRadius()                      { return biomeClusterCheckRadius; }
    public List<AABB> getExclusionZones()                        { return exclusionZones; }
    public TerrainAdaptationMode getTerrainAdaptation()          { return terrainAdaptation; }
    public StructureIntegrityProfile getStructureIntegrity()     { return structureIntegrity; }
    public int getMaxRoomCount()                                 { return maxRoomCount; }
    public List<RoomType> getRequiredRoomTypes()                 { return requiredRoomTypes; }
    public long getMaxAssemblyTimeMs()                           { return maxAssemblyTimeMs; }
    public int getMaxRetriesPerChunkPos()                        { return maxRetriesPerChunkPos; }
    public boolean isUseDeterministicSeed()                      { return useDeterministicSeed; }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Fluent builder pro {@link GenerationRules}.
     * Všechna pole jsou volitelná — bez nastavení se použijí výchozí hodnoty.
     */
    public static final class Builder {

        // A-2-5 defaults
        private final Set<ResourceLocation> allowedBiomes       = new HashSet<>();
        private final Set<ResourceKey<Level>> allowedDimensions = new HashSet<>();
        private int minDepth                    = 0;
        private int maxDepth                    = Integer.MAX_VALUE;
        private int minDistanceFromSurface      = 0;
        private int minDistanceBetweenInstances = 0;

        // A-2-6 defaults
        private float flatnessThreshold         = Float.MAX_VALUE;
        private boolean biomeClusterCheck       = false;
        private int biomeClusterCheckRadius     = 16;
        private final List<AABB> exclusionZones = new ArrayList<>();
        private TerrainAdaptationMode terrainAdaptation      = TerrainAdaptationMode.NONE;
        private StructureIntegrityProfile structureIntegrity = StructureIntegrityProfile.PERFECT;

        // A-2-7 defaults
        private int maxRoomCount                         = 50;
        private final List<RoomType> requiredRoomTypes   = new ArrayList<>();
        private long maxAssemblyTimeMs                   = 50L;
        private int maxRetriesPerChunkPos                = 3;
        private boolean useDeterministicSeed             = true;

        private Builder() {}

        // ---- A-2-5 setters ---------------------------------------------------

        /**
         * Nastaví povolené biomy. Prázdná množina = všechny biomy povoleny.
         *
         * @param biomes set {@link ResourceLocation} klíčů biomů; nesmí být {@code null}
         * @return {@code this}
         */
        public Builder allowedBiomes(Set<ResourceLocation> biomes) {
            this.allowedBiomes.addAll(Objects.requireNonNull(biomes));
            return this;
        }

        /**
         * Nastaví povolené dimenze. Prázdná množina = všechny dimenze povoleny.
         *
         * @param dimensions set dimension resource keys; nesmí být {@code null}
         * @return {@code this}
         */
        public Builder allowedDimensions(Set<ResourceKey<Level>> dimensions) {
            this.allowedDimensions.addAll(Objects.requireNonNull(dimensions));
            return this;
        }

        /**
         * Minimální hloubka od povrchu v blocích.
         *
         * @param depth musí být &ge; 0; výchozí {@code 0}
         * @return {@code this}
         */
        public Builder minDepth(int depth) {
            if (depth < 0) throw new IllegalArgumentException("minDepth must be >= 0");
            this.minDepth = depth;
            return this;
        }

        /**
         * Maximální hloubka od povrchu v blocích.
         *
         * @param depth musí být &ge; {@link #minDepth}; výchozí {@link Integer#MAX_VALUE}
         * @return {@code this}
         */
        public Builder maxDepth(int depth) {
            this.maxDepth = depth;
            return this;
        }

        /**
         * Minimální vzdálenost stropu struktury od povrchu terénu.
         *
         * @param distance musí být &ge; 0; výchozí {@code 0}
         * @return {@code this}
         */
        public Builder minDistanceFromSurface(int distance) {
            if (distance < 0) throw new IllegalArgumentException("minDistanceFromSurface must be >= 0");
            this.minDistanceFromSurface = distance;
            return this;
        }

        /**
         * Minimální vzdálenost v chuncích mezi instancemi stejného structureId.
         *
         * @param chunks musí být &ge; 0; výchozí {@code 0}
         * @return {@code this}
         */
        public Builder minDistanceBetweenInstances(int chunks) {
            if (chunks < 0) throw new IllegalArgumentException("minDistanceBetweenInstances must be >= 0");
            this.minDistanceBetweenInstances = chunks;
            return this;
        }

        // ---- A-2-6 setters ---------------------------------------------------

        /**
         * Maximální povolená výšková odchylka terénu v footprintu.
         * {@link Float#MAX_VALUE} = žádné omezení.
         *
         * <p>Noise-based výpočet — bez fyzických chunk přístupů (viz A-7-7).
         *
         * @param threshold musí být &ge; 0; výchozí {@link Float#MAX_VALUE}
         * @return {@code this}
         */
        public Builder flatnessThreshold(float threshold) {
            if (threshold < 0) throw new IllegalArgumentException("flatnessThreshold must be >= 0");
            this.flatnessThreshold = threshold;
            return this;
        }

        /**
         * Zapne/vypne biome cluster check.
         * Radius nastavuje {@link #biomeClusterCheckRadius(int)}.
         *
         * <p>Noise-based lookup přes {@code BiomeSource.getNoiseBiome()} (viz A-7-6).
         *
         * @param enabled výchozí {@code false}
         * @return {@code this}
         */
        public Builder biomeClusterCheck(boolean enabled) {
            this.biomeClusterCheck = enabled;
            return this;
        }

        /**
         * Poloměr biome clusteru v blocích pro biome cluster check.
         * Ignorováno pokud {@code biomeClusterCheck == false}.
         *
         * @param radius musí být &gt; 0; výchozí {@code 16}
         * @return {@code this}
         */
        public Builder biomeClusterCheckRadius(int radius) {
            if (radius <= 0) throw new IllegalArgumentException("biomeClusterCheckRadius must be > 0");
            this.biomeClusterCheckRadius = radius;
            return this;
        }

        /**
         * Přidá exkluzní zóny kde se nesmí generovat.
         *
         * @param zones seznam {@link AABB} v absolutních souřadnicích; nesmí být {@code null}
         * @return {@code this}
         */
        public Builder exclusionZones(List<AABB> zones) {
            this.exclusionZones.addAll(Objects.requireNonNull(zones));
            return this;
        }

        /**
         * Globální terrain adaptation mód.
         * Per-template override má přednost (viz {@link RoomTemplate#getTerrainAdaptationMode()}).
         *
         * @param mode nesmí být {@code null}; výchozí {@link TerrainAdaptationMode#NONE}
         * @return {@code this}
         */
        public Builder terrainAdaptation(TerrainAdaptationMode mode) {
            this.terrainAdaptation = Objects.requireNonNull(mode);
            return this;
        }

        /**
         * Globální integrity profil.
         * Per-template override má přednost (viz {@link RoomTemplate#getIntegrityProfile()}).
         *
         * @param profile nesmí být {@code null}; výchozí {@link StructureIntegrityProfile#PERFECT}
         * @return {@code this}
         */
        public Builder structureIntegrity(StructureIntegrityProfile profile) {
            this.structureIntegrity = Objects.requireNonNull(profile);
            return this;
        }

        // ---- A-2-7 setters ---------------------------------------------------

        /**
         * Maximální počet místností komplexu (bez terminátorů).
         *
         * @param count musí být &ge; 1; výchozí {@code 50}
         * @return {@code this}
         */
        public Builder maxRoomCount(int count) {
            if (count < 1) throw new IllegalArgumentException("maxRoomCount must be >= 1");
            this.maxRoomCount = count;
            return this;
        }

        /**
         * Typy místností které musí být přítomny ve výsledném komplexu.
         * Assembly engine dynamicky zvyšuje váhu chybějících typů úměrně
         * postupu generování (viz A-5-6).
         *
         * @param types seznam povinných typů; nesmí být {@code null}
         * @return {@code this}
         */
        public Builder requiredRoomTypes(List<RoomType> types) {
            this.requiredRoomTypes.addAll(Objects.requireNonNull(types));
            return this;
        }

        /**
         * Maximální doba jednoho assembly pokusu v milisekundách.
         *
         * @param ms musí být &gt; 0; výchozí {@code 50}
         * @return {@code this}
         */
        public Builder maxAssemblyTimeMs(long ms) {
            if (ms <= 0) throw new IllegalArgumentException("maxAssemblyTimeMs must be > 0");
            this.maxAssemblyTimeMs = ms;
            return this;
        }

        /**
         * Maximální počet pokusů per ChunkPos před zápisem permanent failed markeru.
         *
         * @param retries musí být &ge; 1; výchozí {@code 3}
         * @return {@code this}
         */
        public Builder maxRetriesPerChunkPos(int retries) {
            if (retries < 1) throw new IllegalArgumentException("maxRetriesPerChunkPos must be >= 1");
            this.maxRetriesPerChunkPos = retries;
            return this;
        }

        /**
         * Zapne/vypne deterministický seed výpočet.
         *
         * @param deterministic výchozí {@code true}
         * @return {@code this}
         */
        public Builder useDeterministicSeed(boolean deterministic) {
            this.useDeterministicSeed = deterministic;
            return this;
        }

        /**
         * Sestaví {@link GenerationRules} a validuje konzistenci polí.
         *
         * @return nová immutable instance
         * @throws IllegalStateException pokud {@code minDepth > maxDepth}
         */
        public GenerationRules build() {
            if (minDepth > maxDepth) {
                throw new IllegalStateException(
                    "GenerationRules: minDepth (" + minDepth + ") must be <= maxDepth (" + maxDepth + ")"
                );
            }
            return new GenerationRules(this);
        }
    }
}