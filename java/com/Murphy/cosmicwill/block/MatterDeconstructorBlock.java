package com.Murphy.cosmicwill.block;

import com.Murphy.cosmicwill.blockentity.MatterDeconstructorBlockEntity;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * 物质解构器：
 * 1 个普通物品 = 1 MU。
 * 收到红石信号时暂停。
 */
public final class MatterDeconstructorBlock
        extends BaseEntityBlock {

    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    public static final BooleanProperty LIT =
            BlockStateProperties.LIT;

    public MatterDeconstructorBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, false)
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
                .setValue(LIT, false);
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
        return new MatterDeconstructorBlockEntity(pos, state);
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
                CWBlockEntities.MATTER_DECONSTRUCTOR.get(),
                MatterDeconstructorBlockEntity::serverTick
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
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity
                    instanceof MatterDeconstructorBlockEntity machine) {
                NetworkHooks.openScreen(
                        serverPlayer,
                        machine,
                        pos
                );
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, LIT);
    }

    @Override
    public boolean hasAnalogOutputSignal(
            BlockState state
    ) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(
            BlockState state,
            Level level,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity
                instanceof MatterDeconstructorBlockEntity machine)) {
            return 0;
        }

        long capacity = machine.getCapacity();
        if (capacity <= 0L || machine.getMatter() <= 0L) {
            return 0;
        }

        return Math.min(
                15,
                1 + (int) (
                        14L
                                * machine.getMatter()
                                / capacity
                )
        );
    }

    /**
     * 将 BlockEntity 状态写进掉落方块自身。
     * 不额外弹出输入槽物品，避免复制。
     */
    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder builder
    ) {
        ItemStack result = new ItemStack(this);

        BlockEntity blockEntity =
                builder.getOptionalParameter(
                        LootContextParams.BLOCK_ENTITY
                );

        if (blockEntity
                instanceof MatterDeconstructorBlockEntity machine) {
            CompoundTag stateTag =
                    machine.saveWithoutMetadata();

            result.addTagElement(
                    "BlockEntityTag",
                    stateTag
            );
        }

        return Collections.singletonList(result);
    }

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!state.getValue(LIT)) {
            return;
        }

        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.45D;
        double z = pos.getZ() + 0.5D;

        double frontOffset = 0.52D;
        double sideOffset =
                (random.nextDouble() - 0.5D) * 0.45D;

        double particleX = x
                + facing.getStepX() * frontOffset
                + facing.getStepZ() * sideOffset;

        double particleZ = z
                + facing.getStepZ() * frontOffset
                + facing.getStepX() * sideOffset;

        if (random.nextDouble() < 0.65D) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    particleX,
                    y + random.nextDouble() * 0.18D,
                    particleZ,
                    0.0D,
                    0.01D,
                    0.0D
            );
        }

        if (random.nextDouble() < 0.30D) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    particleX,
                    y,
                    particleZ,
                    0.0D,
                    0.015D,
                    0.0D
            );
        }
    }
}
