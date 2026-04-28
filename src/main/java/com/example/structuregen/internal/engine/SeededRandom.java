package com.example.structuregen.internal.engine;

import java.util.List;
import java.util.Random;

/**
 * Wrapper kolem {@link Random} s deterministickým seedem pro assembly engine.
 *
 * <p>Seed je kombinací world seed + chunk pozice:
 * {@code worldSeed ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L)}
 *
 * <p>Tato kombinace je totožná s tou používanou vanilla Minecraft pro
 * feature placement — zaručuje konzistenci s ostatními worldgen operacemi.
 */
final class SeededRandom {

    private final Random random;

    /**
     * Vytvoří nový SeededRandom s deterministickým seedem.
     *
     * @param worldSeed seed světa
     * @param chunkX    X souřadnice chunku
     * @param chunkZ    Z souřadnice chunku
     */
    SeededRandom(long worldSeed, int chunkX, int chunkZ) {
        long seed = worldSeed
            ^ (chunkX * 341873128712L)
            ^ (chunkZ * 132897987541L);
        this.random = new Random(seed);
    }

    /**
     * Vytvoří SeededRandom s explicitním seedem — pro on-demand generování.
     *
     * @param seed explicitní seed
     */
    SeededRandom(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Vybere náhodný prvek ze seznamu s ohledem na váhy.
     * Každý prvek má váhu danou {@code weightFunction}.
     *
     * @param candidates   seznam kandidátů; nesmí být prázdný
     * @param weightFunction funkce vracející váhu pro každý prvek
     * @param <T>          typ prvku
     * @return vybraný prvek; nikdy {@code null}
     */
    <T> T weightedRandom(List<T> candidates,
                          java.util.function.ToDoubleFunction<T> weightFunction) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        if (candidates.size() == 1) return candidates.get(0);

        double totalWeight = 0;
        for (T c : candidates) {
            totalWeight += Math.max(0, weightFunction.applyAsDouble(c));
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (T c : candidates) {
            cumulative += Math.max(0, weightFunction.applyAsDouble(c));
            if (roll <= cumulative) return c;
        }

        // Fallback — float precision edge case
        return candidates.get(candidates.size() - 1);
    }

    /** @return náhodný float v [0, 1) */
    float nextFloat() {
        return random.nextFloat();
    }

    /** @return náhodný int v [0, bound) */
    int nextInt(int bound) {
        return random.nextInt(bound);
    }

    /** @return náhodný boolean */
    boolean nextBoolean() {
        return random.nextBoolean();
    }
}