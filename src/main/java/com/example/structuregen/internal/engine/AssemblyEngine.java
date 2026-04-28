package com.example.structuregen.internal.engine;

import com.example.structuregen.StructureGenMod;
import com.example.structuregen.api.ConnectionPoint;
import com.example.structuregen.api.GridMutationRecord;
import com.example.structuregen.api.RoomTemplate;
import com.example.structuregen.api.RoomType;
import com.example.structuregen.api.StructureDefinition;
import com.example.structuregen.api.StructureInstance;
import com.example.structuregen.api.StructureIntegrityProfile;
import com.example.structuregen.api.event.RoomPlacedEvent;
import com.example.structuregen.internal.world.StructureWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Jádro procedurálního assembly algoritmu.
 *
 * <p>Vstup: {@link StructureDefinition}, origin {@link BlockPos}, seed.
 * Výstup: {@link Optional}&lt;{@link StructureInstance}&gt; —
 * prázdný pokud assembly selhal (timeout, requiredRoomTypes nesplněny,
 * příliš mnoho backtrack selhání).
 *
 * <h2>Algoritmus (A-5-3)</h2>
 * <ol>
 *   <li>ROOT template umístěn na origin.</li>
 *   <li>ConnectionPointy ROOT přidány do frontier fronty.</li>
 *   <li>Iterativní smyčka: vybere CP z fronty, hledá kompatibilní kandidáty,
 *       zkusí umístit (s backtrackingem). Při úspěchu: nové CPs do fronty.</li>
 *   <li>Po dosažení maxRoomCount: všechny zbývající CPs dostanou terminator/seal.</li>
 *   <li>Post-assembly validace requiredRoomTypes.</li>
 * </ol>
 *
 * <h2>Thread safety</h2>
 * <p>Jedna instance {@code AssemblyEngine} je použita pro jeden assembly pokus
 * a poté zahozena — není thread-safe pro sdílené použití.
 */
public final class AssemblyEngine {

    /** Maximální hloubka backtracking stacku. */
    private static final int MAX_BACKTRACK_DEPTH = 8;

    /**
     * Boost multiplikátor pro required room types při weight výběru.
     * Viz A-5-6.
     */
    private static final double BOOST_MULTIPLIER = 5.0;

    // ---- Vstupní parametry --------------------------------------------------

    private final StructureDefinition definition;
    private final BlockPos origin;
    private final long seed;
    private final ResourceKey<Level> dimension;
    private final SeededRandom random;

    // ---- Assembly state -----------------------------------------------------

    private final ChunkAlignedGrid grid = new ChunkAlignedGrid();
    private final BlockPlacementQueue placementQueue = new BlockPlacementQueue();
    private final Map<RoomTemplate, BlockPos> roomPositions = new HashMap<>();
    private final Map<String, Object> aggregatedMetadata = new HashMap<>();
    private int placedRoomCount = 0;

    // ---- Backtracking stack -------------------------------------------------

    private final Deque<BacktrackFrame> backtrackStack = new ArrayDeque<>();

    // ---- Timing -------------------------------------------------------------

    private long assemblyStartTime;

    // =========================================================================
    // Konstruktor
    // =========================================================================

    /**
     * @param definition  definice struktury; nesmí být {@code null}
     * @param origin      absolutní origin pozice; nesmí být {@code null}
     * @param seed        deterministický seed pro tento assembly
     * @param dimension   dimenze ve které generujeme
     */
    public AssemblyEngine(StructureDefinition definition,
                           BlockPos origin,
                           long seed,
                           ResourceKey<Level> dimension) {
        this.definition = definition;
        this.origin     = origin;
        this.seed       = seed;
        this.dimension  = dimension;
        this.random     = new SeededRandom(seed);
    }

    // =========================================================================
    // A-5-3 — Hlavní assembly metoda
    // =========================================================================

