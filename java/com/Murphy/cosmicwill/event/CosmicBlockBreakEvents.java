package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.items.NullifierSwordItem;
import com.Murphy.cosmicwill.items.SealedChargedEnergyBladeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CosmicBlockBreakEvents {

    private CosmicBlockBreakEvents() {
    }

    /**
     * 使用服务端收到的 START_DESTROY_BLOCK 事件处理特殊采掘。
     *
     * receiveCanceled=true：
     * 其他模组若仅通过 Forge 左键事件限制工具等级，归零者仍可进入
     * 自己的强制拆除流程。
     */
    @SubscribeEvent(
            priority = EventPriority.HIGHEST,
            receiveCanceled = true
    )
    public static void onLeftClickBlock(
            PlayerInteractEvent.LeftClickBlock event
    ) {
        if (event.getAction()
                != PlayerInteractEvent.LeftClickBlock
                .Action.START) {
            return;
        }

        if (event.getLevel().isClientSide) {
            return;
        }

        if (!(event.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return;
        }

        if (held.getItem()
                instanceof SealedChargedEnergyBladeItem) {
            if (!state.is(Blocks.BEDROCK)) {
                return;
            }

            /*
             * 覆盖此前其他事件的取消结果，然后立即完成基岩拆除。
             */
            event.setCanceled(false);

            breakBedrockWithDrop(
                    level,
                    pos,
                    player
            );

            event.setCanceled(true);
            return;
        }

        if (!(held.getItem()
                instanceof NullifierSwordItem)) {
            return;
        }

        /*
         * 归零者不依赖正常工具等级、硬度或挖掘进度。
         */
        event.setCanceled(false);

        if (NullifierSwordItem.getMode(held)
                == NullifierSwordItem.MODE_DESTRUCTION) {
            destroyWithDrops(
                    level,
                    pos,
                    player,
                    held
            );
        } else {
            eraseWithoutDrops(
                    level,
                    pos,
                    player
            );
        }

        event.setCanceled(true);
    }

    /**
     * 对会查询 Forge BreakSpeed 的模组返回极大采掘速度。
     */
    @SubscribeEvent(
            priority = EventPriority.HIGHEST,
            receiveCanceled = true
    )
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        if (!(event.getEntity()
                .getMainHandItem()
                .getItem()
                instanceof NullifierSwordItem)) {
            return;
        }

        event.setCanceled(false);
        event.setNewSpeed(Float.MAX_VALUE);
    }

    /**
     * 对标准 Forge 收获等级检查始终返回可收获。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHarvestCheck(
            PlayerEvent.HarvestCheck event
    ) {
        if (event.getEntity()
                .getMainHandItem()
                .getItem()
                instanceof NullifierSwordItem) {
            event.setCanHarvest(true);
        }
    }

    private static void breakBedrockWithDrop(
            ServerLevel level,
            BlockPos pos,
            Player player
    ) {
        BlockState oldState =
                level.getBlockState(pos);

        level.removeBlockEntity(pos);

        level.setBlock(
                pos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
                        | Block.UPDATE_SUPPRESS_DROPS
        );

        Block.popResource(
                level,
                pos,
                new ItemStack(Blocks.BEDROCK)
        );

        playBreakEffect(
                level,
                pos,
                oldState,
                player
        );
    }

    /**
     * 毁灭模式：绕过硬度和工具等级，但保留正常掉落。
     */
    private static void destroyWithDrops(
            ServerLevel level,
            BlockPos pos,
            Player player,
            ItemStack held
    ) {
        BlockState oldState =
                level.getBlockState(pos);

        if (oldState.is(Blocks.BEDROCK)) {
            breakBedrockWithDrop(
                    level,
                    pos,
                    player
            );
            return;
        }

        boolean destroyed =
                level.destroyBlock(
                        pos,
                        true,
                        player
                );

        if (!destroyed
                || !level.getBlockState(pos).isAir()) {
            /*
             * 对拒绝 Level#destroyBlock 的模组方块执行最终后备：
             * 先按当前方块战利品表结算，再强制清除方块与方块实体。
             */
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            Block.dropResources(
                    oldState,
                    level,
                    pos,
                    blockEntity,
                    player,
                    held
            );

            level.removeBlockEntity(pos);

            level.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
                            | Block.UPDATE_SUPPRESS_DROPS
            );
        }

        playBreakEffect(
                level,
                pos,
                oldState,
                player
        );
    }

    /**
     * 归零模式：方块本体、方块实体和内部库存均无掉落消失。
     */
    private static void eraseWithoutDrops(
            ServerLevel level,
            BlockPos pos,
            Player player
    ) {
        BlockState oldState =
                level.getBlockState(pos);

        level.removeBlockEntity(pos);

        level.setBlock(
                pos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
                        | Block.UPDATE_SUPPRESS_DROPS
        );

        playBreakEffect(
                level,
                pos,
                oldState,
                player
        );
    }

    private static void playBreakEffect(
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            Player player
    ) {
        level.levelEvent(
                player,
                2001,
                pos,
                Block.getId(oldState)
        );
    }
}
