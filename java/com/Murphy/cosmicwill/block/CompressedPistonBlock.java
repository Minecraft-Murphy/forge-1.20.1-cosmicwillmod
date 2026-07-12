package com.Murphy.cosmicwill.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 第一阶段强化活塞：
 * 保留原版普通活塞行为，并额外允许推动连续的黑曜石/哭泣黑曜石。
 *
 * 当前仅处理前方连续硬质块且末端存在空位的情况；
 * 方块实体及其他原版不可推动方块暂不处理。
 */
public final class CompressedPistonBlock
        extends PistonBaseBlock {

    private static final int PUSH_LIMIT = 12;

    public CompressedPistonBlock(Properties properties) {
        super(false, properties);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        prepareHardBlockLine(
                state,
                level,
                pos
        );

        super.neighborChanged(
                state,
                level,
                pos,
                neighborBlock,
                neighborPos,
                movedByPiston
        );
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        prepareHardBlockLine(
                state,
                level,
                pos
        );

        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );
    }

    private static void prepareHardBlockLine(
            BlockState pistonState,
            Level level,
            BlockPos pistonPos
    ) {
        if (level.isClientSide
                || pistonState.getValue(EXTENDED)
                || !isPowered(level, pistonPos)) {
            return;
        }

        Direction direction =
                pistonState.getValue(FACING);

        List<BlockPos> hardBlocks =
                new ArrayList<>();

        BlockPos cursor =
                pistonPos.relative(direction);

        for (int i = 0; i < PUSH_LIMIT; i++) {
            BlockState state =
                    level.getBlockState(cursor);

            if (!isExtraPushable(state)) {
                break;
            }

            if (state.hasBlockEntity()) {
                return;
            }

            hardBlocks.add(cursor.immutable());
            cursor = cursor.relative(direction);
        }

        if (hardBlocks.isEmpty()) {
            return;
        }

        BlockState destination =
                level.getBlockState(cursor);

        if (!destination.isAir()
                && !destination.canBeReplaced()) {
            return;
        }

        for (int i = hardBlocks.size() - 1;
             i >= 0;
             i--) {
            BlockPos sourcePos =
                    hardBlocks.get(i);

            BlockPos destinationPos =
                    sourcePos.relative(direction);

            BlockState movingState =
                    level.getBlockState(sourcePos);

            level.setBlock(
                    destinationPos,
                    movingState,
                    Block.UPDATE_ALL
                            | Block.UPDATE_MOVE_BY_PISTON
            );

            level.setBlock(
                    sourcePos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
                            | Block.UPDATE_MOVE_BY_PISTON
                            | Block.UPDATE_SUPPRESS_DROPS
            );
        }
    }

    private static boolean isPowered(
            Level level,
            BlockPos pos
    ) {
        return level.hasNeighborSignal(pos)
                || level.hasNeighborSignal(
                pos.above()
        );
    }

    private static boolean isExtraPushable(
            BlockState state
    ) {
        return state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN);
    }
}
