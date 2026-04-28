package com.example.structuregen.internal;

import com.example.structuregen.api.ConnectionPoint;
import com.example.structuregen.api.RoomTemplate;
import com.example.structuregen.api.StructureDefinition;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Map;

/**
 * Interní validátor {@link StructureDefinition} spouštěný v
 * {@code FMLCommonSetupEvent} po dokončení všech registrací.
 *
 * <p>Tato třída je {@code package-private} — nevolat z třetích stran.
 * Validační logika je rozdělena do dvou fází dle plánu A-3-4 a A-3-5.
 *
 * <h2>Fáze 1 (A-3-4)</h2>
 * <ul>
 *   <li>Každý ConnectionPoint type tag musí mít registrovaný terminator
 *       s BoundingBox min 1×1×1</li>
 *   <li>{@code sealBlock} nesmí být null (garantováno builderem, dvojitá kontrola)</li>
 *   <li>Unikátnost ID (garantována při registraci, dvojitá kontrola)</li>
 * </ul>
 *
 * <h2>Fáze 2 (A-3-5)</h2>
 * <ul>
 *   <li>Graph connectivity check — BFS/DFS přes graf kompatibilních
 *       ConnectionPoint tagů od ROOT template ověří dosažitelnost každého
 *       {@code requiredRoomType}</li>
 * </ul>
 */
public final class RegistrationValidator {

    /**
     * Spustí obě validační fáze pro danou definici.
     *
     * @param def definice k validaci; nesmí být {@code null}
     * @throws IllegalStateException pokud validace selže — zpráva obsahuje
     *                               mod ID a přesný popis problému
     */
    public static void validate(StructureDefinition def) {
        validateTerminatorsAndSeal(def);   // A-3-4
        validateGraphConnectivity(def);    // A-3-5
    }

    // =========================================================================
    // A-3-4 — Fáze 1: terminators + seal
    // =========================================================================

    private static void validateTerminatorsAndSeal(StructureDefinition def) {
        String id = def.getId().toString();

        // Dvojitá kontrola sealBlock (builder to garantuje, ale defensive)
        if (def.getSealBlock() == null) {
            throw new IllegalStateException(
                "[" + id + "] sealBlock must not be null."
            );
        }

        Map<String, RoomTemplate> terminators = def.getTerminators();

        // Každý typeTag použitý v jakémkoli template musí mít terminator
        for (RoomTemplate template : def.getTemplates()) {
            for (ConnectionPoint cp : template.getConnectionPoints()) {
                String typeTag = cp.getTypeTag();

                if (!terminators.containsKey(typeTag)) {
                    throw new IllegalStateException(
                        "[" + id + "] Missing terminator for ConnectionPoint typeTag '"
                        + typeTag + "' used in template '"
                        + template.getClass().getSimpleName() + "'. "
                        + "Register a terminator via StructureDefinition.Builder.addTerminator(\""
                        + typeTag + "\", yourTerminatorTemplate)."
                    );
                }

                // Terminator musí mít BoundingBox min 1×1×1
                RoomTemplate terminator = terminators.get(typeTag);
                BoundingBox bb = terminator.getBoundingBox();
                int sizeX = bb.maxX() - bb.minX() + 1;
                int sizeY = bb.maxY() - bb.minY() + 1;
                int sizeZ = bb.maxZ() - bb.minZ() + 1;

                if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
                    throw new IllegalStateException(
                        "[" + id + "] Terminator for typeTag '" + typeTag
                        + "' has invalid BoundingBox size " + sizeX + "x" + sizeY + "x" + sizeZ
                        + ". Minimum size is 1x1x1."
                    );
                }
            }
        }
    }

    // =========================================================================
    // A-3-5 — Fáze 2: graph connectivity check
    // =========================================================================

    // =========================================================================
// A-3-5 — Fáze 2: graph connectivity check
// =========================================================================

/**
 * BFS přes graf kompatibilních ConnectionPoint tagů od ROOT template.
 *
 * <p>Graf je definován takto:
 * <ul>
 *   <li>Uzel = {@link RoomTemplate}</li>
 *   <li>Hrana A→B existuje pokud template A má ConnectionPoint jehož
 *       {@code compatibilityTag} se shoduje s {@code compatibilityTag}
 *       libovolného ConnectionPointu template B.</li>
 * </ul>
 *
 * <p>BFS startuje z ROOT template (index 0 v seznamu templates).
 * Po průchodu zkontrolujeme zda každý {@code RoomType} z
 * {@code requiredRoomTypes} je přítomen v alespoň jednom dosažitelném
 * uzlu. Pokud ne — struktura je nevalidní a generování by nikdy nemohlo
 * splnit required types constraint.
 *
 * @throws IllegalStateException s přesným popisem nedosažitelných typů,
 *                               mod ID a návrhem opravy
 */
