package com.example.structuregen.api.event;

import com.example.structuregen.api.StructureInstance;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import java.util.Objects;

/**
 * Fired po dokončení assembly, před terrain adaptation a PostProcessorChain.
 *
 * <h2>Cancellable — veto semantika</h2>
 * <p>Cancel tohoto eventu <strong>nezapíše</strong> permanent failed marker —
 * místo toho engine výsledek zahodí a provede retry pokus (pokud zbývají
 * pokusy dle {@link com.example.structuregen.api.GenerationRules#getMaxRetriesPerChunkPos()}).
 * Tím má třetí strana možnost ovlivnit výsledek bez přímé mutace interního stavu.
 *
 * <h2>Read-only instance</h2>
 * <p>Payload {@link StructureInstance} je read-only draft — přímá mutace
 * umístěných místností není povolena. Pro ovlivnění výsledku použij
 * {@link StructureGenerationStartEvent#setModifiedRules(com.example.structuregen.api.GenerationRules)}.
 */
@Cancelable
public final class StructureAssemblyCompleteEvent extends Event {

    private final StructureInstance draft;

    /**
     * @param draft read-only draft instance; nikdy {@code null}
     */
    public StructureAssemblyCompleteEvent(StructureInstance draft) {
        this.draft = Objects.requireNonNull(draft, "draft must not be null");
    }

    /**
     * @return read-only draft {@link StructureInstance} — nikdy nemodifikuj přímo;
     *         nikdy {@code null}
     */
    public StructureInstance getDraft() { return draft; }
}