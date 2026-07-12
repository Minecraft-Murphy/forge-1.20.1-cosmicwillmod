package com.Murphy.cosmicwill.block;

import com.Murphy.cosmicwill.blockentity.MatterCompressorBlockEntity;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import com.Murphy.cosmicwill.singularity.SingularityCatastrophe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public final class MatterCompressorBlock
        extends BaseEntityBlock {

    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    public static final BooleanProperty WORKING =
            BooleanProperty.create("working");

    public static final BooleanProperty SINGULARITY =
            BooleanProperty.create("singularity");

    public MatterCompressorBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(WORKING, false)
                        .setValue(SINGULARITY, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context
                                .getHorizontalDirection()
                                .getOpposite()
                )
                .setValue(WORKING, false)
                .setValue(SINGULARITY, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new MatterCompressorBlockEntity(
                pos,
                state
        );
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
                CWBlockEntities.MATTER_COMPRESSOR.get(),
                MatterCompressorBlockEntity::serverTick
        );
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity
                instanceof MatterCompressorBlockEntity machine)) {
            return InteractionResult.PASS;
        }

        if (player.isSecondaryUseActive()
                || player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                machine.handleShiftInteraction(
                        player,
                        hand
                );
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        if (machine.hasInstalledSingularity()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.cosmicwill.matter_compressor.singularity_locked"
                        ),
                        true
                );
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    machine,
                    pos
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                WORKING,
                SINGULARITY
        );
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder builder
    ) {
        BlockEntity blockEntity =
                builder.getOptionalParameter(
                        LootContextParams.BLOCK_ENTITY
                );

        boolean singularityState =
                state.hasProperty(SINGULARITY)
                        && state.getValue(SINGULARITY);

        boolean singularityEntity =
                blockEntity
                        instanceof MatterCompressorBlockEntity machine
                        && machine.hasInstalledSingularity();

        if (singularityState || singularityEntity) {
            return Collections.emptyList();
        }

        ItemStack result = new ItemStack(this);

        if (blockEntity
                instanceof MatterCompressorBlockEntity machine) {
            CompoundTag stateTag =
                    machine.saveWithoutMetadata();

            result.addTagElement(
                    "BlockEntityTag",
                    stateTag
            );
        }

        return Collections.singletonList(result);
    }

    /**
     * 正常玩家挖掘仍会被事件拦截；
     * 如果其他模组绕过保护强制替换方块，则在真正移除时引爆。
     */
    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        boolean removed =
                !state.is(newState.getBlock());

        boolean armed =
                state.hasProperty(SINGULARITY)
                        && state.getValue(SINGULARITY);

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );

        if (removed
                && armed
                && level instanceof ServerLevel serverLevel) {
            SingularityCatastrophe.trigger(
                    serverLevel,
                    pos
            );
        }
    }
}
