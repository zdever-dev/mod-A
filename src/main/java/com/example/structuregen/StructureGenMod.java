package com.example.structuregen;

import com.example.structuregen.network.ModNetworking;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for the Structure Generator mod (Mod A).
 *
 * <p>Architecture overview:
 * <ul>
 *   <li>{@link #MOD_EVENT_BUS} — the mod-specific bus used for lifecycle events
 *       ({@code FMLCommonSetupEvent}, registry events, …). All mod-internal
 *       registrations subscribe here.</li>
 *   <li>{@link #FORGE_EVENT_BUS} — the global Forge bus used for game-world
 *       events (server start, entity events, …). External consumers that react
 *       to Structure Generator events also subscribe here.</li>
 * </ul>
 *
 * <p>Thread safety: both bus references are effectively final after mod
 * construction and are safe to read from any thread.
 */
@Mod(StructureGenMod.MOD_ID)
public final class StructureGenMod {

    /** The unique mod identifier — must match {@code modId} in {@code mods.toml}. */
    public static final String MOD_ID = "structuregen";

    /**
     * Shared logger for all classes inside this mod.
     * Use {@code LOGGER.debug()} for verbose internals so servers can suppress
     * them with default Forge logging settings.
     */
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    // ---- Event bus references ------------------------------------------------
    //
    // Stored as static fields so inner subsystems (registries, networking, …)
    // can reach them without needing a reference to the mod instance itself.
    // Both fields are set during mod construction and are never reassigned.

    /**
     * The mod-specific event bus provided by FML.
     * Subscribe lifecycle events ({@code FMLCommonSetupEvent},
     * {@code RegisterCapabilitiesEvent}, registry {@code RegisterEvent}, …)
     * on this bus.
     */
    public static IEventBus MOD_EVENT_BUS;

    /**
     * The global Forge event bus ({@code MinecraftForge.EVENT_BUS}).
     * Subscribe in-game world events (server start, entity join, …) here.
     * Structure Generator fires all its public events on this bus so that
     * third-party mods only need a single bus subscription point.
     */
    public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    // ---- Construction --------------------------------------------------------

    /**
     * Called once by FML when the mod is loaded.
     * Registers lifecycle listeners and initialises all subsystems.
     */
    public StructureGenMod() {
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        // Register lifecycle listeners on the mod bus
        MOD_EVENT_BUS.addListener(this::onCommonSetup);

        // Initialise the network channel early — channel registration must
        // happen before FMLCommonSetupEvent (Forge requirement).
        ModNetworking.init();

        LOGGER.debug("Structure Generator mod constructed, awaiting FML lifecycle events.");
    }

    // ---- Lifecycle -----------------------------------------------------------

    /**
     * {@code FMLCommonSetupEvent} handler — runs on a parallel thread pool.
     * All Forge-registry-touching work must be wrapped in
     * {@code event.enqueueWork()} to guarantee execution on the main loading
     * thread.
     *
     * <p>Future steps (A-3, A-7) will add their setup work here via
     * {@code event.enqueueWork()}.
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.debug("FMLCommonSetupEvent enqueueWork running on main thread.");
            // A-3-3: StructureGeneratorAPI registry validation will be added here.
            // A-7-2: ServerAboutToStartEvent will handle the datapack injection.
        });
    }
}
