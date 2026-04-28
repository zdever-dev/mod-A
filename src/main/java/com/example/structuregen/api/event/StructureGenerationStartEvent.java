package com.example.structuregen.api.event;

import com.example.structuregen.api.GenerationRules;
import com.example.structuregen.api.StructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

/**
 * Fired na {@code MinecraftForge.EVENT_BUS} těsně před spuštěním assembly
 * pro jednu strukturu.
 *
 * <h2>Cancellable</h2>
 * <p>Pokud je event zrušen ({@code event.setCanceled(true)}), assembly se
 * nespustí a engine zapíše permanent "failed" marker pro daný ChunkPos
 * a structureId. Marker lze smazat přes {@code /sgdebug reset <id>}.
 *
 * <h2>Mutable rules</h2>
 * <p>Listener může za běhu nahradit {@link GenerationRules} přes
 * {@link #setModifiedRules(GenerationRules)}. Engine použije modifikované
 * rules výhradně pro toto jedno generování — globální definice není dotčena.
 */
@Cancelable
public final class StructureGenerationStartEvent extends Event {

    private final StructureDefinition definition;
    private final BlockPos origin;
    private GenerationRules modifiedRules;

    /**
     * @param definition definice struktury která se bude generovat; nikdy {@code null}
     * @param origin     absolutní origin pozice assembly; nikdy {@code null}
     * @param rules      aktuální {@link GenerationRules} (může být přepsáno); nikdy {@code null}
     */
    public StructureGenerationStartEvent(StructureDefinition definition,
                                          BlockPos origin,
                                          GenerationRules rules) {
        this.definition    = Objects.requireNonNull(definition, "definition must not be null");
        this.origin        = Objects.requireNonNull(origin,     "origin must not be null");
        this.modifiedRules = Objects.requireNonNull(rules,      "rules must not be null");
    }

    /** @return definice struktury; nikdy {@code null} */
    public StructureDefinition getDefinition() { return definition; }

    /** @return absolutní origin pozice; nikdy {@code null} */
    public BlockPos getOrigin() { return origin; }

    /**
     * @return aktuálně platné {@link GenerationRules} (původní nebo modifikované);
     *         nikdy {@code null}
     */
    public GenerationRules getModifiedRules() { return modifiedRules; }

    /**
     * Nahradí {@link GenerationRules} pro toto jedno generování.
     * Globální {@link StructureDefinition} není dotčena.
     *
     * @param rules nové rules; nesmí být {@code null}
     */
    public void setModifiedRules(GenerationRules rules) {
        this.modifiedRules = Objects.requireNonNull(rules, "rules must not be null");
    }
}