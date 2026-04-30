package com.example.structuregen;

import com.example.structuregen.api.SpawnPopulator;
import com.example.structuregen.api.StructureDefinition;
import com.example.structuregen.api.StructureGeneratorAPI;
import com.example.structuregen.api.StructureInstance;
import com.example.structuregen.api.TerrainAdaptationMode;
import com.example.structuregen.api.TerrainAdapterRegistry;
import com.example.structuregen.internal.RegistrationValidator;
import com.example.structuregen.internal.terrain.BuryTerrainAdapter;
import com.example.structuregen.internal.terrain.CarveAboveTerrainAdapter;
import com.example.structuregen.internal.terrain.FillBelowTerrainAdapter;
import com.example.structuregen.internal.terrain.NoneTerrainAdapter;
import com.example.structuregen.internal.terrain.SmoothTransitionTerrainAdapter;
import com.example.structuregen.internal.world.StructureWorldState;
import com.example.structuregen.network.ModNetworking;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * Entry point pro Structure Generator mod (Mod A).
 *
 * <h2>Event bus architektura</h2>
 * <ul>
 *   <li>{@link #MOD_EVENT_BUS} — mod-specific bus pro lifecycle eventy
 *       ({@code FMLCommonSetupEvent}, registry eventy, …).</li>
 *   <li>{@link #FORGE_EVENT_BUS} — globální Forge bus pro in-game eventy
 *       (server start, entity eventy, …). Všechny veřejné Structure Generator
 *       eventy jsou fired na tomto busu.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>Obě bus reference jsou effectively final po konstrukci modu —
 * bezpečné číst z libovolného threadu.
 */
@Mod(StructureGenMod.MOD_ID)
public final class StructureGenMod {

    /** Unikátní mod identifikátor — musí odpovídat {@code modId} v {@code mods.toml}. */
    public static final String MOD_ID = "structuregen";

    /**
     * Sdílený logger pro všechny třídy tohoto modu.
     * Používej {@code LOGGER.debug()} pro verbose interní logy.
     */
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    // ---- Event bus reference ------------------------------------------------

    /**
     * Mod-specific event bus poskytnutý FML.
     * Registruj lifecycle eventy ({@code FMLCommonSetupEvent},
     * {@code RegisterCapabilitiesEvent}, registry {@code RegisterEvent}, …)
     * na tento bus.
     */
    public static IEventBus MOD_EVENT_BUS;

    /**
     * Globální Forge event bus ({@code MinecraftForge.EVENT_BUS}).
     * Structure Generator fires všechny veřejné eventy na tento bus —
     * třetí strany potřebují pouze jeden subscription point.
     */
    public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    // ---- Construction -------------------------------------------------------

    /**
     * Voláno jednou FML při načítání modu.
     * Registruje lifecycle listenery a inicializuje subsystémy.
     */
    public StructureGenMod() {
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        // Registrace lifecycle listeneru na mod busu
        MOD_EVENT_BUS.addListener(this::onCommonSetup);

        // Registrace in-game event listenerů na Forge busu
        FORGE_EVENT_BUS.addListener(this::onServerStarted);

        // Inicializace network channelu — musí proběhnout před FMLCommonSetupEvent
        ModNetworking.init();

        LOGGER.debug("Structure Generator mod constructed, awaiting FML lifecycle events.");
    }

    // ---- Lifecycle ----------------------------------------------------------

    /**
     * {@code FMLCommonSetupEvent} handler — běží na parallel thread poolu.
     *
     * <p>Veškerá práce dotýkající se Forge registrů musí být obalena
     * {@code event.enqueueWork()} pro garantované spuštění na main
     * loading threadu.
     *
     * <p>Pořadí operací:
     * <ol>
     *   <li>Validace všech registrovaných {@link StructureDefinition} (A-3-3)</li>
     *   <li>Registrace terrain adapterů (A-6-1)</li>
     * </ol>
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.debug("FMLCommonSetupEvent enqueueWork: running structure registry validation.");

            // ---- A-3-3: Validace registrovaných StructureDefinition ---------
            for (StructureDefinition def : StructureGeneratorAPI.getAllDefinitions()) {
                try {
                    RegistrationValidator.validate(def);
                    LOGGER.debug(
                        "StructureGeneratorAPI: validation passed for '{}'.", def.getId()
                    );
                } catch (IllegalStateException e) {
                    // Validace selhala — zaloguj ale necrashni server.
                    // Nevalidní definice nebude generována.
                    LOGGER.error(
                        "StructureGeneratorAPI: validation FAILED for '{}': {}",
                        def.getId(), e.getMessage()
                    );
                }
            }

            LOGGER.debug(
                "StructureGeneratorAPI: setup complete. {} structure(s) registered.",
                StructureGeneratorAPI.getRegistrySize()
            );

            // ---- A-6-1: Terrain adapter registrace --------------------------
            TerrainAdapterRegistry registry = TerrainAdapterRegistry.getInstance();
            registry.register(TerrainAdaptationMode.NONE,              NoneTerrainAdapter.INSTANCE);
            registry.register(TerrainAdaptationMode.FILL_BELOW,        FillBelowTerrainAdapter.INSTANCE);
            registry.register(TerrainAdaptationMode.CARVE_ABOVE,       CarveAboveTerrainAdapter.INSTANCE);
            registry.register(TerrainAdaptationMode.SMOOTH_TRANSITION, SmoothTransitionTerrainAdapter.INSTANCE);
            registry.register(TerrainAdaptationMode.BURY,              BuryTerrainAdapter.INSTANCE);
            registry.register(TerrainAdaptationMode.CUSTOM,            NoneTerrainAdapter.INSTANCE);
            LOGGER.debug("TerrainAdapterRegistry: all built-in adapters registered.");

            // A-7-2: ServerAboutToStartEvent handler zajistí datapack injekci —
            // server registry není dostupná v setup fázi.
        });
    }

    /**
     * {@code ServerStartedEvent} handler — voláno po prvním tiku serveru.
     *
     * <p>Provede {@link SpawnPopulator} recovery pro všechny instance
     * s {@code pendingPopulation = true} v každé dimenzi (A-5-16).
     *
     * <p>Vývojáři třetích stran kteří chtějí garantované generování při
     * startu světa musí použít {@code TickEvent.ServerTickEvent} s one-shot
     * flag — nikdy {@code WorldEvent.Load} (viz API Javadoc {@code generateAt()}).
     */
    private void onServerStarted(final ServerStartedEvent event) {
        event.getServer().getAllLevels().forEach(level -> {
            StructureWorldState state = StructureWorldState.get(level);
            var pending = state.getPendingPopulations();

            if (!pending.isEmpty()) {
                LOGGER.info(
                    "SpawnPopulator recovery: found {} pending instance(s) in dimension '{}'.",
                    pending.size(), level.dimension().location()
                );
            }

            for (StructureWorldState.PendingEntry entry : pending) {
                StructureGeneratorAPI.getDefinition(entry.structureId()).ifPresent(def -> {
                    SpawnPopulator populator = def.getPopulator();

                    if (populator == null) {
                        // Žádný populator — jen smaž pending flag
                        state.setPendingPopulation(
                            entry.structureId(), entry.chunkPos(), false
                        );
                        return;
                    }

                    try {
                        // Rekonstruuj minimální StructureInstance pro recovery.
                        // roomPositions a metadata nejsou persistovány — populator
                        // musí být schopen pracovat s prázdnou mapou (dokumentováno
                        // v SpawnPopulator Javadoc).
                        StructureInstance recoveryInstance = new StructureInstance(
                            entry.structureId(),
                            entry.seed(),
                            new HashMap<>(),
                            new HashMap<>(),
                            entry.chunkPos()
                        );

                        populator.populate(recoveryInstance, level);
                        state.setPendingPopulation(
                            entry.structureId(), entry.chunkPos(), false
                        );

                        LOGGER.debug(
                            "SpawnPopulator recovery: populated '{}' at {}.",
                            entry.structureId(), entry.chunkPos()
                        );

                    } catch (Throwable t) {
                        LOGGER.error(
                            "SpawnPopulator recovery: populate() threw for '{}' at {}: {}",
                            entry.structureId(), entry.chunkPos(), t.getMessage(), t
                        );
                        // Pending flag zůstane true — recovery bude opakována
                        // při příštím startu serveru.
                    }
                });
            }
        });
    }
}