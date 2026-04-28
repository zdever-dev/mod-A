package com.example.structuregen.internal.engine;

/**
 * Vyhazována assembly enginem pokud assembly překročí
 * {@code GenerationRules.maxAssemblyTimeMs()}.
 *
 * <p>Zachytávána v {@code RoomTemplate.placeWithIntegrity()} finally blokem —
 * partial state je zahozen a engine pokračuje bez pádu server threadu.
 */
final class AssemblyTimeoutException extends RuntimeException {

    AssemblyTimeoutException(long elapsedMs, long limitMs) {
        super("Assembly timed out after " + elapsedMs + "ms (limit: " + limitMs + "ms)");
    }
}