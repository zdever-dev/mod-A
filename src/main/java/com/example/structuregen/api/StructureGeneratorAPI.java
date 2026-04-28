package com.example.structuregen.api;

import com.example.structuregen.StructureGenMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hlavní entry point pro registraci struktur v Structure Generator modu.
 *
 * <p>Toto je jediná třída kterou vývojář třetí strany musí znát pro
 * registraci vlastních struktur. Veškerá ostatní funkcionalita je dostupná
 * přes ostatní API třídy v tomto balíku.
 *
 * <h2>Použití</h2>
 * <pre>{@code
 * // V FMLCommonSetupEvent handleru:
 * StructureGeneratorAPI.register(
 *     StructureDefinition.builder()
 *         .id(new ResourceLocation("mymod", "my_structure"))
 *         .addTemplates(new MyRoomTemplate())
 *         .rules(GenerationRules.builder().minDepth(10).build())
 *         .uniqueRule(UniqueRule.once())
 *         .build()
 * );
 * }</pre>
 *
 * <h2>Timing</h2>
 * <p>Registrace musí proběhnout v {@code FMLCommonSetupEvent} nebo dříve.
 * Registrace po {@code FMLCommonSetupEvent} není podporována a výsledek
 * je nedefinovaný.
 *
 * <h2>Thread safety</h2>
 * <p>Interní registry je {@link ConcurrentHashMap} — čtení je bezpečné
 * z libovolného threadu. Zápis (registrace) by měl probíhat výhradně
 * v {@code FMLCommonSetupEvent} fázi kdy FML garantuje single-threaded
 * přístup k mod setup logice (přes {@code enqueueWork()}).
 */
public final class StructureGeneratorAPI {

    // ---- Internal registry ---------------------------------------------------

    /**
     * Interní mapa {@code ResourceLocation → StructureDefinition}.
     *
     * <p>{@link ConcurrentHashMap} pro bezpečné čtení z worldgen worker
     * threadů po dokončení setup fáze. Zápis probíhá výhradně v setup fázi.
     */
    private static final Map<ResourceLocation, StructureDefinition> REGISTRY =
        new ConcurrentHashMap<>();

    // ---- Public API ----------------------------------------------------------

    /**
     * Zaregistruje {@link StructureDefinition} do runtime registru.
     *
     * <p>Tato metoda by měla být volána výhradně z {@code FMLCommonSetupEvent}
     * handleru (přes {@code event.enqueueWork()} pro thread safety).
     *
     * <p><strong>Validace při registraci</strong> (A-3-4, A-3-5) probíhá
     * v {@code FMLCommonSetupEvent} po dokončení všech registrací — ne zde.
     * Zde je prováděna pouze základní kontrola duplicitního ID.
     *
     * @param definition definice struktury k registraci; nesmí být {@code null}
     * @throws IllegalArgumentException pokud je {@code definition.getId()} již registrováno
     */
    public static void register(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException(
                "StructureGeneratorAPI.register(): definition must not be null"
            );
        }

        ResourceLocation id = definition.getId();

        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException(
                "StructureGeneratorAPI.register(): duplicate structure ID '" + id + "'. "
                + "Each StructureDefinition must have a unique ResourceLocation ID. "
                + "If you are registering from multiple mods, ensure your modid prefix is unique."
            );
        }

        REGISTRY.put(id, definition);
        StructureGenMod.LOGGER.debug(
            "StructureGeneratorAPI: registered structure '{}'.", id
        );
    }

    /**
     * Vrátí {@link StructureDefinition} pro dané ID.
     *
     * <p>Bezpečné volat z libovolného threadu po dokončení setup fáze.
     *
     * @param id ID struktury; nesmí být {@code null}
     * @return {@link Optional} s definicí, nebo prázdný pokud ID není registrováno
     */
    public static Optional<StructureDefinition> getDefinition(ResourceLocation id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(id));
    }

    /**
     * Vrátí immutable pohled na všechny registrované definice.
     *
     * <p>Bezpečné volat z libovolného threadu po dokončení setup fáze.
     *
     * @return unmodifiable kolekce všech {@link StructureDefinition}; nikdy {@code null}
     */
    public static Collection<StructureDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * Vrátí počet registrovaných struktur.
     * Primárně pro debug a testování.
     *
     * @return počet registrací
     */
    public static int getRegistrySize() {
        return REGISTRY.size();
    }

    // ---- Package-private (pro interní použití enginu) ------------------------

    /**
     * Interní přístup k raw mapě pro validační logiku v A-3-3.
     * Není součástí veřejného API — nevolat z třetích stran.
     *
     * @return unmodifiable mapa; nikdy {@code null}
     */
    static Map<ResourceLocation, StructureDefinition> getRegistry() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    // ---- Utility -------------------------------------------------------------

    /** Utility třída — žádná instance. */
    private StructureGeneratorAPI() {
        throw new UnsupportedOperationException("StructureGeneratorAPI is a utility class");
    }
}