    /**
     * Spustí assembly algoritmus.
     *
     * @param server Minecraft server pro fyzické umístění bloků
     * @return {@link Optional} s výslednou instancí, nebo prázdný při selhání
     */
    public Optional<AssemblyResult> assemble(MinecraftServer server) {
        assemblyStartTime = System.currentTimeMillis();
        long timeLimit = definition.getRules().getMaxAssemblyTimeMs();

        try {
            // ---- Krok 1: ROOT template na origin ----------------------------
            RoomTemplate root = definition.getTemplates().get(0);
            if (!placeRoom(root, origin, Rotation.NONE, Mirror.NONE)) {
                StructureGenMod.LOGGER.debug(
                    "AssemblyEngine: ROOT template '{}' failed to place at {} — aborting.",
                    root.getClass().getSimpleName(), origin
                );
                return Optional.empty();
            }

            // ---- Krok 2: Frontier inicializace ------------------------------
            Deque<ConnectionPoint> frontier = new ArrayDeque<>(root.getConnectionPoints());

            // ---- Krok 3: Hlavní iterativní smyčka ---------------------------
            while (!frontier.isEmpty()) {

                // A-5-13: timeout check
                long elapsed = System.currentTimeMillis() - assemblyStartTime;
                if (elapsed > timeLimit) {
                    throw new AssemblyTimeoutException(elapsed, timeLimit);
                }

                // A-5-10: maxRoomCount enforcement
                if (placedRoomCount >= definition.getRules().getMaxRoomCount()) {
                    sealRemainingFrontier(frontier);
                    break;
                }

                ConnectionPoint cp = frontier.pollFirst();
                boolean resolved = tryResolveConnectionPoint(cp, frontier);

                if (!resolved) {
                    // Seal jako ultimate fallback
                    SealOperation.apply(
                        placementQueue, cp, origin,
                        definition.getSealBlock(), dimension,
                        definition.getId()
                    );
                }
            }

            // ---- Krok 4: Post-assembly validace (A-5-12) --------------------
            if (!validateRequiredRoomTypes()) {
                StructureGenMod.LOGGER.debug(
                    "AssemblyEngine: requiredRoomTypes validation failed for '{}'.",
                    definition.getId()
                );
                return Optional.empty();
            }

            // ---- Krok 5: Fyzické umístění bloků (flush) ---------------------
            List<BlockPos> placedPositions = placementQueue.flush(server);

            // ---- Krok 6: Sestavení StructureInstance ------------------------
            StructureInstance instance = new StructureInstance(
                definition.getId(),
                seed,
                Map.copyOf(roomPositions),
                Map.copyOf(aggregatedMetadata),
                new ChunkPos(origin)
            );

            return Optional.of(new AssemblyResult(instance, placedPositions));

        } catch (AssemblyTimeoutException e) {
            StructureGenMod.LOGGER.debug(
                "AssemblyEngine: {} for structure '{}'.", e.getMessage(), definition.getId()
            );
            placementQueue.clear();
            return Optional.empty();

        } catch (Throwable t) {
            StructureGenMod.LOGGER.error(
                "AssemblyEngine: unexpected error assembling '{}': {}",
                definition.getId(), t.getMessage(), t
            );
            placementQueue.clear();
            return Optional.empty();
        }
    }

    // =========================================================================
    // A-5-4 — ConnectionPoint matching + A-5-6 weight boost + A-5-7 backtracking
    // =========================================================================

    /**
     * Pokusí se vyřešit (napojit místnost na) daný ConnectionPoint.
     * Zahrnuje výběr kandidátů, weight boost, a backtracking.
     *
     * @param cp       connection point k vyřešení
     * @param frontier fronta do které přidáme nové CPs při úspěchu
     * @return {@code true} pokud byl CP úspěšně vyřešen (místnost nebo terminator umístěn)
     */
    private boolean tryResolveConnectionPoint(ConnectionPoint cp,
                                               Deque<ConnectionPoint> frontier) {
        // A-5-4: najdi kompatibilní kandidáty
        List<RoomTemplate> candidates = findCompatibleTemplates(cp);

        if (candidates.isEmpty()) {
            // Zkus terminator
            return tryPlaceTerminator(cp);
        }

        // A-5-6: weight boost pro required room types
        double progressRatio = (double) placedRoomCount
            / Math.max(1, definition.getRules().getMaxRoomCount());

        // A-5-7: backtracking — zkus kandidáty jeden po druhém
        List<RoomTemplate> remaining = new ArrayList<>(candidates);
        int backtrackDepth = 0;

        while (!remaining.isEmpty() && backtrackDepth <= MAX_BACKTRACK_DEPTH) {
            RoomTemplate chosen = random.weightedRandom(remaining,
                t -> boostedWeight(t, progressRatio));

            remaining.remove(chosen);

            BlockPos candidatePos = calculatePlacementPos(cp, chosen);
            if (candidatePos == null) continue;

            GridMutationRecord record = null;
            if (!grid.checkCollision(chosen.getBoundingBox().moved(
                    candidatePos.getX(), candidatePos.getY(), candidatePos.getZ()))) {

                record = grid.occupyCells(
                    chosen.getBoundingBox().moved(
                        candidatePos.getX(), candidatePos.getY(), candidatePos.getZ()),
                    definition.getId()
                );

                if (placeRoom(chosen, candidatePos, Rotation.NONE, Mirror.NONE)) {
                    // Úspěch — přidej nové CPs do fronty
                    for (ConnectionPoint newCp : chosen.getConnectionPoints()) {
                        frontier.addLast(newCp);
                    }
                    return true;
                } else {
                    // placeRoom selhal — rollback grid
                    grid.rollback(record);
                }
            }

            backtrackDepth++;
        }

        // Backtracking vyčerpán — zkus terminator
        return tryPlaceTerminator(cp);
    }

