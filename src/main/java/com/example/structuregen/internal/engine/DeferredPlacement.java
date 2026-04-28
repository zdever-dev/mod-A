package com.example.structuregen.internal.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable záznam jednoho odloženého block placement.
 *
 * <p><strong>Nikdy</strong> neuchovává přímou referenci na {@code IChunkAccess}
 * nebo {@code ServerLevel} — pouze {@link ChunkPos} a {@link ResourceKey}.
 * Při dequeue engine fetchuje aktuální chunk objekt přes
 * {@code level.getChunk(chunkPos.x, chunkPos.z)} — funguje pro
 * {@code ProtoChunk} i {@code LevelChunk} bez stale reference problémů.
 */
record DeferredPlacement(
    ChunkPos chunkPos,
    ResourceKey<Level> dimension,
    BlockPos blockPos,
    BlockState state,
    @Nullable CompoundTag nbt
) {

    /**
     * Vytvoří placement bez NBT dat (pro běžné bloky bez BlockEntity).
     */
    static DeferredPlacement of(ChunkPos chunkPos, ResourceKey<Level> dimension,
                                 BlockPos blockPos, BlockState state) {
        return new DeferredPlacement(chunkPos, dimension, blockPos, state, null);
    }

    /**
     * Vytvoří placement s NBT daty (pro bloky s BlockEntity — truhly, apod.).
     */
    static DeferredPlacement withNbt(ChunkPos chunkPos, ResourceKey<Level> dimension,
                                      BlockPos blockPos, BlockState state, CompoundTag nbt) {
        return new DeferredPlacement(chunkPos, dimension, blockPos, state, nbt);
    }
}