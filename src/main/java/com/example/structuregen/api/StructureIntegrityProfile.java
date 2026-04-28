package com.example.structuregen.api;

import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Definuje míru "integrity" (zachovalosti) struktury při generování.
 *
 * <p>Assembly engine čte {@code baseIntegrity} a {@code integrityVariance}
 * z tohoto profilu a pro každou instanci vypočítá:
 * <pre>
 *   effectiveIntegrity = baseIntegrity ± (seededRandom.nextFloat() * integrityVariance)
 * </pre>
 * Každý blok v {@code doPlace()} je s pravděpodobností
 * {@code (1 - effectiveIntegrity)} přeskočen — kromě bloků v
 * {@link #exemptBlocks}. Z bloků určených k vynechání jsou preferovány
 * ty v {@link #preferredRemovalBlocks}.
 *
 * <p>Integrity enforcement je implementován výhradně v
 * {@link RoomTemplate#placeWithIntegrity} — vývojář třetí strany ho
 * nemůže obejít ani přepsat.
 *
 * <h2>Příklad</h2>
 * <pre>{@code
 * StructureIntegrityProfile profile = StructureIntegrityProfile.builder()
 *     .baseIntegrity(0.85f)
 *     .integrityVariance(0.1f)
 *     .exemptBlocks(Set.of(Blocks.STONE_BRICKS))
 *     .preferredRemovalBlocks(List.of(Blocks.CRACKED_STONE_BRICKS))
 *     .build();
 * }</pre>
 */
public final class StructureIntegrityProfile {

    // ---- Constant ------------------------------------------------------------

    /**
     * Profil bez jakéhokoli poškození — všechny bloky jsou vždy umístěny.
     * Výchozí hodnota pro {@link RoomTemplate#getIntegrityProfile()}.
     */
    public static final StructureIntegrityProfile PERFECT = builder()
        .baseIntegrity(1.0f)
        .integrityVariance(0.0f)
        .build();

    // ---- Fields --------------------------------------------------------------

    /**
     * Základní pravděpodobnost že blok bude umístěn. Rozsah {@code [0.0, 1.0]}.
     * {@code 1.0} = všechny bloky umístěny, {@code 0.0} = žádný blok umístěn.
     */
    private final float baseIntegrity;

    /**
     * Maximální odchylka integrity per instance (seed-based).
     * Výsledná integrity je v rozsahu
     * {@code [baseIntegrity - variance, baseIntegrity + variance]},
     * clampováno na {@code [0.0, 1.0]}.
     */
    private final float integrityVariance;

    /**
     * Bloky které jsou vždy umístěny bez ohledu na integrity výpočet.
     * Typicky strukturálně kritické bloky (dveře, connection point záplavy).
     */
    private final Set<Block> exemptBlocks;

    /**
     * Bloky které jsou preferovány pro vynechání při integrity &lt; 1.0.
     * Engine nejprve náhodně vynechá bloky z tohoto setu před vynecháváním
     * ostatních bloků.
     */
    private final Set<Block> preferredRemovalBlocks;

    // ---- Constructor (private) -----------------------------------------------

    private StructureIntegrityProfile(Builder b) {
        this.baseIntegrity         = b.baseIntegrity;
        this.integrityVariance     = b.integrityVariance;
        this.exemptBlocks          = Collections.unmodifiableSet(new HashSet<>(b.exemptBlocks));
        this.preferredRemovalBlocks = Collections.unmodifiableSet(new HashSet<>(b.preferredRemovalBlocks));
    }

    // ---- Factory -------------------------------------------------------------

    /** @return nový builder; nikdy {@code null} */
    public static Builder builder() {
        return new Builder();
    }

    // ---- Accessors -----------------------------------------------------------

    /** @return base integrity v rozsahu [0.0, 1.0] */
    public float getBaseIntegrity()          { return baseIntegrity; }

    /** @return maximální variance per instance */
    public float getIntegrityVariance()      { return integrityVariance; }

    /** @return immutable set bloků vždy umístěných */
    public Set<Block> getExemptBlocks()      { return exemptBlocks; }

    /** @return immutable set bloků preferovaných pro vynechání */
    public Set<Block> getPreferredRemovalBlocks() { return preferredRemovalBlocks; }

    // =========================================================================
    // Builder
    // =========================================================================

    public static final class Builder {

        private float baseIntegrity     = 1.0f;
        private float integrityVariance = 0.0f;
        private final Set<Block> exemptBlocks           = new HashSet<>();
        private final Set<Block> preferredRemovalBlocks = new HashSet<>();

        private Builder() {}

        /**
         * @param value base integrity; musí být v [0.0, 1.0]
         * @return {@code this}
         * @throws IllegalArgumentException pokud je mimo rozsah
         */
        public Builder baseIntegrity(float value) {
            if (value < 0f || value > 1f)
                throw new IllegalArgumentException("baseIntegrity must be in [0.0, 1.0], got: " + value);
            this.baseIntegrity = value;
            return this;
        }

        /**
         * @param value variance; musí být &ge; 0
         * @return {@code this}
         */
        public Builder integrityVariance(float value) {
            if (value < 0f)
                throw new IllegalArgumentException("integrityVariance must be >= 0, got: " + value);
            this.integrityVariance = value;
            return this;
        }

        /** @return {@code this} */
        public Builder exemptBlocks(Set<Block> blocks) {
            this.exemptBlocks.addAll(Objects.requireNonNull(blocks));
            return this;
        }

        /** @return {@code this} */
        public Builder preferredRemovalBlocks(Set<Block> blocks) {
            this.preferredRemovalBlocks.addAll(Objects.requireNonNull(blocks));
            return this;
        }

        /** @return nová immutable instance */
        public StructureIntegrityProfile build() {
            return new StructureIntegrityProfile(this);
        }
    }
}