    // =========================================================================
    // A-5-4 — findCompatibleTemplates
    // =========================================================================

    /**
     * Najde templates kompatibilní s daným ConnectionPoint.
     *
     * <p>Kompatibilita = template má alespoň jeden ConnectionPoint jehož
     * {@code compatibilityTag} se shoduje s {@code cp.compatibilityTag}.
     */
    private List<RoomTemplate> findCompatibleTemplates(ConnectionPoint cp) {
        List<RoomTemplate> result = new ArrayList<>();
        String requiredTag = cp.getCompatibilityTag();

        for (RoomTemplate template : definition.getTemplates()) {
            for (ConnectionPoint tCp : template.getConnectionPoints()) {
                if (tCp.getCompatibilityTag().equals(requiredTag)) {
                    result.add(template);
                    break;
                }
            }
        }

        return result;
    }

    // =========================================================================
    // A-5-6 — Dynamický weight boost
    // =========================================================================

    /**
     * Vypočítá boosted váhu pro template s ohledem na required room types.
     *
     * @param template      kandidátní template
     * @param progressRatio aktuální progress (0.0–1.0)
     * @return boosted váha
     */
    private double boostedWeight(RoomTemplate template, double progressRatio) {
        double weight = template.getWeight();
        List<RoomType> required = definition.getRules().getRequiredRoomTypes();

        if (!required.isEmpty() && required.contains(template.getRoomType())) {
            // Zkontroluj zda tento typ již byl splněn
            boolean alreadySatisfied = roomPositions.keySet().stream()
                .anyMatch(t -> t.getRoomType() == template.getRoomType());

            if (!alreadySatisfied) {
                weight *= (1.0 + progressRatio * BOOST_MULTIPLIER);
            }
        }

        return weight;
    }

    // =========================================================================
    // A-5-8 — Terminator systém
    // =========================================================================

    /**
     * Pokusí se umístit terminator pro daný ConnectionPoint type tag.
     *
     * @return {@code true} pokud byl terminator úspěšně umístěn
     */
    private boolean tryPlaceTerminator(ConnectionPoint cp) {
        RoomTemplate terminator = definition.getTerminators().get(cp.getTypeTag());
        if (terminator == null) return false;

        BlockPos terminatorPos = calculatePlacementPos(cp, terminator);
        if (terminatorPos == null) return false;

        var movedBounds = terminator.getBoundingBox().moved(
            terminatorPos.getX(), terminatorPos.getY(), terminatorPos.getZ()
        );

        if (grid.checkCollision(movedBounds)) return false;

        GridMutationRecord record = grid.occupyCells(movedBounds, definition.getId());

        if (placeRoom(terminator, terminatorPos, Rotation.NONE, Mirror.NONE)) {
            return true;
        } else {
            grid.rollback(record);
            return false;
        }
    }

    // =========================================================================
    // A-5-10 — maxRoomCount enforcement
    // =========================================================================

    /**
     * Zapečetí všechny zbývající ConnectionPointy ve frontě —
     * voláno při dosažení maxRoomCount.
     */
    private void sealRemainingFrontier(Deque<ConnectionPoint> frontier) {
        StructureGenMod.LOGGER.debug(
            "AssemblyEngine: maxRoomCount ({}) reached for '{}', "
            + "sealing {} remaining connection points.",
            definition.getRules().getMaxRoomCount(),
            definition.getId(),
            frontier.size()
        );

        while (!frontier.isEmpty()) {
            ConnectionPoint cp = frontier.pollFirst();
            boolean terminated = tryPlaceTerminator(cp);
            if (!terminated) {
                SealOperation.apply(
                    placementQueue, cp, origin,
                    definition.getSealBlock(), dimension,
                    definition.getId()
                );
            }
        }
    }

    // =========================================================================
    // A-5-11 — StructureIntegrityProfile enforcement (integrováno do placeRoom)
    // =========================================================================

