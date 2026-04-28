package com.example.structuregen.api.event;

import com.example.structuregen.api.StructureInstance;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

/**
 * Fired po kompletním dokončení generování — po assembly, terrain adaptation
 * i celém PostProcessorChain.
 *
 * <p>Toto je <strong>primární event pro třetí strany</strong>. V době firing
 * jsou všechny bloky fyzicky umístěny, terrain adaptation dokončena a
 * {@link SpawnPopulator} ještě nebyl zavolán (viz A-5-16).
 *
 * <p>Event <strong>není</strong> cancellable — struktura již existuje
 * ve světě.
 *
 * <p>Payload {@link StructureInstance} je finální a immutable.
 */
public final class StructureGeneratedEvent extends Event {

    private final StructureInstance instance;

    /**
     * @param instance finální {@link StructureInstance}; nikdy {@code null}
     */
    public StructureGeneratedEvent(StructureInstance instance) {
        this.instance = Objects.requireNonNull(instance, "instance must not be null");
    }

    /**
     * @return finální immutable instance struktury; nikdy {@code null}
     */
    public StructureInstance getInstance() { return instance; }
}