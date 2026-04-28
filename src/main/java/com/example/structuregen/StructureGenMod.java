package com.example.structuregen;

import com.example.structuregen.api.StructureDefinition;
import com.example.structuregen.api.StructureGeneratorAPI;
import com.example.structuregen.internal.RegistrationValidator;
import com.example.structuregen.network.ModNetworking;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.debug("FMLCommonSetupEvent enqueueWork: running structure registry validation.");

            // Iterace přes všechny registrované StructureDefinition
            // a spuštění validace (A-3-4, A-3-5).
            // REGISTRY je v tuto chvíli plně naplněn registracemi
            // z FMLCommonSetupEvent handlerů třetích stran.
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

            // A-7-2: ServerAboutToStartEvent handler zajistí datapack injekci —
            // server registry není dostupná v setup fázi.
        });
    }
}