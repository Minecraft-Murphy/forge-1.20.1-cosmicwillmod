package com.Murphy.cosmicwill.block;

import com.Murphy.cosmicwill.registry.CWBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 压缩活塞专属活塞头。
 */
public final class CompressedPistonHeadBlock
        extends PistonHeadBlock {

    public CompressedPistonHeadBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        Direction facing =
                state.getValue(FACING);

        BlockState behind =
                level.getBlockState(
                        pos.relative(
                                facing.getOpposite()
                        )
                );

        if (behind.is(
                CWBlocks.COMPRESSED_PISTON.get()
        )) {
            return behind.getValue(
                    PistonBaseBlock.FACING
            ) == facing
                    && behind.getValue(
                    PistonBaseBlock.EXTENDED
            );
        }

        return behind.is(Blocks.MOVING_PISTON)
                && behind.hasProperty(FACING)
                && behind.getValue(FACING) == facing;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        Direction facing =
                state.getValue(FACING);

        if (direction == facing.getOpposite()) {
            return canSurvive(
                    state,
                    level,
                    pos
            )
                    ? state
                    : Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );
    }
}