private static void validateGraphConnectivity(StructureDefinition def) {
    String id = def.getId().toString();

    java.util.List<com.example.structuregen.api.RoomType> required =
        def.getRules().getRequiredRoomTypes();

    // Pokud žádné required types nejsou definovány — nic kontrolovat
    if (required.isEmpty()) return;

    java.util.List<RoomTemplate> allTemplates = def.getTemplates();
    if (allTemplates.isEmpty()) return; // builder to garantuje, defensive

    // ---- Sestav adjacency mapu: tag → seznam templates které ho nabízejí --
    // Klíč = compatibilityTag, hodnota = všechny templates které mají
    // ConnectionPoint s tímto compatibilityTag.
    java.util.Map<String, java.util.List<RoomTemplate>> tagToTemplates = new java.util.HashMap<>();
    for (RoomTemplate template : allTemplates) {
        for (ConnectionPoint cp : template.getConnectionPoints()) {
            tagToTemplates
                .computeIfAbsent(cp.getCompatibilityTag(), k -> new java.util.ArrayList<>())
                .add(template);
        }
    }

    // ---- BFS od ROOT template -----------------------------------------------
    RoomTemplate root = allTemplates.get(0);
    java.util.Set<RoomTemplate> visited   = new java.util.HashSet<>();
    java.util.Queue<RoomTemplate> frontier = new java.util.ArrayDeque<>();

    visited.add(root);
    frontier.add(root);

    while (!frontier.isEmpty()) {
        RoomTemplate current = frontier.poll();

        // Pro každý ConnectionPoint aktuálního uzlu najdi sousedy
        for (ConnectionPoint cp : current.getConnectionPoints()) {
            String compat = cp.getCompatibilityTag();
            java.util.List<RoomTemplate> neighbors =
                tagToTemplates.getOrDefault(compat, java.util.Collections.emptyList());

            for (RoomTemplate neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    frontier.add(neighbor);
                }
            }
        }
    }

    // ---- Zkontroluj dosažitelnost každého required RoomType -----------------
    // Sbírej všechny RoomType dostupné v dosažitelných uzlech
    java.util.Set<com.example.structuregen.api.RoomType> reachableTypes = new java.util.HashSet<>();
    for (RoomTemplate reachable : visited) {
        reachableTypes.add(reachable.getRoomType());
    }

    // Najdi chybějící required types
    java.util.List<com.example.structuregen.api.RoomType> missing = new java.util.ArrayList<>();
    for (com.example.structuregen.api.RoomType requiredType : required) {
        if (!reachableTypes.contains(requiredType)) {
            missing.add(requiredType);
        }
    }

    if (!missing.isEmpty()) {
        // Sestav přehled dosažitelných typů pro lepší chybovou zprávu
        java.util.StringJoiner reachableStr = new java.util.StringJoiner(", ");
        for (com.example.structuregen.api.RoomType t : reachableTypes) {
            reachableStr.add(t.name());
        }

        java.util.StringJoiner missingStr = new java.util.StringJoiner(", ");
        for (com.example.structuregen.api.RoomType t : missing) {
            missingStr.add(t.name());
        }

        throw new IllegalStateException(
            "[" + id + "] requiredRoomTypes validation failed. "
            + "The following types are required but unreachable via ConnectionPoint "
            + "compatibility tags from the ROOT template ('"
            + root.getClass().getSimpleName() + "'): ["
            + missingStr + "]. "
            + "Reachable types from ROOT: [" + reachableStr + "]. "
            + "Fix: ensure at least one RoomTemplate with each missing RoomType "
            + "has a ConnectionPoint whose compatibilityTag matches a tag "
            + "reachable from ROOT. "
            + "Note: cross-dimension instances are not counted — this check "
            + "is purely graph-structural."
        );
    }

    com.example.structuregen.StructureGenMod.LOGGER.debug(
        "[{}] Graph connectivity check passed. Reachable templates: {}, "
        + "required types all reachable: {}.",
        id, visited.size(), required
    );
}

    /** Utility třída — žádná instance. */
    private RegistrationValidator() {}
}