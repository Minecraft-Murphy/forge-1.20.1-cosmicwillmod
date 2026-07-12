package com.Murphy.cosmicwill.block;

import com.Murphy.cosmicwill.blockentity.CompressedFurnaceBlockEntity;
import com.Murphy.cosmicwill.config.CWServerConfig;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class CompressedFurnaceBlock
        extends AbstractFurnaceBlock {

    public CompressedFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new CompressedFurnaceBlockEntity(
                pos,
                state
        );
    }

    @Override
    protected void openContainer(
            Level level,
            BlockPos pos,
            Player player
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (blockEntity
                instanceof MenuProvider provider) {
            player.openMenu(provider);

            player.awardStat(
                    Stats.INTERACT_WITH_FURNACE
            );
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                CWBlockEntities.COMPRESSED_FURNACE.get(),
                (tickLevel, pos, tickState, blockEntity) -> {
                    int configuredMultiplier =
                            CWServerConfig
                                    .COMPRESSED_FURNACE_SPEED_MULTIPLIER
                                    .get();

                    /*
                     * 恒星残骸的高炉配方为 2400 Tick。
                     * 这里固定每世界 Tick 运行两次，
                     * 因而压缩熔炉比普通高炉快一倍。
                     */
                    int multiplier =
                            blockEntity
                                    .isStellarRemnantInput()
                                    ? 2
                                    : configuredMultiplier;

                    blockEntity.applyStellarHeat(
                            multiplier
                    );

                    for (int i = 0;
                         i < multiplier;
                         i++) {
                        BlockState currentState =
                                tickLevel.getBlockState(pos);

                        AbstractFurnaceBlockEntity.serverTick(
                                tickLevel,
                                pos,
                                currentState,
                                blockEntity
                        );
                    }
                }
        );
    }
}