    /**
     * Umístí místnost — zavolá {@code placeWithIntegrity()} s integrity kontrolou.
     * Integrity enforcement je v RoomTemplate.placeWithIntegrity() (template method).
     *
     * <p>Integrity variance je aplikována na základě seed + instance indexu.
     *
     * @return {@code true} pokud placement proběhl bez výjimky
     */
    private boolean placeRoom(RoomTemplate template, BlockPos pos,
                               Rotation rotation, Mirror mirror) {
        try {
            // Integrity profil — per-template override nebo globální
            StructureIntegrityProfile profile = template.getIntegrityProfile();
            if (profile == StructureIntegrityProfile.PERFECT) {
                profile = definition.getRules().getStructureIntegrity();
            }

            // effectiveIntegrity výpočet (A-5-11)
            float base = profile.getBaseIntegrity();
            float variance = profile.getIntegrityVariance();
            float effectiveIntegrity = base + (random.nextFloat() * 2 - 1) * variance;
            effectiveIntegrity = Math.max(0f, Math.min(1f, effectiveIntegrity));

            // Předáme integrity kontext přes thread-local nebo přímým voláním
            // V 1.20.1 nemáme přístup k thread-local kontextu přímo v base třídě —
            // integrity enforcement je v placeWithIntegrity() přes IntegrityContext.
            IntegrityContext.set(effectiveIntegrity, profile);
            template.placeWithIntegrity(null, pos, rotation, mirror);
            // Poznámka: ServerLevel je null zde protože bloky jdou přes
            // BlockPlacementQueue — template implementace musí enqueue přes
            // PlacementContext.getQueue() (viz níže).

            roomPositions.put(template, pos);
            aggregatedMetadata.putAll(template.getMetadata());
            placedRoomCount++;

            // A-5-15: RoomPlacedEvent firing
            StructureInstance partialSnapshot = buildPartialInstance();
            MinecraftForge.EVENT_BUS.post(new RoomPlacedEvent(template, pos, partialSnapshot));

            return true;

        } catch (Throwable t) {
            StructureGenMod.LOGGER.error(
                "AssemblyEngine.placeRoom(): failed for template '{}' at {}: {}",
                template.getClass().getSimpleName(), pos, t.getMessage()
            );
            return false;
        } finally {
            IntegrityContext.clear();
        }
    }

    // =========================================================================
    // A-5-12 — requiredRoomTypes post-assembly validace
    // =========================================================================

    /**
     * Ověří přítomnost všech required room types ve výsledném assembly.
     *
     * @return {@code true} pokud jsou všechny required typy přítomny
     */
    private boolean validateRequiredRoomTypes() {
        List<RoomType> required = definition.getRules().getRequiredRoomTypes();
        if (required.isEmpty()) return true;

        Set<RoomType> placed = new HashSet<>();
        for (RoomTemplate t : roomPositions.keySet()) {
            placed.add(t.getRoomType());
        }

        List<RoomType> missing = new ArrayList<>();
        for (RoomType rt : required) {
            if (!placed.contains(rt)) missing.add(rt);
        }

        if (!missing.isEmpty()) {
            StructureGenMod.LOGGER.warn(
                "AssemblyEngine: requiredRoomTypes validation failed for '{}'. "
                + "Missing types: {}. Placed rooms: {}. Will retry if attempts remain.",
                definition.getId(), missing, placedRoomCount
            );
            return false;
        }

        return true;
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Vypočítá absolutní pozici pro umístění template napojením na ConnectionPoint.
     * Vrátí {@code null} pokud pozici nelze vypočítat.
     */
    private BlockPos calculatePlacementPos(ConnectionPoint cp, RoomTemplate template) {
        // Jednoduchý výpočet: posun origin o relativeOffset CP + offset pro napojení
        BlockPos cpAbsPos = origin.offset(cp.getRelativeOffset());
        // Napojení: sousední místnost začíná 1 blok za CP ve směru CP.direction
        return cpAbsPos.relative(cp.getDirection());
    }

    /** Sestaví partial StructureInstance snapshot pro RoomPlacedEvent. */
    private StructureInstance buildPartialInstance() {
        return new StructureInstance(
            definition.getId(),
            seed,
            Map.copyOf(roomPositions),
            Map.copyOf(aggregatedMetadata),
            new ChunkPos(origin)
        );
    }

    /**
     * Vrátí BlockPlacementQueue pro použití z RoomTemplate.doPlace().
     * Přistupuje přes {@link PlacementContext}.
     */
    BlockPlacementQueue getPlacementQueue() {
        return placementQueue;
    }

    // =========================================================================
    // Vnořené třídy
    // =========================================================================

    /**
     * Frame pro backtracking stack.
     */
    private record BacktrackFrame(
        ConnectionPoint connectionPoint,
        List<RoomTemplate> remainingCandidates,
        GridMutationRecord mutationRecord
    ) {}

    /**
     * Výsledek úspěšného assembly.
     */
    public record AssemblyResult(
        StructureInstance instance,
        List<BlockPos> placedPositions
    ) {}
}