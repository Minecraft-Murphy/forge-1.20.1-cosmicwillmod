package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.blockentity.MatterCompressorBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 捕获物质压缩器的潜行右键交互。
 *
 * <p>原版在玩家潜行并手持物品时，会优先执行物品自己的 useOn，
 * 并跳过方块的 Block#use。因此仅在 MatterCompressorBlock#use
 * 中判断 Shift 无法接收到钻石、方块等大多数手持物品。</p>
 */
@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MatterCompressorInteractionEvents {

    private MatterCompressorInteractionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        /*
         * 只处理主手，避免同一次右键又由副手重复触发。
         */
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!event.getEntity().isSecondaryUseActive()) {
            return;
        }

        Level level = event.getLevel();
        BlockEntity blockEntity =
                level.getBlockEntity(event.getPos());

        if (!(blockEntity
                instanceof MatterCompressorBlockEntity machine)) {
            return;
        }

        if (!level.isClientSide) {
            machine.handleShiftInteraction(
                    event.getEntity(),
                    event.getHand()
            );
        }

        /*
         * 阻止手中的方块被放置、桶被使用等原版物品行为。
         */
        event.setCanceled(true);
        event.setCancellationResult(
                InteractionResult.sidedSuccess(
                        level.isClientSide
                )
        );
    }
}
