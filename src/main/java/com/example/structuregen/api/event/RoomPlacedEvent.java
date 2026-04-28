package com.example.structuregen.api.event;

import com.example.structuregen.api.RoomTemplate;
import com.example.structuregen.api.StructureInstance;
import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

/**
 * Fired po úspěšném umístění každé místnosti v průběhu assembly.
 *
 * <p>V době firing je místnost fyzicky umístěna (bloky jsou v deferred
 * placement frontě nebo již v chunku). Payload {@link #partialInstance}
 * je partial snapshot — obsahuje pouze místnosti umístěné dosud, nikoli
 * finální kompletní strukturu.
 *
 * <p>Event není cancellable — místnost již byla umístěna. Pro ovlivnění
 * assembly před začátkem použij {@link StructureGenerationStartEvent}.
 *
 * <h2>Výkonnostní poznámka</h2>
 * <p>Tento event je fired pro každou místnost — v komplexu s 50 místnostmi
 * to je 50 event firings per assembly. Drž handler logiku co nejlehčí.
 */
public final class RoomPlacedEvent extends Event {

    private final RoomTemplate template;
    private final BlockPos origin;
    private final StructureInstance partialInstance;

    /**
     * @param template        umístěný template; nikdy {@code null}
     * @param origin          absolutní origin pozice; nikdy {@code null}
     * @param partialInstance partial snapshot struktury v tomto momentě; nikdy {@code null}
     */
    public RoomPlacedEvent(RoomTemplate template, BlockPos origin, StructureInstance partialInstance) {
        this.template        = Objects.requireNonNull(template,        "template must not be null");
        this.origin          = Objects.requireNonNull(origin,          "origin must not be null");
        this.partialInstance = Objects.requireNonNull(partialInstance, "partialInstance must not be null");
    }

    /** @return umístěný template; nikdy {@code null} */
    public RoomTemplate getTemplate() { return template; }

    /** @return absolutní origin pozice; nikdy {@code null} */
    public BlockPos getOrigin() { return origin; }

    /**
     * @return partial snapshot — obsahuje místnosti umístěné dosud;
     *         nikdy {@code null}
     */
    public StructureInstance getPartialInstance() { return partialInstance; }
}