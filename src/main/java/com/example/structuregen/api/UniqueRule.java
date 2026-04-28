package com.example.structuregen.api;

/**
 * Definuje globální pravidla unikátnosti pro jednu {@link StructureDefinition}.
 *
 * <p>Všechna omezení jsou vynucována atomicky přes {@code StructureWorldState}
 * write lock (viz A-4-7). Cross-dimension instance se nepočítají — každá
 * dimenze má vlastní world state.
 *
 * <h2>Static factories</h2>
 * <ul>
 *   <li>{@link #once()} — přesně jedna instance per dimenze per world</li>
 *   <li>{@link #unlimited()} — bez omezení počtu</li>
 *   <li>{@link #atMost(int)} — maximálně N instancí</li>
 * </ul>
 */
public final class UniqueRule {

    // ---- Fields --------------------------------------------------------------

    /**
     * Maximální počet instancí ve stejné dimenzi.
     * {@code 0} = neomezeno.
     *
     * <p>Check probíhá atomicky spolu s zápisem nové instance pod write lockem
     * v {@code StructureWorldState.tryRegisterInstance()} (viz A-4-7).
     */
    private final int maxInstances;

    /**
     * Počet herních ticků které musí uplynout od poslední vygenerované
     * instance před tím než smí být vygenerována další.
     *
     * <p>{@code 0} = žádné cooldown omezení.
     *
     * <p>Výchozí hodnota: {@code 0}.
     */
    private final int cooldownTicks;

    /**
     * Minimální vzdálenost v chuncích mezi dvěma instancemi stejného
     * {@code structureId} ve stejné dimenzi.
     *
     * <p>Překrývá se s {@link GenerationRules#getMinDistanceBetweenInstances()} —
     * {@code UniqueRule} kontrola probíhá uvnitř write locku jako součást
     * atomické check+write sekvence, zatímco {@code GenerationRules} kontrola
     * probíhá dříve v pipeline bez zámku.
     *
     * <p>{@code 0} = žádné omezení vzdálenosti.
     */
    private final int minChunkDistanceBetweenInstances;

    // ---- Constructor (private) -----------------------------------------------

    private UniqueRule(int maxInstances, int cooldownTicks, int minChunkDistance) {
        if (maxInstances < 0)
            throw new IllegalArgumentException("maxInstances must be >= 0 (0 = unlimited)");
        if (cooldownTicks < 0)
            throw new IllegalArgumentException("cooldownTicks must be >= 0");
        if (minChunkDistance < 0)
            throw new IllegalArgumentException("minChunkDistanceBetweenInstances must be >= 0");

        this.maxInstances                    = maxInstances;
        this.cooldownTicks                   = cooldownTicks;
        this.minChunkDistanceBetweenInstances = minChunkDistance;
    }

    // ---- Static factories ----------------------------------------------------

    /**
     * Přesně jedna instance per dimenze per world.
     * Nejčastěji používáno pro unikátní SCP containment struktury.
     *
     * @return {@code UniqueRule} s {@code maxInstances = 1}
     */
    public static UniqueRule once() {
        return new UniqueRule(1, 0, 0);
    }

    /**
     * Bez omezení počtu instancí.
     *
     * @return {@code UniqueRule} s {@code maxInstances = 0}
     */
    public static UniqueRule unlimited() {
        return new UniqueRule(0, 0, 0);
    }

    /**
     * Maximálně {@code n} instancí per dimenze.
     *
     * @param n maximální počet; musí být &ge; 1
     * @return {@code UniqueRule} s {@code maxInstances = n}
     */
    public static UniqueRule atMost(int n) {
        if (n < 1) throw new IllegalArgumentException("atMost(n): n must be >= 1");
        return new UniqueRule(n, 0, 0);
    }

    /**
     * Builder-style factory pro pokročilé konfiguraci cooldownu a vzdálenosti.
     *
     * @param maxInstances              max počet instancí; {@code 0} = unlimited
     * @param cooldownTicks             cooldown mezi instancemi v tickách
     * @param minChunkDistanceBetween   min chunk vzdálenost mezi instancemi
     * @return nová {@code UniqueRule} instance
     */
    public static UniqueRule of(int maxInstances, int cooldownTicks, int minChunkDistanceBetween) {
        return new UniqueRule(maxInstances, cooldownTicks, minChunkDistanceBetween);
    }

    // ---- Accessors -----------------------------------------------------------

    /**
     * @return maximální počet instancí; {@code 0} = neomezeno
     */
    public int getMaxInstances() { return maxInstances; }

    /**
     * @return cooldown v tickách; {@code 0} = žádný cooldown
     */
    public int getCooldownTicks() { return cooldownTicks; }

    /**
     * @return minimální chunk vzdálenost mezi instancemi; {@code 0} = žádná
     */
    public int getMinChunkDistanceBetweenInstances() { return minChunkDistanceBetweenInstances; }

    /**
     * @return {@code true} pokud je počet instancí omezen ({@code maxInstances > 0})
     */
    public boolean hasInstanceLimit() { return maxInstances > 0; }

    @Override
    public String toString() {
        return "UniqueRule{max=" + maxInstances
            + ", cooldown=" + cooldownTicks
            + ", minDist=" + minChunkDistanceBetweenInstances + "}";
    }
}