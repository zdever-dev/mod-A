package com.example.structuregen.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

/**
 * Fired při admin resetu struktury přes {@code /sgdebug reset <structure_id>}.
 *
 * <p><strong>DŮLEŽITÉ:</strong> tento event signalizuje pouze smazání záznamu
 * v {@code StructureWorldState} — fyzické bloky ve světě <strong>nejsou</strong>
 * odstraněny. Pokud chceš fyzicky odstranit bloky, musíš to implementovat
 * ve svém event listeneru.
 *
 * <p>Event není cancellable — reset již byl proveden.
 */
public final class StructureRemovedEvent extends Event {

    private final ResourceLocation structureId;
    private final ChunkPos chunkPos;

    /**
     * @param structureId ID resetované struktury; nikdy {@code null}
     * @param chunkPos    chunk ve kterém byl origin struktury; nikdy {@code null}
     */
    public StructureRemovedEvent(ResourceLocation structureId, ChunkPos chunkPos) {
        this.structureId = Objects.requireNonNull(structureId, "structureId must not be null");
        this.chunkPos    = Objects.requireNonNull(chunkPos,    "chunkPos must not be null");
    }

    /**
     * @return ID resetované struktury; nikdy {@code null}
     */
    public ResourceLocation getStructureId() { return structureId; }

    /**
     * @return chunk origin struktury; nikdy {@code null}
     * @apiNote Fyzické bloky v tomto chunku NEJSOU odstraněny —
     *          pouze world state záznam byl smazán.
     */
    public ChunkPos getChunkPos() { return chunkPos; }
}