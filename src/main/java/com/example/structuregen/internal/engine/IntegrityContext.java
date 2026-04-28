package com.example.structuregen.internal.engine;

import com.example.structuregen.api.StructureIntegrityProfile;

/**
 * Thread-local kontext předávající integrity parametry do
 * {@link com.example.structuregen.api.RoomTemplate#placeWithIntegrity}.
 *
 * <p>Nastavován assembly enginem před voláním {@code placeWithIntegrity()}
 * a čistěn v finally bloku.
 */
final class IntegrityContext {

    private static final ThreadLocal<Float> EFFECTIVE_INTEGRITY = new ThreadLocal<>();
    private static final ThreadLocal<StructureIntegrityProfile> PROFILE = new ThreadLocal<>();

    static void set(float effectiveIntegrity, StructureIntegrityProfile profile) {
        EFFECTIVE_INTEGRITY.set(effectiveIntegrity);
        PROFILE.set(profile);
    }

    static void clear() {
        EFFECTIVE_INTEGRITY.remove();
        PROFILE.remove();
    }

    /**
     * Vrátí effective integrity pro aktuální thread.
     * {@code 1.0} pokud není nastaven (= perfect integrity).
     */
    public static float getEffectiveIntegrity() {
        Float val = EFFECTIVE_INTEGRITY.get();
        return val != null ? val : 1.0f;
    }

    /**
     * Vrátí aktuální integrity profil.
     * {@link StructureIntegrityProfile#PERFECT} pokud není nastaven.
     */
    public static StructureIntegrityProfile getProfile() {
        StructureIntegrityProfile p = PROFILE.get();
        return p != null ? p : StructureIntegrityProfile.PERFECT;
    }

    private IntegrityContext() {}
}