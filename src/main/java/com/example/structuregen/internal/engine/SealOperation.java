package com.example.structuregen.internal.engine;

import com.example.structuregen.StructureGenMod;
import com.example.structuregen.api.ConnectionPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Ultimate fallback operace pro nezapečetěné {@link ConnectionPoint} objekty.
 *
 * <p>Pokud ani terminator neprojde collision check (extrémně omezený prostor),
 * engine umístí jeden {@code sealBlock} na koordináty ConnectionPointu.
 * Garantuje že žádný ConnectionPoint nikdy nezůstane fyzicky otevřený.
 *
 * <p>Loguje varování na {@code WARN} level s pozicí a structureId.
 */
final class SealOperation {

    /**
     * Zapečetí ConnectionPoint jedním blokem do fronty umístění.
     *
     * @param queue       placement fronta; nesmí být {@code null}
     * @param cp          connection point k zapečetění; nesmí být {@code null}
     * @param origin      absolutní origin místnosti; nesmí být {@code null}
     * @param sealBlock   block použitý pro zapečetění; nesmí být {@code null}
     * @param dimension   dimenze umístění
     * @param structureId ID struktury pro log zprávu
     */
    static void apply(BlockPlacementQueue queue,
                       ConnectionPoint cp,
                       BlockPos origin,
                       Block sealBlock,
                       ResourceKey<Level> dimension,
                       ResourceLocation structureId) {

        BlockPos sealPos = origin.offset(cp.getRelativeOffset());

        StructureGenMod.LOGGER.warn(
            "SealOperation: sealing ConnectionPoint at {} (structure='{}', "
            + "typeTag='{}', direction={}). "
            + "Neither terminator nor compatible template fit — using sealBlock '{}'.",
            sealPos, structureId, cp.getTypeTag(), cp.getDirection(),
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(sealBlock)
        );

        queue.enqueue(DeferredPlacement.of(
            new net.minecraft.world.level.ChunkPos(sealPos),
            dimension,
            sealPos,
            sealBlock.defaultBlockState()
        ));
    }

    private SealOperation() {}
}