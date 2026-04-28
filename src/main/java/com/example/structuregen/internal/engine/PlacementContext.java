package com.example.structuregen.internal.engine;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Thread-local kontext předávající {@link BlockPlacementQueue} a dimenzi
 * do {@link com.example.structuregen.api.RoomTemplate#doPlace}.
 *
 * <p>Vývojáři třetích stran volají {@link #enqueue} z {@code doPlace()}
 * místo přímého {@code level.setBlock()} pro zápis do deferred fronty.
 */
public final class PlacementContext {

    private static final ThreadLocal<BlockPlacementQueue> QUEUE    = new ThreadLocal<>();
    private static final ThreadLocal<ResourceKey<Level>>  DIM_KEY  = new ThreadLocal<>();

    static void set(BlockPlacementQueue queue, ResourceKey<Level> dimension) {
        QUEUE.set(queue);
        DIM_KEY.set(dimension);
    }

    static void clear() {
        QUEUE.remove();
        DIM_KEY.remove();
    }

    /**
     * Enqueue-uje deferred block placement z kontextu {@code doPlace()}.
     *
     * @param placement deferred placement; nesmí být {@code null}
     * @throws IllegalStateException pokud voláno mimo assembly kontext
     */
    public static void enqueue(DeferredPlacement placement) {
        BlockPlacementQueue queue = QUEUE.get();
        if (queue == null) {
            throw new IllegalStateException(
                "PlacementContext.enqueue() called outside of assembly context. "
                + "Only call from RoomTemplate.doPlace()."
            );
        }
        queue.enqueue(placement);
    }

    /**
     * @return aktuální dimenze key; {@code null} mimo assembly kontext
     */
    public static ResourceKey<Level> getDimension() {
        return DIM_KEY.get();
    }

    private PlacementContext() {}
}